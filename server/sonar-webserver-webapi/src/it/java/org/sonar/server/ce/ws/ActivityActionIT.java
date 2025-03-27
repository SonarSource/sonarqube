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
package org.sonar.server.ce.ws;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.ce.task.taskprocessor.CeTaskProcessor;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeActivityDto.Status;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.PortfolioData;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.ActivityResponse;
import org.sonarqube.ws.Ce.Task;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;
import static org.sonar.db.ce.CeActivityDto.Status.FAILED;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_STATUS;

public class ActivityActionIT {

  private static final long EXECUTED_AT = System2.INSTANCE.now();
  private static final String NODE_NAME = "nodeName1";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final TaskFormatter formatter = new TaskFormatter(db.getDbClient(), System2.INSTANCE);
  private final ActivityAction underTest = new ActivityAction(userSession, db.getDbClient(), formatter, new CeTaskProcessor[]{mock(CeTaskProcessor.class)});
  private final WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void get_all_past_activity() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    SnapshotDto analysisProject1 = db.components().insertSnapshot(project1.getMainBranchDto());
    insertActivity("T1", project1.projectUuid(), project1.mainBranchUuid(), SUCCESS, analysisProject1);
    insertActivity("T2", project2.projectUuid(), project2.mainBranchUuid(), FAILED, null);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(PARAM_MAX_EXECUTED_AT, formatDateTime(EXECUTED_AT + 2_000)));

    assertThat(activityResponse.getTasksCount()).isEqualTo(2);
    // chronological order, from newest to oldest
    Task task = activityResponse.getTasks(0);
    assertThat(task.getId()).isEqualTo("T2");
    assertThat(task.getStatus()).isEqualTo(Ce.TaskStatus.FAILED);
    assertThat(task.getNodeName()).isEqualTo(NODE_NAME);
    assertThat(task.getComponentId()).isEqualTo(project2.getMainBranchComponent().uuid());
    assertThat(task.hasAnalysisId()).isFalse();
    assertThat(task.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(task.getWarningCount()).isZero();

    task = activityResponse.getTasks(1);
    assertThat(task.getId()).isEqualTo("T1");
    assertThat(task.getNodeName()).isEqualTo(NODE_NAME);
    assertThat(task.getStatus()).isEqualTo(Ce.TaskStatus.SUCCESS);
    assertThat(task.getComponentId()).isEqualTo(project1.getMainBranchComponent().uuid());
    assertThat(task.getWarningCount()).isZero();
  }

  @Test
  public void filter_by_status() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, FAILED);
    insertQueue("T3", project1, IN_PROGRESS);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(2);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T3");
    assertThat(activityResponse.getTasks(1).getId()).isEqualTo("T2");
  }

  @Test
  public void filter_by_max_executed_at_exclude() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, FAILED);
    insertQueue("T3", project1, IN_PROGRESS);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS,SUCCESS")
      .setParam(PARAM_MAX_EXECUTED_AT, "2016-02-15"));

    assertThat(activityResponse.getTasksCount()).isZero();
  }

  @Test
  public void filter_by_min_submitted_and_max_executed_at_include_day() {
    logInAsSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    insertActivity("T1", project, SUCCESS);
    String today = formatDate(new Date(EXECUTED_AT));

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(PARAM_MIN_SUBMITTED_AT, today)
      .setParam(PARAM_MAX_EXECUTED_AT, today));

    assertThat(activityResponse.getTasksCount()).isOne();
  }

  @Test
  public void filter_on_current_activities() {
    logInAsSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    // T2 is the current activity (the most recent one)
    insertActivity("T1", project, SUCCESS);
    insertActivity("T2", project, FAILED);
    insertQueue("T3", project, PENDING);

    ActivityResponse activityResponse = call(
      ws.newRequest()
        .setParam("onlyCurrents", "true"));

    assertThat(activityResponse.getTasksCount()).isOne();
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T2");
  }

  @Test
  public void task_without_project() {
    logInAsSystemAdministrator();
    insertQueue("T3", null, PENDING);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam("status", "PENDING"));

    assertThat(activityResponse.getTasksList()).hasSize(1);
  }

  @Test
  public void limit_results() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, FAILED);
    insertQueue("T3", project1, IN_PROGRESS);
    insertQueue("T4", project2, IN_PROGRESS);
    insertQueue("T5", project1, IN_PROGRESS);

    assertPage(1, 1, singletonList("T5"));
    assertPage(1, 2, asList("T5", "T4"));
    assertPage(1, 10, asList("T5", "T4", "T3", "T2", "T1"));

    assertPage(4, 1, singletonList("T2"));
    assertPage(3, 1, singletonList("T3"));
    assertPage(1, 2, asList("T5", "T4"));
    assertPage(2, 2, asList("T3", "T2"));
    assertPage(3, 2, singletonList("T1"));
  }

  @Test
  public void remove_queued_already_completed() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();

    insertActivity("T1", project1, SUCCESS);
    insertQueue("T1", project1, IN_PROGRESS);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(Param.PAGE_SIZE, Integer.toString(10))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));

    assertThat(activityResponse.getTasksList())
      .extracting(Task::getId, Ce.Task::getStatus)
      .containsExactlyInAnyOrder(
        tuple("T1", Ce.TaskStatus.SUCCESS));
  }

  @Test
  public void return_warnings_count_on_queue_and_activity_and_warnings_list() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, FAILED);
    insertQueue("T3", project1, IN_PROGRESS);
    List<String> messagesT1 = insertMessages(MessageType.GENERIC,"T1", 2);
    List<String> messagesT2 = insertMessages(MessageType.GENERIC,"T2", 1);
    insertMessages(MessageType.GENERIC,"T3", 5);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(Param.PAGE_SIZE, Integer.toString(10))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));
    assertThat(activityResponse.getTasksList())
      .extracting(Task::getId, Task::getWarningCount, Task::getWarningsList)
      .containsOnly(tuple("T1", messagesT1.size(), messagesT1), tuple("T2", messagesT2.size(), messagesT2), tuple("T3", 0, emptyList()));
  }

  @Test
  public void return_infoMessages() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, FAILED);
    insertQueue("T3", project1, IN_PROGRESS);
    List<String> messagesT1 = insertMessages(MessageType.INFO,"T1", 2);
    List<String> messagesT2 = insertMessages(MessageType.INFO,"T2", 1);
    insertMessages(MessageType.INFO,"T3", 5);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(Param.PAGE_SIZE, Integer.toString(10))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));
    assertThat(activityResponse.getTasksList())
      .extracting(Task::getId, Task::getInfoMessagesList)
      .containsOnly(tuple("T1",  messagesT1), tuple("T2",  messagesT2), tuple("T3",  emptyList()));
  }

  private List<String> insertMessages(MessageType messageType, String taskUuid, int messageCount) {
    List<CeTaskMessageDto> ceTaskMessageDtos = IntStream.range(0, messageCount)
      .mapToObj(i -> new CeTaskMessageDto()
        .setUuid("uuid_" + taskUuid + "_" + i)
        .setTaskUuid(taskUuid)
        .setMessage("m_" + taskUuid + "_" + i)
        .setType(messageType)
        .setCreatedAt(taskUuid.hashCode() + i))
      .toList();

    ceTaskMessageDtos.forEach(ceTaskMessageDto -> db.getDbClient().ceTaskMessageDao().insert(db.getSession(), ceTaskMessageDto));
    db.commit();
    return ceTaskMessageDtos.stream().map(CeTaskMessageDto::getMessage).toList();
  }

  @Test
  public void project_administrator_can_access_his_project_activity_using_component_key() {
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    // no need to be a system admin
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project1.getProjectDto());
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, FAILED);

    ActivityResponse activityResponse = call(ws.newRequest().setParam("component", project1.projectKey()));

    assertThat(activityResponse.getTasksCount()).isOne();
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T1");
    assertThat(activityResponse.getTasks(0).getStatus()).isEqualTo(Ce.TaskStatus.SUCCESS);
    assertThat(activityResponse.getTasks(0).getComponentId()).isEqualTo(project1.getMainBranchComponent().uuid());
  }

  @Test
  public void return_401_if_user_is_not_logged_in() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.anonymous();

    TestRequest request = ws.newRequest().setParam("componentId", project.projectUuid());
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void search_activity_by_component_name() {
    ProjectData struts = db.components().insertPrivateProject(c -> c.setName("old apache struts"));
    ProjectData zookeeper = db.components().insertPrivateProject(c -> c.setName("new apache zookeeper"));
    ProjectData eclipse = db.components().insertPrivateProject(c -> c.setName("eclipse"));
    db.components().insertSnapshot(struts.getMainBranchDto());
    db.components().insertSnapshot(zookeeper.getMainBranchDto());
    db.components().insertSnapshot(eclipse.getMainBranchDto());
    logInAsSystemAdministrator();
    insertActivity("T1", struts, SUCCESS);
    insertActivity("T2", zookeeper, SUCCESS);
    insertActivity("T3", eclipse, SUCCESS);

    ActivityResponse activityResponse = call(ws.newRequest().setParam(TEXT_QUERY, "apac"));

    assertThat(activityResponse.getTasksList()).extracting("id").containsOnly("T1", "T2");
  }

  @Test
  public void search_activity_returns_views() {
    PortfolioData apacheView = db.components().insertPrivatePortfolioData(v -> v.setName("Apache View"));
    db.components().insertSnapshot(apacheView.getPortfolioDto());
    logInAsSystemAdministrator();
    insertActivity("T2", apacheView, SUCCESS);

    ActivityResponse activityResponse = call(ws.newRequest().setParam(TEXT_QUERY, "apac"));

    assertThat(activityResponse.getTasksList()).extracting("id").containsOnly("T2");
  }

  @Test
  public void search_activity_returns_application() {
    ProjectData apacheApp = db.components().insertPublicApplication(a -> a.setName("Apache App"));
    db.components().insertSnapshot(apacheApp.getMainBranchDto());
    logInAsSystemAdministrator();
    insertActivity("T2", apacheApp, SUCCESS);

    ActivityResponse activityResponse = call(ws.newRequest().setParam(TEXT_QUERY, "apac"));

    assertThat(activityResponse.getTasksList()).extracting(Task::getId).containsOnly("T2");
  }

  @Test
  public void search_task_id_in_queue_ignoring_other_parameters() {
    logInAsSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    insertQueue("T1", project, IN_PROGRESS);

    ActivityResponse result = call(
      ws.newRequest()
        .setParam(Param.TEXT_QUERY, "T1")
        .setParam(PARAM_STATUS, PENDING.name()));

    assertThat(result.getTasksCount()).isOne();
    assertThat(result.getTasks(0).getId()).isEqualTo("T1");
  }

  @Test
  public void search_task_id_in_activity() {
    logInAsSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    insertActivity("T1", project, SUCCESS);

    ActivityResponse result = call(ws.newRequest().setParam(Param.TEXT_QUERY, "T1"));

    assertThat(result.getTasksCount()).isOne();
    assertThat(result.getTasks(0).getId()).isEqualTo("T1");
  }

  @Test
  public void search_by_task_id_returns_403_if_project_admin_but_not_root() {
    // WS api/ce/task must be used in order to search by task id.
    // Here it's a convenient feature of search by text query, which
    // is reserved to roots
    PortfolioData view = db.components().insertPrivatePortfolioData();
    insertActivity("T1", view, SUCCESS);
    userSession.logIn().addPortfolioPermission(ProjectPermission.ADMIN, view.getPortfolioDto());

    TestRequest request = ws.newRequest().setParam(TEXT_QUERY, "T1");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void branch_in_past_activity() {
    logInAsSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(ProjectPermission.USER, project.getProjectDto());
    String branchName = "branch1";
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setBranchType(BRANCH).setKey(branchName));
    SnapshotDto analysis = db.components().insertSnapshot(branch);
    CeActivityDto activity = insertActivity("T1", project.projectUuid(), project.mainBranchUuid(), SUCCESS, analysis);
    insertCharacteristic(activity, CeTaskCharacteristics.BRANCH, branchName);
    insertCharacteristic(activity, BRANCH_TYPE, BRANCH.name());

    ActivityResponse response = ws.newRequest().executeProtobuf(ActivityResponse.class);

    assertThat(response.getTasksList())
      .extracting(Task::getId, Ce.Task::getBranch, Ce.Task::getBranchType, Ce.Task::getStatus, Ce.Task::getComponentKey)
      .containsExactlyInAnyOrder(
        tuple("T1", branchName, Common.BranchType.BRANCH, Ce.TaskStatus.SUCCESS, branch.getKey()));
  }

  @Test
  public void branch_in_queue_analysis() {
    logInAsSystemAdministrator();
    String branch = "ny_branch";
    CeQueueDto queue1 = insertQueue("T1", null, IN_PROGRESS);
    insertCharacteristic(queue1, CeTaskCharacteristics.BRANCH, branch);
    insertCharacteristic(queue1, BRANCH_TYPE, BRANCH.name());
    CeQueueDto queue2 = insertQueue("T2", null, PENDING);
    insertCharacteristic(queue2, CeTaskCharacteristics.BRANCH, branch);
    insertCharacteristic(queue2, BRANCH_TYPE, BRANCH.name());

    ActivityResponse response = ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS,PENDING")
      .executeProtobuf(ActivityResponse.class);

    assertThat(response.getTasksList())
      .extracting(Task::getId, Ce.Task::getBranch, Ce.Task::getBranchType, Ce.Task::getStatus)
      .containsExactlyInAnyOrder(
        tuple("T1", branch, Common.BranchType.BRANCH, Ce.TaskStatus.IN_PROGRESS),
        tuple("T2", branch, Common.BranchType.BRANCH, Ce.TaskStatus.PENDING));
  }

  @Test
  public void pull_request_in_past_activity() {
    logInAsSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(ProjectPermission.USER, project.getProjectDto());
    String pullRequestKey = RandomStringUtils.secure().nextAlphanumeric(100);
    ComponentDto pullRequest = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setBranchType(BranchType.PULL_REQUEST).setKey(pullRequestKey));
    SnapshotDto analysis = db.components().insertSnapshot(pullRequest);
    CeActivityDto activity = insertActivity("T1", project.projectUuid(), project.getMainBranchComponent().uuid(), SUCCESS, analysis);
    insertCharacteristic(activity, PULL_REQUEST, pullRequestKey);

    ActivityResponse response = ws.newRequest().executeProtobuf(ActivityResponse.class);

    assertThat(response.getTasksList())
      .extracting(Task::getId, Ce.Task::getPullRequest, Ce.Task::hasPullRequestTitle, Ce.Task::getStatus, Ce.Task::getComponentKey)
      .containsExactlyInAnyOrder(
        // TODO the pull request title must be loaded from db
        tuple("T1", pullRequestKey, false, Ce.TaskStatus.SUCCESS, pullRequest.getKey()));
  }

  @Test
  public void pull_request_in_queue_analysis() {
    logInAsSystemAdministrator();
    String branch = "pr-123";
    CeQueueDto queue1 = insertQueue("T1", null, IN_PROGRESS);
    insertCharacteristic(queue1, PULL_REQUEST, branch);
    CeQueueDto queue2 = insertQueue("T2", null, PENDING);
    insertCharacteristic(queue2, PULL_REQUEST, branch);

    ActivityResponse response = ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS,PENDING")
      .executeProtobuf(ActivityResponse.class);

    assertThat(response.getTasksList())
      .extracting(Task::getId, Ce.Task::getPullRequest, Ce.Task::hasPullRequestTitle, Ce.Task::getStatus)
      .containsExactlyInAnyOrder(
        tuple("T1", branch, false, Ce.TaskStatus.IN_PROGRESS),
        tuple("T2", branch, false, Ce.TaskStatus.PENDING));
  }

  @Test
  public void fail_if_both_filters_on_component_key_and_name() {
    TestRequest request = ws.newRequest()
      .setParam("q", "apache")
      .setParam("component", "apache")
      .setMediaType(MediaTypes.PROTOBUF);
    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("component and q must not be set at the same time");
  }

  @Test
  public void fail_if_page_size_greater_than_1000() {
    TestRequest request = ws.newRequest()
      .setParam(Param.PAGE_SIZE, "1001");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'ps' value (1001) must be less than 1000");
  }

  @Test
  public void fail_if_date_is_not_well_formatted() {
    logInAsSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_MAX_EXECUTED_AT, "ill-formatted-date");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Date 'ill-formatted-date' cannot be parsed as either a date or date+time");
  }

  @Test
  public void throws_IAE_if_pageSize_is_0() {
    logInAsSystemAdministrator();
    TestRequest request = ws.newRequest()
      .setParam(Param.PAGE, Integer.toString(1))
      .setParam(Param.PAGE_SIZE, Integer.toString(0))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("page size must be >= 1");

  }

  @Test
  public void throws_IAE_if_page_is_0() {
    logInAsSystemAdministrator();
    TestRequest request = ws.newRequest()
      .setParam(Param.PAGE, Integer.toString(0))
      .setParam(Param.PAGE_SIZE, Integer.toString(1))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING");

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("page index must be >= 1");
  }

  @Test
  public void throws_IAE_if_page_is_higher_than_available() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();

    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project1, SUCCESS);
    insertActivity("T3", project1, SUCCESS);

    TestRequest request = ws.newRequest()
      .setParam(Param.PAGE, Integer.toString(2))
      .setParam(Param.PAGE_SIZE, Integer.toString(3))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING");

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Can return only the first 3 results. 4th result asked.");
  }

  @Test
  public void fail_when_project_does_not_exist() {
    logInAsSystemAdministrator();

    TestRequest request = ws.newRequest().setParam(PARAM_COMPONENT, "unknown");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component 'unknown' does not exist");
  }

  private void assertPage(int page, int pageSize, List<String> expectedOrderedTaskIds) {
    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(Param.PAGE, Integer.toString(page))
      .setParam(Param.PAGE_SIZE, Integer.toString(pageSize))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));

    assertThat(activityResponse.getPaging().getPageIndex()).isEqualTo(page);
    assertThat(activityResponse.getPaging().getPageSize()).isEqualTo(pageSize);
    assertThat(activityResponse.getTasksCount()).isEqualTo(expectedOrderedTaskIds.size());
    for (int i = 0; i < expectedOrderedTaskIds.size(); i++) {
      String expectedTaskId = expectedOrderedTaskIds.get(i);
      assertThat(activityResponse.getTasks(i).getId()).isEqualTo(expectedTaskId);
    }
  }

  @Test
  public void support_json_response() {
    logInAsSystemAdministrator();
    TestResponse wsResponse = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .execute();

    JsonAssert.assertJson(wsResponse.getInput()).isSimilarTo("{\"tasks\":[]}");
  }

  @Test
  public void filter_out_duplicate_tasks_in_progress_and_success() {
    logInAsSystemAdministrator();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    ProjectData project3 = db.components().insertPrivateProject();
    insertQueue("T2", project2, IN_PROGRESS);
    insertQueue("T3", project3, IN_PROGRESS);
    insertActivity("T1", project1, SUCCESS);
    insertActivity("T2", project2, SUCCESS);

    ActivityResponse response = ws.newRequest().setParam("status", "FAILED,IN_PROGRESS,SUCCESS").executeProtobuf(ActivityResponse.class);

    assertThat(response.getTasksList())
      .extracting(Task::getId)
      .containsExactlyInAnyOrder("T1", "T2", "T3");
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private CeQueueDto insertQueue(String taskUuid, @Nullable ProjectData project, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    if (project != null) {
      queueDto.setComponentUuid(project.mainBranchUuid());
      queueDto.setEntityUuid(project.projectUuid());
    }
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    db.getDbClient().ceQueueDao().insert(db.getSession(), queueDto);
    db.commit();
    return queueDto;
  }

  private CeActivityDto insertActivity(String taskUuid, ProjectData project, Status status) {
    return insertActivity(taskUuid, project.projectUuid(), project.mainBranchUuid(), status, db.components().insertSnapshot(project.getMainBranchDto()));
  }

  private CeActivityDto insertActivity(String taskUuid, PortfolioData portfolio, Status status) {
    return insertActivity(taskUuid, portfolio.portfolioUuid(), portfolio.rootComponentUuid(), status, db.components().insertSnapshot(portfolio.getPortfolioDto()));
  }

  private CeActivityDto insertActivity(String taskUuid, String entityUuid, String rootComponentUuid, Status status, @Nullable SnapshotDto analysis) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(rootComponentUuid);
    queueDto.setEntityUuid(entityUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setCreatedAt(EXECUTED_AT);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setExecutedAt(EXECUTED_AT);
    activityDto.setNodeName(NODE_NAME);
    activityDto.setAnalysisUuid(analysis == null ? null : analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }

  private CeTaskCharacteristicDto insertCharacteristic(CeQueueDto queueDto, String key, String value) {
    return insertCharacteristic(queueDto.getUuid(), key, value);
  }

  private CeTaskCharacteristicDto insertCharacteristic(CeActivityDto activityDto, String key, String value) {
    return insertCharacteristic(activityDto.getUuid(), key, value);
  }

  private CeTaskCharacteristicDto insertCharacteristic(String taskUuid, String key, String value) {
    CeTaskCharacteristicDto dto = new CeTaskCharacteristicDto()
      .setUuid(Uuids.createFast())
      .setTaskUuid(taskUuid)
      .setKey(key)
      .setValue(value);
    db.getDbClient().ceTaskCharacteristicsDao().insert(db.getSession(), singletonList(dto));
    db.commit();
    return dto;
  }

  private static ActivityResponse call(TestRequest request) {
    return request.executeProtobuf(ActivityResponse.class);
  }
}
