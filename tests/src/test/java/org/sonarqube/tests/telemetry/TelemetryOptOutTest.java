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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.jsonToMap;
import static util.ItUtils.xooPlugin;

public class TelemetryOptOutTest {

  public static MockWebServer server = new MockWebServer();

  private static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .setServerProperty("sonar.telemetry.enable", "false")
    .setServerProperty("sonar.telemetry.url", server.url("").toString())
    .setServerProperty("sonar.telemetry.frequencyInSeconds", "1")
    .build();
  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(server)
    .around(orchestrator)
    .around(tester);

  @Test
  public void opt_out_of_telemetry() throws Exception {
    RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

    assertThat(request.getMethod()).isEqualTo("DELETE");
    assertThat(request.getHeader("User-Agent")).contains("SonarQube");
    Map<String, Object> json = jsonToMap(request.getBody().readUtf8());
    assertThat(json.get("id")).isEqualTo(serverId());
  }

  private String serverId() {
    Map<String, Object> json = jsonToMap(tester.wsClient().wsConnector().call(new GetRequest("api/system/status")).failIfNotSuccessful().content());
    return (String) json.get("id");
  }
}
