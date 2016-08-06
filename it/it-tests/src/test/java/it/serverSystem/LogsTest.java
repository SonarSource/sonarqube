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
package it.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import util.ItUtils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class LogsTest {

  public static final String ACCESS_LOGS_PATTERN = "\"%reqAttribute{LOGIN}\" \"%r\" %s";
  private static final String PATH = "/called/from/LogsTest";

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  /**
   * SONAR-7581
   */
  @Test
  public void test_access_logs() throws Exception {
    // log "-" for anonymous
    sendHttpRequest(ItUtils.newWsClient(orchestrator), PATH);
    assertThat(accessLogsFile()).isFile().exists();
    verifyLastAccessLogLine("-", PATH, 404);

    sendHttpRequest(ItUtils.newAdminWsClient(orchestrator), PATH);
    verifyLastAccessLogLine("admin", PATH, 404);
  }

  private void verifyLastAccessLogLine(String login, String path, int status) throws IOException {
    assertThat(readLastAccessLog()).isEqualTo(format("\"%s\" \"GET %s HTTP/1.1\" %d", login, path, status));
  }

  private String readLastAccessLog() throws IOException {
    try (ReversedLinesFileReader tailer = new ReversedLinesFileReader(accessLogsFile())) {
      return tailer.readLine();
    }
  }

  private void sendHttpRequest(WsClient client, String path) {
    client.wsConnector().call(new GetRequest(path));
  }

  private File accessLogsFile() {
    return new File(orchestrator.getServer().getHome(), "logs/access.log");
  }
}
