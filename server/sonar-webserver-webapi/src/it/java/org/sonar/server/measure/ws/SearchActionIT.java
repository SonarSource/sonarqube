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
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Measures.Measure;
import org.sonarqube.ws.Measures.SearchWsResponse;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PROJECT_KEYS;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester ws = new WsActionTester(new SearchAction(userSession, db.getDbClient()));

  @Test
  public void json_example() {
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT_1").setName("Project 1")).getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT_2").setName("Project 2")).getMainBranchComponent();
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT_3").setName("Project 3")).getMainBranchComponent();

    userSession.addProjectPermission(UserRole.USER, project1);
    userSession.addProjectPermission(UserRole.USER, project2);
    userSession.addProjectPermission(UserRole.USER, project3);

    MetricDto complexity = db.measures().insertMetric(m -> m.setKey("complexity").setValueType(INT.name()));
    db.measures().insertMeasure(project1, m -> m.addValue(complexity.getKey(), 12.0d));
    db.measures().insertMeasure(project2, m -> m.addValue(complexity.getKey(), 35.0d));
    db.measures().insertMeasure(project3, m -> m.addValue(complexity.getKey(), 42.0d));

    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.name()));
    db.measures().insertMeasure(project1, m -> m.addValue(ncloc.getKey(), 114.0d));
    db.measures().insertMeasure(project2, m -> m.addValue(ncloc.getKey(), 217.0d));
    db.measures().insertMeasure(project3, m -> m.addValue(ncloc.getKey(), 1984.0d));

    MetricDto newViolations = db.measures().insertMetric(m -> m.setKey("new_violations").setValueType(INT.name()));
    db.measures().insertMeasure(project1, m -> m.addValue(newViolations.getKey(), 25.0d));
    db.measures().insertMeasure(project2, m -> m.addValue(newViolations.getKey(), 25.0d));
    db.measures().insertMeasure(project3, m -> m.addValue(newViolations.getKey(), 255.0d));

    List<String> projectKeys = Arrays.asList(project1.getKey(), project2.getKey(), project3.getKey());

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEYS, Joiner.on(",").join(projectKeys))
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .execute()
      .getInput();

    assertJson(result).withStrictArrayOrder().isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void return_measures() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertMeasure(project, m -> m.addValue(coverage.getKey(), 15.5d));

    SearchWsResponse result = call(singletonList(project.getKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void search_shouldReturnAcceptedIssuesMetric_whenIsCalledWithDeprecatedWontFixIssuesMetric() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto acceptedIssues = db.measures().insertMetric(m -> m.setValueType(INT.name())
      .setKey("accepted_issues")
      .setShortName("Accepted Issues"));
    db.measures().insertMeasure(project, m -> m.addValue(acceptedIssues.getKey(), 10d));

    SearchWsResponse result = call(singletonList(project.getKey()), singletonList("wont_fix_issues"));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo("wont_fix_issues");
    assertThat(measure.getValue()).isEqualTo("10");
  }

  @Test
  public void return_best_value() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto matchBestValue = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()).setBestValue(15.5d));
    db.measures().insertMeasure(project, m -> m.addValue(matchBestValue.getKey(), 15.5d));
    MetricDto doesNotMatchBestValue = db.measures().insertMetric(m -> m.setValueType(INT.name()).setBestValue(50d));
    db.measures().insertMeasure(project, m -> m.addValue(doesNotMatchBestValue.getKey(), 40d));
    MetricDto noBestValue = db.measures().insertMetric(m -> m.setValueType(INT.name()).setBestValue(null));
    db.measures().insertMeasure(project, m -> m.addValue(noBestValue.getKey(), 123d));

    SearchWsResponse result = call(singletonList(project.getKey()),
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
  public void return_measures_on_new_code_period() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey("new_metric").setValueType(FLOAT.name()));
    db.measures().insertMeasure(project, m -> m.addValue(coverage.getKey(), 10d));

    SearchWsResponse result = call(singletonList(project.getKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEmpty();
    assertThat(measure.getPeriod().getValue()).isEqualTo("10.0");
  }

  @Test
  public void sort_by_metric_key_then_project_name() {
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey("coverage").setValueType(FLOAT.name()));
    MetricDto complexity = db.measures().insertMetric(m -> m.setKey("complexity").setValueType(INT.name()));
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("C")).getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setName("A")).getMainBranchComponent();
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setName("B")).getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project1);
    userSession.addProjectPermission(UserRole.USER, project2);
    userSession.addProjectPermission(UserRole.USER, project3);
    db.measures().insertMeasure(project1, m -> m.addValue(coverage.getKey(), 5.5d));
    db.measures().insertMeasure(project2, m -> m.addValue(coverage.getKey(), 6.5d));
    db.measures().insertMeasure(project3, m -> m.addValue(coverage.getKey(), 7.5d));
    db.measures().insertMeasure(project1, m -> m.addValue(complexity.getKey(), 10d));
    db.measures().insertMeasure(project2, m -> m.addValue(complexity.getKey(), 15d));
    db.measures().insertMeasure(project3, m -> m.addValue(complexity.getKey(), 20d));

    SearchWsResponse result = call(asList(project1.getKey(), project2.getKey(), project3.getKey()), asList(coverage.getKey(), complexity.getKey()));

    assertThat(result.getMeasuresList()).extracting(Measure::getMetric, Measure::getComponent)
      .containsExactly(
        tuple(complexity.getKey(), project2.getKey()), tuple(complexity.getKey(), project3.getKey()), tuple(complexity.getKey(), project1.getKey()),
        tuple(coverage.getKey(), project2.getKey()), tuple(coverage.getKey(), project3.getKey()), tuple(coverage.getKey(), project1.getKey()));
  }

  @Test
  public void return_measures_on_view() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    userSession.addProjectPermission(UserRole.USER, view);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertMeasure(view, m -> m.addValue(coverage.getKey(), 15.5d));

    SearchWsResponse result = call(singletonList(view.getKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_measures_on_application() {
    ComponentDto application = db.components().insertPrivateApplication().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, application);
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertMeasure(application, m -> m.addValue(coverage.getKey(), 15.5d));

    SearchWsResponse result = call(singletonList(application.getKey()), singletonList(coverage.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(coverage.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void return_measures_on_sub_view() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subView = db.components().insertComponent(newSubPortfolio(view));
    userSession.addProjectPermission(UserRole.USER, view);
    userSession.addProjectPermission(UserRole.USER, subView);
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    db.measures().insertMeasure(subView, m -> m.addValue(metric.getKey(), 15.5d));

    SearchWsResponse result = call(singletonList(subView.getKey()), singletonList(metric.getKey()));

    List<Measure> measures = result.getMeasuresList();
    assertThat(measures).hasSize(1);
    Measure measure = measures.get(0);
    assertThat(measure.getMetric()).isEqualTo(metric.getKey());
    assertThat(measure.getValue()).isEqualTo("15.5");
  }

  @Test
  public void only_returns_authorized_projects() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    db.measures().insertMeasure(project1, m -> m.addValue(metric.getKey(), 15.5d));
    db.measures().insertMeasure(project2, m -> m.addValue(metric.getKey(), 42.0d));
    Arrays.stream(new ComponentDto[] {project1}).forEach(p -> userSession.addProjectPermission(UserRole.USER, p));

    SearchWsResponse result = call(asList(project1.getKey(), project2.getKey()), singletonList(metric.getKey()));

    assertThat(result.getMeasuresList()).extracting(Measure::getComponent).containsOnly(project1.getKey());
  }

  @Test
  public void does_not_return_branch_when_using_db_key() {
    MetricDto coverage = db.measures().insertMetric(m -> m.setValueType(FLOAT.name()));
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.measures().insertMeasure(branch, m -> m.addValue(coverage.getKey(), 10d));
    userSession.addProjectPermission(UserRole.USER, project);

    SearchWsResponse result = call(singletonList(branch.getKey()), singletonList(coverage.getKey()));

    assertThat(result.getMeasuresList()).isEmpty();
  }

  @Test
  public void fail_if_no_metric() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);

    assertThatThrownBy(() -> call(singletonList(project.uuid()), null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'metricKeys' parameter is missing");
  }

  @Test
  public void fail_if_empty_metric() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);

    assertThatThrownBy(() -> call(singletonList(project.uuid()), emptyList()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Metric keys must be provided");
  }

  @Test
  public void fail_if_unknown_metric() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    assertThatThrownBy(() -> call(singletonList(project.getKey()), newArrayList("violations", metric.getKey(), "ncloc")))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("The following metrics are not found: ncloc, violations");
  }

  @Test
  public void fail_if_no_project() {
    MetricDto metric = db.measures().insertMetric();

    assertThatThrownBy(() -> call(null, singletonList(metric.getKey())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Project keys must be provided");
  }

  @Test
  public void fail_if_empty_project_key() {
    MetricDto metric = db.measures().insertMetric();

    assertThatThrownBy(() -> call(emptyList(), singletonList(metric.getKey())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Project keys must be provided");
  }

  @Test
  public void fail_if_more_than_100_project_keys() {
    List<String> keys = IntStream.rangeClosed(1, 101)
      .mapToObj(i -> db.components().insertPrivateProject().getMainBranchComponent())
      .map(ComponentDto::getKey)
      .toList();
    MetricDto metric = db.measures().insertMetric();

    assertThatThrownBy(() -> call(keys, singletonList(metric.getKey())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("101 projects provided, more than maximum authorized (100)");
  }

  @Test
  public void does_not_fail_on_100_projects() {
    List<String> keys = IntStream.rangeClosed(1, 100)
      .mapToObj(i -> db.components().insertPrivateProject().getMainBranchComponent())
      .map(ComponentDto::getKey)
      .toList();
    MetricDto metric = db.measures().insertMetric();

    call(keys, singletonList(metric.getKey()));
  }

  @Test
  public void fail_if_directory() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(newDirectory(project, "dir"));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    assertThatThrownBy(() -> call(singletonList(dir.getKey()), singletonList(metric.getKey())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only component of qualifiers [TRK, APP, VW, SVW] are allowed");
  }

  @Test
  public void fail_if_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    userSession.addProjectPermission(UserRole.USER, project);
    MetricDto metric = db.measures().insertMetric();

    assertThatThrownBy(() -> call(singletonList(file.getKey()), singletonList(metric.getKey())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only component of qualifiers [TRK, APP, VW, SVW] are allowed");
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
