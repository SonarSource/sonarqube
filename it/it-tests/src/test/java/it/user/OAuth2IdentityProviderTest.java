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

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import java.io.File;
import java.net.HttpURLConnection;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.user.CreateRequest;
import pageobjects.Navigation;
import util.user.UserRule;
import util.user.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;
import static util.selenium.Selenese.runSelenese;

/**
 * There's only tests specific to OAuth2 in this class
 */
public class OAuth2IdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category4Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  static String FAKE_PROVIDER_KEY = "fake-oauth2-id-provider";

  static String USER_LOGIN = "john";
  static String USER_PROVIDER_ID = "fake-john";
  static String USER_NAME = "John";
  static String USER_EMAIL = "john@email.com";

  static WsClient adminWsClient;

  MockWebServer fakeServerAuthProvider;
  String fakeServerAuthProviderUrl;

  @BeforeClass
  public static void resetData() {
    ORCHESTRATOR.resetData();
    adminWsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @After
  public void resetUsers() throws Exception {
    userRule.resetUsers();
  }

  @Before
  public void setUp() throws Exception {
    fakeServerAuthProvider = new MockWebServer();
    fakeServerAuthProvider.start();
    fakeServerAuthProviderUrl = fakeServerAuthProvider.url("").url().toString();
    userRule.resetUsers();
    resetSettings(ORCHESTRATOR, null, "sonar.auth.fake-oauth2-id-provider.enabled",
      "sonar.auth.fake-oauth2-id-provider.url",
      "sonar.auth.fake-oauth2-id-provider.user",
      "sonar.auth.fake-oauth2-id-provider.throwUnauthorizedMessage",
      "sonar.auth.fake-oauth2-id-provider.allowsUsersToSignUp");
  }

  @After
  public void tearDown() throws Exception {
    fakeServerAuthProvider.shutdown();
  }

  @Test
  public void create_new_user_when_authenticate() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();

    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void authenticate_user_through_ui() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();

    Navigation.get(ORCHESTRATOR).openLogin().useOAuth2().shouldBeLoggedIn();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void display_unauthorized_page_when_authentication_failed_in_callback() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();

    // As this property is null, the plugin will throw an exception
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.user", null);

    runSelenese(ORCHESTRATOR,"/user/OAuth2IdentityProviderTest/display_unauthorized_page_when_authentication_failed.html");

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void fail_to_authenticate_when_not_allowed_to_sign_up() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.allowsUsersToSignUp", "false");

    runSelenese(ORCHESTRATOR, "/user/OAuth2IdentityProviderTest/fail_to_authenticate_when_not_allowed_to_sign_up.html");

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void display_message_in_ui_but_not_in_log_when_unauthorized_exception_in_callback() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.throwUnauthorizedMessage", "true");

    runSelenese(ORCHESTRATOR,"/user/OAuth2IdentityProviderTest/display_message_in_ui_but_not_in_log_when_unauthorized_exception.html");

    File logFile = ORCHESTRATOR.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("A functional error has happened");
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("UnauthorizedException");

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void fail_when_email_already_exists() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();
    userRule.createUser("another", "Another", USER_EMAIL, "another");

    runSelenese(ORCHESTRATOR,"/user/OAuth2IdentityProviderTest/fail_when_email_already_exists.html");

    File logFile = ORCHESTRATOR.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("You can't sign up because email 'john@email.com' is already used by an existing user. This means that you probably already registered with another account");
  }

  @Test
  public void provision_user_before_authentication() {
    simulateRedirectionToCallback();
    enablePlugin();

    // Provision none local user in database
    newAdminWsClient(ORCHESTRATOR).users().create(CreateRequest.builder()
      .setLogin(USER_LOGIN)
      .setName(USER_NAME)
      .setEmail(USER_EMAIL)
      .setLocal(false)
      .build());
    assertThat(userRule.getUserByLogin(USER_LOGIN).get())
      .extracting(Users.User::isLocal, Users.User::getExternalIdentity, Users.User::getExternalProvider)
      .containsOnly(false, USER_LOGIN, "sonarqube");

    // Authenticate with external system -> It will update external provider info
    authenticateWithFakeAuthProvider();

    assertThat(userRule.getUserByLogin(USER_LOGIN).get())
      .extracting(Users.User::isLocal, Users.User::getExternalIdentity, Users.User::getExternalProvider)
      .containsOnly(false, USER_PROVIDER_ID, FAKE_PROVIDER_KEY);
  }

  private void authenticateWithFakeAuthProvider() {
    WsResponse response = adminWsClient.wsConnector().call(
      new GetRequest(("/sessions/init/" + FAKE_PROVIDER_KEY)));
    assertThat(response.code()).isEqualTo(200);
  }

  private void simulateRedirectionToCallback() {
    fakeServerAuthProvider.enqueue(new MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
      .addHeader("Location: " + ORCHESTRATOR.getServer().getUrl() + "/oauth2/callback/" + FAKE_PROVIDER_KEY)
      .setBody("Redirect to SonarQube"));
  }

  private void enablePlugin() {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.enabled", "true");
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.url", fakeServerAuthProviderUrl);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-oauth2-id-provider.user", USER_LOGIN + "," + USER_PROVIDER_ID + "," + USER_NAME + "," + USER_EMAIL);
  }

}
