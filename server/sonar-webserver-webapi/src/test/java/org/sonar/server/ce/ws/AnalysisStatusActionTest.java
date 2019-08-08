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
package org.sonar.server.ce.ws;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_BRANCH;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.test.JsonAssert.assertJson;

@RunWith(DataProviderRunner.class)
public class AnalysisStatusActionTest {
  private static final String BRANCH_WITH_WARNING = "feature-with-warning";
  private static final String BRANCH_WITHOUT_WARNING = "feature-without-warning";
  private static final String PULL_REQUEST = "pr1";

  private static final String WARNING_IN_MAIN = "warning in main";
  private static final String WARNING_IN_BRANCH = "warning in branch";
  private static final String WARNING_IN_PR = "warning in pr";

  private static int counter = 1;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private WsActionTester ws = new WsActionTester(new AnalysisStatusAction(userSession, dbClient, TestComponentFinder.from(db)));

  @Test
  public void fail_if_component_key_not_provided() {
    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_if_component_key_is_unknown() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest().setParam(PARAM_COMPONENT, "nonexistent").execute();
  }

  @Test
  public void fail_if_both_branch_and_pullRequest_are_specified() {
    expectedException.expect(BadRequestException.class);

    ws.newRequest()
      .setParam(PARAM_COMPONENT, "dummy")
      .setParam(PARAM_BRANCH, "feature1")
      .setParam(PARAM_PULL_REQUEST, "pr1")
      .execute();
  }

  @Test
  @UseDataProvider("nonProjectComponentFactory")
  public void fail_if_component_is_not_a_project(Function<ComponentDto, ComponentDto> nonProjectComponentFactory) {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("must be a project");

    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    ComponentDto component = nonProjectComponentFactory.apply(project);
    db.components().insertComponent(component);

    ws.newRequest()
      .setParam(PARAM_COMPONENT, component.getKey())
      .execute();
  }

  @DataProvider
  public static Object[][] nonProjectComponentFactory() {
    return new Object[][] {
      {(Function<ComponentDto, ComponentDto>) ComponentTesting::newModuleDto},
      {(Function<ComponentDto, ComponentDto>) p -> ComponentTesting.newDirectory(p, "foo")},
      {(Function<ComponentDto, ComponentDto>) ComponentTesting::newFileDto}
    };
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insert(o -> o.setKey("my-org-1"));
    ComponentDto project = db.components().insertPrivateProject(organization,
      p -> p.setUuid("AU_w74XMgAS1Hm6h4-Y-"),
      p -> p.setDbKey("com.github.kevinsawicki:http-request-parent"),
      p -> p.setName("HttpRequest"));

    userSession.addProjectPermission(UserRole.USER, project);

    String result = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(getClass().getResource("analysis_status-example.json"));
  }

  @Test
  public void no_errors_no_warnings() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList()).isEmpty();
  }

  @Test
  public void return_warnings_for_last_analysis_of_main() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, project, SUCCESS, analysis, REPORT);
    createTaskMessage(activity, WARNING_IN_MAIN);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList()).containsExactly(WARNING_IN_MAIN);

    SnapshotDto analysis2 = db.components().insertSnapshot(project);
    insertActivity("task-uuid" + counter++, project, SUCCESS, analysis2, REPORT);
    insertActivity("task-uuid" + counter++, project, SUCCESS, null, "PROJECT_EXPORT");

    Ce.AnalysisStatusWsResponse response2 = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response2.getComponent().getWarningsList()).isEmpty();
  }

  @Test
  public void return_warnings_for_last_analysis_of_branch() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITH_WARNING));
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, branch, SUCCESS, analysis, REPORT);
    createTaskMessage(activity, WARNING_IN_BRANCH);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITH_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList()).containsExactly(WARNING_IN_BRANCH);

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
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> {
      b.setBranchType(BranchType.PULL_REQUEST);
      b.setKey(PULL_REQUEST);
    });
    SnapshotDto analysis = db.components().insertSnapshot(pullRequest);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, pullRequest, SUCCESS, analysis, REPORT);
    createTaskMessage(activity, WARNING_IN_PR);

    Ce.AnalysisStatusWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_PULL_REQUEST, PULL_REQUEST)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(response.getComponent().getWarningsList()).containsExactly(WARNING_IN_PR);

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
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, project, SUCCESS, analysis, REPORT);
    createTaskMessage(activity, WARNING_IN_MAIN);

    ComponentDto branchWithWarning = db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITH_WARNING));
    SnapshotDto branchAnalysis = db.components().insertSnapshot(branchWithWarning);
    CeActivityDto branchActivity = insertActivity("task-uuid" + counter++, branchWithWarning, SUCCESS, branchAnalysis, REPORT);
    createTaskMessage(branchActivity, WARNING_IN_BRANCH);

    ComponentDto branchWithoutWarning = db.components().insertProjectBranch(project, b -> b.setKey(BRANCH_WITHOUT_WARNING));
    SnapshotDto branchWithoutWarningAnalysis = db.components().insertSnapshot(branchWithoutWarning);
    insertActivity("task-uuid" + counter++, branchWithoutWarning, SUCCESS, branchWithoutWarningAnalysis, REPORT);

    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> {
      b.setBranchType(BranchType.PULL_REQUEST);
      b.setKey(PULL_REQUEST);
    });
    SnapshotDto prAnalysis = db.components().insertSnapshot(pullRequest);
    CeActivityDto prActivity = insertActivity("task-uuid" + counter++, pullRequest, SUCCESS, prAnalysis, REPORT);
    createTaskMessage(prActivity, WARNING_IN_PR);

    Ce.AnalysisStatusWsResponse responseForMain = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForMain.getComponent().getWarningsList()).containsExactly(WARNING_IN_MAIN);

    Ce.AnalysisStatusWsResponse responseForBranchWithWarning = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITH_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForBranchWithWarning.getComponent().getWarningsList()).containsExactly(WARNING_IN_BRANCH);

    Ce.AnalysisStatusWsResponse responseForBranchWithoutWarning = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, BRANCH_WITHOUT_WARNING)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForBranchWithoutWarning.getComponent().getWarningsList()).isEmpty();

    Ce.AnalysisStatusWsResponse responseForPr = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_PULL_REQUEST, PULL_REQUEST)
      .executeProtobuf(Ce.AnalysisStatusWsResponse.class);

    assertThat(responseForPr.getComponent().getWarningsList()).containsExactly(WARNING_IN_PR);
  }

  @Test
  public void response_contains_branch_or_pullRequest_for_branch_or_pullRequest_only() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

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

  private void createTaskMessage(CeActivityDto activity, String warning) {
    db.getDbClient().ceTaskMessageDao().insert(db.getSession(), new CeTaskMessageDto()
      .setUuid("m-uuid-" + counter++)
      .setTaskUuid(activity.getUuid())
      .setMessage(warning)
      .setCreatedAt(counter));
    db.commit();
  }

  private CeActivityDto insertActivity(String taskUuid, ComponentDto component, CeActivityDto.Status status,
    @Nullable SnapshotDto analysis, String taskType) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(taskType);
    queueDto.setComponent(component);
    queueDto.setUuid(taskUuid);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(analysis == null ? null : analysis.getUuid());
    activityDto.setExecutedAt((long) counter++);
    activityDto.setTaskType(taskType);
    activityDto.setComponentUuid(component.uuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.getSession().commit();
    return activityDto;
  }
}
