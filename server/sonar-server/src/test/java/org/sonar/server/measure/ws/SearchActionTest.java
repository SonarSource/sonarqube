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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsMeasures;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.WsMeasures.SearchWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.db.measure.MeasureTesting.newMeasureDto;
import static org.sonar.db.metric.MetricTesting.newMetricDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonarqube.ws.client.measure.MeasuresWsParameters.PARAM_PROJECT_KEYS;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private UserDto user;
  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, dbClient));

  @Before
  public void setUp() throws Exception {
    user = db.users().insertUser("john");
    userSession.logIn(user);
  }

  @Test
  public void json_example() {
    List<String> projectKeys = insertJsonExampleData();

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEYS, Joiner.on(",").join(projectKeys))
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .execute()
      .getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void return_measures() throws Exception {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto projectSnapshot = db.components().insertProjectAndSnapshot(project);
    setBrowsePermissionOnUser(project);
    MetricDto coverage = insertCoverageMetric();
    dbClient.measureDao().insert(dbSession, newMeasureDto(coverage, project, projectSnapshot).setValue(15.5d));
    db.commit();

    SearchWsResponse result = call(singletonList(project.key()), singletonList("coverage"));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo("coverage");
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_measures_on_leak_period() throws Exception {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.organizations().insert());
    SnapshotDto projectSnapshot = db.components().insertProjectAndSnapshot(project);
    setBrowsePermissionOnUser(project);
    MetricDto coverage = insertCoverageMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(coverage, project, projectSnapshot)
        .setValue(15.5d)
        .setVariation(10d));
    db.commit();

    SearchWsResponse result = call(singletonList(project.key()), singletonList("coverage"));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo("coverage");
    assertThat(measure.getValue()).isEqualTo("15.5");
    assertThat(measure.getPeriods().getPeriodsValueList())
      .extracting(WsMeasures.PeriodValue::getIndex, WsMeasures.PeriodValue::getValue)
      .containsOnly(tuple(1, "10.0"));
  }

  @Test
  public void sort_by_metric_key_then_project_name() throws Exception {
    MetricDto coverage = insertCoverageMetric();
    MetricDto complexity = insertComplexityMetric();
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(organizationDto).setName("C");
    SnapshotDto projectSnapshot1 = db.components().insertProjectAndSnapshot(project1);
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organizationDto).setName("A");
    SnapshotDto projectSnapshot2 = db.components().insertProjectAndSnapshot(project2);
    ComponentDto project3 = ComponentTesting.newPrivateProjectDto(organizationDto).setName("B");
    SnapshotDto projectSnapshot3 = db.components().insertProjectAndSnapshot(project3);
    setBrowsePermissionOnUser(project1, project2, project3);
    dbClient.measureDao().insert(dbSession, newMeasureDto(coverage, project1, projectSnapshot1).setValue(5.5d));
    dbClient.measureDao().insert(dbSession, newMeasureDto(coverage, project2, projectSnapshot2).setValue(6.5d));
    dbClient.measureDao().insert(dbSession, newMeasureDto(coverage, project3, projectSnapshot3).setValue(7.5d));
    dbClient.measureDao().insert(dbSession, newMeasureDto(complexity, project1, projectSnapshot1).setValue(10d));
    dbClient.measureDao().insert(dbSession, newMeasureDto(complexity, project2, projectSnapshot2).setValue(15d));
    dbClient.measureDao().insert(dbSession, newMeasureDto(complexity, project3, projectSnapshot3).setValue(20d));
    db.commit();

    SearchWsResponse result = call(asList(project1.key(), project2.key(), project3.key()), asList("coverage", "complexity"));

    assertThat(result.getMeasuresList()).extracting(Measure::getMetric, Measure::getComponent)
      .containsExactly(
        tuple("complexity", project2.key()), tuple("complexity", project3.key()), tuple("complexity", project1.key()),
        tuple("coverage", project2.key()), tuple("coverage", project3.key()), tuple("coverage", project1.key()));
  }

  @Test
  public void return_measures_on_view() throws Exception {
    ComponentDto view = newView(db.getDefaultOrganization());
    SnapshotDto viewSnapshot = db.components().insertProjectAndSnapshot(view);
    MetricDto coverage = insertCoverageMetric();
    dbClient.measureDao().insert(dbSession, newMeasureDto(coverage, view, viewSnapshot).setValue(15.5d));
    db.commit();

    SearchWsResponse result = call(singletonList(view.key()), singletonList("coverage"));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo("coverage");
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_measures_on_sub_view() throws Exception {
    ComponentDto view = newView(db.getDefaultOrganization());
    SnapshotDto viewSnapshot = db.components().insertProjectAndSnapshot(view);
    ComponentDto subView = db.components().insertComponent(newSubView(view));
    MetricDto coverage = insertCoverageMetric();
    dbClient.measureDao().insert(dbSession, newMeasureDto(coverage, subView, viewSnapshot).setValue(15.5d));
    db.commit();

    SearchWsResponse result = call(singletonList(subView.key()), singletonList("coverage"));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo("coverage");
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void only_returns_authorized_projects() {
    MetricDto metricDto = insertComplexityMetric();
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto projectSnapshot1 = db.components().insertProjectAndSnapshot(project1);
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto projectSnapshot2 = db.components().insertProjectAndSnapshot(project2);
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(metricDto, project1, projectSnapshot1).setValue(15.5d),
      newMeasureDto(metricDto, project2, projectSnapshot2).setValue(42.0d));
    db.commit();
    setBrowsePermissionOnUser(project1);

    SearchWsResponse result = call(asList(project1.key(), project2.key()), singletonList("complexity"));

    assertThat(result.getMeasuresList()).extracting(Measure::getComponent).containsOnly(project1.key());
  }

  @Test
  public void do_not_verify_permissions_if_user_is_root() {
    MetricDto metricDto = insertComplexityMetric();
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    SnapshotDto projectSnapshot1 = db.components().insertProjectAndSnapshot(project1);
    dbClient.measureDao().insert(dbSession, newMeasureDto(metricDto, project1, projectSnapshot1).setValue(15.5d));
    db.commit();

    userSession.setNonRoot();
    SearchWsResponse result = call(asList(project1.key()), singletonList("complexity"));
    assertThat(result.getMeasuresCount()).isEqualTo(0);

    userSession.setRoot();
    result = call(asList(project1.key()), singletonList("complexity"));
    assertThat(result.getMeasuresCount()).isEqualTo(1);
  }

  @Test
  public void fail_if_no_metric() {
    ComponentDto project = db.components().insertPrivateProject();
    setBrowsePermissionOnUser(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'metricKeys' parameter is missing");

    call(singletonList(project.uuid()), null);
  }

  @Test
  public void fail_if_empty_metric() {
    ComponentDto project = db.components().insertPrivateProject();
    setBrowsePermissionOnUser(project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric keys must be provided");

    call(singletonList(project.uuid()), emptyList());
  }

  @Test
  public void fail_if_unknown_metric() {
    ComponentDto project = db.components().insertPrivateProject();
    setBrowsePermissionOnUser(project);
    insertComplexityMetric();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The following metrics are not found: ncloc, violations");

    call(singletonList(project.key()), newArrayList("violations", "complexity", "ncloc"));
  }

  @Test
  public void fail_if_no_project() {
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project keys must be provided");

    call(null, singletonList("complexity"));
  }

  @Test
  public void fail_if_empty_project_key() {
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project keys must be provided");

    call(emptyList(), singletonList("complexity"));
  }

  @Test
  public void fail_if_more_than_100_project_keys() {
    List<String> keys = IntStream.rangeClosed(1, 101)
      .mapToObj(i -> db.components().insertPrivateProject())
      .map(ComponentDto::key)
      .collect(Collectors.toList());
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("101 projects provided, more than maximum authorized (100)");

    call(keys, singletonList("complexity"));
  }

  @Test
  public void does_not_fail_on_100_projects() {
    List<String> keys = IntStream.rangeClosed(1, 100)
      .mapToObj(i -> db.components().insertPrivateProject())
      .map(ComponentDto::key)
      .collect(Collectors.toList());
    insertComplexityMetric();

    call(keys, singletonList("complexity"));
  }

  @Test
  public void fail_if_module() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    setBrowsePermissionOnUser(project);
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only component of qualifiers [TRK, VW, SVW] are allowed");

    call(singletonList(module.key()), singletonList("complexity"));
  }

  @Test
  public void fail_if_directory() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto dir = db.components().insertComponent(newDirectory(project, "dir"));
    setBrowsePermissionOnUser(project);
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only component of qualifiers [TRK, VW, SVW] are allowed");

    call(singletonList(dir.key()), singletonList("complexity"));
  }

  @Test
  public void fail_if_file() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    setBrowsePermissionOnUser(project);
    insertComplexityMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only component of qualifiers [TRK, VW, SVW] are allowed");

    call(singletonList(file.key()), singletonList("complexity"));
  }

  @Test
  public void definition() {
    WebService.Action result = ws.getDef();

    assertThat(result.key()).isEqualTo("search");
    assertThat(result.isPost()).isFalse();
    assertThat(result.isInternal()).isTrue();
    assertThat(result.since()).isEqualTo("6.2");
    assertThat(result.params()).hasSize(2);
    assertThat(result.responseExampleAsString()).isNotEmpty();
  }

  private SearchWsResponse call(@Nullable List<String> keys, @Nullable List<String> metrics) {
    TestRequest request = ws.newRequest();
    if (keys != null) {
      request.setParam(PARAM_PROJECT_KEYS, String.join(",", keys));
    }
    if (metrics != null) {
      request.setParam(PARAM_METRIC_KEYS, String.join(",", metrics));
    }
    return request.executeProtobuf(SearchWsResponse.class);
  }

  private static MetricDto newMetricDtoWithoutOptimization() {
    return newMetricDto()
      .setWorstValue(null)
      .setBestValue(null)
      .setOptimizedBestValue(false)
      .setUserManaged(false);
  }

  private MetricDto insertNewViolationsMetric() {
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
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
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
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
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
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
    MetricDto metric = dbClient.metricDao().insert(dbSession, newMetricDtoWithoutOptimization()
      .setKey("coverage")
      .setShortName("Coverage")
      .setDescription("Code Coverage")
      .setDomain("Coverage")
      .setValueType(Metric.ValueType.FLOAT.name())
      .setDirection(1)
      .setQualitative(false)
      .setHidden(false)
      .setUserManaged(false));
    db.commit();
    return metric;
  }

  private List<String> insertJsonExampleData() {
    List<String> projectKeys = new ArrayList<>();
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(organizationDto).setKey("MY_PROJECT_1").setName("Project 1");
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organizationDto).setKey("MY_PROJECT_2").setName("Project 2");
    ComponentDto project3 = ComponentTesting.newPrivateProjectDto(organizationDto).setKey("MY_PROJECT_3").setName("Project 3");
    projectKeys.addAll(asList(project1.key(), project2.key(), project3.key()));
    db.components().insertComponents(project1, project2, project3);
    SnapshotDto projectSnapshot1 = dbClient.snapshotDao().insert(dbSession, newAnalysis(project1)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));
    SnapshotDto projectSnapshot2 = dbClient.snapshotDao().insert(dbSession, newAnalysis(project2)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));
    SnapshotDto projectSnapshot3 = dbClient.snapshotDao().insert(dbSession, newAnalysis(project3)
      .setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));

    MetricDto complexity = insertComplexityMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(complexity, project1, projectSnapshot1)
        .setValue(12.0d),
      newMeasureDto(complexity, project2, projectSnapshot2)
        .setValue(35.0d)
        .setVariation(0.0d),
      newMeasureDto(complexity, project3, projectSnapshot3)
        .setValue(42.0d));

    MetricDto ncloc = insertNclocMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(ncloc, project1, projectSnapshot1)
        .setValue(114.0d),
      newMeasureDto(ncloc, project2, projectSnapshot2)
        .setValue(217.0d)
        .setVariation(0.0d),
      newMeasureDto(ncloc, project3, projectSnapshot3)
        .setValue(1984.0d));

    MetricDto newViolations = insertNewViolationsMetric();
    dbClient.measureDao().insert(dbSession,
      newMeasureDto(newViolations, project1, projectSnapshot1)
        .setVariation(25.0d),
      newMeasureDto(newViolations, project2, projectSnapshot2)
        .setVariation(25.0d),
      newMeasureDto(newViolations, project3, projectSnapshot3)
        .setVariation(255.0d));
    db.commit();
    setBrowsePermissionOnUser(project1, project2, project3);
    return projectKeys;
  }

  private void setBrowsePermissionOnUser(ComponentDto... projects) {
    Arrays.stream(projects).forEach(p -> userSession.addProjectPermission(UserRole.USER, p));
  }
}
