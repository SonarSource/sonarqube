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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class TaskActionTest {

  static final ComponentDto PROJECT = ComponentTesting.newProjectDto()
    .setUuid("PROJECT_1")
    .setName("Project One")
    .setKey("P1");
  private static final String SOME_TASK_UUID = "TASK_1";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), System2.INSTANCE);
  TaskAction underTest = new TaskAction(dbTester.getDbClient(), formatter, userSession);
  WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), PROJECT);
  }

  @Test
  public void task_is_in_queue() throws Exception {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setComponentUuid(PROJECT.uuid());
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    queueDto.setSubmitterLogin("john");
    persist(queueDto);

    TestResponse wsResponse = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    assertThat(taskResponse.getTask().getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(taskResponse.getTask().getStatus()).isEqualTo(WsCe.TaskStatus.PENDING);
    assertThat(taskResponse.getTask().getSubmitterLogin()).isEqualTo("john");
    assertThat(taskResponse.getTask().getComponentId()).isEqualTo(PROJECT.uuid());
    assertThat(taskResponse.getTask().getComponentKey()).isEqualTo(PROJECT.key());
    assertThat(taskResponse.getTask().getComponentName()).isEqualTo(PROJECT.name());
    assertThat(taskResponse.getTask().hasExecutionTimeMs()).isFalse();
    assertThat(taskResponse.getTask().getLogs()).isFalse();
  }

  @Test
  public void task_is_archived() throws Exception {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID);
    persist(activityDto);

    TestResponse wsResponse = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    WsCe.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(task.getComponentId()).isEqualTo(PROJECT.uuid());
    assertThat(task.getComponentKey()).isEqualTo(PROJECT.key());
    assertThat(task.getComponentName()).isEqualTo(PROJECT.name());
    assertThat(task.getAnalysisId()).isEqualTo(activityDto.getAnalysisUuid());
    assertThat(task.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(task.getLogs()).isFalse();
  }

  @Test
  public void return_stacktrace_of_failed_activity_with_stacktrace_when_additionalField_is_set() {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setErrorMessage("error msg")
      .setErrorStacktrace("error stack");
    persist(activityDto);

    TestResponse wsResponse = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .setParam("additionalFields", "stacktrace")
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    WsCe.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getErrorMessage()).isEqualTo(activityDto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isTrue();
    assertThat(task.getErrorStacktrace()).isEqualTo(activityDto.getErrorStacktrace());
  }

  @Test
  public void do_not_return_stacktrace_of_failed_activity_with_stacktrace_when_additionalField_is_not_set() {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setErrorMessage("error msg")
      .setErrorStacktrace("error stack");
    persist(activityDto);

    TestResponse wsResponse = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    WsCe.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getErrorMessage()).isEqualTo(activityDto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  @Test
  public void return_scannerContext_of_activity_with_scannerContext_when_additionalField_is_set() {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    String scannerContext = "this is some scanner context, yeah!";
    persist(createActivityDto(SOME_TASK_UUID));
    persistScannerContext(SOME_TASK_UUID, scannerContext);

    TestResponse wsResponse = ws.newRequest()
        .setMediaType(PROTOBUF)
        .setParam("id", SOME_TASK_UUID)
        .setParam("additionalFields", "scannerContext")
        .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    WsCe.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getScannerContext()).isEqualTo(scannerContext);
  }

  @Test
  public void do_not_return_scannerContext_of_activity_with_scannerContext_when_additionalField_is_not_set() {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    String scannerContext = "this is some scanner context, yeah!";
    persist(createActivityDto(SOME_TASK_UUID));
    persistScannerContext(SOME_TASK_UUID, scannerContext);

    TestResponse wsResponse = ws.newRequest()
        .setMediaType(PROTOBUF)
        .setParam("id", SOME_TASK_UUID)
        .setParam("additionalFields", "stacktrace")
        .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    WsCe.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.hasScannerContext()).isFalse();
  }

  @Test
  public void do_not_return_stacktrace_of_failed_activity_without_stacktrace() {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setErrorMessage("error msg");
    persist(activityDto);

    TestResponse wsResponse = ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    WsCe.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getErrorMessage()).isEqualTo(activityDto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  @Test
  public void task_not_found() throws Exception {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("id", "DOES_NOT_EXIST")
      .execute();
  }

  @Test
  public void not_fail_on_queue_task_not_linked_on_project_with_system_admin_permissions() {
    userSession.login("john").setGlobalPermissions(SYSTEM_ADMIN);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType("fake");
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    persist(queueDto);

    ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam("id", SOME_TASK_UUID)
      .execute();
  }

  @Test
  public void not_fail_on_queue_task_not_linked_on_project_with_global_scan_permissions() {
    userSession.login("john").setGlobalPermissions(SCAN_EXECUTION);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType("fake");
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    persist(queueDto);

    ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam("id", SOME_TASK_UUID)
      .execute();
  }

  @Test
  public void fail_on_queue_task_not_linked_on_project_if_not_admin_nor_scan_permission() {
    userSession.login("john").setGlobalPermissions(PROVISIONING);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType("fake");
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    persist(queueDto);

    expectedException.expect(ForbiddenException.class);
    ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .execute();
  }

  @Test
  public void not_fail_on_queue_task_linked_on_project_with_project_scan_permission() {
    userSession.login("john").addProjectUuidPermissions(SCAN_EXECUTION, PROJECT.uuid());

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType("fake");
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    queueDto.setComponentUuid(PROJECT.uuid());
    persist(queueDto);

    ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam("id", SOME_TASK_UUID)
      .execute();
  }

  @Test
  public void not_fail_on_archived_task_linked_on_project_with_project_scan_permission() throws Exception {
    userSession.login("john").addProjectUuidPermissions(SCAN_EXECUTION, PROJECT.uuid());

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setComponentUuid(PROJECT.uuid());
    persist(activityDto);

    ws.newRequest()
      .setMediaType(PROTOBUF)
      .setParam("id", SOME_TASK_UUID)
      .execute();
  }

  private CeActivityDto createActivityDto(String uuid) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(uuid);
    queueDto.setComponentUuid(PROJECT.uuid());
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(CeActivityDto.Status.FAILED);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(uuid + "u1");
    return activityDto;
  }

  private void persist(CeQueueDto queueDto) {
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.commit();
  }

  private CeActivityDto persist(CeActivityDto activityDto) {
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.commit();
    return activityDto;
  }

  private void persistScannerContext(String taskUuid, String scannerContext) {
    dbTester.getDbClient().ceScannerContextDao().insert(dbTester.getSession(), taskUuid, CloseableIterator.from(singleton(scannerContext).iterator()));
    dbTester.commit();
  }

}
