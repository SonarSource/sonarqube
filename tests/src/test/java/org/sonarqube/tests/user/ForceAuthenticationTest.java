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
package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.WsRequest.Method.GET;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class ForceAuthenticationTest {

  @ClassRule
  public static final Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();
  private User user;

  @Before
  public void setUp() {
    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");
    user = tester.users().generate();
  }

  @After
  public void tearDown() {
    resetSettings(orchestrator, null, "sonar.forceAuthentication");
  }

  @Test
  public void batch_ws_does_not_require_authentication() {
    WsConnector anonymousConnector = tester.asAnonymous().wsClient().wsConnector();
    WsResponse batchIndex = anonymousConnector.call(new GetRequest("/batch/index")).failIfNotSuccessful();
    String batchIndexContent = batchIndex.content();

    assertThat(batchIndexContent).isNotEmpty();
    String jar = batchIndexContent.split("\\|")[0];

    assertThat(anonymousConnector.call(
      new GetRequest("/batch/file").setParam("name", jar)).failIfNotSuccessful().contentStream()).isNotNull();

    // As sonar-runner is still using deprecated /batch/key, we have to also verify it
    assertThat(anonymousConnector.call(new GetRequest("/batch/" + jar)).failIfNotSuccessful().contentStream()).isNotNull();
  }

  @Test
  public void authentication_ws_does_not_require_authentication() {
    WsConnector anonymousConnector = tester.asAnonymous().wsClient().wsConnector();
    assertThat(anonymousConnector.call(new PostRequest("/api/authentication/login")
      .setParam("login", user.getLogin())
      .setParam("password", user.getLogin())).isSuccessful()).isTrue();
    verifyPathDoesNotRequiresAuthentication("/api/authentication/logout", POST);
  }

  @Test
  public void check_ws_not_requiring_authentication() {
    verifyPathDoesNotRequiresAuthentication("/api/system/db_migration_status", GET);
    verifyPathDoesNotRequiresAuthentication("/api/system/status", GET);
    verifyPathDoesNotRequiresAuthentication("/api/system/migrate_db", POST);
    verifyPathDoesNotRequiresAuthentication("/api/users/identity_providers", GET);
    verifyPathDoesNotRequiresAuthentication("/api/l10n/index", GET);
  }

  @Test
  public void check_ws_requiring_authentication() {
    verifyPathRequiresAuthentication("/api/issues/search", GET);
    verifyPathRequiresAuthentication("/api/rules/search", GET);
  }

  @Test
  public void redirect_to_login_page() {
    User administrator = tester.users().generateAdministrator();
    Navigation page = tester.openBrowser().openHome();
    page.shouldBeRedirectedToLogin();
    page.openLogin().submitCredentials(administrator.getLogin()).shouldBeLoggedIn();
    page.logOut().shouldBeRedirectedToLogin();
  }

  private void verifyPathRequiresAuthentication(String path, WsRequest.Method method) {
    assertThat(call(tester.asAnonymous().wsClient(), path, method).code()).isEqualTo(401);
    WsResponse wsResponse = call(tester.wsClient(), path, method);
    assertThat(wsResponse.isSuccessful()).as("code is %s on path %s", wsResponse.code(), path).isTrue();
  }

  private void verifyPathDoesNotRequiresAuthentication(String path, WsRequest.Method method) {
    WsResponse wsResponse = call(tester.asAnonymous().wsClient(), path, method);
    assertThat(wsResponse.isSuccessful()).as("code is %s on path %s", wsResponse.code(), path).isTrue();
    wsResponse = call(tester.wsClient(), path, method);
    assertThat(wsResponse.isSuccessful()).as("code is %s on path %s", wsResponse.code(), path).isTrue();
  }

  private WsResponse call(WsClient client, String path, WsRequest.Method method) {
    WsRequest request = method.equals(GET) ? new GetRequest(path) : new PostRequest(path);
    return client.wsConnector().call(request);
  }

}
