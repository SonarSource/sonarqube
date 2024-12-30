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

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Common;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.db.component.BranchType.BRANCH;

public class TaskActionIT {

  private static final String SOME_TASK_UUID = "TASK_1";

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final TaskFormatter formatter = new TaskFormatter(db.getDbClient(), System2.INSTANCE);
  private final TaskAction underTest = new TaskAction(db.getDbClient(), formatter, userSession);
  private final WsActionTester ws = new WsActionTester(underTest);

  private ComponentDto privateProjectMainBranch;
  private ComponentDto publicProjectMainBranch;
  private ProjectDto publicProject;
  private ProjectDto privateProject;

  @Before
  public void setUp() {
    ProjectData privateProjectData = db.components().insertPrivateProject();
    privateProject = privateProjectData.getProjectDto();
    privateProjectMainBranch = privateProjectData.getMainBranchComponent();
    userSession.logIn().addProjectPermission(ADMIN, privateProject);
    ProjectData publicProjectData = db.components().insertPublicProject();
    publicProject = publicProjectData.getProjectDto();
    publicProjectMainBranch = publicProjectData.getMainBranchComponent();
  }

  @Test
  public void task_is_in_queue() {
    UserDto user = db.users().insertUser();
    loginAndAddProjectPermission(null, SCAN);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(SOME_TASK_UUID);
    queueDto.setComponentUuid(privateProjectMainBranch.uuid());
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    queueDto.setSubmitterUuid(user.getUuid());
    persist(queueDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    assertThat(taskResponse.getTask().getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(taskResponse.getTask().getStatus()).isEqualTo(Ce.TaskStatus.PENDING);
    assertThat(taskResponse.getTask().getSubmitterLogin()).isEqualTo(user.getLogin());
    assertThat(taskResponse.getTask().getComponentId()).isEqualTo(privateProjectMainBranch.uuid());
    assertThat(taskResponse.getTask().getComponentKey()).isEqualTo(privateProjectMainBranch.getKey());
    assertThat(taskResponse.getTask().getComponentName()).isEqualTo(privateProjectMainBranch.name());
    assertThat(taskResponse.getTask().hasExecutionTimeMs()).isFalse();
    assertThat(taskResponse.getTask().getWarningCount()).isZero();
    assertThat(taskResponse.getTask().getWarningsList()).isEmpty();
  }

  @Test
  public void no_warning_detail_on_task_in_queue() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    CeQueueDto queueDto = createAndPersistQueueTask(null, user);
    IntStream.range(0, 6)
      .forEach(i -> db.getDbClient().ceTaskMessageDao().insert(db.getSession(),
        new CeTaskMessageDto()
          .setUuid("u_" + i)
          .setTaskUuid(queueDto.getUuid())
          .setMessage("m_" + i)
          .setType(MessageType.GENERIC)
          .setCreatedAt(queueDto.getUuid().hashCode() + i)));
    db.commit();

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getWarningCount()).isZero();
    assertThat(task.getWarningsList()).isEmpty();
  }

  @Test
  public void task_is_archived() {
    UserDto user = db.users().insertUser();
    loginAndAddProjectPermission(user, GlobalPermission.SCAN.getKey());

    CeActivityDto activityDto = createActivityDto(SOME_TASK_UUID);
    persist(activityDto);

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(SOME_TASK_UUID);
    assertThat(task.getStatus()).isEqualTo(Ce.TaskStatus.FAILED);
    assertThat(task.getComponentId()).isEqualTo(privateProjectMainBranch.uuid());
    assertThat(task.getComponentKey()).isEqualTo(privateProjectMainBranch.getKey());
    assertThat(task.getComponentName()).isEqualTo(privateProjectMainBranch.name());
    assertThat(task.getAnalysisId()).isEqualTo(activityDto.getAnalysisUuid());
    assertThat(task.getExecutionTimeMs()).isEqualTo(500L);
    assertThat(task.getWarningCount()).isZero();
    assertThat(task.getWarningsList()).isEmpty();
  }

  @Test
  public void branch_in_past_activity() {
    logInAsSystemAdministrator();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    userSession.addProjectPermission(UserRole.USER, projectData.getProjectDto());
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BRANCH).setKey(branchName));
    db.components().insertSnapshot(branch);
    CeActivityDto activity = createAndPersistArchivedTask(mainBranch);
    insertCharacteristic(activity, CeTaskCharacteristics.BRANCH, branchName);
    insertCharacteristic(activity, BRANCH_TYPE, BRANCH.name());

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);

    assertThat(taskResponse.getTask())
      .extracting(Ce.Task::getId, Ce.Task::getBranch, Ce.Task::getBranchType, Ce.Task::getComponentKey)
      .containsExactlyInAnyOrder(SOME_TASK_UUID, branchName, Common.BranchType.BRANCH, branch.getKey());
  }

  @Test
  public void branch_in_queue_analysis() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();

    String branch = "my_branch";
    CeQueueDto queueDto = createAndPersistQueueTask(null, user);
    insertCharacteristic(queueDto, CeTaskCharacteristics.BRANCH, branch);
    insertCharacteristic(queueDto, BRANCH_TYPE, BRANCH.name());

    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", SOME_TASK_UUID)
      .executeProtobuf(Ce.TaskResponse.class);

    assertThat(taskResponse.getTask())
      .extracting(Ce.Task::getId, Ce.Task::getBranch, Ce.Task::getBranchType, Ce.Task::hasComponentKey)
      .containsExactlyInAnyOrder(SOME_TASK_UUID, branch, Common.BranchType.BRANCH, false);
  }

  @Test
  public void return_stacktrace_of_failed_activity_with_stacktrace_when_additionalField_is_set() {
    logInAsSystemAdministrator();

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
    logInAsSystemAdministrator();

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
    logInAsSystemAdministrator();

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
    logInAsSystemAdministrator();

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
    logInAsSystemAdministrator();

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
    logInAsSystemAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("id", "DOES_NOT_EXIST");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void get_project_queue_task_with_scan_permission_on_project() {
    UserDto user = db.users().insertUser();
    loginAndAddProjectPermission(user, GlobalPermission.SCAN.getKey());
    CeQueueDto task = createAndPersistQueueTask(privateProjectMainBranch, user);

    call(task.getUuid());
  }

  @Test
  public void getting_project_queue_task_of_public_project_fails_with_ForbiddenException() {
    UserDto user = db.users().insertUser();
    userSession.logIn().registerProjects(publicProject);
    CeQueueDto task = createAndPersistQueueTask(publicProjectMainBranch, user);

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_project_queue_task_of_private_project_with_user_permission_fails_with_ForbiddenException() {
    UserDto user = db.users().insertUser();
    userSession.logIn().addProjectPermission(UserRole.USER, privateProject);
    CeQueueDto task = createAndPersistQueueTask(privateProjectMainBranch, user);

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_project_queue_task_on_public_project() {
    UserDto user = db.users().insertUser();
    loginAndAddProjectPermission(user, GlobalPermission.SCAN.getKey());
    CeQueueDto task = createAndPersistQueueTask(privateProjectMainBranch, user);

    call(task.getUuid());
  }

  private void loginAndAddProjectPermission(@Nullable UserDto user, String permission) {
    if (user != null) {
      userSession.logIn(user);
    } else {
      userSession.logIn();
    }
    userSession.addProjectPermission(permission, privateProject)
      .addProjectBranchMapping(privateProject.getUuid(), privateProjectMainBranch);
  }

  @Test
  public void get_project_queue_task_with_scan_permission_but_not_on_project() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.SCAN);
    CeQueueDto task = createAndPersistQueueTask(privateProjectMainBranch, user);

    call(task.getUuid());
  }

  @Test
  public void get_project_queue_task_with_project_admin_permission() {
    loginAndAddProjectPermission(null, ADMIN);
    CeActivityDto task = createAndPersistArchivedTask(privateProjectMainBranch);

    call(task.getUuid());
  }

  @Test
  public void getting_project_queue_task_throws_ForbiddenException_if_no_admin_nor_scan_permissions() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    CeQueueDto task = createAndPersistQueueTask(privateProjectMainBranch, user);

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void getting_global_queue_task_requires_to_be_system_administrator() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    CeQueueDto task = createAndPersistQueueTask(null, user);

    call(task.getUuid());
  }

  @Test
  public void getting_global_queue_throws_ForbiddenException_if_not_system_administrator() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setNonSystemAdministrator();
    CeQueueDto task = createAndPersistQueueTask(null, user);

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_project_archived_task_with_scan_permission_on_project() {
    loginAndAddProjectPermission(null, SCAN);
    CeActivityDto task = createAndPersistArchivedTask(privateProjectMainBranch);

    call(task.getUuid());
  }

  @Test
  public void getting_archived_task_of_public_project_fails_with_ForbiddenException() {
    userSession.logIn().registerProjects(publicProject);
    CeActivityDto task = createAndPersistArchivedTask(publicProjectMainBranch);

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_project_archived_task_with_scan_permission_but_not_on_project() {
    userSession.logIn().addPermission(GlobalPermission.SCAN);
    CeActivityDto task = createAndPersistArchivedTask(privateProjectMainBranch);

    call(task.getUuid());
  }

  @Test
  public void getting_project_archived_task_throws_ForbiddenException_if_no_admin_nor_scan_permissions() {
    userSession.logIn();
    CeActivityDto task = createAndPersistArchivedTask(privateProjectMainBranch);

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
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

    String uuid = task.getUuid();
    assertThatThrownBy(() -> call(uuid))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_warnings_on_global_archived_task_requires_to_be_system_administrator() {
    logInAsSystemAdministrator();

    insertWarningsCallEndpointAndAssertWarnings(createAndPersistArchivedTask(null));
  }

  @Test
  public void get_warnings_on_public_project_archived_task_if_not_admin_fails_with_ForbiddenException() {
    userSession.logIn().registerProjects(publicProject);

    CeActivityDto persistArchivedTask = createAndPersistArchivedTask(publicProjectMainBranch);
    assertThatThrownBy(() -> insertWarningsCallEndpointAndAssertWarnings(persistArchivedTask))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_warnings_on_private_project_archived_task_if_user_fails_with_ForbiddenException() {
    userSession.logIn().addProjectPermission(UserRole.USER, privateProject);

    CeActivityDto persistArchivedTask = createAndPersistArchivedTask(privateProjectMainBranch);
    assertThatThrownBy(() -> insertWarningsCallEndpointAndAssertWarnings(persistArchivedTask))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void get_warnings_on_private_project_archived_task_if_scan() {
    loginAndAddProjectPermission(null, GlobalPermission.SCAN.getKey());

    insertWarningsCallEndpointAndAssertWarnings(createAndPersistArchivedTask(privateProjectMainBranch));
  }

  @Test
  public void get_warnings_on_private_project_archived_task_if_global_scan_permission() {
    userSession.logIn().addPermission(GlobalPermission.SCAN);

    insertWarningsCallEndpointAndAssertWarnings(createAndPersistArchivedTask(privateProjectMainBranch));
  }

  @Test
  public void get_warnings_on_global_archived_task_requires_to_be_system_administrator2() {
    logInAsSystemAdministrator();

    CeActivityDto activityDto = persist(createActivityDto("uuid1"));
    insertMessage(activityDto, 1, MessageType.INFO);
    CeTaskMessageDto warning = insertMessage(activityDto, 2, MessageType.GENERIC);

    callEndpointAndAssertWarnings(activityDto, List.of(warning));
  }

  private void insertWarningsCallEndpointAndAssertWarnings(CeActivityDto task) {
    List<CeTaskMessageDto> warnings = IntStream.range(0, 6)
      .mapToObj(i -> insertWarning(task, i))
      .toList();
    callEndpointAndAssertWarnings(task, warnings);
  }

  private void callEndpointAndAssertWarnings(CeActivityDto task, List<CeTaskMessageDto> warnings) {
    Ce.Task taskWithWarnings = callWithWarnings(task.getUuid());
    assertThat(taskWithWarnings.getWarningCount()).isEqualTo(warnings.size());
    assertThat(taskWithWarnings.getWarningsList()).isEqualTo(warnings.stream().map(CeTaskMessageDto::getMessage).toList());
  }

  private CeTaskMessageDto insertWarning(CeActivityDto task, int i) {
    return insertMessage(task, i, MessageType.GENERIC);
  }

  private CeTaskMessageDto insertMessage(CeActivityDto task, int i, MessageType messageType) {
    CeTaskMessageDto res = new CeTaskMessageDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setTaskUuid(task.getUuid())
      .setMessage("msg_" + task.getUuid() + "_" + i)
      .setType(messageType)
      .setCreatedAt(task.getUuid().hashCode() + i);
    db.getDbClient().ceTaskMessageDao().insert(db.getSession(), res);
    db.getSession().commit();
    return res;
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
    CeQueueDto queueDto = createQueueDto(uuid);
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(CeActivityDto.Status.FAILED);
    activityDto.setExecutionTimeMs(500L);
    activityDto.setAnalysisUuid(uuid + "u1");
    return activityDto;
  }

  private CeQueueDto createQueueDto(String uuid) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid(uuid);
    queueDto.setComponentUuid(privateProjectMainBranch.uuid());
    return queueDto;
  }

  private CeQueueDto createAndPersistQueueTask(@Nullable ComponentDto component, UserDto user) {
    CeQueueDto dto = new CeQueueDto();
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setUuid(SOME_TASK_UUID);
    dto.setStatus(CeQueueDto.Status.PENDING);
    dto.setSubmitterUuid(user.getUuid());
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
    userSession.addPermission(GlobalPermission.ADMINISTER);
  }

  private void call(String taskUuid) {
    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", taskUuid)
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(taskUuid);
  }

  private Ce.Task callWithWarnings(String taskUuid) {
    Ce.TaskResponse taskResponse = ws.newRequest()
      .setParam("id", taskUuid)
      .setParam("additionalFields", "warnings")
      .executeProtobuf(Ce.TaskResponse.class);
    Ce.Task task = taskResponse.getTask();
    assertThat(task.getId()).isEqualTo(taskUuid);
    return task;
  }

}
