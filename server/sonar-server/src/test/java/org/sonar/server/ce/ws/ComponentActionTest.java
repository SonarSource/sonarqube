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
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.log.LogFileRef;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsCe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  CeLogging ceLogging = mock(CeLogging.class);
  TaskFormatter formatter = new TaskFormatter(dbTester.getDbClient(), ceLogging, System2.INSTANCE);
  ComponentAction underTest = new ComponentAction(userSession, dbTester.getDbClient(), formatter);
  WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    when(ceLogging.getFile(any(LogFileRef.class))).thenReturn(Optional.<File>absent());
  }

  @Test
  public void empty_queue_and_empty_activity() {
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");

    TestResponse wsResponse = tester.newRequest()
      .setParam("componentId", "PROJECT_1")
      .setMediaType(MediaTypes.PROTOBUF)
      .execute();

    WsCe.ProjectResponse response = Protobuf.read(wsResponse.getInputStream(), WsCe.ProjectResponse.parser());
    assertThat(response.getQueueCount()).isEqualTo(0);
    assertThat(response.hasCurrent()).isFalse();
  }

  @Test
  public void project_tasks() {
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertActivity("T3", "PROJECT_1", CeActivityDto.Status.FAILED);
    insertQueue("T4", "PROJECT_1", CeQueueDto.Status.IN_PROGRESS);
    insertQueue("T5", "PROJECT_1", CeQueueDto.Status.PENDING);

    TestResponse wsResponse = tester.newRequest()
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
  public void canceled_tasks_must_not_be_picked_as_current_analysis() {
    userSession.addComponentUuidPermission(UserRole.USER, "PROJECT_1", "PROJECT_1");
    insertActivity("T1", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T2", "PROJECT_2", CeActivityDto.Status.FAILED);
    insertActivity("T3", "PROJECT_1", CeActivityDto.Status.SUCCESS);
    insertActivity("T4", "PROJECT_1", CeActivityDto.Status.CANCELED);
    insertActivity("T5", "PROJECT_1", CeActivityDto.Status.CANCELED);

    TestResponse wsResponse = tester.newRequest()
        .setParam("componentId", "PROJECT_1")
        .setMediaType(MediaTypes.PROTOBUF)
        .execute();

    WsCe.ProjectResponse response = Protobuf.read(wsResponse.getInputStream(), WsCe.ProjectResponse.parser());
    assertThat(response.getQueueCount()).isEqualTo(0);
    // T3 is the latest task executed on PROJECT_1 ignoring Canceled ones
    assertThat(response.hasCurrent()).isTrue();
    assertThat(response.getCurrent().getId()).isEqualTo("T3");
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
    activityDto.setSnapshotId(123_456L);
    dbTester.getDbClient().ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.getSession().commit();
    return activityDto;
  }
}
