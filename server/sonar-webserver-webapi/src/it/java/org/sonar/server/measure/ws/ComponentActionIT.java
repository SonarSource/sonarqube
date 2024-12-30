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

import com.google.gson.Gson;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.PortfolioData;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Measures;
import org.sonarqube.ws.Measures.Component;
import org.sonarqube.ws.Measures.ComponentWsResponse;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_ISSUES;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_ADDITIONAL_FIELDS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_COMPONENT;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_METRIC_KEYS;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.Common.ImpactSeverity.HIGH;
import static org.sonarqube.ws.Common.ImpactSeverity.LOW;
import static org.sonarqube.ws.Common.ImpactSeverity.MEDIUM;

public class ComponentActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester ws = new WsActionTester(new ComponentAction(db.getDbClient(), TestComponentFinder.from(db), userSession));

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("5.4");
    assertThat(def.params()).extracting(Param::key)
      .containsExactlyInAnyOrder("component", "branch", "pullRequest", "metricKeys", "additionalFields");

    WebService.Param component = def.param(PARAM_COMPONENT);
    assertThat(component.isRequired()).isTrue();

    WebService.Param branch = def.param("branch");
    assertThat(branch.since()).isEqualTo("6.6");
    assertThat(branch.isInternal()).isFalse();
    assertThat(branch.isRequired()).isFalse();
  }

  @Test
  public void provided_project() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    ComponentWsResponse response = newRequest(mainBranch.getKey(), metric.getKey());

    assertThat(response.getMetrics().getMetricsCount()).isOne();
    assertThat(response.hasPeriod()).isFalse();
    assertThat(response.getComponent().getKey()).isEqualTo(mainBranch.getKey());
  }

  @Test
  public void without_additional_fields() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    db.components().insertSnapshot(mainBranch);
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, metric.getKey())
      .execute().getInput();

    assertThat(response)
      .doesNotContain("period")
      .doesNotContain("metrics");
  }

  @Test
  public void branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    String branchName = "my_branch";
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(branchName));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    db.components().insertSnapshot(branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    MetricDto complexity = db.measures().insertMetric(m1 -> m1.setKey("complexity").setValueType("INT"));
    MeasureDto measure = db.measures().insertMeasure(file, m -> m.addValue(complexity.getKey(), 12.0d));

    ComponentWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .setParam(PARAM_METRIC_KEYS, complexity.getKey())
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(file.getKey(), branchName);
    assertThat(response.getComponent().getMeasuresList())
      .extracting(Measures.Measure::getMetric, m -> parseDouble(m.getValue()))
      .containsExactlyInAnyOrder(tuple(complexity.getKey(), measure.getDouble(complexity.getKey())));
  }

  @Test
  public void branch_not_set_if_main_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    MetricDto complexity = db.measures().insertMetric(m1 -> m1.setKey("complexity").setValueType("INT"));

    ComponentWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, DEFAULT_MAIN_BRANCH_NAME)
      .setParam(PARAM_METRIC_KEYS, "complexity")
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(file.getKey(), "");
  }

  @Test
  public void pull_request() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto());
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey("pr-123").setBranchType(PULL_REQUEST));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    MetricDto complexity = db.measures().insertMetric(m1 -> m1.setKey("complexity").setValueType("INT"));
    MeasureDto measure = db.measures().insertMeasure(file, m -> m.addValue(complexity.getKey(), 12.0d));

    ComponentWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_PULL_REQUEST, "pr-123")
      .setParam(PARAM_METRIC_KEYS, complexity.getKey())
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent()).extracting(Component::getKey, Component::getPullRequest)
      .containsExactlyInAnyOrder(file.getKey(), "pr-123");
    assertThat(response.getComponent().getMeasuresList())
      .extracting(Measures.Measure::getMetric, m -> parseDouble(m.getValue()))
      .containsExactlyInAnyOrder(tuple(complexity.getKey(), measure.getDouble(complexity.getKey())));
  }

  @Test
  public void reference_key_in_the_response() {
    ProjectData mainBranch = db.components().insertPrivateProject();
    PortfolioData view = db.components().insertPrivatePortfolioData();
    userSession.addProjectPermission(USER, view.getRootComponent());
    db.components().insertSnapshot(view.getPortfolioDto());
    ComponentDto projectCopy = db.components().insertComponent(newProjectCopy("project-uuid-copy", mainBranch.getMainBranchComponent(), view.getRootComponent()));
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    ComponentWsResponse response = newRequest(projectCopy.getKey(), metric.getKey());

    assertThat(response.getComponent().getRefKey()).isEqualTo(mainBranch.getMainBranchComponent().getKey());
  }

  @Test
  public void use_deprecated_component_id_parameter() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    ComponentWsResponse response = ws.newRequest()
      .setParam("component", mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, metric.getKey())
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent().getKey()).isEqualTo(mainBranch.getKey());
  }

  @Test
  public void metric_without_a_domain() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    MetricDto metricWithoutDomain = db.measures().insertMetric(m -> m
      .setValueType("INT")
      .setDomain(null));
    db.measures().insertMeasure(mainBranch, m -> m.addValue(metricWithoutDomain.getKey(), 123));

    ComponentWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_METRIC_KEYS, metricWithoutDomain.getKey())
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent().getMeasuresList()).extracting(Measures.Measure::getMetric).containsExactly(metricWithoutDomain.getKey());
    Common.Metric responseMetric = response.getMetrics().getMetrics(0);
    assertThat(responseMetric.getKey()).isEqualTo(metricWithoutDomain.getKey());
    assertThat(responseMetric.hasDomain()).isFalse();
  }

  @Test
  public void use_best_values() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    MetricDto metric = db.measures().insertMetric(m -> m
      .setValueType("INT")
      .setBestValue(7.0d)
      .setOptimizedBestValue(true)
      .setDomain(null));

    // add any measure for the component
    db.measures().insertMeasure(file);

    ComponentWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_METRIC_KEYS, metric.getKey())
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics")
      .executeProtobuf(ComponentWsResponse.class);

    assertThat(response.getComponent().getMeasuresList())
      .extracting(Measures.Measure::getMetric, Measures.Measure::getValue, Measures.Measure::getBestValue)
      .containsExactly(tuple(metric.getKey(), "7", true));
  }

  @Test
  public void fail_when_a_metric_is_not_found() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
        .registerBranches(projectData.getMainBranchDto());
    db.components().insertSnapshot(mainBranch);
    db.measures().insertMetric(m -> m.setKey("ncloc").setValueType("INT"));
    db.measures().insertMetric(m -> m.setKey("complexity").setValueType("INT"));

    assertThatThrownBy(() -> newRequest(mainBranch.getKey(), "ncloc, complexity, unknown-metric, another-unknown-metric"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("The following metric keys are not found: unknown-metric, another-unknown-metric");
  }

  @Test
  public void fail_when_empty_metric_keys_parameter() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto());
    db.components().insertSnapshot(mainBranch);

    assertThatThrownBy(() -> newRequest(mainBranch.getKey(), ""))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("At least one metric key must be provided");
  }

  @Test
  public void fail_when_not_enough_permission() {
    userSession.logIn();
    ComponentDto mainBranch = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    assertThatThrownBy(() -> newRequest(mainBranch.getKey(), metric.getKey()))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_component_does_not_exist() {
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, "project-key")
        .setParam(PARAM_METRIC_KEYS, metric.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'project-key' not found");
  }

  @Test
  public void fail_when_component_is_removed() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setEnabled(false));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto());
    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("INT"));

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam(PARAM_COMPONENT, mainBranch.getKey())
        .setParam(PARAM_METRIC_KEYS, metric.getKey())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component key '%s' not found", mainBranch.getKey()));
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    userSession.addProjectPermission(USER, projectData.getProjectDto());
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
  public void should_return_data_type_measure() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT").setName("My Project"));
    userSession.addProjectPermission(USER, projectData.getProjectDto()).registerBranches(projectData.getMainBranchDto());
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    SnapshotDto analysis = db.components().insertSnapshot(mainBranch, s -> s.setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));

    MetricDto metric = db.measures().insertMetric(m -> m.setValueType("DATA")
      .setShortName(RELIABILITY_ISSUES.getName())
      .setKey(RELIABILITY_ISSUES.getKey())
      .setBestValue(null)
      .setWorstValue(null));

    Map<String, Long> reliabilityIssuesMap = Map.of(HIGH.name(), 1L, MEDIUM.name(), 2L, LOW.name(), 3L, "total", 6L);
    String expectedJson = new Gson().toJson(reliabilityIssuesMap);
    db.measures().insertMeasure(mainBranch, m -> m.addValue(metric.getKey(), expectedJson));

    db.commit();

    ComponentWsResponse response = newRequest(projectData.projectKey(), RELIABILITY_ISSUES.getKey());
    String json = response.getComponent().getMeasures(0).getValue();

    assertThat(json).isEqualTo(expectedJson);
  }

  @Test
  public void shouldReturnRenamedMetric() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("MY_PROJECT")
      .setName("My Project"));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    SnapshotDto analysis = db.components().insertSnapshot(mainBranch, s -> s.setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
      .setPeriodMode("previous_version")
      .setPeriodParam("1.0-SNAPSHOT"));

    MetricDto accepted_issues = insertAcceptedIssuesMetric();
    db.measures().insertMeasure(mainBranch, m -> m.addValue(accepted_issues.getKey(), 10d));

    db.commit();

    ComponentWsResponse response = newRequest(projectData.projectKey(), "wont_fix_issues");
    assertThat(response.getMetrics().getMetrics(0).getKey()).isEqualTo("wont_fix_issues");
  }

  @Test
  public void json_example() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    SnapshotDto analysis = db.components().insertSnapshot(mainBranch,
      s -> s.setPeriodDate(parseDateTime("2016-01-11T10:49:50+0100").getTime())
        .setPeriodMode("previous_version")
        .setPeriodParam("1.0-SNAPSHOT"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch)
      .setKey("MY_PROJECT:ElementImpl.java")
      .setName("ElementImpl.java")
      .setLanguage("java")
      .setPath("src/main/java/com/sonarsource/markdown/impl/ElementImpl.java"));

    MetricDto complexity = db.measures().insertMetric(m -> m.setKey("complexity")
      .setShortName("Complexity")
      .setDescription("Cyclomatic complexity")
      .setDomain("Complexity")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false));
    db.measures().insertMeasure(file, m -> m.addValue(complexity.getKey(), 12.0d));

    MetricDto ncloc = db.measures().insertMetric(m1 -> m1.setKey("ncloc")
      .setShortName("Lines of code")
      .setDescription("Non Commenting Lines of Code")
      .setDomain("Size")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false));
    db.measures().insertMeasure(file, m -> m.addValue(ncloc.getKey(), 114.0d));

    MetricDto newViolations = db.measures().insertMetric(m -> m.setKey("new_violations")
      .setShortName("New issues")
      .setDescription("New Issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(true)
      .setHidden(false));
    db.measures().insertMeasure(file, m -> m.addValue(newViolations.getKey(), 25.0d));

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_METRIC_KEYS, "ncloc, complexity, new_violations")
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,period")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("component-example.json"));
  }

  private MetricDto insertAcceptedIssuesMetric() {
    MetricDto acceptedIssues = db.measures().insertMetric(m -> m.setKey("accepted_issues")
      .setShortName("Accepted Issues")
      .setDescription("Accepted issues")
      .setDomain("Issues")
      .setValueType("INT")
      .setDirection(-1)
      .setQualitative(false)
      .setHidden(false));
    db.commit();
    return acceptedIssues;
  }


  private ComponentWsResponse newRequest(String componentKey, String metricKeys) {
    return ws.newRequest()
      .setParam(PARAM_COMPONENT, componentKey)
      .setParam(PARAM_METRIC_KEYS, metricKeys)
      .setParam(PARAM_ADDITIONAL_FIELDS, "metrics,period")
      .executeProtobuf(ComponentWsResponse.class);
  }
}
