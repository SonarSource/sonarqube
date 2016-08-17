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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.connectors.HttpClient4Connector;
import org.sonar.wsclient.services.AuthenticationQuery;
import org.sonar.wsclient.services.UserPropertyCreateQuery;
import org.sonar.wsclient.services.UserPropertyQuery;
import org.sonar.wsclient.user.UserParameters;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.WsResponse;
import util.selenium.SeleneseTest;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.setServerProperty;

/**
 * Test REALM authentication.
 *
 * It starts its own server as it's using a different authentication system
 */
public class RealmAuthenticationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final String TECH_USER = "techUser";
  static final String USER_LOGIN = "tester";

  /**
   * Property from security-plugin for user management.
   */
  private static final String USERS_PROPERTY = "sonar.fakeauthenticator.users";
  private static String AUTHORIZED = "authorized";
  private static String NOT_AUTHORIZED = "not authorized";

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(pluginArtifact("security-plugin"))
    .setServerProperty("sonar.security.realm", "FakeRealm")
    .build();

  @Before
  @After
  public void resetData() throws Exception {
    setServerProperty(orchestrator, USERS_PROPERTY, null);
    setServerProperty(orchestrator, "sonar.security.updateUserAttributes", null);
    setServerProperty(orchestrator, "sonar.authenticator.createUsers", null);
    resetUsers(USER_LOGIN, TECH_USER);
  }

  private void resetUsers(String... logins) {
    for (String login : logins) {
      String result = orchestrator.getServer().adminWsClient().get("/api/users/search?q=" + login);
      if (result.contains(login)) {
        orchestrator.getServer().adminWsClient().userClient().deactivate(login);
      }
    }
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
    assertThat(loginAttempt(username, password)).isEqualTo(AUTHORIZED);
    // with external details and groups
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("external-user-details",
        "/user/ExternalAuthenticationTest/external-user-details.html")
        .build()).runOn(orchestrator);

    // SONAR-4462
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("system-info",
        "/user/ExternalAuthenticationTest/system-info.html")
        .build()).runOn(orchestrator);
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
    assertThat(loginAttempt(username, password)).isEqualTo(AUTHORIZED);
    // with external details and groups
    // TODO replace by WS ? Or with new Selenese utils
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("external-user-details",
        "/user/ExternalAuthenticationTest/external-user-details.html")
        .build()).runOn(orchestrator);

    // Now update user details
    users.put(username + ".name", "Tester2 Testerovich");
    users.put(username + ".email", "tester2@example.org");
    updateUsersInExtAuth(users);
    // Then
    assertThat(loginAttempt(username, password)).isEqualTo(AUTHORIZED);
    // with external details and groups updated
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("external-user-details2",
        "/user/ExternalAuthenticationTest/external-user-details2.html").build()).runOn(orchestrator);
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
    assertThat(loginAttempt(login, password)).isEqualTo(AUTHORIZED);

    // When external system does not work
    users.remove(login + ".password");
    updateUsersInExtAuth(users);
    // Then
    assertThat(loginAttempt(login, password)).isEqualTo(NOT_AUTHORIZED);
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
    assertThat(loginAttempt(login, remotePassword)).isEqualTo(NOT_AUTHORIZED);
    assertThat(loginAttempt(login, localPassword)).isEqualTo(AUTHORIZED);
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
    assertThat(loginAttempt(username, password)).isEqualTo(NOT_AUTHORIZED);

    // When user created in external system
    users.put(username + ".password", password);
    updateUsersInExtAuth(users);
    // Then
    assertThat(loginAttempt(username, password)).isEqualTo(AUTHORIZED);
    assertThat(loginAttempt(username, "wrong")).isEqualTo(NOT_AUTHORIZED);
  }

  /**
   * SONAR-1334 (createUsers=false)
   */
  @Test
  public void shouldNotCreateNewUsers() {
    // Given clean Sonar installation and no users in external system
    setServerProperty(orchestrator, "sonar.authenticator.createUsers", "false");
    // Use a random user name because if we use existing disabled user then it doesn't work because rails doesn't handle this case
    // (it's using User.find_by_login to know if user exists or not
    String username = RandomStringUtils.randomAlphanumeric(20);
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user not exists in external system
    // Then
    assertThat(loginAttempt(username, password)).isEqualTo(NOT_AUTHORIZED);

    // When user created in external system
    users.put(username + ".password", password);
    updateUsersInExtAuth(users);
    // Then
    assertThat(loginAttempt(username, password)).isEqualTo(NOT_AUTHORIZED);
  }

  // SONAR-3258
  @Test
  public void shouldAutomaticallyReactivateDeletedUser() throws Exception {
    // Given clean Sonar installation and no users in external system

    // Let's create and delete the user "tester" in Sonar DB
    new SeleneseTest(
      Selenese.builder().setHtmlTestsInClasspath("external-user-create-and-delete-user",
        "/user/ExternalAuthenticationTest/create-and-delete-user.html").build()).runOn(orchestrator);

    // And now update the security with the user that was deleted
    String login = USER_LOGIN;
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();
    users.put(login + ".password", password);
    updateUsersInExtAuth(users);
    // check that the deleted/deactivated user "tester" has been reactivated and can now log in
    assertThat(loginAttempt(login, password)).isEqualTo(AUTHORIZED);
  }

  /**
   * SONAR-7036
   */
  @Test
  public void update_password_of_technical_user() throws Exception {
    // Create user in external authentication
    updateUsersInExtAuth(ImmutableMap.of(USER_LOGIN + ".password", USER_LOGIN));
    assertThat(loginAttempt(USER_LOGIN, USER_LOGIN)).isEqualTo(AUTHORIZED);

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
  public void authentication_with_ws() throws Exception {
    // Given clean Sonar installation and no users in external system
    String login = USER_LOGIN;
    String password = "1234567";
    Map<String, String> users = Maps.newHashMap();

    // When user created in external system
    users.put(login + ".password", password);
    updateUsersInExtAuth(users);

    assertThat(checkAuthenticationWithWebService(login, password).code()).isEqualTo(HTTP_OK);
    assertThat(checkAuthenticationWithWebService("wrong", password).code()).isEqualTo(HTTP_UNAUTHORIZED);
    assertThat(checkAuthenticationWithWebService(login, "wrong").code()).isEqualTo(HTTP_UNAUTHORIZED);
    assertThat(checkAuthenticationWithWebService(login, null).code()).isEqualTo(HTTP_UNAUTHORIZED);
    assertThat(checkAuthenticationWithWebService(null, null).code()).isEqualTo(HTTP_OK);

    setServerProperty(orchestrator, "sonar.forceAuthentication", "true");

    assertThat(checkAuthenticationWithWebService(login, password).code()).isEqualTo(HTTP_OK);
    assertThat(checkAuthenticationWithWebService("wrong", password).code()).isEqualTo(HTTP_UNAUTHORIZED);
    assertThat(checkAuthenticationWithWebService(login, "wrong").code()).isEqualTo(HTTP_UNAUTHORIZED);
    assertThat(checkAuthenticationWithWebService(login, null).code()).isEqualTo(HTTP_UNAUTHORIZED);
    assertThat(checkAuthenticationWithWebService(null, null).code()).isEqualTo(HTTP_UNAUTHORIZED);
  }

  protected void verifyHttpException(Exception e, int expectedCode) {
    assertThat(e).isInstanceOf(HttpException.class);
    HttpException exception = (HttpException) e;
    assertThat(exception.status()).isEqualTo(expectedCode);
  }

  private boolean checkAuthenticationThroughWebService(String login, String password) {
    return createWsClient(login, password).find(new AuthenticationQuery()).isValid();
  }

  /**
   * Utility method to check that user can be authorized.
   *
   * @throws IllegalStateException
   */
  private String loginAttempt(String username, String password) {
    String expectedValue = Long.toString(System.currentTimeMillis());
    Sonar wsClient = createWsClient(username, password);
    try {
      wsClient.create(new UserPropertyCreateQuery("auth", expectedValue));
    } catch (ConnectionException e) {
      return NOT_AUTHORIZED;
    }
    try {
      String value = wsClient.find(new UserPropertyQuery("auth")).getValue();
      if (!Objects.equals(value, expectedValue)) {
        // exceptional case - update+retrieval were successful, but value doesn't match
        throw new IllegalStateException("Expected " + expectedValue + " , but got " + value);
      }
    } catch (ConnectionException e) {
      // exceptional case - update was successful, but not retrieval
      throw new IllegalStateException(e);
    }
    return AUTHORIZED;
  }

  /**
   * Updates information about users in security-plugin.
   */
  private static void updateUsersInExtAuth(Map<String, String> users) {
    setServerProperty(orchestrator, USERS_PROPERTY, format(users));
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

  private static String format(Map<String, String> map) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }
    return sb.toString();
  }

  private WsResponse checkAuthenticationWithWebService(String login, String password) {
    WsClient wsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder().url(orchestrator.getServer().getUrl()).credentials(login, password).build());
    // Call any WS
    return wsClient.wsConnector().call(new GetRequest("api/rules/search"));
  }

}
