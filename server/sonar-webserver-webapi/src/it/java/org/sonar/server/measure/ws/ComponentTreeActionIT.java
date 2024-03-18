/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Measures.ComponentTreeWsResponse;
import org.sonarqube.ws.Measures.PeriodValue;

import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_ISSUES_KEY;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.DIRECTORY;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentDbTester.toProjectDto;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.server.component.ws.MeasuresWsParameters.ADDITIONAL_PERIOD;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_PERIOD_SORT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_SORT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_SORT_FILTER;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_QUALIFIERS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.LEAVES_STRATEGY;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_PERIOD_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.WITH_MEASURES_ONLY_METRIC_SORT_FILTER;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Measures.Component;
import static org.sonarqube.ws.Measures.Measure;

class ComponentTreeActionIT {
  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone().logIn();
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final I18nRule i18n = new I18nRule();

  private final ResourceTypes defaultResourceTypes = new ResourceTypes(new ResourceTypeTree[]{DefaultResourceTypes.get()});
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule()
    .setRootQualifiers(defaultResourceTypes.getRoots())
    .setAllQualifiers(defaultResourceTypes.getAll())
    .setLeavesQualifiers(FILE, UNIT_TEST_FILE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final WsActionTester ws = new WsActionTester(
    new ComponentTreeAction(
      dbClient, new ComponentFinder(dbClient, resourceTypes), userSession,
      i18n, resourceTypes));

  @Test
  void json_example() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT")
      .setName("My Project"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto analysis = db.components().insertSnapshot(mainBranch, s -> s.setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(mainBranch)
      .setUuid("AVIwDXE-bJbJqrw6wFv5")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/ElementImpl.java")
      .setName("ElementImpl.java")
      .setLanguage("java")
      .setQualifier(FILE)
      .setPath("src/main/java/com/sonarsource/markdown/impl/ElementImpl.java"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(mainBranch)
      .setUuid("AVIwDXE_bJbJqrw6wFwJ")
      .setKey("com.sonarsource:java-markdown:src/test/java/com/sonarsource/markdown/impl/ElementImplTest.java")
      .setName("ElementImplTest.java")
      .setLanguage("java")
      .setQualifier(UNIT_TEST_FILE)
      .setPath("src/test/java/com/sonarsource/markdown/impl/ElementImplTest.java"));
    ComponentDto dir = db.components().insertComponent(newDirectory(mainBranch, "src/main/java/com/sonarsource/markdown/impl")
      .setUuid("AVIwDXE-bJbJqrw6wFv8")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(DIRECTORY));

    MetricDto complexity = insertComplexityMetric();
    db.measures().insertLiveMeasure(file1, complexity, m -> m.setValue(12.0d));
    db.measures().insertLiveMeasure(dir, complexity, m -> m.setValue(35.0d));
    db.measures().insertLiveMeasure(mainBranch, complexity, m -> m.setValue(42.0d));

    MetricDto ncloc = insertNclocMetric();
    db.measures().insertLiveMeasure(file1, ncloc, m -> m.setValue(114.0d));
    db.measures().insertLiveMeasure(dir, ncloc, m -> m.setValue(217.0d));
    db.measures().insertLiveMeasure(mainBranch, ncloc, m -> m.setValue(1984.0d));

    MetricDto newViolations = insertNewViolationsMetric();
    db.measures().insertLiveMeasure(file1, newViolations, m -> m.setValue(25.0d));
    db.measures().insertLiveMeasure(dir, newViolations, m -> m.setValue(25.0d));
    db.measures().insertLiveMeasure(mainBranch, newViolations, m -> m.setValue(255.0d));

    MetricDto accepted_issues = insertAcceptedIssuesMetric();
    db.measures().insertLiveMeasure(file1, accepted_issues, m -> m.setValue(10d));
    db.measures().insertLiveMeasure(dir, accepted_issues, m -> m.setValue(10d));
    db.measures().insertLiveMeasure(mainBranch, accepted_issues, m -> m.setValue(10d));

    db.commit();

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations, accepted_issues")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,period")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("component_tree-example.json"));
  }

  private UserSessionRule addProjectPermission(ProjectData projectData) {
    return userSession.addProjectPermission(USER, projectData.getProjectDto())
      .addProjectBranchMapping(projectData.projectUuid(), projectData.getMainBranchComponent());
  }

  @Test
  void shouldReturnRenamedMetric() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT")
      .setName("My Project"));
    addProjectPermission(projectData);
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    SnapshotDto analysis = db.components().insertSnapshot(mainBranch, s -> s.setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));

    MetricDto accepted_issues = insertAcceptedIssuesMetric();
    db.measures().insertLiveMeasure(mainBranch, accepted_issues, m -> m.setValue(10d));

    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "wont_fix_issues")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getMetrics().getMetrics(0).getKey()).isEqualTo("wont_fix_issues");
    assertThat(response.getBaseComponent().getMeasures(0).getMetric()).isEqualTo("wont_fix_issues");
  }

  @Test
  void empty_response() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getBaseComponent().getKey()).isEqualTo(mainBranch.getKey());
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getMetrics().getMetricsList()).isEmpty();
    assertThat(response.hasPeriod()).isFalse();
  }

  @Test
  void load_measures_and_periods() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(dbSession,
      newAnalysis(mainBranch)
        .setPeriodDate(System.currentTimeMillis())
        .setPeriodMode("last_version")
        .setPeriodDate(System.currentTimeMillis()));
    userSession.anonymous().addProjectPermission(USER, mainBranch);
    ComponentDto directory = newDirectory(mainBranch, "directory-uuid", "path/to/directory").setName("directory-1");
    db.components().insertComponent(directory);
    ComponentDto file = newFileDto(directory, null, "file-uuid").setName("file-1");
    db.components().insertComponent(file);
    MetricDto ncloc = insertNclocMetric();
    MetricDto coverage = insertCoverageMetric();
    db.commit();
    db.measures().insertLiveMeasure(file, ncloc, m -> m.setValue(5.0d));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(15.5d));
    db.measures().insertLiveMeasure(directory, coverage, m -> m.setValue(15.5d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc,coverage")
      .setParam(PARAM_ADDITIONAL_FIELDS, ADDITIONAL_PERIOD)
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList().get(0).getMeasuresList()).extracting("metric").containsOnly("coverage");
    // file measures
    List<Measure> fileMeasures = response.getComponentsList().get(1).getMeasuresList();
    assertThat(fileMeasures).extracting("metric").containsOnly("ncloc", "coverage");
    assertThat(fileMeasures).extracting("value").containsOnly("5", "15.5");
    assertThat(response.getPeriod().getMode()).isEqualTo("last_version");
  }

  @Test
  void load_measures_with_best_value() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto projectSnapshot = db.components().insertSnapshot(mainBranch);
    userSession.anonymous().addProjectPermission(USER, mainBranch);
    ComponentDto directory = newDirectory(mainBranch, "directory-uuid", "path/to/directory").setName("directory-1");
    db.components().insertComponent(directory);
    ComponentDto file = newFileDto(directory, null, "file-uuid").setName("file-1");
    db.components().insertComponent(file);
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
    db.commit();
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(15.5d));
    db.measures().insertLiveMeasure(directory, coverage, m -> m.setValue(42.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc,coverage,new_violations")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    // directory measures
    assertThat(response.getComponentsList().get(0).getMeasuresList()).extracting("metric").containsOnly("coverage");
    // file measures
    List<Measure> fileMeasures = response.getComponentsList().get(1).getMeasuresList();
    assertThat(fileMeasures)
      .extracting(Measure::getMetric, Measure::getValue, Measure::getBestValue, Measure::hasBestValue)
      .containsExactlyInAnyOrder(tuple("ncloc", "100", true, true),
        tuple("coverage", "15.5", false, false),
        tuple("new_violations", "", false, false));

    List<Common.Metric> metrics = response.getMetrics().getMetricsList();
    assertThat(metrics).extracting("bestValue").contains("100", "");
    assertThat(metrics).extracting("worstValue").contains("1000");
  }

  @Test
  void return_is_best_value_on_leak_measures() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    userSession.anonymous().addProjectPermission(USER, mainBranch);
    ComponentDto file = newFileDto(mainBranch);
    db.components().insertComponent(file);

    MetricDto matchingBestValue = db.measures().insertMetric(m -> m
      .setKey("new_lines")
      .setValueType(INT.name())
      .setBestValue(100d));
    MetricDto doesNotMatchBestValue = db.measures().insertMetric(m -> m
      .setKey("new_lines_2")
      .setValueType(INT.name())
      .setBestValue(100d));
    MetricDto noBestValue = db.measures().insertMetric(m -> m
      .setKey("new_violations")
      .setValueType(INT.name())
      .setBestValue(null));
    db.measures().insertLiveMeasure(file, matchingBestValue, m -> m.setData((String) null).setValue(100d));
    db.measures().insertLiveMeasure(file, doesNotMatchBestValue, m -> m.setData((String) null).setValue(10d));
    db.measures().insertLiveMeasure(file, noBestValue, m -> m.setData((String) null).setValue(42.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "new_lines,new_lines_2,new_violations")
      .executeProtobuf(ComponentTreeWsResponse.class);

    // file measures

    // verify backward compatibility
    List<Measure> fileMeasures = response.getComponentsList().get(0).getMeasuresList();

    assertThat(fileMeasures)
      .extracting(Measure::getMetric, Measure::getPeriod)
      .containsExactlyInAnyOrder(
        tuple(matchingBestValue.getKey(), PeriodValue.newBuilder().setIndex(1).setValue("100").setBestValue(true).build()),
        tuple(doesNotMatchBestValue.getKey(), PeriodValue.newBuilder().setIndex(1).setValue("10").setBestValue(false).build()),
        tuple(noBestValue.getKey(), PeriodValue.newBuilder().setIndex(1).setValue("42").build()));
  }

  @Test
  void use_best_value_for_rating() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.anonymous().addProjectPermission(USER, mainBranch);
    SnapshotDto projectSnapshot = dbClient.snapshotDao().insert(dbSession, newAnalysis(mainBranch)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));
    ComponentDto directory = newDirectory(mainBranch, "directory-uuid", "path/to/directory").setName("directory-1");
    db.components().insertComponent(directory);
    ComponentDto file = newFileDto(directory, null, "file-uuid").setName("file-1");
    db.components().insertComponent(file);
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey(NEW_SECURITY_RATING_KEY)
      .setOptimizedBestValue(true)
      .setBestValue(1d)
      .setValueType(RATING.name()));
    db.commit();
    db.measures().insertLiveMeasure(directory, metric, m -> m.setValue(2d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, NEW_SECURITY_RATING_KEY)
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    // directory
    assertThat(response.getComponentsList().get(0).getMeasuresList().get(0).getPeriod().getValue()).isEqualTo("2.0");
    assertThat(response.getComponentsList().get(0).getMeasuresList().get(0).getPeriod().getValue()).isEqualTo("2.0");
    // file measures
    assertThat(response.getComponentsList().get(1).getMeasuresList().get(0).getPeriod().getValue()).isEqualTo("1.0");
    assertThat(response.getComponentsList().get(1).getMeasuresList().get(0).getPeriod().getValue()).isEqualTo("1.0");
  }

  @Test
  void load_measures_multi_sort_with_metric_key_and_paginated() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto projectSnapshot = db.components().insertSnapshot(mainBranch);
    ComponentDto file9 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-9").setName("file-1").setKey("file-9-key"));
    ComponentDto file8 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-8").setName("file-1").setKey("file-8-key"));
    ComponentDto file7 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-7").setName("file-1").setKey("file-7-key"));
    ComponentDto file6 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-6").setName("file-1").setKey("file-6-key"));
    ComponentDto file5 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-5").setName("file-1").setKey("file-5-key"));
    ComponentDto file4 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-4").setName("file-1").setKey("file-4-key"));
    ComponentDto file3 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-3").setName("file-1").setKey("file-3-key"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-2").setName("file-1").setKey("file-2-key"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-1").setName("file-1").setKey("file-1-key"));
    MetricDto coverage = insertCoverageMetric();
    db.commit();
    db.measures().insertLiveMeasure(file1, coverage, m -> m.setValue(1.0d));
    db.measures().insertLiveMeasure(file2, coverage, m -> m.setValue(2.0d));
    db.measures().insertLiveMeasure(file3, coverage, m -> m.setValue(3.0d));
    db.measures().insertLiveMeasure(file4, coverage, m -> m.setValue(4.0d));
    db.measures().insertLiveMeasure(file5, coverage, m -> m.setValue(5.0d));
    db.measures().insertLiveMeasure(file6, coverage, m -> m.setValue(6.0d));
    db.measures().insertLiveMeasure(file7, coverage, m -> m.setValue(7.0d));
    db.measures().insertLiveMeasure(file8, coverage, m -> m.setValue(8.0d));
    db.measures().insertLiveMeasure(file9, coverage, m -> m.setValue(9.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(SORT, NAME_SORT + ", " + METRIC_SORT)
      .setParam(PARAM_METRIC_SORT, "coverage")
      .setParam(PARAM_METRIC_KEYS, "coverage")
      .setParam(PARAM_STRATEGY, "leaves")
      .setParam(PARAM_QUALIFIERS, "FIL,UTS")
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-4-key", "file-5-key", "file-6-key");
    assertThat(response.getPaging().getPageIndex()).isEqualTo(2);
    assertThat(response.getPaging().getPageSize()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(9);
  }

  @Test
  void sort_by_metric_value() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto projectSnapshot = db.components().insertSnapshot(mainBranch);
    ComponentDto file4 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-4").setKey("file-4-key"));
    ComponentDto file3 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-3").setKey("file-3-key"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-1").setKey("file-1-key"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-2").setKey("file-2-key"));
    MetricDto ncloc = newMetricDto().setKey("ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    db.commit();
    db.measures().insertLiveMeasure(file1, ncloc, m -> m.setValue(1.0d));
    db.measures().insertLiveMeasure(file2, ncloc, m -> m.setValue(2.0d));
    db.measures().insertLiveMeasure(file3, ncloc, m -> m.setValue(3.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(SORT, METRIC_SORT)
      .setParam(PARAM_METRIC_SORT, "ncloc")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-1-key", "file-2-key", "file-3-key", "file-4-key");
    assertThat(response.getPaging().getTotal()).isEqualTo(4);
  }

  @Test
  void remove_components_without_measure_on_the_metric_sort() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto projectSnapshot = db.components().insertSnapshot(mainBranch);
    ComponentDto file1 = newFileDto(mainBranch, null, "file-uuid-1").setKey("file-1-key");
    ComponentDto file2 = newFileDto(mainBranch, null, "file-uuid-2").setKey("file-2-key");
    ComponentDto file3 = newFileDto(mainBranch, null, "file-uuid-3").setKey("file-3-key");
    ComponentDto file4 = newFileDto(mainBranch, null, "file-uuid-4").setKey("file-4-key");
    db.components().insertComponent(file1);
    db.components().insertComponent(file2);
    db.components().insertComponent(file3);
    db.components().insertComponent(file4);
    MetricDto ncloc = newMetricDto().setKey("ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    db.measures().insertLiveMeasure(file1, ncloc, m -> m.setData((String) null).setValue(1.0d));
    db.measures().insertLiveMeasure(file2, ncloc, m -> m.setData((String) null).setValue(2.0d));
    db.measures().insertLiveMeasure(file3, ncloc, m -> m.setData((String) null).setValue(3.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(SORT, METRIC_SORT)
      .setParam(PARAM_METRIC_SORT, "ncloc")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER)
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key")
      .containsExactly(file1.getKey(), file2.getKey(), file3.getKey())
      .doesNotContain(file4.getKey());
    assertThat(response.getPaging().getTotal()).isEqualTo(3);
  }

  @Test
  void sort_by_metric_period() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto projectSnapshot = db.components().insertSnapshot(mainBranch);
    ComponentDto file3 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-3").setKey("file-3-key"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-1").setKey("file-1-key"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-2").setKey("file-2-key"));
    MetricDto ncloc = newMetricDto().setKey("ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    db.commit();
    db.measures().insertLiveMeasure(file1, ncloc, m -> m.setValue(1.0d));
    db.measures().insertLiveMeasure(file2, ncloc, m -> m.setValue(2.0d));
    db.measures().insertLiveMeasure(file3, ncloc, m -> m.setValue(3.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(SORT, METRIC_PERIOD_SORT)
      .setParam(PARAM_METRIC_SORT, "ncloc")
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .setParam(PARAM_METRIC_PERIOD_SORT, "1")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-1-key", "file-2-key", "file-3-key");
  }

  @Test
  void remove_components_without_measure_on_the_metric_period_sort() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto projectSnapshot = db.components().insertSnapshot(mainBranch);
    ComponentDto file4 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-4").setKey("file-4-key"));
    ComponentDto file3 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-3").setKey("file-3-key"));
    ComponentDto file2 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-2").setKey("file-2-key"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(mainBranch, null, "file-uuid-1").setKey("file-1-key"));
    MetricDto ncloc = newMetricDto().setKey("new_ncloc").setValueType(INT.name()).setDirection(1);
    dbClient.metricDao().insert(dbSession, ncloc);
    db.measures().insertLiveMeasure(file1, ncloc, m -> m.setData((String) null).setValue(1.0d));
    db.measures().insertLiveMeasure(file2, ncloc, m -> m.setData((String) null).setValue(2.0d));
    db.measures().insertLiveMeasure(file3, ncloc, m -> m.setData((String) null).setValue(3.0d));
    db.commit();

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(SORT, METRIC_PERIOD_SORT + "," + NAME_SORT)
      .setParam(PARAM_METRIC_SORT, "new_ncloc")
      .setParam(PARAM_METRIC_KEYS, "new_ncloc")
      .setParam(PARAM_METRIC_PERIOD_SORT, "1")
      .setParam(PARAM_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER)
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key")
      .containsExactly("file-1-key", "file-2-key", "file-3-key")
      .doesNotContain("file-4-key");
  }

  @Test
  void load_measures_when_no_leave_qualifier() {
    resourceTypes.setLeavesQualifiers();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(mainBranch);
    db.components().insertComponent(newFileDto(mainBranch));
    insertNclocMetric();

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_STRATEGY, LEAVES_STRATEGY)
      .setParam(PARAM_METRIC_KEYS, "ncloc")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getBaseComponent().getKey()).isEqualTo(mainBranch.getKey());
    assertThat(result.getComponentsCount()).isZero();
  }

  @Test
  void branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    String branchName = "my-branch";
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(branchName));
    userSession.addProjectBranchMapping(projectData.getProjectDto().getUuid(), branch);
    db.components().insertSnapshot(branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    MetricDto complexity = db.measures().insertMetric(m -> m.setValueType(INT.name()));
    LiveMeasureDto measure = db.measures().insertLiveMeasure(file, complexity, m -> m.setValue(12.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .setParam(PARAM_METRIC_KEYS, complexity.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(file.getKey(), branchName);
    assertThat(response.getBaseComponent().getMeasuresList())
      .extracting(Measure::getMetric, m -> parseDouble(m.getValue()))
      .containsExactlyInAnyOrder(tuple(complexity.getKey(), measure.getValue()));
  }

  @Test
  void dont_show_branch_if_main_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    MetricDto complexity = db.measures().insertMetric(m -> m.setValueType(INT.name()));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, BranchDto.DEFAULT_MAIN_BRANCH_NAME)
      .setParam(PARAM_METRIC_KEYS, complexity.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(file.getKey(), "");
  }

  @Test
  void show_branch_on_empty_response_if_not_main_branch() {
    ComponentDto mainProjectBranch = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(USER, mainProjectBranch);
    ComponentDto branch = db.components().insertProjectBranch(mainProjectBranch, b -> b.setKey("develop"));
    userSession.addProjectBranchMapping(mainProjectBranch.uuid(), branch);

    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainProjectBranch.uuid()));
    MetricDto complexity = db.measures().insertMetric(m -> m.setValueType(INT.name()));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, "develop")
      .setParam(PARAM_METRIC_KEYS, complexity.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(file.getKey(), "develop");
  }

  @Test
  void pull_request() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey("pr-123").setBranchType(PULL_REQUEST));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    MetricDto complexity = db.measures().insertMetric(m -> m.setValueType(INT.name()));
    LiveMeasureDto measure = db.measures().insertLiveMeasure(file, complexity, m -> m.setValue(12.0d));

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_PULL_REQUEST, "pr-123")
      .setParam(PARAM_METRIC_KEYS, complexity.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getPullRequest)
      .containsExactlyInAnyOrder(file.getKey(), "pr-123");
    assertThat(response.getBaseComponent().getMeasuresList())
      .extracting(Measure::getMetric, m -> parseDouble(m.getValue()))
      .containsExactlyInAnyOrder(tuple(complexity.getKey(), measure.getValue()));
  }

  @Test
  void metric_without_a_domain() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    SnapshotDto analysis = db.getDbClient().snapshotDao().insert(dbSession, newAnalysis(mainBranch));
    MetricDto metricWithoutDomain = db.measures().insertMetric(m -> m
      .setValueType(Metric.ValueType.INT.name())
      .setDomain(null));
    db.measures().insertLiveMeasure(mainBranch, metricWithoutDomain);

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, metricWithoutDomain.getKey())
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getBaseComponent().getMeasures(0).getMetric()).isEqualTo(metricWithoutDomain.getKey());
    Common.Metric responseMetric = result.getMetrics().getMetrics(0);
    assertThat(responseMetric.getKey()).isEqualTo(metricWithoutDomain.getKey());
    assertThat(responseMetric.hasDomain()).isFalse();
  }

  @Test
  void project_reference_from_portfolio() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    ComponentDto view = db.components().insertPrivatePortfolio();
    userSession.addProjectPermission(USER, view);
    SnapshotDto viewAnalysis = db.components().insertSnapshot(view);
    ComponentDto projectCopy = db.components().insertComponent(newProjectCopy(mainBranch, view));
    MetricDto ncloc = insertNclocMetric();
    db.measures().insertLiveMeasure(projectCopy, ncloc, m -> m.setValue(5d));

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, view.getKey())
      .setParam(PARAM_METRIC_KEYS, ncloc.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getRefKey)
      .containsExactlyInAnyOrder(tuple(projectCopy.getKey(), mainBranch.getKey()));
  }

  @Test
  void portfolio_local_reference_in_portfolio() {
    ComponentDto view = db.components().insertComponent(ComponentTesting.newPortfolio("VIEW1-UUID")
      .setKey("Apache-Projects").setName("Apache Projects"));
    userSession.registerPortfolios(view);
    ComponentDto view2 = db.components().insertPrivatePortfolio();
    userSession.addProjectPermission(USER, view2);
    ComponentDto localView = db.components().insertComponent(
      ComponentTesting.newSubPortfolio(view, "SUB-VIEW-UUID", "All-Projects").setName("All projects").setCopyComponentUuid(view2.uuid()));
    db.components().insertSnapshot(view);
    MetricDto ncloc = insertNclocMetric();
    db.measures().insertLiveMeasure(localView, ncloc, m -> m.setValue(5d));

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, view.getKey())
      .setParam(PARAM_METRIC_KEYS, ncloc.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getRefKey, Component::getQualifier)
      .containsExactlyInAnyOrder(tuple(localView.getKey(), view2.getKey(), "SVW"));
  }

  @Test
  void application_local_reference_in_portfolio() {
    ComponentDto apache_projects = ComponentTesting.newPortfolio("VIEW1-UUID")
      .setKey("Apache-Projects").setName("Apache Projects").setPrivate(true);
    userSession.addProjectPermission(USER, apache_projects);
    ComponentDto view = db.components().insertComponent(apache_projects);
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    userSession.addProjectPermission(USER, application);
    ComponentDto localView = db.components().insertComponent(
      ComponentTesting.newSubPortfolio(view, "SUB-VIEW-UUID", "All-Projects").setName("All projects").setCopyComponentUuid(application.uuid()));
    db.components().insertSnapshot(view);
    MetricDto ncloc = insertNclocMetric();
    db.measures().insertLiveMeasure(localView, ncloc, m -> m.setValue(5d));

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, view.getKey())
      .setParam(PARAM_METRIC_KEYS, ncloc.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getRefKey, Component::getQualifier)
      .containsExactlyInAnyOrder(tuple(localView.getKey(), application.getKey(), "APP"));
  }

  @Test
  void project_branch_reference_from_application_branch() {
    MetricDto ncloc = insertNclocMetric();
    ProjectData applicationData = db.components().insertPublicProject(c -> c.setQualifier(APP).setKey("app-key"));
    ComponentDto application = applicationData.getMainBranchComponent();
    userSession.registerApplication(applicationData.getProjectDto());
    String branchName = "app-branch";
    ComponentDto applicationBranch = db.components().insertProjectBranch(application, a -> a.setKey(branchName), a -> a.setUuid("custom-uuid"));
    userSession.addProjectBranchMapping(applicationData.projectUuid(), applicationBranch);
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("project-key"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    ComponentDto projectBranch = db.components().insertProjectBranch(mainBranch, b -> b.setKey("project-branch"));
    userSession.addProjectBranchMapping(projectData.projectUuid(), projectBranch);
    ComponentDto techProjectBranch = db.components().insertComponent(newProjectCopy(projectBranch, applicationBranch)
      .setKey(applicationBranch.getKey() + branchName + projectBranch.getKey()));

    SnapshotDto applicationBranchAnalysis = db.components().insertSnapshot(applicationBranch);
    db.measures().insertLiveMeasure(applicationBranch, ncloc, m -> m.setValue(5d));
    db.measures().insertLiveMeasure(techProjectBranch, ncloc, m -> m.setValue(1d));

    ComponentTreeWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENT, applicationBranch.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .setParam(PARAM_METRIC_KEYS, ncloc.getKey())
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(result.getBaseComponent())
      .extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(applicationBranch.getKey(), branchName);
    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getBranch, Component::getRefKey)
      .containsExactlyInAnyOrder(tuple(techProjectBranch.getKey(), "project-branch", mainBranch.getKey()));
  }

  @Test
  void fail_when_metric_keys_parameter_is_empty() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "")
        .executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The 'metricKeys' parameter must contain at least one metric key");
  }

  @Test
  void fail_when_a_metric_is_not_found() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(mainBranch);
    insertNclocMetric();
    insertNewViolationsMetric();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc, new_violations, unknown-metric, another-unknown-metric").executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("The following metric keys are not found: unknown-metric, another-unknown-metric");
  }

  @Test
  void fail_when_using_DISTRIB_metrics() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(mainBranch);
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("distrib1").setValueType(DISTRIB.name()));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("distrib2").setValueType(DISTRIB.name()));
    db.commit();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "distrib1,distrib2")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metrics distrib1, distrib2 can't be requested in this web service. Please use api/measures/component");
  }

  @Test
  void fail_when_using_unsupported_DATA_metrics() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(mainBranch);

    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("data1").setValueType(DATA.name()));
    dbClient.metricDao().insert(dbSession, newMetricDto().setKey("data2").setValueType(DATA.name()));
    db.commit();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "data1,data2")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metrics data1, data2 can't be requested in this web service. Please use api/measures/component");
  }

  @Test
  void execute_whenUsingSupportedDATAMetrics_shouldReturnMetrics() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.getDbClient().snapshotDao().insert(dbSession, newAnalysis(mainBranch));

    insertMetricAndLiveMeasure(mainBranch, SECURITY_ISSUES_KEY, "_data");
    insertMetricAndLiveMeasure(mainBranch, MAINTAINABILITY_ISSUES_KEY, "_data");
    insertMetricAndLiveMeasure(mainBranch, RELIABILITY_ISSUES_KEY, "_data");

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, format("%s,%s,%s", SECURITY_ISSUES_KEY, MAINTAINABILITY_ISSUES_KEY, RELIABILITY_ISSUES_KEY))
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getMetrics().getMetricsList()).size().isEqualTo(3);

    List<Measure> dataMeasures = response.getBaseComponent().getMeasuresList();

    assertThat(dataMeasures)
      .extracting(Measure::getMetric, Measure::getValue)
      .containsExactlyInAnyOrder(tuple(SECURITY_ISSUES_KEY, SECURITY_ISSUES_KEY + "_data"),
        tuple(MAINTAINABILITY_ISSUES_KEY, MAINTAINABILITY_ISSUES_KEY + "_data"),
        tuple(RELIABILITY_ISSUES_KEY, RELIABILITY_ISSUES_KEY + "_data"));
  }

  @Test
  void execute_whenUsingSupportedNewDATAMetrics_shouldReturnMetrics() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    db.components().insertSnapshot(mainBranch);
    ComponentDto file1 = db.components().insertComponent(newFileDto(mainBranch));

    insertMetricAndLiveMeasure(file1, NEW_SECURITY_ISSUES_KEY, "_data");
    insertMetricAndLiveMeasure(file1, NEW_MAINTAINABILITY_ISSUES_KEY, "_data");
    insertMetricAndLiveMeasure(file1, NEW_RELIABILITY_ISSUES_KEY, "_data");

    ComponentTreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, format("%s,%s,%s",
        NEW_SECURITY_ISSUES_KEY,
        NEW_MAINTAINABILITY_ISSUES_KEY,
        NEW_RELIABILITY_ISSUES_KEY
      ))
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,period")
      .executeProtobuf(ComponentTreeWsResponse.class);

    assertThat(response.getComponents(0).getMeasuresCount()).isEqualTo(3);

    List<Measure> dataMeasures = response.getComponents(0).getMeasuresList();

    assertThat(dataMeasures)
      .extracting(Measure::getMetric, m-> m.getPeriod().getValue())
      .containsExactlyInAnyOrder(tuple(NEW_SECURITY_ISSUES_KEY, NEW_SECURITY_ISSUES_KEY + "_data"),
        tuple(NEW_MAINTAINABILITY_ISSUES_KEY, NEW_MAINTAINABILITY_ISSUES_KEY + "_data"),
        tuple(NEW_RELIABILITY_ISSUES_KEY, NEW_RELIABILITY_ISSUES_KEY + "_data")
      );
  }

  @Test
  void fail_when_setting_more_than_15_metric_keys() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    List<String> metrics = IntStream.range(0, 20)
      .mapToObj(i -> "metric" + i)
      .toList();
    db.commit();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, Joiner.on(",").join(metrics))
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'metricKeys' can contains only 15 values, got 20");
  }

  @Test
  void fail_when_search_query_have_less_than_3_characters() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    insertNclocMetric();
    insertNewViolationsMetric();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc, new_violations")
        .setParam(Param.TEXT_QUERY, "fi")
        .executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'q' length (2) is shorter than the minimum authorized (3)");
  }

  @Test
  void fail_when_insufficient_privileges() {
    userSession.logIn();
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);

    var request = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc");
    assertThatThrownBy(() -> request.executeProtobuf(ComponentTreeWsResponse.class))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fail_when_app_with_insufficient_privileges_for_projects() {
    userSession.logIn();
    ComponentDto app = db.components().insertPrivateApplication().getMainBranchComponent();
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(app);

    userSession.registerApplication(
      toProjectDto(app, 1L),
      toProjectDto(project1, 1L),
      toProjectDto(project2, 1L));

    userSession.addProjectPermission(USER, app, project1);

    var request = ws.newRequest()
      .setParam(PARAM_COMPONENT, app.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc");
    assertThatThrownBy(() -> request.executeProtobuf(ComponentTreeWsResponse.class))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fail_when_sort_by_metric_and_no_metric_sort_provided() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        // PARAM_METRIC_SORT is not set
        .setParam(SORT, METRIC_SORT)
        .executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("To sort by a metric, the 's' parameter must contain 'metric' or 'metricPeriod', and a metric key must be provided in the 'metricSort' parameter");
  }

  @Test
  void fail_when_sort_by_metric_and_not_in_the_list_of_metric_keys() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc,violations")
        .setParam(PARAM_METRIC_SORT, "complexity")
        .setParam(SORT, METRIC_SORT)
        .executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("To sort by the 'complexity' metric, it must be in the list of metric keys in the 'metricKeys' parameter");
  }

  @Test
  void fail_when_sort_by_metric_period_and_no_metric_period_sort_provided() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        .setParam(PARAM_METRIC_SORT, "ncloc")
        // PARAM_METRIC_PERIOD_SORT_IS_NOT_SET
        .setParam(SORT, METRIC_PERIOD_SORT)
        .executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("To sort by a metric period, the 's' parameter must contain 'metricPeriod' and the 'metricPeriodSort' must be provided.");
  }

  @Test
  void fail_when_paging_parameter_is_too_big() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    insertNclocMetric();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        .setParam(Param.PAGE_SIZE, "2540")
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'ps' value (2540) must be less than 500");
  }

  @Test
  void fail_when_with_measures_only_and_no_metric_sort() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    insertNclocMetric();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        .setParam(PARAM_METRIC_SORT_FILTER, WITH_MEASURES_ONLY_METRIC_SORT_FILTER)
        .executeProtobuf(ComponentTreeWsResponse.class);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("To filter components based on the sort metric, the 's' parameter must contain 'metric' or 'metricPeriod' and the 'metricSort' parameter must be provided");
  }

  @Test
  void fail_when_component_does_not_exist() {
    insertNclocMetric();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, "project-key")
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'project-key' not found");
  }

  @Test
  void fail_when_component_is_removed() {
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch).setKey("file-key").setEnabled(false));
    userSession.anonymous().addProjectPermission(USER, mainBranch);
    insertNclocMetric();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, file.getKey())
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component key '%s' not found", file.getKey()));
  }

  @Test
  void fail_if_branch_does_not_exist() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    addProjectPermission(projectData);
    db.components().insertProjectBranch(mainBranch, b -> b.setKey("my_branch"));

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, file.getKey())
        .setParam(PARAM_BRANCH, "another_branch")
        .setParam(PARAM_METRIC_KEYS, "ncloc")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));
  }

  @Test
  void doHandle_whenPassingUnexpectedQualifier_shouldThrowException() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    addProjectPermission(projectData);
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    MetricDto complexity = db.measures().insertMetric(m -> m.setValueType(INT.name()));

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, BranchDto.DEFAULT_MAIN_BRANCH_NAME)
      .setParam(PARAM_QUALIFIERS, "BRC")
      .setParam(PARAM_METRIC_KEYS, complexity.getKey());

    assertThatThrownBy(() -> testRequest.execute()).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'qualifiers' (BRC) must be one of: [UTS, FIL, DIR, TRK]");
  }

  private static MetricDto newMetricDto() {
    return MetricTesting.newMetricDto()
      .setWorstValue(null)
      .setBestValue(null)
      .setOptimizedBestValue(false);
  }

  private void insertMetricAndLiveMeasure(ComponentDto dto,  String key, String additionalData) {
    MetricDto dataMetric = dbClient.metricDao().insert(dbSession, newDataMetricDto(key));
    db.measures().insertLiveMeasure(dto, dataMetric, c -> c.setData(key + additionalData));

  }

  private static MetricDto newDataMetricDto(String key) {
    return newMetricDto().setValueType(DATA.name()).setKey(key);
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
      .setOptimizedBestValue(true)
      .setBestValue(0.0d));
    db.commit();
    return metric;
  }

  private MetricDto insertAcceptedIssuesMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("accepted_issues")
      .setShortName("Accepted Issues")
      .setDescription("Accepted issues")
      .setDomain("Issues")
      .setValueType(INT.name())
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false));
    db.commit();
    return metric;
  }

  private MetricDto insertNclocMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("ncloc")
      .setShortName("Lines of code")
      .setDescription("Non Commenting Lines of Code")
      .setDomain("Size")
      .setValueType(INT.name())
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false));
    db.commit();
    return metric;
  }

  private MetricDto insertComplexityMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDto()
      .setKey("complexity")
      .setShortName("Complexity")
      .setDescription("Cyclomatic complexity")
      .setDomain("Complexity")
      .setValueType(INT.name())
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false));
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
      .setHidden(false));
    db.commit();
    return metric;
  }
}
