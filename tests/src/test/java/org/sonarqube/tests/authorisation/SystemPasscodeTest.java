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
package org.sonarqube.tests.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class SystemPasscodeTest {

  private static final String VALID_PASSCODE = "123456";
  private static final String INVALID_PASSCODE = "not" + VALID_PASSCODE;
  private static final String PASSCODE_HEADER = "X-Sonar-Passcode";

  private static Orchestrator orchestrator;

  @BeforeClass
  public static void setUp() throws Exception {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      // this privileged plugin provides the WS api/system_passcode/check
      // that is used by the tests
      .addPlugin(pluginArtifact("fake-governance-plugin"))
      .setServerProperty("sonar.web.systemPasscode", VALID_PASSCODE);
    orchestrator = builder.build();
    orchestrator.start();
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void system_access_is_granted_if_valid_passcode_is_sent_through_http_header() {
    WsRequest request = newRequest()
      .setHeader(PASSCODE_HEADER, VALID_PASSCODE);

    WsResponse response = tester.asAnonymous().wsClient().wsConnector().call(request);
    assertThat(response.code()).isEqualTo(200);
  }

  @Test
  public void system_access_is_rejected_if_invalid_passcode_is_sent_through_http_header() {
    WsRequest request = newRequest()
      .setHeader(PASSCODE_HEADER, INVALID_PASSCODE);

    WsResponse response = tester.asAnonymous().wsClient().wsConnector().call(request);
    assertThat(response.code()).isEqualTo(401);
  }

  @Test
  public void system_access_is_rejected_if_passcode_is_not_sent() {
    WsRequest request = newRequest();

    WsResponse response = tester.asAnonymous().wsClient().wsConnector().call(request);
    assertThat(response.code()).isEqualTo(401);
  }

  private static GetRequest newRequest() {
    return new GetRequest("api/system_passcode/check");
  }
}
