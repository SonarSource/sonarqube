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

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.net.HttpURLConnection;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.Users.SearchWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.users.CreateRequest;
import util.selenium.Selenese;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * There's only tests specific to OAuth2 in this class
 */
public class OAuth2IdentityProviderTest {

  @ClassRule
  public static Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  private static String FAKE_PROVIDER_KEY = "fake-oauth2-id-provider";

  private static String USER_LOGIN = "john";
  private static String USER_PROVIDER_ID = "fake-john";
  private static String USER_NAME = "John";
  private static String USER_EMAIL = "john@email.com";

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  private MockWebServer fakeServerAuthProvider;
  private String fakeServerAuthProviderUrl;

  @Before
  public void setUp() throws Exception {
    fakeServerAuthProvider = new MockWebServer();
    fakeServerAuthProvider.start();
    fakeServerAuthProviderUrl = fakeServerAuthProvider.url("").url().toString();
    resetData();
  }

  @After
  public void tearDown() throws Exception {
    resetData();
    fakeServerAuthProvider.shutdown();
  }

  private void resetData() {
    tester.settings().resetSettings(
      "sonar.auth.fake-oauth2-id-provider.enabled",
      "sonar.auth.fake-oauth2-id-provider.url",
      "sonar.auth.fake-oauth2-id-provider.user",
      "sonar.auth.fake-oauth2-id-provider.throwUnauthorizedMessage",
      "sonar.auth.fake-oauth2-id-provider.allowsUsersToSignUp");
  }

  @Test
  public void create_user_when_authenticating_for_the_first_time() {
    simulateRedirectionToCallback();
    enablePlugin();

    authenticateWithFakeAuthProvider();

    verifyUser(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void authenticate_user_through_ui() {
    simulateRedirectionToCallback();
    enablePlugin();

    Navigation nav = tester.openBrowser();
    nav.openLogin().useOAuth2().shouldBeLoggedIn();

    verifyUser(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void redirect_to_requested_page() {
    simulateRedirectionToCallback();
    enablePlugin();
    tester.users().generate(u -> u.setLogin(USER_LOGIN));
    // Give user global admin permission as we want to go to a page where authentication is required
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(USER_LOGIN).setPermission("admin"));

    Navigation nav = tester.openBrowser();
    // Try to go to the settings page
    nav.open("/settings");
    // User should be redirected to login page
    $("#login_form").should(Condition.exist);
    // User click on the link to authenticate with OAuth2
    $(".oauth-providers a").click();

    // User is correctly redirected to the settings page
    $("#settings-page").shouldBe(visible);
  }

  @Test
  public void display_unauthorized_page_when_authentication_failed_in_callback() {
    simulateRedirectionToCallback();
    enablePlugin();

    // As this property is null, the plugin will throw an exception
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.user", null);

    Selenese.runSelenese(orchestrator, "/user/OAuth2IdentityProviderTest/display_unauthorized_page_when_authentication_failed.html");

    assertThatUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void fail_to_authenticate_when_not_allowed_to_sign_up() {
    simulateRedirectionToCallback();
    enablePlugin();
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.allowsUsersToSignUp", "false");

    Selenese.runSelenese(orchestrator, "/user/OAuth2IdentityProviderTest/fail_to_authenticate_when_not_allowed_to_sign_up.html");

    assertThatUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void display_message_in_ui_but_not_in_log_when_unauthorized_exception_in_callback() throws Exception {
    simulateRedirectionToCallback();
    enablePlugin();
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.throwUnauthorizedMessage", "true");

    Selenese.runSelenese(orchestrator, "/user/OAuth2IdentityProviderTest/display_message_in_ui_but_not_in_log_when_unauthorized_exception.html");

    File logFile = orchestrator.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("A functional error has happened");
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("UnauthorizedException");

    assertThatUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void authenticate_new_user_when_email_already_exists() {
    simulateRedirectionToCallback();
    enablePlugin();
    tester.users().generate(u -> u.setLogin("another").setName("Another").setEmail(USER_EMAIL));

    tester.openBrowser()
      .logIn()
      .useOAuth2()
      .asEmailAlreadyExistsPage()
      .shouldHaveExistingAccount("another")
      .shouldHaveNewAccount(USER_LOGIN)
      .clickContinue();

    assertThat(tester.users().getByLogin(USER_LOGIN).get().getEmail()).isEqualTo(USER_EMAIL);
    assertThat(tester.users().getByLogin("another").get().getEmail()).isNullOrEmpty();
  }

  @Test
  public void authenticating_existing_user_using_existing_email() {
    simulateRedirectionToCallback();
    enablePlugin();
    tester.users().generate(u -> u.setLogin(USER_LOGIN).setName(USER_NAME).setEmail(null));
    tester.users().generate(u -> u.setLogin("another").setName("Another").setEmail(USER_EMAIL));

    tester.openBrowser()
      .logIn()
      .useOAuth2()
      .asEmailAlreadyExistsPage()
      .shouldHaveExistingAccount("another")
      .shouldHaveNewAccount("john")
      .clickContinue();

    assertThat(tester.users().getByLogin(USER_LOGIN).get().getEmail()).isEqualTo(USER_EMAIL);
    assertThat(tester.users().getByLogin("another").get().getEmail()).isNullOrEmpty();
  }

  @Test
  public void provision_user_before_authentication() {
    simulateRedirectionToCallback();
    enablePlugin();

    // Provision none local user in database
    tester.wsClient().users().create(new CreateRequest()
      .setLogin(USER_LOGIN)
      .setName(USER_NAME)
      .setEmail(USER_EMAIL)
      .setLocal("false"));
    User user = tester.users().getByLogin(USER_LOGIN).get();
    assertThat(user.getLocal()).isFalse();
    assertThat(user.getExternalIdentity()).isEqualTo(USER_LOGIN);
    assertThat(user.getExternalProvider()).isEqualTo("sonarqube");

    // Authenticate with external system -> It will update external provider info
    authenticateWithFakeAuthProvider();

    user = tester.users().getByLogin(USER_LOGIN).get();
    assertThat(user.getLocal()).isFalse();
    assertThat(user.getExternalIdentity()).isEqualTo(USER_PROVIDER_ID);
    assertThat(user.getExternalProvider()).isEqualTo(FAKE_PROVIDER_KEY);
  }

  private void verifyUser(String login, String name, String email) {
    User user = tester.users().getByLogin(login).orElseThrow(IllegalStateException::new);
    assertThat(user.getLogin()).isEqualTo(login);
    assertThat(user.getName()).isEqualTo(name);
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getActive()).isTrue();
  }

  private void authenticateWithFakeAuthProvider() {
    tester.wsClient().wsConnector().call(new GetRequest(("/sessions/init/" + FAKE_PROVIDER_KEY))).failIfNotSuccessful();
  }

  private void simulateRedirectionToCallback() {
    fakeServerAuthProvider.enqueue(new MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
      .addHeader("Location: " + orchestrator.getServer().getUrl() + "/oauth2/callback/" + FAKE_PROVIDER_KEY)
      .setBody("Redirect to SonarQube"));
    fakeServerAuthProvider.enqueue(new MockResponse()
      .setResponseCode(HttpURLConnection.HTTP_MOVED_TEMP)
      .addHeader("Location: " + orchestrator.getServer().getUrl() + "/oauth2/callback/" + FAKE_PROVIDER_KEY)
      .setBody("Redirect to SonarQube"));
  }

  private void enablePlugin() {
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.enabled", "true");
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.url", fakeServerAuthProviderUrl);
    tester.settings().setGlobalSettings("sonar.auth.fake-oauth2-id-provider.user", USER_LOGIN + "," + USER_PROVIDER_ID + "," + USER_NAME + "," + USER_EMAIL);
  }

  private void assertThatUserDoesNotExist(String login) {
    assertThat(tester.users().getByLogin(login)).isEmpty();
  }

}
