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
package org.sonarqube.tests.user;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import java.util.UUID;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.pageobjects.LoginPage;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsUserTokens;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;
import org.sonarqube.ws.client.usertoken.RevokeWsRequest;
import org.sonarqube.ws.client.usertoken.SearchWsRequest;
import org.sonarqube.ws.client.usertoken.UserTokensService;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class LocalAuthenticationTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @After
  public void resetProperties() throws Exception {
    resetSettings(orchestrator, null, "sonar.forceAuthentication");
  }

  @Test
  public void log_in_with_correct_credentials_then_log_out() {
    User user = tester.users().generate();
    Navigation nav = tester.openBrowser();
    nav.shouldNotBeLoggedIn();
    nav.logIn().submitCredentials(user.getLogin()).shouldBeLoggedIn();
    nav.logOut().shouldNotBeLoggedIn();
  }

  @Test
  public void log_in_with_wrong_credentials() {
    User user = tester.users().generate();
    Navigation nav = tester.openBrowser();
    LoginPage page = nav
      .logIn()
      .submitWrongCredentials(user.getLogin(), "wrong");
    page.getErrorMessage().shouldHave(Condition.text("Authentication failed"));

    nav.openHome();
    nav.shouldNotBeLoggedIn();
  }

  @Test
  public void basic_authentication_based_on_login_and_password() {
    String userId = UUID.randomUUID().toString();
    String login = format("login-%s", userId);
    String name = format("name-%s", userId);
    String password = "!ascii-only:-)@";
    tester.users().generate(u -> u.setLogin(login).setName(name).setPassword(password));

    // authenticate
    WsClient wsClient = tester.as(login, password).wsClient();
    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));
    assertThat(response.content()).isEqualTo("{\"valid\":true}");
  }

  @Test
  public void basic_authentication_based_on_token() {
    User user = tester.users().generate();
    String tokenName = "Validate token based authentication";
    UserTokensService tokensService = tester.wsClient().userTokens();
    WsUserTokens.GenerateWsResponse generateWsResponse = tokensService.generate(new GenerateWsRequest()
      .setLogin(user.getLogin())
      .setName(tokenName));
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(orchestrator.getServer().getUrl())
      .token(generateWsResponse.getToken()).build());

    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));

    assertThat(response.content()).isEqualTo("{\"valid\":true}");

    WsUserTokens.SearchWsResponse searchResponse = tokensService.search(new SearchWsRequest().setLogin(user.getLogin()));
    assertThat(searchResponse.getUserTokensCount()).isEqualTo(1);
    tokensService.revoke(new RevokeWsRequest().setLogin(user.getLogin()).setName(tokenName));
    searchResponse = tokensService.search(new SearchWsRequest().setLogin(user.getLogin()));
    assertThat(searchResponse.getUserTokensCount()).isEqualTo(0);
  }

  @Test
  public void web_login_form_should_support_utf8_passwords() {
    String login = "user_with_utf8_password";
    // see http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    String password = "κόσμε";

    // create user with a UTF-8 password
    tester.users().generate(u -> u.setLogin(login).setPassword(password));

    tester.openBrowser()
      .logIn()
      .submitCredentials(login, password)
      .shouldBeLoggedIn();
  }

  @Test
  public void basic_authentication_does_not_support_utf8_passwords() {
    String login = "user_with_utf8_password";
    // see http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    String password = "κόσμε";

    // create user with a UTF-8 password
    tester.users().generate(u -> u.setLogin(login).setPassword(password));

    // authenticate
    assertThat(checkAuthenticationWithAuthenticateWebService(login, password)).isFalse();
  }

  @Test
  public void allow_user_login_with_2_characters() throws Exception {
    tester.users().generate(u -> u.setLogin("jo").setPassword("password"));

    assertThat(checkAuthenticationWithAuthenticateWebService("jo", "password")).isTrue();
  }

  @Test
  public void test_authentication_in_ui() {
    tester.users().generate(u -> u.setLogin("simple-user").setPassword("password"));
    tester.users().generateAdministrator(u -> u.setLogin("admin-user").setPassword("admin-user"));
    tester.runHtmlTests(
      "/user/LocalAuthenticationTest/login_successful.html",
      "/user/LocalAuthenticationTest/login_wrong_password.html",
      "/user/LocalAuthenticationTest/should_not_be_unlogged_when_going_to_login_page.html");
  }

  @Test
  public void test_authentication_redirects_in_ui() {
    tester.users().generate(u -> u.setLogin("simple-user").setPassword("password"));
    tester.users().generateAdministrator(u -> u.setLogin("admin-user").setPassword("admin-user"));
    tester.runHtmlTests(
    "/user/LocalAuthenticationTest/redirect_to_login_when_not_enough_privilege.html",
      // SONAR-2132
      "/user/LocalAuthenticationTest/redirect_to_original_url_after_direct_login.html",
      "/user/LocalAuthenticationTest/redirect_to_original_url_with_parameters_after_direct_login.html",
      // SONAR-2009
      "/user/LocalAuthenticationTest/redirect_to_original_url_after_indirect_login.html");
  }

  @Test
  public void force_authentication_in_ui() {
    tester.users().generate(u -> u.setLogin("simple-user").setPassword("password"));
    tester.users().generateAdministrator(u -> u.setLogin("admin-user").setPassword("admin-user"));
    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");

    tester.runHtmlTests(
      // SONAR-3473
      "/user/LocalAuthenticationTest/force-authentication.html");
  }

  @Test
  public void authentication_with_authentication_ws() {
    User user = tester.users().generate(u -> u.setLogin("test").setPassword("password"));

    assertThat(checkAuthenticationWithAuthenticateWebService("test", "password")).isTrue();
    assertThat(checkAuthenticationWithAuthenticateWebService("wrong", "password")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService("test", "wrong")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService(null, null)).isTrue();

    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");

    assertThat(checkAuthenticationWithAuthenticateWebService("test", "password")).isTrue();
    assertThat(checkAuthenticationWithAuthenticateWebService("wrong", "password")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService("test", "wrong")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService(null, null)).isFalse();
  }

  /**
   * SONAR-7640
   */
  @Test
  public void authentication_with_any_ws() throws Exception {
    tester.users().generate(u -> u.setLogin("test").setPassword("password"));

    assertThat(checkAuthenticationWithAnyWS("test", "password").code()).isEqualTo(200);
    assertThat(checkAuthenticationWithAnyWS("wrong", "password").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("test", "wrong").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("test", null).code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS(null, null).code()).isEqualTo(200);

    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");

    assertThat(checkAuthenticationWithAnyWS("test", "password").code()).isEqualTo(200);
    assertThat(checkAuthenticationWithAnyWS("wrong", "password").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("test", "wrong").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("test", null).code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS(null, null).code()).isEqualTo(401);
  }

  private boolean checkAuthenticationWithAuthenticateWebService(String login, String password) {
    String result = tester.as(login, password).wsClient().wsConnector().call(new PostRequest("/api/authentication/validate")).content();
    return result.contains("{\"valid\":true}");
  }

  private WsResponse checkAuthenticationWithAnyWS(String login, String password) {
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder().url(orchestrator.getServer().getUrl()).credentials(login, password).build());
    // Call any WS
    return wsClient.wsConnector().call(new GetRequest("api/rules/search"));
  }

}
