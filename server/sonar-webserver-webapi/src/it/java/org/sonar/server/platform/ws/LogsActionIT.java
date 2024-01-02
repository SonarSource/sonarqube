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
package org.sonar.server.platform.ws;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogsActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final ServerLogging serverLogging = mock(ServerLogging.class);
  private final LogsAction underTest = new LogsAction(userSession, serverLogging);
  private final WsActionTester actionTester = new WsActionTester(underTest);

  // values are lower-case and alphabetically ordered
  @Test
  public void possibleValues_shouldReturnPossibleLogFileValues() {
    Set<String> values = actionTester.getDef().param("name").possibleValues();
    assertThat(values).containsExactly("access", "app", "ce", "deprecation", "es", "web");
  }

  @Test
  public void execute_whenUserNotLoggedIn_shouldFailWithForbiddenException() {
    TestRequest request = actionTester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void execute_whenUserIsNotSystemAdministrator_shouldFailWithForbiddenException() {
    userSession.logIn();

    TestRequest request = actionTester.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void execute_whenNoLogNameParamProvided_shouldReturnAppLogs() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{app}");
  }

  @Test
  public void execute_whenUsingDeprecatedProcessParameter_shouldReturnCorrectLogs() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    TestResponse response = actionTester.newRequest()
      .setParam("process", "deprecation")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{deprecation}");
  }

  @Test
  public void execute_whenFileDoesNotExist_shouldReturn404NotFound() throws IOException {
    logInAsSystemAdministrator();

    createLogsDir();

    TestResponse response = actionTester.newRequest().execute();
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void execute_whenLogNameProvided_shouldRespondWithLogsAccording() throws IOException {
    logInAsSystemAdministrator();

    createAllLogsFiles();

    asList("ce", "es", "web", "access", "deprecation").forEach(process -> {
      TestResponse response = actionTester.newRequest()
        .setParam("name", process)
        .execute();
      assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
      assertThat(response.getInput()).isEqualTo("{" + process + "}");
    });
  }

  @Test
  public void execute_whenNumberRollingPolicy_shouldReturnLatestOnly() throws IOException {
    logInAsSystemAdministrator();

    File dir = createLogsDir();
    writeTestLogFile(dir, "sonar.1.log", "{old}");
    writeTestLogFile(dir, "sonar.log", "{recent}");

    TestResponse response = actionTester.newRequest()
      .setParam("name", "app")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{recent}");
  }

  @Test
  public void execute_whenDateRollingPolicy_shouldReturnLatestLogFile() throws IOException {
    logInAsSystemAdministrator();

    File dir = createLogsDir();
    writeTestLogFile(dir, "sonar.20210101.log", "{old}");
    writeTestLogFile(dir, "sonar.20210201.log", "{recent}");

    TestResponse response = actionTester.newRequest()
      .setParam("name", "app")
      .execute();
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.TXT);
    assertThat(response.getInput()).isEqualTo("{recent}");
  }

  private void createAllLogsFiles() throws IOException {
    File dir = createLogsDir();
    writeTestLogFile(dir, "access.log", "{access}");
    writeTestLogFile(dir, "sonar.log", "{app}");
    writeTestLogFile(dir, "ce.log", "{ce}");
    writeTestLogFile(dir, "es.log", "{es}");
    writeTestLogFile(dir, "web.log", "{web}");
    writeTestLogFile(dir, "deprecation.log", "{deprecation}");

    writeTestLogFile(dir, "fake.access.log", "{fake-access}");
    writeTestLogFile(dir, "access.19900110.log", "{fake-access}");
    writeTestLogFile(dir, "fake.sonar.log", "{fake-app}");
    writeTestLogFile(dir, "sonar.19900110.log", "{date-app}");
    writeTestLogFile(dir, "fake.ce.log", "{fake-ce}");
    writeTestLogFile(dir, "ce.19900110.log", "{date-ce}");
    writeTestLogFile(dir, "fake.es.log", "{fake-es}");
    writeTestLogFile(dir, "es.19900110.log", "{date-es}");
    writeTestLogFile(dir, "fake.web.log", "{fake-web}");
    writeTestLogFile(dir, "web.19900110.log", "{date-web}");
    writeTestLogFile(dir, "fake.deprecation.log", "{fake-deprecation}");
    writeTestLogFile(dir, "deprecation.19900110.log", "{date-deprecation}");
  }

  private static void writeTestLogFile(File dir, String child, String data) throws IOException {
    FileUtils.write(new File(dir, child), data, Charset.defaultCharset());
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
