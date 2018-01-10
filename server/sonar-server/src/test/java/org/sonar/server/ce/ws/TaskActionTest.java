/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Ce;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeTaskCharacteristicDto.BRANCH_KEY;
import static org.sonar.db.ce.CeTaskCharacteristicDto.BRANCH_TYPE_KEY;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class TaskActionTest {

  private static final String SOME_TASK_UUID = "TASK_1";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private OrganizationDto organizationDto;
  private ComponentDto project;
  private TaskFormatter formatter = new TaskFormatter(db.getDbClient(), System2.INSTANCE);
  private TaskAction underTest = new TaskAction(db.getDbClient(), formatter, userSession);
  private WsActionTester ws = new WsActionTester(underTest);

  @Before
  public void setUp() {
    organizationDto = db.organizations().insert();
    project = db.components().insertPrivateProject(organizationDto);
  }

  @Test
  public void task_is_in_queue() {
    logInAsRoot();

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setComponentUuid(project.uuid());
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    queueDto.setSubmitterLogin("john");
    persist(queueDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    assertThat(taskResponse.getTask().getOrganization()).isEqualTo(organizationDto.getKey());
    assertThat(taskResponse.getTask().getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(taskResponse.getTask().getStatus()).isEqualTo(Ce.TaskStatus.PENDING);
    assertThat(taskResponse.getTask().getSubmitterLogin()).isEqualTo("john");
    assertThat(taskResponse.getTask().getComponentId()).isEqualTo(project.uuid());
    assertThat(taskResponse.getTask().getComponentKey()).isEqualTo(project.getDbKey());
    assertThat(taskResponse.getTask().getComponentName()).isEqualTo(project.name());
    assertThat(taskResponse.getTask().hasExecutionTimeMs()).isFalse();
    assertThat(taskResponse.getTask().getLogs()).isFalse();
  }

  @Test
  public void task_is_archived() {
    logInAsRoot();

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID);
    persist(activityDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getOrganization()).isEqualTo(organizationDto.getKey());
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getStatus()).isEqualTo(Ce.TaskStatus.FAILED);
    assertThat(task.getComponentId()).isEqualTo(project.uuid());
    assertThat(task.getComponentKey()).isEqualTo(project.getDbKey());
    assertThat(task.getComponentName()).isEqualTo(project.name());
    assertThat(task.getAnalysisId()).isEqualTo(activityDto.getAnalysisUuid());
    assertThat(task.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(task.getLogs()).isFalse();
  }

  @Test
  public void long_living_branch_in_past_activity() {
    logInAsRoot();
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto longLivingBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(LONG));
    db.components().insertSnapshot(longLivingBranch);
    CeActivityDto activity = createAndPersistArchivedTask(project);
    insertCharacteristic(activity, BRANCH_KEY, longLivingBranch.getBranch());
    insertCharacteristic(activity, BRANCH_TYPE_KEY, LONG.name());

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);

    assertThat(taskResponse.getTask())
      .extracting(Ce.Task::getId, Ce.Task::getBranch, Ce.Task::getBranchType, Ce.Task::getComponentKey)
      .containsExactlyInAnyOrder(SOME_TASK_UUID, longLivingBranch.getBranch(), Common.BranchType.LONG, longLivingBranch.getKey());
  }

  @Test
  public void long_living_branch_in_queue_analysis() {
    logInAsRoot();
    String branch = "my_branch";
    CeQueueDto queueDto = createAndPersistQueueTask(null);
    insertCharacteristic(queueDto, BRANCH_KEY, branch);
    insertCharacteristic(queueDto, BRANCH_TYPE_KEY, LONG.name());

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);

    assertThat(taskResponse.getTask())
      .extracting(Ce.Task::getId, Ce.Task::getBranch, Ce.Task::getBranchType, Ce.Task::hasComponentKey)
      .containsExactlyInAnyOrder(SOME_TASK_UUID, branch, Common.BranchType.LONG, false);
  }

  @Test
  public void return_stacktrace_of_failed_activity_with_stacktrace_when_additionalField_is_set() {
    logInAsRoot();

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setErrorMessage("error msg")
      .setErrorStacktrace("error stack");
    persist(activityDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .setParam("additionalFields", "stacktrace")
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getErrorMessage()).isEqualTo(activityDto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isTrue();
    assertThat(task.getErrorStacktrace()).isEqualTo(activityDto.getErrorStacktrace());
  }

  @Test
  public void do_not_return_stacktrace_of_failed_activity_with_stacktrace_when_additionalField_is_not_set() {
    logInAsRoot();

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setErrorMessage("error msg")
      .setErrorStacktrace("error stack");
    persist(activityDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getErrorMessage()).isEqualTo(activityDto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  @Test
  public void return_scannerContext_of_activity_with_scannerContext_when_additionalField_is_set() {
    logInAsRoot();

    String scannerContext = "this is some scanner context, yeah!";
    persist(createActivityDto(SOME_TASK_UUID));
    persistScannerContext(SOME_TASK_UUID, scannerContext);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .setParam("additionalFields", "scannerContext")
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getScannerContext()).isEqualTo(scannerContext);
  }

  @Test
  public void do_not_return_scannerContext_of_activity_with_scannerContext_when_additionalField_is_not_set() {
    logInAsRoot();

    String scannerContext = "this is some scanner context, yeah!";
    persist(createActivityDto(SOME_TASK_UUID));
    persistScannerContext(SOME_TASK_UUID, scannerContext);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .setParam("additionalFields", "stacktrace")
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.hasScannerContext()).isFalse();
  }

  @Test
  public void do_not_return_stacktrace_of_failed_activity_without_stacktrace() {
    logInAsRoot();

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID)
      .setErrorMessage("error msg");
    persist(activityDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getErrorMessage()).isEqualTo(activityDto.getErrorMessage());
    assertThat(task.hasErrorStacktrace()).isFalse();
  }

  @Test
  public void throw_NotFoundException_if_id_does_not_exist() {
    logInAsRoot();

    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam("id", "DOES_NOT_EXIST")
      .execute();
  }

  @Test
  public void get_project_queue_task_with_scan_permission_on_project() {
    userSession.logIn().addProjectPermission(GlobalPermissions.SCAN_EXECUTION, project);
    CeQueueDto task = createAndPersistQueueTask(project);

    call(task.getUuid());
  }

  @Test
  public void get_project_queue_task_with_scan_permission_on_organization_but_not_on_project() {
    userSession.logIn().addPermission(SCAN, project.getOrganizationUuid());
    CeQueueDto task = createAndPersistQueueTask(project);

    call(task.getUuid());
  }

  @Test
  public void getting_project_queue_task_throws_ForbiddenException_if_no_admin_nor_scan_permissions() {
    userSession.logIn();
    CeQueueDto task = createAndPersistQueueTask(project);

    expectedException.expect(ForbiddenException.class);

    call(task.getUuid());
  }

  @Test
  public void getting_global_queue_task_requires_to_be_system_administrator() {
    logInAsSystemAdministrator();
    CeQueueDto task = createAndPersistQueueTask(null);

    call(task.getUuid());
  }

  @Test
  public void getting_global_queue_throws_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();
    CeQueueDto task = createAndPersistQueueTask(null);

    expectedException.expect(ForbiddenException.class);

    call(task.getUuid());
  }

  @Test
  public void get_project_archived_task_with_scan_permission_on_project() {
    userSession.logIn().addProjectPermission(GlobalPermissions.SCAN_EXECUTION, project);
    CeActivityDto task = createAndPersistArchivedTask(project);

    call(task.getUuid());
  }

  @Test
  public void get_project_archived_task_with_scan_permission_on_organization_but_not_on_project() {
    userSession.logIn().addPermission(SCAN, project.getOrganizationUuid());
    CeActivityDto task = createAndPersistArchivedTask(project);

    call(task.getUuid());
  }

  @Test
  public void getting_project_archived_task_throws_ForbiddenException_if_no_admin_nor_scan_permissions() {
    userSession.logIn();
    CeActivityDto task = createAndPersistArchivedTask(project);

    expectedException.expect(ForbiddenException.class);

    call(task.getUuid());
  }

  @Test
  public void getting_global_archived_task_requires_to_be_system_administrator() {
    logInAsSystemAdministrator();
    CeActivityDto task = createAndPersistArchivedTask(null);

    call(task.getUuid());
  }

  @Test
  public void getting_global_archived_throws_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();
    CeActivityDto task = createAndPersistArchivedTask(null);

    expectedException.expect(ForbiddenException.class);

    call(task.getUuid());
  }

  private CeActivityDto createAndPersistArchivedTask(@Nullable ComponentDto component) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(SOME_TASK_UUID);
    if (component != null) {
      queueDto.setComponentUuid(component.uuid());
    }
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(CeActivityDto.Status.FAILED);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(SOME_TASK_UUID + "_u1");
    persist(activityDto);
    return activityDto;
  }

  private CeActivityDto createActivityDto(String uuid) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(uuid);
    queueDto.setComponentUuid(project.uuid());
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(CeActivityDto.Status.FAILED);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(uuid + "u1");
    return activityDto;
  }

  private CeQueueDto createAndPersistQueueTask(@Nullable ComponentDto component) {
    CeQueueDto dto = new CeQueueDto();
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setUuid(SOME_TASK_UUID);
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setSubmitterLogin("john");
    if (component != null) {
      dto.setComponentUuid(component.uuid());
    }
    persist(dto);
    return dto;
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
    db.getDbClient().ceTaskCharacteristicsDao().insert(db.getSession(), Collections.singletonList(dto));
    db.commit();
    return dto;
  }

  private void persist(CeQueueDto queueDto) {
    db.getDbClient().ceQueueDao().insert(db.getSession(), queueDto);
    db.commit();
  }

  private CeActivityDto persist(CeActivityDto activityDto) {
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }

  private void persistScannerContext(String taskUuid, String scannerContext) {
    db.getDbClient().ceScannerContextDao().insert(db.getSession(), taskUuid, CloseableIterator.from(singleton(scannerContext).iterator()));
    db.commit();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void logInAsRoot() {
    userSession.logIn().setRoot();
  }

  private void call(String taskUuid) {
    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", taskUuid)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(taskUuid);
  }

}
