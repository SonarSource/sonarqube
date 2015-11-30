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
import java.io.IOException;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  CeLogging ceLogging = mock(CeLogging.class);
  LogsAction underTest = new LogsAction(dbTester.getDbClient(), userSession, ceLogging);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void return_task_logs_if_available() throws IOException {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    // task must exist in database
    insert("TASK_1", null);
    File logFile = temp.newFile();
    FileUtils.write(logFile, "{logs}");
    when(ceLogging.getFile(new LogFileRef(CeTaskTypes.REPORT, "TASK_1", null))).thenReturn(Optional.of(logFile));

    TestResponse response = tester.newRequest()
      .setParam("taskId", "TASK_1")
      .execute();

    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{logs}");
  }

  /**
   * The parameter taskId is present but empty. It's considered as
   * a valid task which does not exist
   */
  @Test(expected = NotFoundException.class)
  public void return_404_if_task_id_is_empty() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    tester.newRequest()
      .setParam("taskId", "")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void bad_request_if_task_id_is_missing() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    tester.newRequest()
      .execute();
  }

  @Test(expected = NotFoundException.class)
  public void return_404_if_task_logs_are_not_available() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    insert("TASK_1", null);
    when(ceLogging.getFile(new LogFileRef(CeTaskTypes.REPORT, "TASK_1", null))).thenReturn(Optional.<File>absent());

    tester.newRequest()
      .setParam("taskId", "TASK_1")
      .execute();
  }

  @Test(expected = NotFoundException.class)
  public void return_404_if_task_does_not_exist() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
    tester.newRequest()
      .setParam("taskId", "TASK_1")
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void require_admin_permission() {
    tester.newRequest()
      .setParam("taskId", "TASK_1")
      .execute();
  }

  private CeQueueDto insert(String taskUuid, @Nullable String componentUuid) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setUuid(taskUuid);
    queueDto.setStatus(CeQueueDto.Status.IN_PROGRESS);
    dbTester.getDbClient().ceQueueDao().insert(dbTester.getSession(), queueDto);
    dbTester.getSession().commit();
    return queueDto;
  }
}
