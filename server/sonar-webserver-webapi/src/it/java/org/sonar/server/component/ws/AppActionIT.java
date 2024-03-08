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
package org.sonar.server.component.ws;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.test.JsonAssert.assertJson;

public class AppActionIT {

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create();

  private final ComponentViewerJsonWriter componentViewerJsonWriter = new ComponentViewerJsonWriter(db.getDbClient());
  private final WsActionTester ws = new WsActionTester(new AppAction(db.getDbClient(), userSession,
    TestComponentFinder.from(db), componentViewerJsonWriter));
  private ProjectData projectData;
  private ComponentDto mainBranchComponent;

  @Before
  public void setup() {
    projectData = db.components().insertPrivateProject();
    mainBranchComponent = projectData.getMainBranchComponent();
  }
  @Test
  public void file_info() {
    ComponentDto directory = db.components().insertComponent(newDirectory(mainBranchComponent, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent, directory));
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"project\": \"" + projectData.getProjectDto().getKey() + "\",\n" +
      "  \"projectName\": \"" + projectData.getProjectDto().getName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void file_on_module() {
    ComponentDto directory = db.components().insertComponent(newDirectory(mainBranchComponent, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent, directory));
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"project\": \"" + projectData.getProjectDto().getKey() + "\",\n" +
      "  \"projectName\": \"" + projectData.getProjectDto().getName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void file_without_measures() {
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent));
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void file_with_measures() {
    ComponentDto directory = db.components().insertComponent(newDirectory(mainBranchComponent, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent, directory));
    MetricDto lines = db.measures().insertMetric(m -> m.setKey(LINES_KEY));
    db.measures().insertLiveMeasure(file, lines, m -> m.setValue(200d));
    MetricDto duplicatedLines = db.measures().insertMetric(m -> m.setKey(DUPLICATED_LINES_DENSITY_KEY));
    db.measures().insertLiveMeasure(file, duplicatedLines, m -> m.setValue(7.4));
    MetricDto tests = db.measures().insertMetric(m -> m.setKey(TESTS_KEY));
    db.measures().insertLiveMeasure(file, tests, m -> m.setValue(3d));
    MetricDto technicalDebt = db.measures().insertMetric(m -> m.setKey(TECHNICAL_DEBT_KEY));
    db.measures().insertLiveMeasure(file, technicalDebt, m -> m.setValue(182d));
    MetricDto issues = db.measures().insertMetric(m -> m.setKey(VIOLATIONS_KEY));
    db.measures().insertLiveMeasure(file, issues, m -> m.setValue(231d));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(95.4d));
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());


    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"measures\": {\n" +
      "    \"lines\": \"200.0\",\n" +
      "    \"coverage\": \"95.4\",\n" +
      "    \"duplicationDensity\": \"7.4\",\n" +
      "    \"issues\": \"231.0\",\n" +
      "    \"tests\": \"3.0\"\n" +
      "  }" +
      "}\n");
  }

  @Test
  public void get_by_component() {
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent, mainBranchComponent));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(95.4d));
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"measures\": {\n" +
      "    \"coverage\": \"95.4\"\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void canMarkAsFavorite_is_true_when_logged() {
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", mainBranchComponent.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "}\n");
  }

  @Test
  public void canMarkAsFavorite_is_false_when_not_logged() {
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", mainBranchComponent.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"canMarkAsFavorite\": false,\n" +
      "}\n");
  }

  @Test
  public void component_is_favorite() {
    BranchDto mainBranch = projectData.getMainBranchDto();
    ProjectDto project = projectData.getProjectDto();
    userSession.logIn("john")
      .addProjectPermission(USER, project)
      .registerBranches(mainBranch);
    db.favorites().add(project, userSession.getUuid(), userSession.getLogin());

    String result = ws.newRequest()
      .setParam("component", project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"fav\": true,\n" +
      "}\n");
  }

  @Test
  public void component_is_not_favorite() {
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    String result = ws.newRequest()
      .setParam("component", mainBranchComponent.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"fav\": false,\n" +
      "}\n");
  }

  @Test
  public void branch() {
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto());
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(mainBranchComponent, b -> b.setKey(branchName));
    userSession.addProjectBranchMapping(projectData.getProjectDto().getUuid(), branch);
    ComponentDto directory = db.components().insertComponent(newDirectory(branch, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent.uuid(), branch, directory));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    db.measures().insertLiveMeasure(file, coverage, m -> m.setValue(95.4d));

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", branchName)
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"branch\": \"" + branchName + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"project\": \"" + projectData.getProjectDto().getKey() + "\",\n" +
      "  \"projectName\": \"" + projectData.getProjectDto().getName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {\n" +
      "    \"coverage\": \"95.4\"\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void fail_if_no_parameter_provided() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'component' parameter is missing");
  }

  @Test
  public void component_and_branch_parameters_provided() {
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto());
    String branchName = randomAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(mainBranchComponent, b -> b.setKey(branchName));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranchComponent.uuid()));

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", branchName)
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"branch\": \"" + branchName + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"project\": \"" + projectData.getProjectDto().getKey() + "\",\n" +
      "  \"projectName\": \"" + projectData.getProjectDto().getName() + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void component_and_pull_request_parameters_provided() {
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    String pullRequestKey = RandomStringUtils.randomAlphanumeric(100);
    ComponentDto branch = db.components().insertProjectBranch(mainBranchComponent, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranchComponent.uuid()));

    String result = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("pullRequest", pullRequestKey)
      .execute()
      .getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"key\": \"" + file.getKey() + "\",\n" +
      "  \"uuid\": \"" + file.uuid() + "\",\n" +
      "  \"path\": \"" + file.path() + "\",\n" +
      "  \"name\": \"" + file.name() + "\",\n" +
      "  \"longName\": \"" + file.longName() + "\",\n" +
      "  \"q\": \"" + file.qualifier() + "\",\n" +
      "  \"project\": \"" + projectData.getProjectDto().getKey() + "\",\n" +
      "  \"projectName\": \"" + projectData.getProjectDto().getName() + "\",\n" +
      "  \"pullRequest\": \"" + pullRequestKey + "\",\n" +
      "  \"fav\": false,\n" +
      "  \"canMarkAsFavorite\": true,\n" +
      "  \"measures\": {}\n" +
      "}\n");
  }

  @Test
  public void fail_if_component_and_pull_request_and_branch_parameters_provided() {
    userSession.logIn("john").addProjectPermission(USER, projectData.getProjectDto());
    ComponentDto branch = db.components().insertProjectBranch(mainBranchComponent, b -> b.setBranchType(PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranchComponent.uuid()));

    TestRequest request = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", "unknown_branch")
      .setParam("pullRequest", "unknown_component");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Either branch or pull request can be provided, not both");
  }

  @Test
  public void fail_when_component_not_found() {
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent));

    TestRequest request = ws.newRequest()
      .setParam("component", "unknown");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_branch_not_found() {
    ComponentDto branch = db.components().insertProjectBranch(mainBranchComponent);
    ComponentDto file = db.components().insertComponent(newFileDto(branch, mainBranchComponent.uuid()));

    TestRequest request = ws.newRequest()
      .setParam("component", file.getKey())
      .setParam("branch", "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_missing_permission() {
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranchComponent));

    TestRequest request = ws.newRequest()
      .setParam("component", file.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void define_app_action() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotNull();

    assertThat(action.params()).hasSize(3);

    WebService.Param paramComponent = action.param(AppAction.PARAM_COMPONENT);
    assertThat(paramComponent).isNotNull();
    assertThat(paramComponent.isRequired()).isTrue();

    WebService.Param paramBranch = action.param(MeasuresWsParameters.PARAM_BRANCH);
    assertThat(paramBranch).isNotNull();
    assertThat(paramBranch.isRequired()).isFalse();

    WebService.Param paramPullRequest = action.param(MeasuresWsParameters.PARAM_PULL_REQUEST);
    assertThat(paramPullRequest).isNotNull();
    assertThat(paramPullRequest.isRequired()).isFalse();
  }

}
