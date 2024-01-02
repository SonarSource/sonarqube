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
package org.sonar.server.ce.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.AnalysisStatusWsResponse.Warning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_BRANCH;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.test.JsonAssert.assertJson;

public class AnalysisStatusActionIT {
  private static final String BRANCH_WITH_WARNING = "feature-with-warning";
  private static final String BRANCH_WITHOUT_WARNING = "feature-without-warning";
  private static final String PULL_REQUEST = "pr1";

  private static final String WARNING_IN_MAIN = "warning in main";
  private static final String WARNING_IN_BRANCH = "warning in branch";
  private static final String WARNING_IN_PR = "warning in pr";

  private static int counter = 1;

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  WsActionTester ws = new WsActionTester(new AnalysisStatusAction(userSession, dbClient, TestComponentFinder.from(db)));

  @Test
  public void no_errors_no_warnings() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList()).isEmpty();
  }

  @Test
  public void allows_unauthenticated_access() {
    ProjectData projectData = db.components().insertPublicProject();
    ProjectDto project = projectData.getProjectDto();
    userSession.registerProjects(project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, projectData.getMainBranchDto(), SUCCESS, analysis, REPORT);
    createTaskMessage(activity, WARNING_IN_MAIN);
    createTaskMessage(activity, "Dismissible warning", MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList()).hasSize(2);
  }

  @Test
  public void return_warnings_for_last_analysis_of_main() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto project = projectData.getProjectDto();
    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, projectData.getMainBranchDto(), SUCCESS, analysis, REPORT);
    CeTaskMessageDto taskMessage = createTaskMessage(activity, WARNING_IN_MAIN);
    CeTaskMessageDto taskMessageDismissible = createTaskMessage(activity, "Dismissible warning", MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList())
      .extracting(Warning::getKey, Warning::getMessage, Warning::getDismissable)
      .containsExactly(
        tuple(taskMessage.getUuid(), WARNING_IN_MAIN, false),
        tuple(taskMessageDismissible.getUuid(), taskMessageDismissible.getMessage(), true));

    SnapshotDto analysis2 = db.components().insertSnapshot(project);
    insertActivity("task-uuid" + counter++, projectData.getMainBranchDto(), SUCCESS, analysis2, REPORT);
    insertActivity("task-uuid" + counter++, projectData.getMainBranchDto(), SUCCESS, null, "PROJECT_EXPORT");

    Ce.AnalysisStatusWsResponse response2 = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response2.getComponent().getWarningsList()).isEmpty();
  }

  @Test
  public void return_warnings_for_last_analysis_of_branch() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    BranchDto branch = db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITH_WARNING));
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, branch, SUCCESS, analysis, REPORT);
    CeTaskMessageDto taskMessage = createTaskMessage(activity, WARNING_IN_BRANCH);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITH_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList())
      .extracting(Warning::getKey, Warning::getMessage, Warning::getDismissable)
      .containsExactly(tuple(taskMessage.getUuid(), WARNING_IN_BRANCH, false));

    SnapshotDto analysis2 = db.components().insertSnapshot(branch);
    insertActivity("task-uuid" + counter++, branch, SUCCESS, analysis2, REPORT);
    insertActivity("task-uuid" + counter++, branch, SUCCESS, null, "PROJECT_EXPORT");

    Ce.AnalysisStatusWsResponse response2 = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITH_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response2.getComponent().getWarningsList()).isEmpty();
  }

  @Test
  public void return_warnings_for_last_analysis_of_pull_request() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    BranchDto pullRequest = db.components().insertProjectBranch(project, b -> {
      b.setBranchType(BranchType.PULL_REQUEST);
      b.setKey(PULL_REQUEST);
    });
    SnapshotDto analysis = db.components().insertSnapshot(pullRequest);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, pullRequest, SUCCESS, analysis, REPORT);
    CeTaskMessageDto taskMessage = createTaskMessage(activity, WARNING_IN_PR);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_PULL_REQUEST, PULL_REQUEST)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList())
      .extracting(Warning::getKey, Warning::getMessage, Warning::getDismissable)
      .containsExactly(tuple(taskMessage.getUuid(), WARNING_IN_PR, false));

    SnapshotDto analysis2 = db.components().insertSnapshot(pullRequest);
    insertActivity("task-uuid" + counter++, pullRequest, SUCCESS, analysis2, REPORT);
    insertActivity("task-uuid" + counter++, pullRequest, SUCCESS, null, "PROJECT_EXPORT");

    Ce.AnalysisStatusWsResponse response2 = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_PULL_REQUEST, PULL_REQUEST)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response2.getComponent().getWarningsList()).isEmpty();
  }

  @Test
  public void return_warnings_per_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto project = projectData.getProjectDto();
    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, projectData.getMainBranchDto(), SUCCESS, analysis, REPORT);
    CeTaskMessageDto warningInMainMessage = createTaskMessage(activity, WARNING_IN_MAIN);

    BranchDto branchWithWarning = db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITH_WARNING));
    SnapshotDto branchAnalysis = db.components().insertSnapshot(branchWithWarning);
    CeActivityDto branchActivity = insertActivity("task-uuid" + counter++, branchWithWarning, SUCCESS, branchAnalysis, REPORT);
    CeTaskMessageDto warningInBranchMessage = createTaskMessage(branchActivity, WARNING_IN_BRANCH);

    BranchDto branchWithoutWarning = db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITHOUT_WARNING));
    SnapshotDto branchWithoutWarningAnalysis = db.components().insertSnapshot(branchWithoutWarning);
    insertActivity("task-uuid" + counter++, branchWithoutWarning, SUCCESS, branchWithoutWarningAnalysis, REPORT);

    BranchDto pullRequest = db.components().insertProjectBranch(project, b -> {
      b.setBranchType(BranchType.PULL_REQUEST);
      b.setKey(PULL_REQUEST);
    });
    SnapshotDto prAnalysis = db.components().insertSnapshot(pullRequest);
    CeActivityDto prActivity = insertActivity("task-uuid" + counter++, pullRequest, SUCCESS, prAnalysis, REPORT);
    CeTaskMessageDto warningInPrMessage = createTaskMessage(prActivity, WARNING_IN_PR);

    Ce.AnalysisStatusWsResponse responseForMain = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForMain.getComponent().getWarningsList())
      .extracting(Warning::getKey, Warning::getMessage, Warning::getDismissable)
      .containsExactly(tuple(warningInMainMessage.getUuid(), WARNING_IN_MAIN, false));

    Ce.AnalysisStatusWsResponse responseForBranchWithWarning = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITH_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForBranchWithWarning.getComponent().getWarningsList())
      .extracting(Warning::getKey, Warning::getMessage, Warning::getDismissable)
      .containsExactly(tuple(warningInBranchMessage.getUuid(), WARNING_IN_BRANCH, false));

    Ce.AnalysisStatusWsResponse responseForBranchWithoutWarning = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITHOUT_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForBranchWithoutWarning.getComponent().getWarningsList()).isEmpty();

    Ce.AnalysisStatusWsResponse responseForPr = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_PULL_REQUEST, PULL_REQUEST)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForPr.getComponent().getWarningsList())
      .extracting(Warning::getKey, Warning::getMessage, Warning::getDismissable)
      .containsExactly(tuple(warningInPrMessage.getUuid(), WARNING_IN_PR, false));
  }

  @Test
  public void response_contains_branch_or_pullRequest_for_branch_or_pullRequest_only() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITHOUT_WARNING));

    db.components().insertProjectBranch(project, b -> {
      b.setBranchType(BranchType.PULL_REQUEST);
      b.setKey(PULL_REQUEST);
    });

    Ce.AnalysisStatusWsResponse responseForMain = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForMain.getComponent().hasBranch()).isFalse();
    assertThat(responseForMain.getComponent().hasPullRequest()).isFalse();

    Ce.AnalysisStatusWsResponse responseForBranchWithoutWarning = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITHOUT_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForBranchWithoutWarning.getComponent().getBranch()).isEqualTo(BRANCH_WITHOUT_WARNING);
    assertThat(responseForBranchWithoutWarning.getComponent().hasPullRequest()).isFalse();

    Ce.AnalysisStatusWsResponse responseForPr = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_PULL_REQUEST, PULL_REQUEST)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForPr.getComponent().hasBranch()).isFalse();
    assertThat(responseForPr.getComponent().getPullRequest()).isEqualTo(PULL_REQUEST);
  }

  @Test
  public void json_example() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setKey("com.github.kevinsawicki:http-request-parent")
      .setName("HttpRequest"));
    ProjectDto project = projectData.getProjectDto();
    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, projectData.getMainBranchDto(), SUCCESS, analysis, REPORT);
    CeTaskMessageDto ceTaskMessage = new CeTaskMessageDto()
      .setUuid("AU-Tpxb--iU5OvuD2FLy")
      .setTaskUuid(activity.getUuid())
      .setMessage("Property \"sonar.jacoco.reportPaths\" is no longer supported. Use JaCoCo xml report and sonar-jacoco plugin.")
      .setType(MessageType.GENERIC)
      .setCreatedAt(counter);
    db.getDbClient().ceTaskMessageDao().insert(db.getSession(), ceTaskMessage);
    db.commit();

    userSession.logIn().setSystemAdministrator().addProjectPermission(UserRole.USER, project);

    String result = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(getClass().getResource("analysis_status-example.json"));
  }

  @Test
  public void fail_if_component_key_not_provided() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_component_key_is_unknown() {
    TestRequest request = ws.newRequest().setParam(PARAM_COMPONENT, "nonexistent");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_if_both_branch_and_pullRequest_are_specified() {
    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, "dummy")
      .setParam(PARAM_BRANCH, "feature1")
      .setParam(PARAM_PULL_REQUEST, "pr1");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class);
  }

  private CeTaskMessageDto createTaskMessage(CeActivityDto activity, String warning) {
    return createTaskMessage(activity, warning, MessageType.GENERIC);
  }

  private CeTaskMessageDto createTaskMessage(CeActivityDto activity, String warning, MessageType messageType) {
    CeTaskMessageDto ceTaskMessageDto = new CeTaskMessageDto()
      .setUuid("m-uuid-" + counter++)
      .setTaskUuid(activity.getUuid())
      .setMessage(warning)
      .setType(messageType)
      .setCreatedAt(counter);
    db.getDbClient().ceTaskMessageDao().insert(db.getSession(), ceTaskMessageDto);
    db.commit();
    return ceTaskMessageDto;
  }

  private CeActivityDto insertActivity(String taskUuid, BranchDto branch, CeActivityDto.Status status, @Nullable SnapshotDto analysis, String taskType) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(taskType);
    queueDto.setComponentUuid(branch.getUuid());
    queueDto.setEntityUuid(branch.getUuid());
    queueDto.setUuid(taskUuid);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(analysis == null ? null : analysis.getUuid());
    activityDto.setExecutedAt((long) counter++);
    activityDto.setTaskType(taskType);
    activityDto.setComponentUuid(branch.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.getSession().commit();
    return activityDto;
  }
}
