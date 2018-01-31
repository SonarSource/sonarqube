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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.connectors.HttpClient4Connector;
import org.sonar.wsclient.services.AuthenticationQuery;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.SystemInfoPage;
import org.sonarqube.qa.util.pageobjects.UsersManagementPage;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.users.CreateRequest;
import util.user.UserRule;
import util.user.Users;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.resetSettings;
import static util.selenium.Selenese.runSelenese;

/**
 * Test REALM authentication.
 *
 * It starts its own server as it's using a different authentication system
 */
public class RealmAuthenticationTest {

  private static final String TECH_USER = "techUser";
  private static final String USER_LOGIN = "tester";
  private static final String ADMIN_USER_LOGIN = "admin-user";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * Property from security-plugin for user management.
   */
  private static final String USERS_PROPERTY = "sonar.fakeauthenticator.users";

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(pluginArtifact("security-plugin"))
    .setServerProperty("sonar.security.realm", "FakeRealm")
    .build();

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  @After
  public void resetData() {
    resetSettings(orchestrator, null, USERS_PROPERTY, "sonar.security.updateUserAttributes");
  }

  @Before
  public void initAdminUser() {
    userRule.createAdminUser(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN);
  }

  @After
  public void deleteAdminUser() {
    userRule.resetUsers();
  }

  /**
   * SONAR-3137, SONAR-2292
   * Restriction on password length (minimum 4 characters) should be disabled, when external system enabled.
   */
  @Test
  public void shouldSynchronizeDetailsAndGroups() {
    // Given clean Sonar installation and no users in external system
    String username = USER_LOGIN;
    String password = "123";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(username + ".password", password);
    users.put(username + ".name", "Tester Testerovich");
    users.put(username + ".email", "tester@example.org");
    users.put(username + ".groups", "sonar-user");
    updateUsersInExtAuth(users);
    // Then
    verifyAuthenticationIsOk(username, password);

    // with external details and groups
    runSelenese(orchestrator, "/user/ExternalAuthenticationTest/external-user-details.html");

    // SONAR-4462
    SystemInfoPage page = tester.openBrowser().logIn().submitCredentials(ADMIN_USER_LOGIN).openSystemInfo();
    page.getCardItem("System").shouldHaveFieldWithValue("External User Authentication", "FakeRealm");
  }

  /**
   * SONAR-4034
   */
  @Test
  public void shouldUpdateDetailsByDefault() {
    // Given clean Sonar installation and no users in external system
    String username = USER_LOGIN;
    String password = "123";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(username + ".password", password);
    users.put(username + ".name", "Tester Testerovich");
    users.put(username + ".email", "tester@example.org");
    users.put(username + ".groups", "sonar-user");
    updateUsersInExtAuth(users);
    // Then
    verifyAuthenticationIsOk(username, password);

    // with external details and groups
    // TODO replace by WS ? Or with new Selenese utils
    runSelenese(orchestrator, "/user/ExternalAuthenticationTest/external-user-details.html");

    // Now update user details
    users.put(username + ".name", "Tester2 Testerovich");
    users.put(username + ".email", "tester2@example.org");
    updateUsersInExtAuth(users);
    // Then
    verifyAuthenticationIsOk(username, password);

    // with external details and groups updated
    runSelenese(orchestrator, "/user/ExternalAuthenticationTest/external-user-details2.html");
  }

  /**
   * SONAR-3138
   */
  @Test
  public void shouldNotFallback() {
    // Given clean Sonar installation and no users in external system
    String login = USER_LOGIN;
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(login + ".password", password);
    updateUsersInExtAuth(users);
    // Then
    verifyAuthenticationIsOk(login, password);

    // When external system does not work
    users.remove(login + ".password");
    updateUsersInExtAuth(users);
    // Then
    verifyAuthenticationIsNotOk(login, password);
  }

  /**
   * SONAR-4543
   */
  @Test
  public void adminIsLocalAccountByDefault() {
    // Given clean Sonar installation and no users in external system
    String login = "admin";
    String localPassword = "admin";
    String remotePassword = "nimda";
    Map<String, String> users = Maps.newHashMap();

    // When admin created in external system with a different password
    users.put(login + ".password", remotePassword);
    updateUsersInExtAuth(users);

    // Then this is local DB that should be used
    verifyAuthenticationIsNotOk(login, remotePassword);
    verifyAuthenticationIsOk(login, localPassword);
  }

  /**
   * SONAR-1334, SONAR-3185 (createUsers=true is default)
   */
  @Test
  public void shouldCreateNewUsers() {
    // Given clean Sonar installation and no users in external system
    String username = USER_LOGIN;
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user not exists in external system
    // Then
    verifyAuthenticationIsNotOk(username, password);

    // When user created in external system
    users.put(username + ".password", password);
    updateUsersInExtAuth(users);
    // Then
    verifyAuthenticationIsOk(username, password);
    verifyAuthenticationIsNotOk(username, "wrong");
  }

  // SONAR-3258
  @Test
  public void shouldAutomaticallyReactivateDeletedUser() {
    // Given clean Sonar installation and no users in external system

    // Let's create and delete the user "tester" in Sonar DB
    Navigation nav = tester.openBrowser();
    UsersManagementPage page = nav.logIn().submitCredentials(ADMIN_USER_LOGIN).openUsersManagement();
    page
      .createUser(USER_LOGIN)
      .hasUsersCount(3)
      .getUser(USER_LOGIN)
      .deactivateUser();
    page.hasUsersCount(2);
    nav.logOut()
      .logIn().submitWrongCredentials(USER_LOGIN, USER_LOGIN)
      .getErrorMessage().shouldHave(Condition.text("Authentication failed"));

    // And now update the security with the user that was deleted
    String login = USER_LOGIN;
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();
    users.put(login + ".password", password);
    updateUsersInExtAuth(users);
    // check that the deleted/deactivated user "tester" has been reactivated and can now log in
    verifyAuthenticationIsOk(login, password);
  }

  /**
   * SONAR-7036
   */
  @Test
  public void update_password_of_technical_user() {
    // Create user in external authentication
    updateUsersInExtAuth(ImmutableMap.of(USER_LOGIN + ".password", USER_LOGIN));
    verifyAuthenticationIsOk(USER_LOGIN, USER_LOGIN);

    // Create technical user in db
    createUserInDb(TECH_USER, "old_password");
    assertThat(checkAuthenticationThroughWebService(TECH_USER, "old_password")).isTrue();

    // Updating password of technical user is allowed
    updateUserPasswordInDb(TECH_USER, "new_password");
    assertThat(checkAuthenticationThroughWebService(TECH_USER, "new_password")).isTrue();

    // But updating password of none local user is not allowed
    try {
      updateUserPasswordInDb(USER_LOGIN, "new_password");
      fail();
    } catch (HttpException e) {
      verifyHttpException(e, 400);
    }
  }

  /**
   * SONAR-7640
   */
  @Test
  public void authentication_with_ws() {
    // Given clean Sonar installation and no users in external system
    String login = USER_LOGIN;
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(login + ".password", password);
    updateUsersInExtAuth(users);

    verifyAuthenticationIsOk(login, password);
    verifyAuthenticationIsNotOk("wrong", password);
    verifyAuthenticationIsNotOk(login, "wrong");
    verifyAuthenticationIsNotOk(login, null);
    verifyAuthenticationIsOk(null, null);

    tester.settings().setGlobalSettings("sonar.forceAuthentication", "true");

    verifyAuthenticationIsOk(login, password);
    verifyAuthenticationIsNotOk("wrong", password);
    verifyAuthenticationIsNotOk(login, "wrong");
    verifyAuthenticationIsNotOk(login, null);
    verifyAuthenticationIsNotOk(null, null);
  }

  @Test
  public void allow_user_login_with_2_characters() {
    String username = "jo";
    String password = "1234567";
    updateUsersInExtAuth(ImmutableMap.of(username + ".password", password));

    verifyAuthenticationIsOk(username, password);
  }

  @Test
  public void provision_user_before_authentication() {
    tester.wsClient().users().create(new CreateRequest()
      .setLogin(USER_LOGIN)
      .setName("Tester Testerovich")
      .setEmail("tester@example.org")
      .setLocal("false"));
    // The user is created in SonarQube but doesn't exist yet in external authentication system
    verifyAuthenticationIsNotOk(USER_LOGIN, "123");

    updateUsersInExtAuth(ImmutableMap.of(
      USER_LOGIN + ".password", "123",
      USER_LOGIN + ".name", "Tester Testerovich",
      USER_LOGIN + ".email", "tester@example.org"));

    verifyAuthenticationIsOk(USER_LOGIN, "123");
    assertThat(userRule.getUserByLogin(USER_LOGIN).get())
      .extracting(Users.User::isLocal, Users.User::getExternalIdentity, Users.User::getExternalProvider)
      .containsOnly(false, USER_LOGIN, "sonarqube");
  }

  @Test
  public void fail_to_authenticate_user_when_email_already_exists() {
    userRule.createUser("another", "Another", "tester@example.org", "another");

    String username = USER_LOGIN;
    String password = "123";
    Map<String, String> users = Maps.newHashMap();
    users.put(username + ".password", password);
    users.put(username + ".name", "Tester Testerovich");
    users.put(username + ".email", "tester@example.org");
    users.put(username + ".groups", "sonar-user");
    updateUsersInExtAuth(users);

    verifyAuthenticationIsNotOk(username, password);
  }

  private void verifyHttpException(Exception e, int expectedCode) {
    assertThat(e).isInstanceOf(HttpException.class);
    HttpException exception = (HttpException) e;
    assertThat(exception.status()).isEqualTo(expectedCode);
  }

  private boolean checkAuthenticationThroughWebService(String login, String password) {
    return createWsClient(login, password).find(new AuthenticationQuery()).isValid();
  }

  /**
   * Updates information about users in security-plugin.
   */
  private void updateUsersInExtAuth(Map<String, String> users) {
    tester.settings().setGlobalSettings(USERS_PROPERTY, format(users));
  }

  private void createUserInDb(String login, String password) {
    orchestrator.getServer().adminWsClient().userClient().create(UserParameters.create().login(login).name(login)
      .password(password).passwordConfirmation(password));
  }

  private void updateUserPasswordInDb(String login, String newPassword) {
    orchestrator.getServer().adminWsClient().post("/api/users/change_password", "login", login, "password", newPassword);
  }

  /**
   * Utility method to create {@link Sonar} with specified {@code username} and {@code password}.
   * Orchestrator does not provide such method.
   */
  private Sonar createWsClient(String username, String password) {
    return new Sonar(new HttpClient4Connector(new Host(orchestrator.getServer().getUrl(), username, password)));
  }

  @CheckForNull
  private static String format(Map<String, String> map) {
    if (map.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }
    return sb.toString();
  }

  private void verifyAuthenticationIsOk(String login, String password) {
    assertThat(checkAuthenticationWithWebService(login, password).code()).isEqualTo(HTTP_OK);
  }

  private void verifyAuthenticationIsNotOk(String login, String password) {
    assertThat(checkAuthenticationWithWebService(login, password).code()).isEqualTo(HTTP_UNAUTHORIZED);
  }

  private WsResponse checkAuthenticationWithWebService(String login, String password) {
    // Call any WS
    return newUserWsClient(orchestrator, login, password).wsConnector().call(new GetRequest("api/rules/search"));
  }

}
