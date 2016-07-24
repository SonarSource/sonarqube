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
package it.ws;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the ability for a web service to call another web services.
 */
public class WsLocalCallTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Test
  public void gets_protobuf() {
    WsResponse response = newAnonymousClient().wsConnector().call(new GetRequest("local_ws_call/protobuf_data"));
    assertThat(response.isSuccessful()).isTrue();
  }

  @Test
  public void gets_json() {
    WsResponse response = newAnonymousClient().wsConnector().call(new GetRequest("local_ws_call/json_data"));
    assertThat(response.isSuccessful()).isTrue();
  }

  @Test
  public void propagates_authorization_rights() {
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .credentials("admin", "admin")
      .build());
    WsResponse response = wsClient.wsConnector().call(new GetRequest("local_ws_call/require_permission"));
    assertThat(response.isSuccessful()).isTrue();
  }

  @Test
  public void fails_if_requires_permissions() {
    WsResponse response = newAnonymousClient().wsConnector().call(new GetRequest("local_ws_call/require_permission"));

    // this is not the unauthorized code as plugin forces it to 500
    assertThat(response.code()).isEqualTo(500);
  }

  private static WsClient newAnonymousClient() {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder().url(orchestrator.getServer().getUrl()).build());
  }
}
