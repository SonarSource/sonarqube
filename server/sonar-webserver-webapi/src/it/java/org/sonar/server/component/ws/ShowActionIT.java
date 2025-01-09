/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.PortfolioData;
import org.sonar.db.component.ProjectData;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.ShowWsResponse;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newDirectoryOnBranch;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_PULL_REQUEST;

public class ShowActionIT {
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone().logIn();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester ws = new WsActionTester(new ShowAction(userSession, db.getDbClient(), TestComponentFinder.from(db),
    new IssueIndexSyncProgressChecker(db.getDbClient())));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.4");
    assertThat(action.description()).isNotNull();
    assertThat(action.responseExample()).isNotNull();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("7.6", "The use of module keys in parameter 'component' is deprecated"),
      tuple("10.1", "The use of module keys in parameter 'component' is removed"));
    assertThat(action.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder("component", "branch", "pullRequest");

    WebService.Param component = action.param(PARAM_COMPONENT);
    assertThat(component.isRequired()).isTrue();
    assertThat(component.description()).isNotNull();
    assertThat(component.exampleValue()).isNotNull();

    WebService.Param branch = action.param(PARAM_BRANCH);
    assertThat(branch.isInternal()).isFalse();
    assertThat(branch.isRequired()).isFalse();
    assertThat(branch.since()).isEqualTo("6.6");

    WebService.Param pullRequest = action.param(PARAM_PULL_REQUEST);
    assertThat(pullRequest.isInternal()).isFalse();
    assertThat(pullRequest.isRequired()).isFalse();
    assertThat(pullRequest.since()).isEqualTo("7.1");
  }

  @Test
  public void json_example() {
    insertJsonExampleComponentsAndSnapshots();

    String response = ws.newRequest()
      .setParam("component", "com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("show-example.json"));
  }

  @Test
  public void tags_displayed_only_for_project() {
    insertJsonExampleComponentsAndSnapshots();

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, "com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
      .execute()
      .getInput();

    assertThat(response).containsOnlyOnce("\"tags\"");
  }

  @Test
  public void show_with_browse_permission() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(mainBranch.getKey());

    assertThat(response.getComponent().getKey()).isEqualTo(mainBranch.getKey());
  }

  @Test
  public void show_with_ancestors_when_not_project() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(mainBranch, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch, directory));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(file.getKey());

    assertThat(response.getComponent().getKey()).isEqualTo(file.getKey());
    assertThat(response.getAncestorsList()).extracting(Component::getKey).containsOnly(directory.getKey(), mainBranch.getKey());
  }

  @Test
  public void show_without_ancestors_when_project() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(mainBranch.getKey());

    assertThat(response.getComponent().getKey()).isEqualTo(mainBranch.getKey());
    assertThat(response.getAncestorsList()).isEmpty();
  }

  @Test
  public void show_with_last_analysis_date() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshots(
      newAnalysis(mainBranch).setCreatedAt(1_000_000_000L).setLast(false),
      newAnalysis(mainBranch).setCreatedAt(2_000_000_000L).setLast(false),
      newAnalysis(mainBranch).setCreatedAt(3_000_000_000L).setLast(true));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(mainBranch.getKey());

    assertThat(response.getComponent().getAnalysisDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_new_code_period_date() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshots(
      newAnalysis(mainBranch).setPeriodDate(1_000_000_000L).setLast(false),
      newAnalysis(mainBranch).setPeriodDate(2_000_000_000L).setLast(false),
      newAnalysis(mainBranch).setPeriodDate(3_000_000_000L).setLast(true));

    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(mainBranch.getKey());

    assertThat(response.getComponent().getLeakPeriodDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_ancestors_and_analysis_date() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(newAnalysis(mainBranch).setCreatedAt(3_000_000_000L).setLast(true));
    ComponentDto directory = db.components().insertComponent(newDirectory(mainBranch, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch, directory));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(file.getKey());

    String expectedDate = formatDateTime(new Date(3_000_000_000L));
    assertThat(response.getAncestorsList()).extracting(Component::getAnalysisDate)
      .containsOnly(expectedDate, expectedDate, expectedDate);
  }

  @Test
  public void should_return_visibility_for_private_project() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse result = newRequest(mainBranch.getKey());
    assertThat(result.getComponent().hasVisibility()).isTrue();
    assertThat(result.getComponent().getVisibility()).isEqualTo("private");
  }

  @Test
  public void should_return_visibility_for_public_project() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto publicProject = projectData.getMainBranchComponent();

    userSession.addProjectBranchMapping(projectData.projectUuid(), projectData.getMainBranchComponent());
    userSession.registerProjects(projectData.getProjectDto());

    ShowWsResponse result = newRequest(publicProject.getKey());
    assertThat(result.getComponent().hasVisibility()).isTrue();
    assertThat(result.getComponent().getVisibility()).isEqualTo("public");
  }

  @Test
  public void should_return_analysis_date_for_portfolio_project() {
    ProjectData project = db.components().insertPrivateProject();
    db.components().insertSnapshot(project.getMainBranchDto(), c -> c.setAnalysisDate(12345L).setCreatedAt(12345L));
    PortfolioData portfolio = db.components().insertPrivatePortfolioData();
    ComponentDto projectSnapshot = db.components().insertComponent(newProjectCopy(project, portfolio));

    userSession.addPortfolioPermission(USER, portfolio.getPortfolioDto());

    ShowWsResponse result = newRequest(projectSnapshot.getKey());
    assertThat(result.getComponent().getAnalysisDate()).isEqualTo(formatDateTime(new Date(12345L)));
  }

  @Test
  public void should_return_visibility_for_portfolio() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    userSession.addPortfolioPermission(USER, view);

    ShowWsResponse result = newRequest(view.getKey());
    assertThat(result.getComponent().hasVisibility()).isTrue();
  }

  @Test
  public void display_version() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(mainBranch, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch, directory));
    db.components().insertSnapshot(mainBranch, s -> s.setProjectVersion("1.1"));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = newRequest(file.getKey());

    assertThat(response.getComponent().getVersion()).isEqualTo("1.1");
    assertThat(response.getAncestorsList())
      .extracting(Component::getVersion)
      .containsOnly("1.1");
  }

  @Test
  public void branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, projectData.getProjectDto());
    String branchKey = "my_branch";
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(branchKey));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    ComponentDto directory = db.components().insertComponent(newDirectoryOnBranch(branch, "dir", mainBranch.uuid()));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch.uuid(), branch, directory));
    db.components().insertSnapshot(branch, s -> s.setProjectVersion("1.1"));

    ShowWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, branchKey)
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getKey, Component::getBranch, Component::getVersion)
      .containsExactlyInAnyOrder(file.getKey(), branchKey, "1.1");
    assertThat(response.getAncestorsList()).extracting(Component::getKey, Component::getBranch, Component::getVersion)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), branchKey, "1.1"),
        tuple(branch.getKey(), branchKey, "1.1"));
  }

  @Test
  public void dont_show_branch_if_main_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    ShowWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_BRANCH, DEFAULT_MAIN_BRANCH_NAME)
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(mainBranch.getKey(), "");
  }

  @Test
  public void pull_request() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, projectData.getProjectDto());
    String pullRequest = "pr-1234";
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(pullRequest).setBranchType(PULL_REQUEST));
    userSession.addProjectBranchMapping(projectData.projectUuid(), branch);
    ComponentDto directory = db.components().insertComponent(newDirectoryOnBranch(branch, "dir", mainBranch.uuid()));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch.uuid(), branch, directory));
    db.components().insertSnapshot(branch, s -> s.setProjectVersion("1.1"));

    ShowWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequest)
      .executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getKey, Component::getPullRequest, Component::getVersion)
      .containsExactlyInAnyOrder(file.getKey(), pullRequest, "1.1");
    assertThat(response.getAncestorsList()).extracting(Component::getKey, Component::getPullRequest, Component::getVersion)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), pullRequest, "1.1"),
        tuple(branch.getKey(), pullRequest, "1.1"));
  }

  @Test
  public void verify_need_issue_sync_pr() {
    ComponentDto portfolio1 = db.components().insertPublicPortfolio();
    ComponentDto portfolio2 = db.components().insertPublicPortfolio();
    ComponentDto subview = db.components().insertSubView(portfolio1);

    String pullRequestKey1 = secure().nextAlphanumeric(100);
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ComponentDto branch1 = db.components().insertProjectBranch(project1, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey1)
      .setNeedIssueSync(true));
    ComponentDto directory = db.components().insertComponent(newDirectoryOnBranch(branch1, "dir", project1.uuid()));
    ComponentDto file = db.components().insertComponent(newFileDto(project1.uuid(), branch1, directory));
    userSession.addProjectBranchMapping(projectData1.projectUuid(), projectData1.getMainBranchComponent());
    userSession.addProjectBranchMapping(projectData1.projectUuid(), branch1);

    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    String branchName2 = secure().nextAlphanumeric(248);
    ComponentDto branch2 = db.components().insertProjectBranch(project2, b -> b.setBranchType(BRANCH).setNeedIssueSync(true).setKey(branchName2));
    String branchName3 = secure().nextAlphanumeric(248);
    ComponentDto branch3 = db.components().insertProjectBranch(project2, b -> b.setBranchType(BRANCH).setNeedIssueSync(false).setKey(branchName3));
    userSession.addProjectBranchMapping(projectData2.projectUuid(), projectData2.getMainBranchComponent());
    userSession.addProjectBranchMapping(projectData2.projectUuid(), branch2);
    userSession.addProjectBranchMapping(projectData2.projectUuid(), branch3);

    ProjectData projectData3 = db.components().insertPrivateProject();
    ComponentDto project3 = projectData3.getMainBranchComponent();
    String pullRequestKey4 = secure().nextAlphanumeric(100);
    ComponentDto branch4 = db.components().insertProjectBranch(project3, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey4).setNeedIssueSync(false));
    ComponentDto directoryOfBranch4 = db.components().insertComponent(newDirectoryOnBranch(branch4, "dir", project3.uuid()));
    ComponentDto fileOfBranch4 = db.components().insertComponent(newFileDto(project3.uuid(), branch4, directoryOfBranch4));
    String branchName5 = secure().nextAlphanumeric(248);
    ComponentDto branch5 = db.components().insertProjectBranch(project3, b -> b.setBranchType(BRANCH).setNeedIssueSync(false).setKey(branchName5));
    userSession.addProjectBranchMapping(projectData3.projectUuid(), projectData3.getMainBranchComponent());
    userSession.addProjectBranchMapping(projectData3.projectUuid(), branch4);
    userSession.addProjectBranchMapping(projectData3.projectUuid(), branch5);

    userSession.addProjectPermission(UserRole.USER, projectData1.getProjectDto(), projectData2.getProjectDto(), projectData3.getProjectDto())
        .registerBranches(projectData1.getMainBranchDto(), projectData2.getMainBranchDto(), projectData3.getMainBranchDto());
    userSession.registerPortfolios(portfolio1, portfolio2, subview);
    userSession.registerProjects(projectData1.getProjectDto(), projectData2.getProjectDto(), projectData3.getProjectDto());

    // for portfolios, sub-views need issue sync flag is set to true if any project need sync
    assertNeedIssueSyncEqual(null, null, portfolio1, true);
    assertNeedIssueSyncEqual(null, null, subview, true);
    assertNeedIssueSyncEqual(null, null, portfolio2, true);

    // if branch need sync it is propagated to other components
    assertNeedIssueSyncEqual(null, null, project1, true);
    assertNeedIssueSyncEqual(pullRequestKey1, null, branch1, true);
    assertNeedIssueSyncEqual(pullRequestKey1, null, directory, true);
    assertNeedIssueSyncEqual(pullRequestKey1, null, file, true);

    assertNeedIssueSyncEqual(null, null, project2, true);
    assertNeedIssueSyncEqual(null, branchName2, branch2, true);
    assertNeedIssueSyncEqual(null, branchName3, branch3, true);

    // if all branches are synced, need issue sync on project is is set to false
    assertNeedIssueSyncEqual(null, null, project3, false);
    assertNeedIssueSyncEqual(pullRequestKey4, null, branch4, false);
    assertNeedIssueSyncEqual(pullRequestKey4, null, directoryOfBranch4, false);
    assertNeedIssueSyncEqual(pullRequestKey4, null, fileOfBranch4, false);
    assertNeedIssueSyncEqual(null, branchName5, branch5, false);
  }

  private void assertNeedIssueSyncEqual(@Nullable String pullRequest, @Nullable String branch, ComponentDto component, boolean needIssueSync) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_COMPONENT, component.getKey());

    Optional.ofNullable(pullRequest).ifPresent(pr -> testRequest.setParam(PARAM_PULL_REQUEST, pr));
    Optional.ofNullable(branch).ifPresent(br -> testRequest.setParam(PARAM_BRANCH, br));

    ShowWsResponse response = testRequest.executeProtobuf(ShowWsResponse.class);

    assertThat(response.getComponent())
      .extracting(Component::getNeedIssueSync)
      .isEqualTo(needIssueSync);
  }

  @Test
  public void throw_ForbiddenException_if_user_doesnt_have_browse_permission_on_project() {
    ComponentDto componentDto = newPrivateProjectDto("project-uuid");
    db.components().insertProjectAndSnapshot(componentDto);

    String componentDtoDbKey = componentDto.getKey();
    assertThatThrownBy(() -> newRequest(componentDtoDbKey))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_component_does_not_exist() {
    assertThatThrownBy(() -> newRequest("unknown-key"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'unknown-key' not found");
  }

  @Test
  public void fail_if_component_is_removed() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    db.components().insertComponent(newFileDto(mainBranch).setKey("file-key").setEnabled(false));

    assertThatThrownBy(() -> newRequest("file-key"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'file-key' not found");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch));
    userSession.addProjectPermission(UserRole.USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    db.components().insertProjectBranch(mainBranch, b -> b.setKey("my_branch"));

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, "another_branch");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage(String.format("Component '%s' on branch '%s' not found", file.getKey(), "another_branch"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void return_isAiCodeFixEnabled(boolean isAiCodeFixEnabled) {
    var projectData = db.components().insertPrivateProject(ComponentDbTester.defaults(),
      p -> p.setAiCodeFixEnabled(isAiCodeFixEnabled));
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());

    var response = newRequest(projectData.projectKey());

    assertThat(response.getComponent().getIsAiCodeFixEnabled()).isEqualTo(isAiCodeFixEnabled);
  }

  private ShowWsResponse newRequest(@Nullable String key) {
    TestRequest request = ws.newRequest();
    if (key != null) {
      request.setParam(PARAM_COMPONENT, key);
    }
    return request.executeProtobuf(ShowWsResponse.class);
  }

  private void insertJsonExampleComponentsAndSnapshots() {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setUuid("AVIF98jgA3Ax6PH2efOW")
        .setBranchUuid("AVIF98jgA3Ax6PH2efOW")
        .setKey("com.sonarsource:java-markdown")
        .setName("Java Markdown")
        .setDescription("Java Markdown Project")
        .setQualifier(ComponentQualifiers.PROJECT),
      p -> p.setTagsString("language, plugin"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(USER, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto());
    db.components().insertSnapshot(mainBranch, snapshot -> snapshot
      .setProjectVersion("1.1")
      .setCreatedAt(parseDateTime("2017-03-01T11:39:03+0100").getTime())
      .setPeriodDate(parseDateTime("2017-01-01T11:39:03+0100").getTime()));
    ComponentDto directory = newDirectory(mainBranch, "AVIF-FfgA3Ax6PH2efPF", "src/main/java/com/sonarsource/markdown/impl")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setName("src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(ComponentQualifiers.DIRECTORY);
    db.components().insertComponent(directory);
    db.components().insertComponent(
      newFileDto(directory, directory, "AVIF-FffA3Ax6PH2efPD")
        .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setName("Rule.java")
        .setPath("src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setLanguage("java")
        .setQualifier(ComponentQualifiers.FILE));
  }
}
