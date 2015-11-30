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
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueueActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  CeLogging ceLogging = mock(CeLogging.class);
  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), ceLogging, System2.INSTANCE);
  QueueAction underTest = new QueueAction(userSession, dbTester.getDbClient(), formatter);
  WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.<File>absent());
  }

  @Test
  public void get_all_queue() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insert("T1", "PROJECT_1", CeQueueDto.Status.PENDING);
    insert("T2", "PROJECT_2", CeQueueDto.Status.IN_PROGRESS);

    TestResponse wsResponse = tester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    // verify the protobuf response
    WsCe.QueueResponse queueResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.QueueResponse.PARSER);
    assertThat(queueResponse.getTasksCount()).isEqualTo(2);
    assertThat(queueResponse.getTasks(0).getId()).isEqualTo("T1");
    assertThat(queueResponse.getTasks(0).getStatus()).isEqualTo(WsCe.TaskStatus.PENDING);
    assertThat(queueResponse.getTasks(0).getComponentId()).isEqualTo("PROJECT_1");
    assertThat(queueResponse.getTasks(1).getId()).isEqualTo("T2");
    assertThat(queueResponse.getTasks(1).getStatus()).isEqualTo(WsCe.TaskStatus.IN_PROGRESS);
    assertThat(queueResponse.getTasks(1).getComponentId()).isEqualTo("PROJECT_2");
  }

  @Test
  public void get_queue_of_project() {
    userSession.addComponentUuidPermission(UserRole.ADMIN, "PROJECT_1", "PROJECT_1");
    insert("T1", "PROJECT_1", CeQueueDto.Status.PENDING);
    insert("T2", "PROJECT_2", CeQueueDto.Status.PENDING);
    insert("T3", "PROJECT_2", CeQueueDto.Status.IN_PROGRESS);

    TestResponse wsResponse = tester.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    // verify the protobuf response
    WsCe.QueueResponse queueResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.QueueResponse.PARSER);
    assertThat(queueResponse.getTasksCount()).isEqualTo(1);
    assertThat(queueResponse.getTasks(0).getId()).isEqualTo("T1");
  }

  @Test(expected = ForbiddenException.class)
  public void requires_admin_permission() {
    tester.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();
  }

  private CeQueueDto insert(String taskUuid, String componentUuid, CeQueueDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(status);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.getSession().commit();
    return queueDto;
  }
}
