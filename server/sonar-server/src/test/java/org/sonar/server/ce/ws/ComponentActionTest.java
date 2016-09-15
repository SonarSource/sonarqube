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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.server.ce.ws.ComponentAction.PARAM_COMPONENT_ID;
import static org.sonar.server.ce.ws.ComponentAction.PARAM_COMPONENT_KEY;

public class ComponentActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);
  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), System2.INSTANCE);
  ComponentAction underTest = new ComponentAction(userSession, dbTester.getDbClient(), formatter, new ComponentFinder(dbTester.getDbClient()));
  WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void empty_queue_and_empty_activity() {
    componentDbTester.insertComponent(newProjectDto("PROJECT_1"));
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");

    TestResponse wsResponse = ws.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ProjectResponse response = Protobuf.read(wsResponse.getInputStream(), WsCe.ProjectResponse.parser());
    assertThat(response.getQueueCount()).isEqualTo(0);
    assertThat(response.hasCurrent()).isFalse();
  }

  @Test
  public void project_tasks() {
    componentDbTester.insertComponent(newProjectDto("PROJECT_1"));
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertActivity("T3", "PROJECT_1", CeActivityDto.Status.FAILED);
    insertQueue("T4", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);
    insertQueue("T5", "PROJECT_1", CeQueueDto.Status.PENDING);

    TestResponse wsResponse = ws.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ProjectResponse response = Protobuf.read(wsResponse.getInputStream(), WsCe.ProjectResponse.parser());
    assertThat(response.getQueueCount()).isEqualTo(2);
    assertThat(response.getQueue(0).getId()).isEqualTo("T4");
    assertThat(response.getQueue(1).getId()).isEqualTo("T5");
    // T3 is the latest task executed on PROJECT_1
    assertThat(response.hasCurrent()).isTrue();
    assertThat(response.getCurrent().getId()).isEqualTo("T3");
  }

  @Test
  public void search_tasks_by_component_key() {
    ComponentDto project = componentDbTester.insertProject();
    setUserWithBrowsePermission(project);
    insertActivity("T1", project.uuid(), CeActivityDto.Status.SUCCESS);

    TestResponse wsResponse = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEY, project.key())
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ProjectResponse response = Protobuf.read(wsResponse.getInputStream(), WsCe.ProjectResponse.parser());
    assertThat(response.hasCurrent()).isTrue();
  }

  @Test
  public void canceled_tasks_must_not_be_picked_as_current_analysis() {
    componentDbTester.insertComponent(newProjectDto("PROJECT_1"));
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertActivity("T3", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T4", "PROJECT_1", CeActivityDto.Status.CANCELED);
    insertActivity("T5", "PROJECT_1", CeActivityDto.Status.CANCELED);

    TestResponse wsResponse = ws.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ProjectResponse response = Protobuf.read(wsResponse.getInputStream(), WsCe.ProjectResponse.parser());
    assertThat(response.getQueueCount()).isEqualTo(0);
    // T3 is the latest task executed on PROJECT_1 ignoring Canceled ones
    assertThat(response.hasCurrent()).isTrue();
    assertThat(response.getCurrent().getId()).isEqualTo("T3");
  }

  @Test
  public void fail_with_404_when_component_does_not_exist() throws Exception {
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");

    expectedException.expect(NotFoundException.class);
    ws.newRequest()
      .setParam("componentId", "UNKNOWN")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();
  }

  @Test
  public void fail_when_insufficient_permissions() {
    ComponentDto project = componentDbTester.insertProject();
    userSession.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, project.uuid())
      .execute();
  }

  @Test
  public void fail_when_no_component_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    setUserWithBrowsePermission(componentDbTester.insertProject());

    ws.newRequest().execute();
  }

  private void setUserWithBrowsePermission(ComponentDto project) {
    userSession.addProjectUuidPermissions(UserRole.USER, project.uuid());
  }

  private CeQueueDto insertQueue(String taskUuid, String componentUuid, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.getSession().commit();
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
    activityDto.setAnalysisUuid("U1");
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.getSession().commit();
    return activityDto;
  }
}
