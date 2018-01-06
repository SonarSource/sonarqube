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
package org.sonarqube.tests.ws;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category4Suite;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newWsClient;

public class WsTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Test
  public void gets_protobuf() {
    WsResponse response = newWsClient(orchestrator).wsConnector().call(new GetRequest("api/issues/search.protobuf"));
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.contentType()).contains("application/x-protobuf");
  }

  @Test
  public void gets_json() {
    WsResponse response = newWsClient(orchestrator).wsConnector().call(new GetRequest("api/issues/search.json"));
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.contentType()).contains("application/json");
  }

  /**
   * SONAR-7484
   */
  @Test
  public void fail_on_unknown_extension() {
    WsResponse response = newWsClient(orchestrator).wsConnector().call(new GetRequest("api/issues/search.unknown"));
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.code()).isEqualTo(HTTP_BAD_REQUEST);
  }

}
