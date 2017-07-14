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
package org.sonar.server.ce.ws;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.ce.taskprocessor.CeTaskProcessor;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsCe.ActivityResponse;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_QUERY;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MAX_EXECUTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_MIN_SUBMITTED_AT;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_TYPE;

public class ActivityActionTest {

  private static final long EXECUTED_AT = System2.INSTANCE.now();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), System2.INSTANCE);
  private ActivityAction underTest = new ActivityAction(userSession, dbTester.getDbClient(), formatter, new CeTaskProcessor[] {mock(CeTaskProcessor.class)});
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void get_all_past_activity() {
    logInAsSystemAdministrator();
    OrganizationDto org1 = dbTester.organizations().insert();
    dbTester.components().insertPrivateProject(org1, "PROJECT_1");
    OrganizationDto org2 = dbTester.organizations().insert();
    dbTester.components().insertPrivateProject(org2, "PROJECT_2");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(PARAM_MAX_EXECUTED_AT, formatDateTime(EXECUTED_AT + 2_000)));

    assertThat(activityResponse.getTasksCount()).isEqualTo(2);
    // chronological order, from newest to oldest
    WsCe.Task task = activityResponse.getTasks(0);
    assertThat(task.getOrganization()).isEqualTo(org2.getKey());
    assertThat(task.getId()).isEqualTo("T2");
    assertThat(task.getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(task.getComponentId()).isEqualTo("PROJECT_2");
    assertThat(task.getAnalysisId()).isEqualTo("U1");
    assertThat(task.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(task.getLogs()).isFalse();
    task = activityResponse.getTasks(1);
    assertThat(task.getId()).isEqualTo("T1");
    assertThat(task.getStatus()).isEqualTo(WsCe.TaskStatus.SUCCESS);
    assertThat(task.getComponentId()).isEqualTo("PROJECT_1");
    assertThat(task.getLogs()).isFalse();
    assertThat(task.getOrganization()).isEqualTo(org1.getKey());
  }

  @Test
  public void filter_by_status() {
    logInAsSystemAdministrator();
    dbTester.components().insertPrivateProject(dbTester.getDefaultOrganization(), "PROJECT_1");
    dbTester.components().insertPrivateProject(dbTester.getDefaultOrganization(), "PROJECT_2");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(2);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T3");
    assertThat(activityResponse.getTasks(1).getId()).isEqualTo("T2");
  }

  @Test
  public void filter_by_max_executed_at_exclude() {
    logInAsSystemAdministrator();
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS,SUCCESS")
      .setParam(PARAM_MAX_EXECUTED_AT, "2016-02-15"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(0);
  }

  @Test
  public void filter_by_min_submitted_and_max_executed_at_include_day() {
    logInAsSystemAdministrator();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    dbTester.components().insertPrivateProject(organizationDto, "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    String today = formatDate(new Date(EXECUTED_AT));

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(PARAM_MIN_SUBMITTED_AT, today)
      .setParam(PARAM_MAX_EXECUTED_AT, today));

    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
  }

  @Test
  public void filter_on_current_activities() {
    dbTester.components().insertPrivateProject(dbTester.organizations().insert(), "PROJECT_1");
    logInAsSystemAdministrator();
    // T2 is the current activity (the most recent one)
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_1", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.PENDING);

    ActivityResponse activityResponse = call(
      ws.newRequest()
        .setParam("onlyCurrents", "true"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T2");
  }

  @Test
  public void limit_results() {
    logInAsSystemAdministrator();
    OrganizationDto organizationDto = dbTester.organizations().insert();
    dbTester.components().insertPrivateProject(organizationDto, "PROJECT_1");
    dbTester.components().insertPrivateProject(organizationDto, "PROJECT_2");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    assertPage(1, asList("T3"));
    assertPage(2, asList("T3", "T2"));
    assertPage(10, asList("T3", "T2", "T1"));
  }

  @Test
  public void throws_IAE_if_pageSize_is_0() {
    logInAsSystemAdministrator();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("page size must be >= 1");

    call(ws.newRequest()
      .setParam(Param.PAGE_SIZE, Integer.toString(0))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));
  }

  private void assertPage(int pageSize, List<String> expectedOrderedTaskIds) {
    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(Param.PAGE_SIZE, Integer.toString(pageSize))
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(expectedOrderedTaskIds.size());
    for (int i = 0; i < expectedOrderedTaskIds.size(); i++) {
      String expectedTaskId = expectedOrderedTaskIds.get(i);
      assertThat(activityResponse.getTasks(i).getId()).isEqualTo(expectedTaskId);
    }
  }

  @Test
  public void project_administrator_can_access_his_project_activity() {
    ComponentDto project = dbTester.components().insertPrivateProject(dbTester.organizations().insert(), "PROJECT_1");
    // no need to be a system admin
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);

    ActivityResponse activityResponse = call(ws.newRequest().setParam("componentId", "PROJECT_1"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T1");
    assertThat(activityResponse.getTasks(0).getStatus()).isEqualTo(WsCe.TaskStatus.SUCCESS);
    assertThat(activityResponse.getTasks(0).getComponentId()).isEqualTo("PROJECT_1");
  }

  @Test
  public void return_401_if_user_is_not_logged_in() {
    ComponentDto project = dbTester.components().insertPrivateProject();
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call(ws.newRequest().setParam("componentId", project.uuid()));
  }

  @Test
  public void search_activity_by_component_name() throws IOException {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto struts = ComponentTesting.newPrivateProjectDto(organizationDto).setName("old apache struts").setUuid("P1").setProjectUuid("P1");
    ComponentDto zookeeper = ComponentTesting.newPrivateProjectDto(organizationDto).setName("new apache zookeeper").setUuid("P2").setProjectUuid("P2");
    ComponentDto eclipse = ComponentTesting.newPrivateProjectDto(organizationDto).setName("eclipse").setUuid("P3").setProjectUuid("P3");
    dbTester.components().insertProjectAndSnapshot(struts);
    dbTester.components().insertProjectAndSnapshot(zookeeper);
    dbTester.components().insertProjectAndSnapshot(eclipse);
    logInAsSystemAdministrator();
    insertActivity("T1", "P1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "P2", CeActivityDto.Status.SUCCESS);
    insertActivity("T3", "P3", CeActivityDto.Status.SUCCESS);

    ActivityResponse activityResponse = call(ws.newRequest().setParam(PARAM_COMPONENT_QUERY, "apac"));

    assertThat(activityResponse.getTasksList()).extracting("id").containsOnly("T1", "T2");
  }

  @Test
  public void search_activity_returns_views() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto apacheView = newView(organizationDto).setName("Apache View").setUuid("V1").setProjectUuid("V1");
    dbTester.components().insertViewAndSnapshot(apacheView);
    logInAsSystemAdministrator();
    insertActivity("T2", "V1", CeActivityDto.Status.SUCCESS);

    ActivityResponse activityResponse = call(ws.newRequest().setParam(PARAM_COMPONENT_QUERY, "apac"));

    assertThat(activityResponse.getTasksList()).extracting("id").containsOnly("T2");
  }

  @Test
  public void search_task_id_in_queue_ignoring_other_parameters() throws IOException {
    logInAsSystemAdministrator();
    dbTester.components().insertPrivateProject(dbTester.getDefaultOrganization(), "PROJECT_1");
    insertQueue("T1", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    ActivityResponse result = call(
      ws.newRequest()
        .setParam(Param.TEXT_QUERY, "T1")
        .setParam(PARAM_STATUS, CeQueueDto.Status.PENDING.name()));

    assertThat(result.getTasksCount()).isEqualTo(1);
    assertThat(result.getTasks(0).getId()).isEqualTo("T1");
  }

  @Test
  public void search_task_id_in_activity() {
    logInAsSystemAdministrator();
    dbTester.components().insertPrivateProject(dbTester.getDefaultOrganization(), "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);

    ActivityResponse result = call(ws.newRequest().setParam(Param.TEXT_QUERY, "T1"));

    assertThat(result.getTasksCount()).isEqualTo(1);
    assertThat(result.getTasks(0).getId()).isEqualTo("T1");
  }

  @Test
  public void search_by_task_id_returns_403_if_project_admin_but_not_root() {
    // WS api/ce/task must be used in order to search by task id.
    // Here it's a convenient feature of search by text query, which
    // is reserved to roots
    ComponentDto view = dbTester.components().insertView();
    insertActivity("T1", view.uuid(), CeActivityDto.Status.SUCCESS);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, view);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(ws.newRequest().setParam(Param.TEXT_QUERY, "T1"));
  }

  @Test
  public void search_task_by_component_id() {
    ComponentDto project = dbTester.components().insertPrivateProject(dbTester.getDefaultOrganization(), "PROJECT_1");
    insertQueue("T1", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);

    ActivityResponse result = call(ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "PROJECT_1")
      .setParam(PARAM_TYPE, CeTaskTypes.REPORT)
      .setParam(PARAM_STATUS, "SUCCESS,FAILED,CANCELED,IN_PROGRESS,PENDING"));

    assertThat(result.getTasksCount()).isEqualTo(2);
  }

  @Test
  public void fail_if_both_filters_on_component_id_and_name() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("componentId and componentQuery must not be set at the same time");

    ws.newRequest()
      .setParam("componentId", "ID1")
      .setParam("componentQuery", "apache")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();
  }

  @Test
  public void fail_if_page_size_greater_than_1000() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'ps' parameter must be less than 1000");

    ws.newRequest()
      .setParam(Param.PAGE_SIZE, "1001")
      .execute();
  }

  @Test
  public void fail_if_date_is_not_well_formatted() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Date 'ill-formatted-date' cannot be parsed as either a date or date+time");

    ws.newRequest()
      .setParam(PARAM_MAX_EXECUTED_AT, "ill-formatted-date")
      .execute();
  }

  @Test
  public void support_json_response() {
    logInAsSystemAdministrator();
    TestResponse wsResponse = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .execute();

    JsonAssert.assertJson(wsResponse.getInput()).isSimilarTo("{\"tasks\":[]}");
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private CeQueueDto insertQueue(String taskUuid, String componentUuid, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.commit();
    return queueDto;
  }

  private CeActivityDto insertActivity(String taskUuid, String componentUuid, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setCreatedAt(EXECUTED_AT);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setExecutedAt(EXECUTED_AT);
    activityDto.setAnalysisUuid("U1");
    dbTester.getDbClient().ceActivityDao(). insert(dbTester.getSession(), activityDto);
    dbTester.commit();
    return activityDto;
  }

  private static ActivityResponse call(TestRequest request) {
    return request.executeProtobuf(ActivityResponse.class);
  }
}
