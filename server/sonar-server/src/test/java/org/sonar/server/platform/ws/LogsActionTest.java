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
package org.sonar.server.platform.ws;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogsActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServerLogging serverLogging = mock(ServerLogging.class);
  private LogsAction underTest = new LogsAction(userSession, serverLogging);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void values_of_process_parameter_are_names_of_processes() {
    Set<String> values = actionTester.getDef().param("process").possibleValues();
    // values are lower-case and alphabetically ordered
    assertThat(values).containsExactly("app", "ce", "es", "web");
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().execute();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().execute();
  }

  @Test
  public void get_app_logs_by_default() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{app}");
  }

  @Test
  public void return_404_not_found_if_file_does_not_exist() throws IOException {
    logInAsSystemAdministrator();

    createLogsDir();

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void get_ce_logs() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest()
      .setParam("process", "ce")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{ce}");
  }

  @Test
  public void get_es_logs() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest()
      .setParam("process", "es")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{es}");
  }

  @Test
  public void get_web_logs() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest()
      .setParam("process", "web")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{web}");
  }

  @Test
  public void do_not_return_rotated_files() throws IOException {
    logInAsSystemAdministrator();

    File dir = createLogsDir();
    FileUtils.write(new File(dir, "sonar.1.log"), "{old}");
    FileUtils.write(new File(dir, "sonar.log"), "{recent}");

    TestResponse response = actionTester.newRequest()
      .setParam("process", "app")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{recent}");
  }

  private File createAllLogsFiles() throws IOException {
    File dir = createLogsDir();
    FileUtils.write(new File(dir, "sonar.log"), "{app}");
    FileUtils.write(new File(dir, "ce.log"), "{ce}");
    FileUtils.write(new File(dir, "es.log"), "{es}");
    FileUtils.write(new File(dir, "web.log"), "{web}");
    return dir;
  }

  private File createLogsDir() throws IOException {
    File dir = temp.newFolder();
    when(serverLogging.getLogsDir()).thenReturn(dir);
    return dir;
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
