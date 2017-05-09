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
package it.user;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsUserTokens;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;
import org.sonarqube.ws.client.usertoken.RevokeWsRequest;
import org.sonarqube.ws.client.usertoken.SearchWsRequest;
import org.sonarqube.ws.client.usertoken.UserTokensService;
import pageobjects.LoginPage;
import pageobjects.Navigation;
import util.user.UserRule;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;
import static util.selenium.Selenese.runSelenese;

public class LocalAuthenticationTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(ORCHESTRATOR);

  @Rule
  public Navigation nav = Navigation.get(ORCHESTRATOR);

  private static WsClient adminWsClient;

  private static UserTokensService userTokensWsClient;

  private static final String LOGIN = "george.orwell";

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();

    adminWsClient = newAdminWsClient(ORCHESTRATOR);
    userTokensWsClient = adminWsClient.userTokens();

    userRule.deactivateUsers(LOGIN, "simple-user");
    userRule.createUser(LOGIN, "123456");
    addUserPermission(LOGIN, "admin");

    userRule.createUser("simple-user", "password");
  }

  @AfterClass
  public static void deleteAndRestoreData() {
    userRule.resetUsers();
  }

  @After
  public void resetProperties() throws Exception {
    resetSettings(ORCHESTRATOR, null, "sonar.forceAuthentication");
  }

  @Test
  public void log_in_with_correct_credentials_then_log_out() {
    nav.shouldNotBeLoggedIn();
    nav.logIn().submitCredentials(LOGIN, "123456").shouldBeLoggedIn();
    nav.logOut().shouldNotBeLoggedIn();
  }

  @Test
  public void log_in_with_wrong_credentials() {
    LoginPage page = nav
      .logIn()
      .submitWrongCredentials(LOGIN, "wrong");
    page.getErrorMessage().shouldHave(Condition.text("Authentication failed"));

    nav.openHomepage();
    nav.shouldNotBeLoggedIn();
  }

  @Test
  public void basic_authentication_based_on_login_and_password() {
    String userId = UUID.randomUUID().toString();
    String login = format("login-%s", userId);
    String name = format("name-%s", userId);
    String password = "!ascii-only:-)@";
    userRule.createUser(login, name, null, password);

    // authenticate
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder().url(ORCHESTRATOR.getServer().getUrl()).credentials(login, password).build());
    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));
    assertThat(response.content()).isEqualTo("{\"valid\":true}");
  }

  @Test
  public void basic_authentication_based_on_token() {
    String tokenName = "Validate token based authentication";
    WsUserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateWsRequest()
      .setLogin(LOGIN)
      .setName(tokenName));
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(ORCHESTRATOR.getServer().getUrl())
      .token(generateWsResponse.getToken()).build());

    WsResponse response = wsClient.wsConnector().call(new GetRequest("api/authentication/validate"));

    assertThat(response.content()).isEqualTo("{\"valid\":true}");

    WsUserTokens.SearchWsResponse searchResponse = userTokensWsClient.search(new SearchWsRequest().setLogin(LOGIN));
    assertThat(searchResponse.getUserTokensCount()).isEqualTo(1);
    userTokensWsClient.revoke(new RevokeWsRequest().setLogin(LOGIN).setName(tokenName));
    searchResponse = userTokensWsClient.search(new SearchWsRequest().setLogin(LOGIN));
    assertThat(searchResponse.getUserTokensCount()).isEqualTo(0);
  }

  @Test
  @Ignore
  public void web_login_form_should_support_utf8_passwords() {
    // TODO selenium
  }

  @Test
  public void basic_authentication_does_not_support_utf8_passwords() {
    String login = "user_with_utf8_password";
    // see http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    String password = "κόσμε";

    // create user with a UTF-8 password
    userRule.createUser(login, password);

    // authenticate
    assertThat(checkAuthenticationWithAuthenticateWebService(login, password)).isFalse();
  }

  @Test
  public void allow_user_login_with_2_characters() throws Exception {
    userRule.createUser("jo", "password");

    assertThat(checkAuthenticationWithAuthenticateWebService("jo", "password")).isTrue();
  }

  @Test
  public void authentication_through_ui() {
    runSelenese(ORCHESTRATOR,
      "/user/LocalAuthenticationTest/login_successful.html",
      "/user/LocalAuthenticationTest/login_wrong_password.html",
      "/user/LocalAuthenticationTest/should_not_be_unlogged_when_going_to_login_page.html",
      "/user/LocalAuthenticationTest/redirect_to_login_when_not_enough_privilege.html",
      // SONAR-2132
      "/user/LocalAuthenticationTest/redirect_to_original_url_after_direct_login.html",
      "/user/LocalAuthenticationTest/redirect_to_original_url_with_parameters_after_direct_login.html",
      // SONAR-2009
      "/user/LocalAuthenticationTest/redirect_to_original_url_after_indirect_login.html");

    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", "true");

    runSelenese(ORCHESTRATOR,
      // SONAR-3473
      "/user/LocalAuthenticationTest/force-authentication.html");
  }

  @Test
  public void authentication_with_authentication_ws() {
    assertThat(checkAuthenticationWithAuthenticateWebService("admin", "admin")).isTrue();
    assertThat(checkAuthenticationWithAuthenticateWebService("wrong", "admin")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService("admin", "wrong")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService(null, null)).isTrue();

    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", "true");

    assertThat(checkAuthenticationWithAuthenticateWebService("admin", "admin")).isTrue();
    assertThat(checkAuthenticationWithAuthenticateWebService("wrong", "admin")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService("admin", "wrong")).isFalse();
    assertThat(checkAuthenticationWithAuthenticateWebService(null, null)).isFalse();
  }

  /**
   * SONAR-7640
   */
  @Test
  public void authentication_with_any_ws() throws Exception {
    assertThat(checkAuthenticationWithAnyWS("admin", "admin").code()).isEqualTo(200);
    assertThat(checkAuthenticationWithAnyWS("wrong", "admin").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("admin", "wrong").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("admin", null).code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS(null, null).code()).isEqualTo(200);

    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", "true");

    assertThat(checkAuthenticationWithAnyWS("admin", "admin").code()).isEqualTo(200);
    assertThat(checkAuthenticationWithAnyWS("wrong", "admin").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("admin", "wrong").code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS("admin", null).code()).isEqualTo(401);
    assertThat(checkAuthenticationWithAnyWS(null, null).code()).isEqualTo(401);
  }

  private boolean checkAuthenticationWithAuthenticateWebService(String login, String password) {
    String result = ORCHESTRATOR.getServer().wsClient(login, password).get("/api/authentication/validate");
    return result.contains("{\"valid\":true}");
  }

  private WsResponse checkAuthenticationWithAnyWS(String login, String password) {
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder().url(ORCHESTRATOR.getServer().getUrl()).credentials(login, password).build());
    // Call any WS
    return wsClient.wsConnector().call(new GetRequest("api/rules/search"));
  }

  private static void addUserPermission(String login, String permission) {
    adminWsClient.permissions().addUser(new AddUserWsRequest()
      .setLogin(login)
      .setPermission(permission));
  }

  private static void removeGroupPermission(String groupName, String permission) {
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest()
      .setGroupName(groupName)
      .setPermission(permission));
  }

  private static void addGroupPermission(String groupName, String permission) {
    adminWsClient.permissions().addGroup(new AddGroupWsRequest()
      .setGroupName(groupName)
      .setPermission(permission));
  }
}
