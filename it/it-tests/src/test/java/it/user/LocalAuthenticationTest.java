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
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import java.io.IOException;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
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
import util.selenium.SeleneseTest;
import util.user.UserRule;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class LocalAuthenticationTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(ORCHESTRATOR);

  private static WsClient adminWsClient;
  private static UserTokensService userTokensWsClient;

  private static final String PROJECT_KEY = "sample";
  private static final String LOGIN = "george.orwell";

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/authorisation/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, "Sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    adminWsClient = newAdminWsClient(ORCHESTRATOR);
    userTokensWsClient = adminWsClient.userTokens();
    removeGroupPermission("anyone", "scan");

    userRule.createUser(LOGIN, "123456");
    addUserPermission(LOGIN, "admin");
    addUserPermission(LOGIN, "scan");

    userRule.createUser("simple-user", "password");
  }

  @AfterClass
  public static void deleteAndRestoreData() {
    userRule.resetUsers();
    addGroupPermission("anyone", "scan");
  }

  @After
  public void resetProperties() throws Exception {
    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", null);
    setServerProperty(ORCHESTRATOR, "sonar.allowUsersToSignUp", null);
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
  public void run_analysis_with_token_authentication() {
    String tokenName = "Analyze Project";
    WsUserTokens.GenerateWsResponse generateWsResponse = userTokensWsClient.generate(new GenerateWsRequest()
      .setLogin(LOGIN)
      .setName(tokenName));
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", generateWsResponse.getToken(),
      "sonar.password", "");

    BuildResult buildResult = ORCHESTRATOR.executeBuild(sampleProject);

    assertThat(buildResult.isSuccess()).isTrue();
    userTokensWsClient.revoke(new RevokeWsRequest().setLogin(LOGIN).setName(tokenName));
  }

  @Test
  public void run_analysis_with_incorrect_token() {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    sampleProject.setProperties(
      "sonar.login", "unknown-token",
      "sonar.password", "");

    BuildResult buildResult = ORCHESTRATOR.executeBuildQuietly(sampleProject);

    assertThat(buildResult.isSuccess()).isFalse();
  }

  /**
   * This is currently a limitation of Ruby on Rails stack.
   */
  @Test
  public void basic_authentication_does_not_support_utf8_passwords() {
    String login = LOGIN;
    // see http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    String password = "κόσμε";

    // create user with a UTF-8 password
    userRule.createUser(login, LOGIN, null, password);

    // authenticate
    assertThat(checkAuthenticationWithAuthenticateWebService(login, password)).isFalse();
  }

  @Test
  public void allow_users_to_sign_up() throws IOException {
    setServerProperty(ORCHESTRATOR, "sonar.allowUsersToSignUp", "true");

    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("allow_users_to_sign_up",
        "/user/LocalAuthenticationTest/allow_users_to_sign_up.html").build()).runOn(ORCHESTRATOR);

    // This check is failing because signup doesn't refresh the users ES index !
    // Will be fixed by SONAR-7308
    // userRule.verifyUserExists("signuplogin", "SignUpName", null);
  }

  @Test
  public void authentication_through_ui() {
    new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("authentication",
      "/user/LocalAuthenticationTest/login_successful.html",
      "/user/LocalAuthenticationTest/login_wrong_password.html",
      "/user/LocalAuthenticationTest/should_not_be_unlogged_when_going_to_login_page.html",
      "/user/LocalAuthenticationTest/redirect_to_login_when_not_enough_privilege.html",
      // SONAR-2132
      "/user/LocalAuthenticationTest/redirect_to_original_url_after_direct_login.html",
      // SONAR-2009
      "/user/LocalAuthenticationTest/redirect_to_original_url_after_indirect_login.html").build()).runOn(ORCHESTRATOR);

    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", "true");

    new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("force-authentication",
      // SONAR-3473
      "/user/LocalAuthenticationTest/force-authentication.html").build()).runOn(ORCHESTRATOR);
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
