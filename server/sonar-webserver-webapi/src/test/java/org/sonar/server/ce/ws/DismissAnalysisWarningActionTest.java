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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.ce.CeTaskMessageType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertThrows;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.ce.CeTaskTypes.REPORT;

public class DismissAnalysisWarningActionTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private static int counter = 1;

  private final WsActionTester underTest = new WsActionTester(new DismissAnalysisWarningAction(userSession, db.getDbClient(), TestComponentFinder.from(db)));

  @Test
  public void definition() {
    WebService.Action def = underTest.getDef();
    assertThat(def.key()).isEqualTo("dismiss_analysis_warning");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key, WebService.Param::isRequired).containsOnly(
      tuple("component", true),
      tuple("warning", true));
  }

  @Test
  public void return_401_if_user_is_not_logged_in() {
    userSession.anonymous();
    TestRequest request = underTest.newRequest()
      .setParam("component", "6653f062-7c03-4b55-bcd2-0dac67640c4d")
      .setParam("warning", "55c40b35-4145-4b78-bdf2-dfb242c25f15");

    assertThrows("Authentication is required", UnauthorizedException.class, request::execute);
  }

  @Test
  public void return_403_if_user_has_no_browse_permission_on_private_project() {
    ProjectDto project = db.components().insertPrivateProjectDto();
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = underTest.newRequest()
      .setParam("component", project.getKee())
      .setParam("warning", "55c40b35-4145-4b78-bdf2-dfb242c25f15");

    assertThrows("Insufficient privileges", ForbiddenException.class, request::execute);
  }

  @Test
  public void return_204_on_success() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, project, SUCCESS, analysis, REPORT);
    CeTaskMessageDto taskMessageDismissible = createTaskMessage(activity, "dismissable warning", CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    TestResponse response = underTest.newRequest()
      .setParam("component", project.getKey())
      .setParam("warning", taskMessageDismissible.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(db.select("select * from user_dismissed_messages"))
      .extracting("USER_UUID", "PROJECT_UUID", "MESSAGE_TYPE")
      .containsExactly(tuple(userSession.getUuid(), project.uuid(), CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE.name()));
  }

  @Test
  public void is_idempotent() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, project, SUCCESS, analysis, REPORT);
    CeTaskMessageDto taskMessageDismissible = createTaskMessage(activity, "dismissable warning", CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    underTest.newRequest()
      .setParam("component", project.getKey())
      .setParam("warning", taskMessageDismissible.getUuid())
      .execute();
    TestResponse response = underTest.newRequest()
      .setParam("component", project.getKey())
      .setParam("warning", taskMessageDismissible.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(db.select("select * from user_dismissed_messages"))
      .extracting("USER_UUID", "PROJECT_UUID", "MESSAGE_TYPE")
      .containsExactly(tuple(userSession.getUuid(), project.uuid(), CeTaskMessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE.name()));
  }

  @Test
  public void returns_400_if_warning_is_not_dismissable() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    CeActivityDto activity = insertActivity("task-uuid" + counter++, project, SUCCESS, analysis, REPORT);
    CeTaskMessageDto taskMessage = createTaskMessage(activity, "generic warning");

    TestRequest request = underTest.newRequest()
      .setParam("component", project.getKey())
      .setParam("warning", taskMessage.getUuid());

    assertThrows(format("Message '%s' cannot be dismissed.", taskMessage.getUuid()), IllegalArgumentException.class, request::execute);
    assertThat(db.countRowsOfTable("USER_DISMISSED_MESSAGES")).isZero();
  }

  @Test
  public void returns_404_if_warning_does_not_exist() {
    UserDto user = db.users().insertUser();
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn(user).addProjectPermission(UserRole.USER, project);
    SnapshotDto analysis = db.components().insertSnapshot(project);
    insertActivity("task-uuid" + counter++, project, SUCCESS, analysis, REPORT);
    String warningUuid = "78d1e2ff-3e67-4037-ba58-0d57d5f88e44";

    TestRequest request = underTest.newRequest()
      .setParam("component", project.getKey())
      .setParam("warning", warningUuid);

    assertThrows(format("Message '%s' not found", warningUuid), NotFoundException.class, request::execute);
    assertThat(db.countRowsOfTable("USER_DISMISSED_MESSAGES")).isZero();
  }

  private CeTaskMessageDto createTaskMessage(CeActivityDto activity, String warning) {
    return createTaskMessage(activity, warning, CeTaskMessageType.GENERIC);
  }

  private CeTaskMessageDto createTaskMessage(CeActivityDto activity, String warning, CeTaskMessageType messageType) {
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

  private CeActivityDto insertActivity(String taskUuid, ComponentDto component, CeActivityDto.Status status, @Nullable SnapshotDto analysis, String taskType) {
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
