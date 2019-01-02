/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.Measures.SearchWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PROJECT_KEYS;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new SearchAction(userSession, db.getDbClient()));

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insert();

    ComponentDto project1 = db.components().insertPrivateProject(organization, p -> p.setDbKey("MY_PROJECT_1").setName("Project 1"));
    ComponentDto project2 = db.components().insertPrivateProject(organization, p -> p.setDbKey("MY_PROJECT_2").setName("Project 2"));
    ComponentDto project3 = db.components().insertPrivateProject(organization, p -> p.setDbKey("MY_PROJECT_3").setName("Project 3"));

    userSession.addProjectPermission(UserRole.USER, project1);
    userSession.addProjectPermission(UserRole.USER, project2);
    userSession.addProjectPermission(UserRole.USER, project3);

    MetricDto complexity = db.measures().insertMetric(m -> m.setKey("complexity").setValueType(INT.name()));
    db.measures().insertLiveMeasure(project1, complexity, m -> m.setValue(12.0d));
    db.measures().insertLiveMeasure(project2, complexity, m -> m.setValue(35.0d).setVariation(0.0d));
    db.measures().insertLiveMeasure(project3, complexity, m -> m.setValue(42.0d));

    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.name()));
    db.measures().insertLiveMeasure(project1, ncloc, m -> m.setValue(114.0d));
    db.measures().insertLiveMeasure(project2, ncloc, m -> m.setValue(217.0d).setVariation(0.0d));
    db.measures().insertLiveMeasure(project3, ncloc, m -> m.setValue(1984.0d));

    MetricDto newViolations = db.measures().insertMetric(m -> m.setKey("new_violations").setValueType(INT.name()));
    db.measures().insertLiveMeasure(project1, newViolations, m -> m.setVariation(25.0d));
    db.measures().insertLiveMeasure(project2, newViolations, m -> m.setVariation(25.0d));
    db.measures().insertLiveMeasure(project3, newViolations, m -> m.setVariation(255.0d));

    List<String> projectKeys = Arrays.asList(project1.getDbKey(), project2.getDbKey(), project3.getDbKey());

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEYS, Joiner.on(",").join(projectKeys))
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .execute()
      .getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void return_measures() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertLiveMeasure(project, coverage, m -> m.setValue(15.5d));

    SearchWsResponse result = call(singletonList(project.getDbKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_best_value() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto matchBestValue = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()).setBestValue(15.5d));
    db.measures().insertLiveMeasure(project, matchBestValue, m -> m.setValue(15.5d));
    MetricDto doesNotMatchBestValue = db.measures().insertMetric(m -> m.setValueType(INT.name()).setBestValue(50d));
    db.measures().insertLiveMeasure(project, doesNotMatchBestValue, m -> m.setValue(40d));
    MetricDto noBestValue = db.measures().insertMetric(m -> m.setValueType(INT.name()).setBestValue(null));
    db.measures().insertLiveMeasure(project, noBestValue, m -> m.setValue(123d));

    SearchWsResponse result = call(singletonList(project.getDbKey()),
      asList(matchBestValue.getKey(), doesNotMatchBestValue.getKey(), noBestValue.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures)
      .extracting(Measure::getMetric, Measure::getValue, Measure::getBestValue, Measure::hasBestValue)
      .containsExactlyInAnyOrder(
        tuple(matchBestValue.getKey(), "15.5", true, true),
        tuple(doesNotMatchBestValue.getKey(), "40", false, true),
        tuple(noBestValue.getKey(), "123", false, false));
  }

  @Test
  public void return_measures_on_leak_period() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertLiveMeasure(project, coverage, m -> m.setValue(15.5d).setVariation(10d));

    SearchWsResponse result = call(singletonList(project.getDbKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
    assertThat(measure.getPeriods().getPeriodsValueList())
      .extracting(Measures.PeriodValue::getIndex, Measures.PeriodValue::getValue)
      .containsOnly(tuple(1, "10.0"));
  }

  @Test
  public void sort_by_metric_key_then_project_name() {
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey("coverage").setValueType(FLOAT.name()));
    MetricDto complexity = db.measures().insertMetric(m -> m.setKey("complexity").setValueType(INT.name()));
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization, p -> p.setName("C"));
    ComponentDto project2 = db.components().insertPrivateProject(organization, p -> p.setName("A"));
    ComponentDto project3 = db.components().insertPrivateProject(organization, p -> p.setName("B"));
    userSession.addProjectPermission(UserRole.USER, project1);
    userSession.addProjectPermission(UserRole.USER, project2);
    userSession.addProjectPermission(UserRole.USER, project3);
    db.measures().insertLiveMeasure(project1, coverage, m -> m.setValue(5.5d));
    db.measures().insertLiveMeasure(project2, coverage, m -> m.setValue(6.5d));
    db.measures().insertLiveMeasure(project3, coverage, m -> m.setValue(7.5d));
    db.measures().insertLiveMeasure(project1, complexity, m -> m.setValue(10d));
    db.measures().insertLiveMeasure(project2, complexity, m -> m.setValue(15d));
    db.measures().insertLiveMeasure(project3, complexity, m -> m.setValue(20d));

    SearchWsResponse result = call(asList(project1.getDbKey(), project2.getDbKey(), project3.getDbKey()), asList(coverage.getKey(), complexity.getKey()));

    assertThat(result.getMeasuresList()).extracting(Measure::getMetric, Measure::getComponent)
      .containsExactly(
        tuple(complexity.getKey(), project2.getDbKey()), tuple(complexity.getKey(), project3.getDbKey()), tuple(complexity.getKey(), project1.getDbKey()),
        tuple(coverage.getKey(), project2.getDbKey()), tuple(coverage.getKey(), project3.getDbKey()), tuple(coverage.getKey(), project1.getDbKey()));
  }

  @Test
  public void return_measures_on_view() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto view = db.components().insertPrivatePortfolio(organization);
    userSession.addProjectPermission(UserRole.USER, view);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertLiveMeasure(view, coverage, m -> m.setValue(15.5d));

    SearchWsResponse result = call(singletonList(view.getDbKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_measures_on_application() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto application = db.components().insertPrivateApplication(organization);
    userSession.addProjectPermission(UserRole.USER, application);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertLiveMeasure(application, coverage, m -> m.setValue(15.5d));

    SearchWsResponse result = call(singletonList(application.getDbKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_measures_on_sub_view() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto view = db.components().insertPrivatePortfolio(organization);
    ComponentDto subView = db.components().insertComponent(newSubView(view));
    userSession.addProjectPermission(UserRole.USER, subView);
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertLiveMeasure(subView, metric, m -> m.setValue(15.5d));

    SearchWsResponse result = call(singletonList(subView.getDbKey()), singletonList(metric.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(metric.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void only_returns_authorized_projects() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    ComponentDto project1 = db.components().insertPrivateProject(db.getDefaultOrganization());
    ComponentDto project2 = db.components().insertPrivateProject(db.getDefaultOrganization());
    db.measures().insertLiveMeasure(project1, metric, m -> m.setValue(15.5d));
    db.measures().insertLiveMeasure(project2, metric, m -> m.setValue(42.0d));
    Arrays.stream(new ComponentDto[] {project1}).forEach(p -> userSession.addProjectPermission(UserRole.USER, p));

    SearchWsResponse result = call(asList(project1.getDbKey(), project2.getDbKey()), singletonList(metric.getKey()));

    assertThat(result.getMeasuresList()).extracting(Measure::getComponent).containsOnly(project1.getDbKey());
  }

  @Test
  public void do_not_verify_permissions_if_user_is_root() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    ComponentDto project1 = db.components().insertPrivateProject(db.getDefaultOrganization());
    db.measures().insertLiveMeasure(project1, metric, m -> m.setValue(15.5d));

    userSession.setNonRoot();
    SearchWsResponse result = call(singletonList(project1.getDbKey()), singletonList(metric.getKey()));
    assertThat(result.getMeasuresCount()).isEqualTo(0);

    userSession.setRoot();
    result = call(singletonList(project1.getDbKey()), singletonList(metric.getKey()));
    assertThat(result.getMeasuresCount()).isEqualTo(1);
  }

  @Test
  public void does_not_return_branch_when_using_db_key() {
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.measures().insertLiveMeasure(branch, coverage, m -> m.setValue(10d));
    userSession.addProjectPermission(UserRole.USER, project);

    SearchWsResponse result = call(singletonList(branch.getDbKey()), singletonList(coverage.getKey()));

    assertThat(result.getMeasuresList()).isEmpty();
  }

  @Test
  public void fail_if_no_metric() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'metricKeys' parameter is missing");

    call(singletonList(project.uuid()), null);
  }

  @Test
  public void fail_if_empty_metric() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Metric keys must be provided");

    call(singletonList(project.uuid()), emptyList());
  }

  @Test
  public void fail_if_unknown_metric() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The following metrics are not found: ncloc, violations");

    call(singletonList(project.getDbKey()), newArrayList("violations", metric.getKey(), "ncloc"));
  }

  @Test
  public void fail_if_no_project() {
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project keys must be provided");

    call(null, singletonList(metric.getKey()));
  }

  @Test
  public void fail_if_empty_project_key() {
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Project keys must be provided");

    call(emptyList(), singletonList(metric.getKey()));
  }

  @Test
  public void fail_if_more_than_100_project_keys() {
    List<String> keys = IntStream.rangeClosed(1, 101)
      .mapToObj(i -> db.components().insertPrivateProject())
      .map(ComponentDto::getDbKey)
      .collect(Collectors.toList());
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("101 projects provided, more than maximum authorized (100)");

    call(keys, singletonList(metric.getKey()));
  }

  @Test
  public void does_not_fail_on_100_projects() {
    List<String> keys = IntStream.rangeClosed(1, 100)
      .mapToObj(i -> db.components().insertPrivateProject())
      .map(ComponentDto::getDbKey)
      .collect(Collectors.toList());
    MetricDto metric = db.measures().insertMetric();

    call(keys, singletonList(metric.getKey()));
  }

  @Test
  public void fail_if_module() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only component of qualifiers [TRK, APP, VW, SVW] are allowed");

    call(singletonList(module.getDbKey()), singletonList(metric.getKey()));
  }

  @Test
  public void fail_if_directory() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto dir = db.components().insertComponent(newDirectory(project, "dir"));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only component of qualifiers [TRK, APP, VW, SVW] are allowed");

    call(singletonList(dir.getDbKey()), singletonList(metric.getKey()));
  }

  @Test
  public void fail_if_file() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Only component of qualifiers [TRK, APP, VW, SVW] are allowed");

    call(singletonList(file.getDbKey()), singletonList(metric.getKey()));
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
}
