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
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import util.ItUtils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class LogsTest {

  public static final String ACCESS_LOGS_PATTERN = "\"%reqAttribute{ID}\" \"%reqAttribute{LOGIN}\" \"%r\" %s";
  private static final String PATH = "/called/from/LogsTest";

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  /**
   * SONAR-7581
   */
  @Test
  public void verify_login_in_access_logs() throws Exception {
    // log "-" for anonymous
    sendHttpRequest(ItUtils.newWsClient(orchestrator), PATH);
    assertThat(accessLogsFile()).isFile().exists();
    verifyLastAccessLogLine("-", PATH, 404);

    sendHttpRequest(ItUtils.newAdminWsClient(orchestrator), PATH);
    verifyLastAccessLogLine("admin", PATH, 404);
  }

  @Test
  public void verify_request_id_in_access_logs() throws IOException {
    sendHttpRequest(ItUtils.newWsClient(orchestrator), PATH);
    String lastAccessLog = readLastAccessLog();
    assertThat(lastAccessLog).doesNotStartWith("\"\"").startsWith("\"");
    int firstQuote = lastAccessLog.indexOf('"');
    String requestId = lastAccessLog.substring(firstQuote + 1, lastAccessLog.indexOf('"', firstQuote + 1));
    assertThat(requestId.length()).isGreaterThanOrEqualTo(20);
  }

  @Test
  public void info_log_in_sonar_log_file_when_SQ_is_done_starting() throws IOException {
    List<String> logs = FileUtils.readLines(orchestrator.getServer().getAppLogs());
    String sqIsUpMessage = "SonarQube is up";
    assertThat(logs.stream().filter(str -> str.contains(sqIsUpMessage)).findFirst()).describedAs("message is there").isNotEmpty();
    assertThat(logs.get(logs.size() - 1)).describedAs("message is the last line of logs").contains(sqIsUpMessage);
  }

  private void verifyLastAccessLogLine(String login, String path, int status) throws IOException {
    assertThat(readLastAccessLog()).endsWith(format("\"%s\" \"GET %s HTTP/1.1\" %d", login, path, status));
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
