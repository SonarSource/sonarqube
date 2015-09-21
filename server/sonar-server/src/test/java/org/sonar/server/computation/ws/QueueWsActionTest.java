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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Protobuf;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;

public class QueueWsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient());
  CeQueueWsAction underTest = new CeQueueWsAction(userSession, dbTester.getDbClient(), formatter);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void get_all_queue() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insert("T1", "PROJECT_1", CeQueueDto.Status.PENDING);
    insert("T2", "PROJECT_2", CeQueueDto.Status.IN_PROGRESS);

    TestResponse wsResponse = tester.newRequest()
      .setMediaType(MimeTypes.PROTOBUF)
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
    userSession.addProjectUuidPermissions(UserRole.USER, "PROJECT_1");
    insert("T1", "PROJECT_1", CeQueueDto.Status.PENDING);
    insert("T2", "PROJECT_2", CeQueueDto.Status.PENDING);
    insert("T3", "PROJECT_2", CeQueueDto.Status.IN_PROGRESS);

    TestResponse wsResponse = tester.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MimeTypes.PROTOBUF)
      .execute();

    // verify the protobuf response
    WsCe.QueueResponse queueResponse = Protobuf.read(wsResponse.getInputStream(), WsCe.QueueResponse.PARSER);
    assertThat(queueResponse.getTasksCount()).isEqualTo(1);
    assertThat(queueResponse.getTasks(0).getId()).isEqualTo("T1");
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
