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
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.web.UserRole;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogsWsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  CeLogging ceLogging = mock(CeLogging.class);
  LogsWsAction underTest = new LogsWsAction(userSession, ceLogging);
  WsActionTester tester = new WsActionTester(underTest);

  @Before
  public void setUp() {
    userSession.setGlobalPermissions(UserRole.ADMIN);
  }

  @Test
  public void return_task_logs_if_available() throws IOException {
    File logFile = temp.newFile();
    FileUtils.write(logFile, "{logs}");
    when(ceLogging.fileForTaskUuid("TASK_1")).thenReturn(Optional.of(logFile));

    TestResponse response = tester.newRequest()
      .setParam("taskId", "TASK_1")
      .execute();

    assertThat(response.getMediaType()).isEqualTo(MimeTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{logs}");
  }

  @Test(expected = NotFoundException.class)
  public void return_404_if_task_logs_do_not_exist() throws IOException {
    when(ceLogging.fileForTaskUuid("TASK_1")).thenReturn(Optional.<File>absent());

    tester.newRequest()
      .setParam("taskId", "TASK_1")
      .execute();

  }
}
