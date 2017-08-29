/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonarqube.ws.client.GetRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.jsonToMap;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;
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
  public void send_telemetry_data() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.telemetry.url", telemetryServer.url("").toString())
      .build();
    // by default telemetry payload is sent 6 hours after startup, once a week
    orchestrator.start();

    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectKey", "xoo-sample-1");
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectKey", "xoo-sample-2");
    runProjectAnalysis(orchestrator, "shared/xoo2-sample", "sonar.projectKey", "xoo2-sample");

    // no payload received at that time
    assertThat(telemetryServer.getRequestCount()).isEqualTo(0);

    // increase frequency so that payload is sent quickly after startup
    setServerProperty(orchestrator, "sonar.telemetry.frequencyInSeconds", "1");
    orchestrator.restartServer();

    RecordedRequest request = telemetryServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getHeader(HttpHeaders.USER_AGENT)).contains("SonarQube");
    Map<String, Object> json = jsonToMap(request.getBody().readUtf8());
    assertThat(json.get("id")).isEqualTo(serverId());
    assertThat(getInteger(json.get("userCount"))).isEqualTo(1);
    assertThat(((Map) json.get("plugins")).keySet()).contains("xoo");
    assertThat(getInteger(json.get("ncloc"))).isEqualTo(13 * 2 + 7);
    assertThat(getInteger(json.get("lines"))).isEqualTo(17 * 3);
    Map projectCountByLanguage = (Map) json.get("projectCountByLanguage");
    assertThat(getInteger(projectCountByLanguage.get("xoo"))).isEqualTo(2);
    assertThat(getInteger(projectCountByLanguage.get("xoo2"))).isEqualTo(1);
    Map nclocByLanguage = (Map) json.get("nclocByLanguage");
    assertThat(getInteger(nclocByLanguage.get("xoo"))).isEqualTo(13 * 2);
    assertThat(getInteger(nclocByLanguage.get("xoo2"))).isEqualTo(7);
  }

  private String serverId() {
    Map<String, Object> json = jsonToMap(ItUtils.newWsClient(orchestrator).wsConnector().call(new GetRequest("api/system/status")).failIfNotSuccessful().content());
    return (String) json.get("id");
  }

  private static int getInteger(Object jsonValue) {
    double value = (Double) jsonValue;
    return (int) Math.round(value);
  }
}
