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
package it.user;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newWsClient;
import static util.ItUtils.setServerProperty;

public class ForceAuthenticationTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  static WsClient wsClient;
  static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() throws Exception {
    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");
    wsClient = newWsClient(orchestrator);
    adminWsClient = newAdminWsClient(orchestrator);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    setServerProperty(orchestrator, "sonar.forceAuthentication", null);
  }

  @Test
  public void batch_ws_does_not_require_authentication() throws Exception {
    WsResponse batchIndex = wsClient.wsConnector().call(new GetRequest("/batch/index")).failIfNotSuccessful();
    String batchIndexContent = batchIndex.content();

    assertThat(batchIndexContent).isNotEmpty();
    String jar = batchIndexContent.split("\\|")[0];

    assertThat(wsClient.wsConnector().call(
      new GetRequest("/batch/file").setParam("name", jar)).failIfNotSuccessful().contentStream()).isNotNull();

    // As sonar-runner is still using deprecated /batch/key, we have to also verify it
    assertThat(wsClient.wsConnector().call(new GetRequest("/batch/" + jar)).failIfNotSuccessful().contentStream()).isNotNull();
  }

  @Test
  public void other_ws_require_authentication() throws Exception {
    assertThat(wsClient.wsConnector().call(new GetRequest("/api/issues/search")).code()).isEqualTo(401);
    assertThat(adminWsClient.wsConnector().call(new GetRequest("/api/issues/search")).code()).isEqualTo(200);

    assertThat(wsClient.wsConnector().call(new GetRequest("/api/rules/search")).code()).isEqualTo(401);
    assertThat(adminWsClient.wsConnector().call(new GetRequest("/api/rules/search")).code()).isEqualTo(200);
  }

}
