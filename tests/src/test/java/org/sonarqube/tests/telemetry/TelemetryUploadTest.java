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
package org.sonarqube.tests.telemetry;

import com.sonar.orchestrator.Orchestrator;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonarqube.ws.client.GetRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.jsonToMap;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.xooPlugin;

public class TelemetryUploadTest {

  @Rule
  public Timeout safeguard = Timeout.seconds(300);

  @Rule
  public MockWebServer telemetryServer = new MockWebServer();

  private Orchestrator orchestrator;

  @After
  public void tearDown() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void sent_telemetry_data() throws Exception {
    telemetryServer.enqueue(new MockResponse().setResponseCode(200));
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.telemetry.url", telemetryServer.url("").toString())
      // increase frequency so that payload is sent quickly after startup
      .setServerProperty("sonar.telemetry.frequencyInSeconds", "1")
      //.setServerProperty("sonar.web.javaAdditionalOpts", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8001")
      .build();
    orchestrator.start();
    // Consume request to no block the telemetry daemon
    telemetryServer.takeRequest();
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectKey", "xoo-sample-1");
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectKey", "xoo-sample-2");
    runProjectAnalysis(orchestrator, "shared/xoo2-sample", "sonar.projectKey", "xoo2-sample");
    // Remove telemetry last ping from internal properties in order to allow telemetry to send another request
    resetTelemetryLastPing();

    RecordedRequest request = telemetryServer.takeRequest();

    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeader("User-Agent")).contains("SonarQube");
    Map<String, Object> json = jsonToMap(request.getBody().readUtf8());
    assertThat(json.get("id")).isEqualTo(serverId());
    Map<String, String> database = (Map<String, String>) json.get("database");
    assertThat(database.get("name")).isNotEmpty();
    assertThat(database.get("version")).isNotEmpty();
    assertThat((Boolean) json.get("usingBranches")).isFalse();
    assertThat(getInteger(json.get("userCount"))).isEqualTo(1);
    List<String> plugins = ((List<Map<String, String>>) json.get("plugins")).stream().map(p -> p.get("name")).collect(Collectors.toList());
    assertThat(plugins).contains("xoo");
    assertThat(getInteger(json.get("ncloc"))).isEqualTo(13 * 2 + 7);
    assertThat(getInteger(json.get("lines"))).isEqualTo(17 * 3);
    List<Map<String, String>> projectCountByLanguage = (List<Map<String, String>>) json.get("projectCountByLanguage");
    assertThat(projectCountByLanguage).extracting(p -> p.get("language"), p -> getInteger(p.get("count")))
      .contains(tuple("xoo", 2), tuple("xoo2", 1));
    List<Map<String, String>> nclocByLanguage = (List<Map<String, String>>) json.get("nclocByLanguage");
    assertThat(nclocByLanguage).extracting(p -> p.get("language"), p -> getInteger(p.get("ncloc")))
      .contains(tuple("xoo", 13 * 2), tuple("xoo2", 7));

    // Check that only 2 requests have been send to the telemetry server
    assertThat(telemetryServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  public void does_not_send_telemetry_data_right_away_by_Default() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.telemetry.url", telemetryServer.url("").toString())
      .build();
    // by default telemetry payload is sent 6 hours after startup, once a week
    orchestrator.start();

    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    // no payload received at that time
    assertThat(telemetryServer.getRequestCount()).isEqualTo(0);
  }

  private String serverId() {
    Map<String, Object> json = jsonToMap(ItUtils.newWsClient(orchestrator).wsConnector().call(new GetRequest("api/system/status")).failIfNotSuccessful().content());
    return (String) json.get("id");
  }

  private static int getInteger(Object jsonValue) {
    double value = (Double) jsonValue;
    return (int) Math.round(value);
  }

  private void resetTelemetryLastPing(){
    try (PreparedStatement preparedStatement = orchestrator.getDatabase().openConnection().prepareStatement("delete from internal_properties where kee='telemetry.lastPing'");) {
      preparedStatement.execute();
      preparedStatement.close();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
