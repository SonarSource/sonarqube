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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemPasscodeTest {

  static final String VALID_PASSCODE = "123456";
  private static final String INVALID_PASSCODE = "not" + VALID_PASSCODE;
  private static final String PASSCODE_HEADER = "X-Sonar-Passcode";

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

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
