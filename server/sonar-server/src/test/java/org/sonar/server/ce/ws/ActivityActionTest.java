/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.log.LogFileRef;
import org.sonar.ce.taskprocessor.CeTaskProcessor;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;
import org.sonarqube.ws.WsCe.ActivityResponse;
import org.sonarqube.ws.client.ce.CeWsParameters;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDate;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_QUERY;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_STATUS;

public class ActivityActionTest {

  private static final long EXECUTED_AT = System2.INSTANCE.now();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ComponentDbTester componentDb = new ComponentDbTester(dbTester);

  CeLogging ceLogging = mock(CeLogging.class);
  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), ceLogging, System2.INSTANCE);
  ActivityAction underTest = new ActivityAction(userSession, dbTester.getDbClient(), formatter, new CeTaskProcessor[] {mock(CeTaskProcessor.class)});
  WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.<File>absent());
  }

  @Test
  public void get_all_past_activity() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(CeWsParameters.PARAM_MAX_EXECUTED_AT, formatDateTime(EXECUTED_AT + 2_000)));

    assertThat(activityResponse.getTasksCount()).isEqualTo(2);
    // chronological order, from newest to oldest
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T2");
    assertThat(activityResponse.getTasks(0).getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(activityResponse.getTasks(0).getComponentId()).isEqualTo("PROJECT_2");
    assertThat(activityResponse.getTasks(0).getAnalysisId()).isEqualTo("123456");
    assertThat(activityResponse.getTasks(0).getExecutionTimeMs()).isEqualTo(500L);
    assertThat(activityResponse.getTasks(0).getLogs()).isFalse();
    assertThat(activityResponse.getTasks(1).getId()).isEqualTo("T1");
    assertThat(activityResponse.getTasks(1).getStatus()).isEqualTo(WsCe.TaskStatus.SUCCESS);
    assertThat(activityResponse.getTasks(1).getComponentId()).isEqualTo("PROJECT_1");
    assertThat(activityResponse.getTasks(1).getLogs()).isFalse();
  }

  @Test
  public void filter_by_status() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
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
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam("status", "FAILED,IN_PROGRESS,SUCCESS")
      .setParam(CeWsParameters.PARAM_MAX_EXECUTED_AT, "2016-02-15"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(0);
  }

  @Test
  public void filter_by_max_executed_at_include_day_filled() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    String today = formatDate(new Date(EXECUTED_AT));
    System.out.println(EXECUTED_AT + " - " + today);

    ActivityResponse activityResponse = call(ws.newRequest()
      .setParam(CeWsParameters.PARAM_MAX_EXECUTED_AT, today));

    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
  }

  @Test
  public void filter_on_current_activities() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
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
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertQueue("T3", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);

    assertPage(1, asList("T3"));
    assertPage(2, asList("T3", "T2"));
    assertPage(10, asList("T3", "T2", "T1"));
    assertPage(0, Collections.<String>emptyList());
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
    // no need to be a system admin
    userSession.addComponentUuidPermission(UserRole.ADMIN, "PROJECT_1", "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);

    ActivityResponse activityResponse = call(ws.newRequest().setParam("componentId", "PROJECT_1"));

    assertThat(activityResponse.getTasksCount()).isEqualTo(1);
    assertThat(activityResponse.getTasks(0).getId()).isEqualTo("T1");
    assertThat(activityResponse.getTasks(0).getStatus()).isEqualTo(WsCe.TaskStatus.SUCCESS);
    assertThat(activityResponse.getTasks(0).getComponentId()).isEqualTo("PROJECT_1");
  }

  @Test
  public void search_activity_by_component_name() throws IOException {
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("apache struts").setUuid("P1"));
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("apache zookeeper").setUuid("P2"));
    componentDb.insertProjectAndSnapshot(newProjectDto().setName("eclipse").setUuid("P3"));
    dbTester.commit();
    componentDb.indexProjects();
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    insertActivity("T1", "P1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "P2", CeActivityDto.Status.SUCCESS);
    insertActivity("T3", "P3", CeActivityDto.Status.SUCCESS);

    ActivityResponse activityResponse = call(ws.newRequest().setParam(PARAM_COMPONENT_QUERY, "apac"));

    assertThat(activityResponse.getTasksList()).extracting("id").containsOnly("T1", "T2");
  }

  @Test
  public void search_task_id_in_queue_ignoring_other_parameters() throws IOException {
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
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);

    ActivityResponse result = call(ws.newRequest().setParam(Param.TEXT_QUERY, "T1"));

    assertThat(result.getTasksCount()).isEqualTo(1);
    assertThat(result.getTasks(0).getId()).isEqualTo("T1");
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
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Date 'ill-formatted-date' cannot be parsed as either a date or date+time");

    ws.newRequest()
      .setParam(CeWsParameters.PARAM_MAX_EXECUTED_AT, "ill-formatted-date")
      .execute();
  }

  @Test
  public void support_json_response() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    TestResponse wsResponse = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .execute();

    JsonAssert.assertJson(wsResponse.getInput()).isSimilarTo("{\"tasks\":[]}");
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
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setExecutedAt(EXECUTED_AT);
    activityDto.setSnapshotId(123_456L);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.commit();
    return activityDto;
  }

  private static ActivityResponse call(TestRequest request) {
    try {
      return ActivityResponse.parseFrom(
        request
          .setMediaType(MediaTypes.PROTOBUF)
          .execute().getInputStream());
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
