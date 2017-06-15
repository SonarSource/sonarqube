/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.measure.ws;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.ComponentTreeWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.server.measure.ws.ComponentTreeAction.LEAVES_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_PERIOD_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.WITH_MEASURES_ONLY_METRIC_SORT_FILTER;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.ADDITIONAL_PERIODS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_BASE_COMPONENT_KEY;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_PERIOD_SORT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_SORT_FILTER;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_STRATEGY;

public class ComponentTreeActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private I18nRule i18n = new I18nRule();
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private ComponentDbTester componentDb = new ComponentDbTester(db);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private WsActionTester ws = new WsActionTester(
    new ComponentTreeAction(
      new ComponentTreeDataLoader(dbClient, new ComponentFinder(dbClient, resourceTypes), userSession, resourceTypes),
      i18n, resourceTypes));

  @Before
  public void setUp() {
    userSession.logIn().setRoot();
    resourceTypes.setChildrenQualifiers(Qualifiers.MODULE, Qualifiers.FILE, Qualifiers.DIRECTORY);
    resourceTypes.setLeavesQualifiers(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
  }

  @Test
  public void json_example() {
    insertJsonExampleData();

    String response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-id")
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,periods")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("component_tree-example.json"));
  }

  @Test
  public void empty_response() {
    componentDb.insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getBaseComponent().getId()).isEqualTo("project-uuid");
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getMetrics().getMetricsList()).isEmpty();
    assertThat(response.getPeriods().getPeriodsList()).isEmpty();
  }

  @Test
  public void load_measures_and_periods() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    componentDb.insertComponent(projectDto);
    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(dbSession,
      newAnalysis(projectDto)
        .setPeriodDate(System.currentTimeMillis())
        .setPeriodMode("last_version")
        .setPeriodDate(System.currentTimeMillis()));
    userSession.anonymous().addProjectPermission(UserRole.USER, projectDto);
    ComponentDto directoryDto = newDirectory(projectDto, "directory-uuid", "path/to/directory").setName("directory-1");
    componentDb.insertComponent(directoryDto);
    ComponentDto file = newFileDto(directoryDto, null, "file-uuid").setName("file-1");
    componentDb.insertComponent(file);
    MetricDto ncloc = insertNclocMetric();
    MetricDto coverage = insertCoverageMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file, projectSnapshot).setValue(5.0d).setVariation(4.0d),
      newMeasureDto(coverage, file, projectSnapshot).setValue(15.5d),
      newMeasureDto(coverage, directoryDto, projectSnapshot).setValue(15.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc,coverage")
      .setParam(PARAM_ADDITIONAL_FIELDS, ADDITIONAL_PERIODS)
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList().get(0).getMeasuresList()).extracting("metric").containsOnly("coverage");
    // file measures
    List<WsMeasures.Measure> fileMeasures = response.getComponentsList().get(1).getMeasuresList();
    assertThat(fileMeasures).extracting("metric").containsOnly("ncloc", "coverage");
    assertThat(fileMeasures).extracting("value").containsOnly("5", "15.5");
    assertThat(response.getPeriods().getPeriodsList()).extracting("mode").containsOnly("last_version");
  }

  @Test
  public void load_measures_with_best_value() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(projectDto);
    userSession.anonymous().addProjectPermission(UserRole.USER, projectDto);
    ComponentDto directoryDto = newDirectory(projectDto, "directory-uuid", "path/to/directory").setName("directory-1");
    componentDb.insertComponent(directoryDto);
    ComponentDto file = newFileDto(directoryDto, null, "file-uuid").setName("file-1");
    componentDb.insertComponent(file);
    MetricDto coverage = insertCoverageMetric();
    dbClient.metricDao().insert(dbSession, MetricTesting.newMetricDto()
      .setKey("ncloc")
      .setValueType(INT.name())
      .setOptimizedBestValue(true)
      .setBestValue(100d)
      .setWorstValue(1000d));
    dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("new_violations")
      .setOptimizedBestValue(true)
      .setBestValue(1984.0d)
      .setValueType(INT.name()));
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(coverage, file, projectSnapshot).setValue(15.5d),
      newMeasureDto(coverage, directoryDto, projectSnapshot).setValue(42.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc,coverage,new_violations")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    // directory measures
    assertThat(response.getComponentsList().get(0).getMeasuresList()).extracting("metric").containsOnly("coverage");
    // file measures
    List<WsMeasures.Measure> fileMeasures = response.getComponentsList().get(1).getMeasuresList();
    assertThat(fileMeasures).extracting("metric").containsOnly("ncloc", "coverage", "new_violations");
    assertThat(fileMeasures).extracting("value").containsOnly("100", "15.5", "");

    List<Common.Metric> metrics = response.getMetrics().getMetricsList();
    assertThat(metrics).extracting("bestValue").contains("100", "");
    assertThat(metrics).extracting("worstValue").contains("1000");
  }

  @Test
  public void use_best_value_for_rating() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    userSession.anonymous().addProjectPermission(UserRole.USER, projectDto);
    componentDb.insertComponent(projectDto);
    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(projectDto)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));
    ComponentDto directoryDto = newDirectory(projectDto, "directory-uuid", "path/to/directory").setName("directory-1");
    componentDb.insertComponent(directoryDto);
    ComponentDto file = newFileDto(directoryDto, null, "file-uuid").setName("file-1");
    componentDb.insertComponent(file);
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey(NEW_SECURITY_RATING_KEY)
      .setOptimizedBestValue(true)
      .setBestValue(1d)
      .setValueType(RATING.name()));
    dbClient.measureDao().insert(dbSession, newMeasureDto(metric, directoryDto, projectSnapshot).setVariation(2d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, NEW_SECURITY_RATING_KEY)
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    // directory
    assertThat(response.getComponentsList().get(0).getMeasuresList().get(0).getPeriods().getPeriodsValue(0).getValue()).isEqualTo("2.0");
    // file measures
    assertThat(response.getComponentsList().get(1).getMeasuresList().get(0).getPeriods().getPeriodsValue(0).getValue()).isEqualTo("1.0");
  }

  @Test
  public void load_measures_multi_sort_with_metric_key_and_paginated() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(projectDto);
    ComponentDto file9 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-9").setName("file-1"));
    ComponentDto file8 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-8").setName("file-1"));
    ComponentDto file7 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-7").setName("file-1"));
    ComponentDto file6 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-6").setName("file-1"));
    ComponentDto file5 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-5").setName("file-1"));
    ComponentDto file4 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-4").setName("file-1"));
    ComponentDto file3 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-3").setName("file-1"));
    ComponentDto file2 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-2").setName("file-1"));
    ComponentDto file1 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-1").setName("file-1"));
    MetricDto coverage = insertCoverageMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(coverage, file1, projectSnapshot).setValue(1.0d),
      newMeasureDto(coverage, file2, projectSnapshot).setValue(2.0d),
      newMeasureDto(coverage, file3, projectSnapshot).setValue(3.0d),
      newMeasureDto(coverage, file4, projectSnapshot).setValue(4.0d),
      newMeasureDto(coverage, file5, projectSnapshot).setValue(5.0d),
      newMeasureDto(coverage, file6, projectSnapshot).setValue(6.0d),
      newMeasureDto(coverage, file7, projectSnapshot).setValue(7.0d),
      newMeasureDto(coverage, file8, projectSnapshot).setValue(8.0d),
      newMeasureDto(coverage, file9, projectSnapshot).setValue(9.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(Param.SORT, NAME_SORT + ", " + METRIC_SORT)
      .setParam(PARAM_METRIC_SORT, "coverage")
      .setParam(PARAM_METRIC_KEYS, "coverage")
      .setParam(PARAM_STRATEGY, "leaves")
      .setParam(PARAM_QUALIFIERS, "FIL,UTS")
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-4", "file-uuid-5", "file-uuid-6");
    assertThat(response.getPaging().getPageIndex()).isEqualTo(2);
    assertThat(response.getPaging().getPageSize()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(9);
  }

  @Test
  public void sort_by_metric_value() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(projectDto);
    ComponentDto file4 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-4"));
    ComponentDto file3 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-3"));
    ComponentDto file1 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-1"));
    ComponentDto file2 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-2"));
    MetricDto ncloc = newMetricDto().setKey("ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file1, projectSnapshot).setValue(1.0d),
      newMeasureDto(ncloc, file2, projectSnapshot).setValue(2.0d),
      newMeasureDto(ncloc, file3, projectSnapshot).setValue(3.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(Param.SORT, METRIC_SORT)
      .setParam(PARAM_METRIC_SORT, "ncloc")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-1", "file-uuid-2", "file-uuid-3", "file-uuid-4");
    assertThat(response.getPaging().getTotal()).isEqualTo(4);
  }

  @Test
  public void remove_components_without_measure_on_the_metric_sort() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    ComponentDto file1 = newFileDto(project, null, "file-uuid-1");
    ComponentDto file2 = newFileDto(project, null, "file-uuid-2");
    ComponentDto file3 = newFileDto(project, null, "file-uuid-3");
    ComponentDto file4 = newFileDto(project, null, "file-uuid-4");
    componentDb.insertComponent(file1);
    componentDb.insertComponent(file2);
    componentDb.insertComponent(file3);
    componentDb.insertComponent(file4);
    MetricDto ncloc = newMetricDto().setKey("ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file1, projectSnapshot).setValue(1.0d),
      newMeasureDto(ncloc, file2, projectSnapshot).setValue(2.0d),
      newMeasureDto(ncloc, file3, projectSnapshot).setValue(3.0d),
      // measure on period 1
      newMeasureDto(ncloc, file4, projectSnapshot).setVariation(4.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, project.uuid())
      .setParam(Param.SORT, METRIC_SORT)
      .setParam(PARAM_METRIC_SORT, "ncloc")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER)
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id")
      .containsExactly(file1.uuid(), file2.uuid(), file3.uuid())
      .doesNotContain(file4.uuid());
    assertThat(response.getPaging().getTotal()).isEqualTo(3);
  }

  @Test
  public void sort_by_metric_period() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(projectDto);
    ComponentDto file3 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-3"));
    ComponentDto file1 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-1"));
    ComponentDto file2 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-2"));
    MetricDto ncloc = newMetricDto().setKey("ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file1, projectSnapshot).setVariation(1.0d),
      newMeasureDto(ncloc, file2, projectSnapshot).setVariation(2.0d),
      newMeasureDto(ncloc, file3, projectSnapshot).setVariation(3.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(Param.SORT, METRIC_PERIOD_SORT)
      .setParam(PARAM_METRIC_SORT, "ncloc")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_METRIC_PERIOD_SORT, "1")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-1", "file-uuid-2", "file-uuid-3");
  }

  @Test
  public void remove_components_without_measure_on_the_metric_period_sort() {
    ComponentDto projectDto = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(projectDto);
    ComponentDto file4 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-4"));
    ComponentDto file3 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-3"));
    ComponentDto file2 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-2"));
    ComponentDto file1 = componentDb.insertComponent(newFileDto(projectDto, null, "file-uuid-1"));
    MetricDto ncloc = newMetricDto().setKey("new_ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file1, projectSnapshot).setVariation(1.0d),
      newMeasureDto(ncloc, file2, projectSnapshot).setVariation(2.0d),
      newMeasureDto(ncloc, file3, projectSnapshot).setVariation(3.0d),
      // file 4 measure is on absolute value
      newMeasureDto(ncloc, file4, projectSnapshot).setValue(4.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(Param.SORT, METRIC_PERIOD_SORT + "," + NAME_SORT)
      .setParam(PARAM_METRIC_SORT, "new_ncloc")
      .setParam(PARAM_METRIC_KEYS, "new_ncloc")
      .setParam(PARAM_METRIC_PERIOD_SORT, "1")
      .setParam(PARAM_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER)
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id")
      .containsExactly("file-uuid-1", "file-uuid-2", "file-uuid-3")
      .doesNotContain("file-uuid-4");
  }

  @Test
  public void load_measures_when_no_leave_qualifier() {
    resourceTypes.setLeavesQualifiers();
    String projectUuid = "project-uuid";
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), projectUuid);
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponent(newFileDto(project, null));
    insertNclocMetric();

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, projectUuid)
      .setParam(PARAM_STRATEGY, LEAVES_STRATEGY)
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getBaseComponent().getId()).isEqualTo(projectUuid);
    assertThat(result.getComponentsCount()).isEqualTo(0);
  }

  @Test
  public void fail_when_metric_keys_parameter_is_empty() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'metricKeys' parameter must contain at least one metric key");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "")
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_a_metric_is_not_found() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    insertNclocMetric();
    insertNewViolationsMetric();
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("The following metric keys are not found: unknown-metric, another-unknown-metric");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc, new_violations, unknown-metric, another-unknown-metric").executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_using_DISTRIB_metrics() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("distrib1").setValueType(DISTRIB.name()));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("distrib2").setValueType(DISTRIB.name()));
    db.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metrics distrib1, distrib2 can't be requested in this web service. Please use api/measures/component");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "distrib1,distrib2")
      .execute();
  }

  @Test
  public void fail_when_using_DATA_metrics() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("data1").setValueType(DISTRIB.name()));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("data2").setValueType(DISTRIB.name()));
    db.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metrics data1, data2 can't be requested in this web service. Please use api/measures/component");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "data1,data2")
      .execute();
  }

  @Test
  public void fail_when_setting_more_than_15_metric_keys() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    List<String> metrics = IntStream.range(0, 20)
      .mapToObj(i -> "metric" + i)
      .collect(MoreCollectors.toList());
    db.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'metricKeys' can contains only 15 values, got 20");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, Joiner.on(",").join(metrics))
      .execute();
  }

  @Test
  public void fail_when_search_query_have_less_than_3_characters() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    insertNclocMetric();
    insertNewViolationsMetric();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'q' parameter must have at least 3 characters");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc, new_violations")
      .setParam(Param.TEXT_QUERY, "fi")
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_insufficient_privileges() {
    userSession.logIn();
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_sort_by_metric_and_no_metric_sort_provided() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    expectedException.expect(BadRequestException.class);
    expectedException
      .expectMessage("To sort by a metric, the 's' parameter must contain 'metric' or 'metricPeriod', and a metric key must be provided in the 'metricSort' parameter");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      // PARAM_METRIC_SORT is not set
      .setParam(Param.SORT, METRIC_SORT)
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_sort_by_metric_and_not_in_the_list_of_metric_keys() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("To sort by the 'complexity' metric, it must be in the list of metric keys in the 'metricKeys' parameter");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc,violations")
      .setParam(PARAM_METRIC_SORT, "complexity")
      .setParam(Param.SORT, METRIC_SORT)
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_sort_by_metric_period_and_no_metric_period_sort_provided() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("To sort by a metric period, the 's' parameter must contain 'metricPeriod' and the 'metricPeriodSort' must be provided.");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_METRIC_SORT, "ncloc")
      // PARAM_METRIC_PERIOD_SORT_IS_NOT_SET
      .setParam(Param.SORT, METRIC_PERIOD_SORT)
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_paging_parameter_is_too_big() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    insertNclocMetric();
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'ps' parameter must be less than 500");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(Param.PAGE_SIZE, "2540")
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_with_measures_only_and_no_metric_sort() {
    componentDb.insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    insertNclocMetric();
    expectedException.expect(BadRequestException.class);
    expectedException
      .expectMessage("To filter components based on the sort metric, the 's' parameter must contain 'metric' or 'metricPeriod' and the 'metricSort' parameter must be provided");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER)
      .executeProtobuf(ComponentTreeWsResponse.class);
  }

  @Test
  public void fail_when_component_does_not_exist() {
    insertNclocMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'project-key' not found");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_KEY, "project-key")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .execute();
  }

  @Test
  public void fail_when_component_is_removed() {
    ComponentDto project = componentDb.insertComponent(newPrivateProjectDto(db.getDefaultOrganization()));
    componentDb.insertComponent(newFileDto(project).setKey("file-key").setEnabled(false));
    userSession.anonymous().addProjectPermission(UserRole.USER, project);
    insertNclocMetric();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'file-key' not found");

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_KEY, "file-key")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .execute();
  }

  private static MetricDto newMetricDto() {
    return MetricTesting.newMetricDto()
      .setWorstValue(null)
      .setBestValue(null)
      .setOptimizedBestValue(false)
      .setUserManaged(false);
  }

  private void insertJsonExampleData() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "project-id")
      .setKey("MY_PROJECT")
      .setName("My Project")
      .setQualifier(Qualifiers.PROJECT);
    componentDb.insertComponent(project);
    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(project)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));

    ComponentDto file1 = componentDb.insertComponent(newFileDto(project, null)
      .setUuid("AVIwDXE-bJbJqrw6wFv5")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/ElementImpl.java")
      .setName("ElementImpl.java")
      .setLanguage("java")
      .setQualifier(Qualifiers.FILE)
      .setPath("src/main/java/com/sonarsource/markdown/impl/ElementImpl.java"));
    ComponentDto file2 = componentDb.insertComponent(newFileDto(project, null)
      .setUuid("AVIwDXE_bJbJqrw6wFwJ")
      .setKey("com.sonarsource:java-markdown:src/test/java/com/sonarsource/markdown/impl/ElementImplTest.java")
      .setName("ElementImplTest.java")
      .setLanguage("java")
      .setQualifier(Qualifiers.UNIT_TEST_FILE)
      .setPath("src/test/java/com/sonarsource/markdown/impl/ElementImplTest.java"));
    ComponentDto dir = componentDb.insertComponent(newDirectory(project, "src/main/java/com/sonarsource/markdown/impl")
      .setUuid("AVIwDXE-bJbJqrw6wFv8")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(Qualifiers.DIRECTORY));

    MetricDto complexity = insertComplexityMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(complexity, file1, projectSnapshot)
        .setValue(12.0d),
      newMeasureDto(complexity, dir, projectSnapshot)
        .setValue(35.0d)
        .setVariation(0.0d),
      newMeasureDto(complexity, project, projectSnapshot)
        .setValue(42.0d));

    MetricDto ncloc = insertNclocMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, file1, projectSnapshot)
        .setValue(114.0d),
      newMeasureDto(ncloc, dir, projectSnapshot)
        .setValue(217.0d)
        .setVariation(0.0d),
      newMeasureDto(ncloc, project, projectSnapshot)
        .setValue(1984.0d));

    MetricDto newViolations = insertNewViolationsMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(newViolations, file1, projectSnapshot)
        .setVariation(25.0d),
      newMeasureDto(newViolations, dir, projectSnapshot)
        .setVariation(25.0d),
      newMeasureDto(newViolations, project, projectSnapshot)
        .setVariation(255.0d));

    db.commit();
  }

  private MetricDto insertNewViolationsMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("new_violations")
      .setShortName("New issues")
      .setDescription("New Issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false)
      .setUserManaged(false)
      .setOptimizedBestValue(true)
      .setBestValue(0.0d));
    db.commit();
    return metric;
  }

  private MetricDto insertNclocMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("ncloc")
      .setShortName("Lines of code")
      .setDescription("Non Commenting Lines of Code")
      .setDomain("Size")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private MetricDto insertComplexityMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("complexity")
      .setShortName("Complexity")
      .setDescription("Cyclomatic complexity")
      .setDomain("Complexity")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private MetricDto insertCoverageMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Code Coverage")
      .setDomain("Coverage")
      .setValueType(FLOAT.name())
      .setDirection(1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }
}
