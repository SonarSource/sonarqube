/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.ws;

import com.google.common.base.Optional;
import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskWsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  CeLogging ceLogging = mock(CeLogging.class);
  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), ceLogging, System2.INSTANCE);
  TaskWsAction underTest = new TaskWsAction(dbTester.getDbClient(), formatter, userSession);
  WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.<File>absent());
  }

  @Test
  public void task_is_in_queue() throws Exception {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    ComponentDto project = ComponentTesting.newProjectDto().setUuid("PROJECT_1").setName("Project One").setKey("P1");
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid("TASK_1");
    queueDto.setComponentUuid(project.uuid());
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    queueDto.setSubmitterLogin("john");
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.commit();

    TestResponse wsResponse = tester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("id", "TASK_1")
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    assertThat(taskResponse.getTask().getId()).isEqualTo("TASK_1");
    assertThat(taskResponse.getTask().getStatus()).isEqualTo(WsCe.TaskStatus.PENDING);
    assertThat(taskResponse.getTask().getSubmitterLogin()).isEqualTo("john");
    assertThat(taskResponse.getTask().getComponentId()).isEqualTo(project.uuid());
    assertThat(taskResponse.getTask().getComponentKey()).isEqualTo(project.key());
    assertThat(taskResponse.getTask().getComponentName()).isEqualTo(project.name());
    assertThat(taskResponse.getTask().hasExecutionTimeMs()).isFalse();
    assertThat(taskResponse.getTask().getLogs()).isFalse();
  }

  @Test
  public void task_is_archived() throws Exception {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    ComponentDto project = ComponentTesting.newProjectDto().setUuid("PROJECT_1").setName("Project One").setKey("P1");
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project);

    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setUuid("TASK_1");
    queueDto.setComponentUuid(project.uuid());
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(CeActivityDto.Status.FAILED);
    activityDto.setExecutionTimeMs(500L);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.commit();

    TestResponse wsResponse = tester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("id", "TASK_1")
      .execute();

    WsCe.TaskResponse taskResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.TaskResponse.PARSER);
    assertThat(taskResponse.getTask().getId()).isEqualTo("TASK_1");
    assertThat(taskResponse.getTask().getStatus()).isEqualTo(WsCe.TaskStatus.FAILED);
    assertThat(taskResponse.getTask().getComponentId()).isEqualTo(project.uuid());
    assertThat(taskResponse.getTask().getComponentKey()).isEqualTo(project.key());
    assertThat(taskResponse.getTask().getComponentName()).isEqualTo(project.name());
    assertThat(taskResponse.getTask().getExecutionTimeMs()).isEqualTo(500L);
    assertThat(taskResponse.getTask().getLogs()).isFalse();
  }

  @Test(expected = NotFoundException.class)
  public void task_not_found() throws Exception {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    tester.newRequest()
      .setParam("id", "DOES_NOT_EXIST")
      .execute();
  }

  @Test
  public void support_json_response() {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType("fake");
    queueDto.setUuid("TASK_1");
    queueDto.setStatus(CeQueueDto.Status.PENDING);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.commit();

    userSession.setGlobalPermissions(UserRole.ADMIN);
    TestResponse wsResponse = tester.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam("id", "TASK_1")
      .execute();

    JsonAssert.assertJson(wsResponse.getInput()).isSimilarTo("{\"task\":{}}");
  }
}
