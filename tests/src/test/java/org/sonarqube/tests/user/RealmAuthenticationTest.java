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
import com.sonar.orchestrator.Orchestrator;
import java.util.Collections;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.qa.util.pageobjects.SystemInfoPage;
import org.sonarqube.qa.util.pageobjects.UsersManagementPage;
import org.sonarqube.ws.UserGroups.Group;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.SearchWsResponse.User;
import org.sonarqube.ws.client.users.ChangePasswordRequest;
import org.sonarqube.ws.client.users.SearchRequest;
import util.ItUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static util.ItUtils.expectHttpError;
import static util.ItUtils.newOrchestrator;
import static util.ItUtils.pluginArtifact;

/**
 * Test REALM authentication.
 *
 * It starts its own server as it's using a different authentication system
 */
public class RealmAuthenticationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static final Orchestrator orchestrator = ItUtils.newOrchestrator(
    builder -> builder
      .addPlugin(pluginArtifact("security-plugin"))
      .setServerProperty("sonar.security.realm", "FakeRealm"));

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  /**
   * SONAR-3137, SONAR-2292
   * Restriction on password length (minimum 4 characters) should be disabled, when external system enabled.
   */
  @Test
  public void synchronize_details_and_groups() {
    // Given clean Sonar installation and no users in external system
    String username = tester.users().generateLogin();
    String password = "123";
    Group group = tester.groups().generate();

    // When user created in external system
    updateUsersInExtAuth(ImmutableMap.of(
      username + ".password", password,
      username + ".name", "Tester Testerovich",
      username + ".email", "tester@example.org",
      username + ".groups", group.getName()));

    // Then
    verifyAuthenticationIsOk(username, password);
    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(username)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail,
        User::getExternalIdentity, User::getExternalProvider, User::getLocal, u -> u.getGroups().getGroupsList())
      .containsExactlyInAnyOrder(tuple(username, "Tester Testerovich", "tester@example.org", username, "sonarqube", false, asList(group.getName(), "sonar-users")));

    // SONAR-4462
    Users.CreateWsResponse.User adminUser = tester.users().generateAdministrator();
    SystemInfoPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin()).openSystemInfo();
    page.getCardItem("System").shouldHaveFieldWithValue("External User Authentication", "FakeRealm");
  }

  /**
   * SONAR-4034
   */
  @Test
  public void update_details_by_default() {
    // Given clean Sonar installation and no users in external system
    String username = tester.users().generateLogin();
    String password = "123";

    // When user created in external system
    updateUsersInExtAuth(ImmutableMap.of(
      username + ".password", password,
      username + ".name", "Tester Testerovich",
      username + ".email", "tester@example.org"));

    // Then
    verifyAuthenticationIsOk(username, password);
    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(username)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail)
      .containsExactlyInAnyOrder(tuple(username, "Tester Testerovich", "tester@example.org"));

    // Now update user details
    updateUsersInExtAuth(ImmutableMap.of(
      username + ".password", password,
      username + ".name", "Tester2 Testerovich",
      username + ".email", "tester2@example.org"));

    // Then
    verifyAuthenticationIsOk(username, password);

    // with external details and groups updated
    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(username)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail)
      .containsExactlyInAnyOrder(tuple(username, "Tester2 Testerovich", "tester2@example.org"));
  }

  /**
   * SONAR-3138
   */
  @Test
  public void does_not_fallback() {
    // Given clean Sonar installation and no users in external system
    String login = tester.users().generateLogin();
    String password = "1234567";

    // When user created in external system
    updateUsersInExtAuth(ImmutableMap.of(login + ".password", password));
    // Then
    verifyAuthenticationIsOk(login, password);

    // When external system does not work
    updateUsersInExtAuth(Collections.emptyMap());
    // Then
    verifyAuthenticationIsNotOk(login, password);
  }

  /**
   * SONAR-4543
   */
  @Test
  public void admin_is_local_account_by_default() {
    // Given clean Sonar installation and no users in external system
    String login = "admin";
    String localPassword = "admin";
    String remotePassword = "nimda";

    // When admin created in external system with a different password
    updateUsersInExtAuth(ImmutableMap.of(login + ".password", remotePassword));

    // Then this is local DB that should be used
    verifyAuthenticationIsNotOk(login, remotePassword);
    verifyAuthenticationIsOk(login, localPassword);
  }

  /**
   * SONAR-1334, SONAR-3185 (createUsers=true is default)
   */
  @Test
  public void create_new_users() {
    // Given clean Sonar installation and no users in external system
    String username = tester.users().generateLogin();
    String password = "1234567";

    // When user not exists in external system
    // Then
    verifyAuthenticationIsNotOk(username, password);

    // When user created in external system
    updateUsersInExtAuth(ImmutableMap.of(username + ".password", password));
    // Then
    verifyAuthenticationIsOk(username, password);
    verifyUser(username);
    verifyAuthenticationIsNotOk(username, "wrong");
  }

  // SONAR-3258
  @Test
  public void reactivate_deleted_user() {
    // Given clean Sonar installation and no users in external system
    Users.CreateWsResponse.User adminUser = tester.users().generateAdministrator();

    // Let's create and delete the user "tester" in Sonar DB
    String login = tester.users().generateLogin();
    Navigation nav = tester.openBrowser();
    UsersManagementPage page = nav.logIn().submitCredentials(adminUser.getLogin()).openUsersManagement();
    page
      .createUser(login)
      .hasUsersCount(3)
      .getUser(login)
      .deactivateUser();
    page.hasUsersCount(2);
    nav.logOut()
      .logIn().submitWrongCredentials(login, login)
      .getErrorMessage().shouldHave(Condition.text("Authentication failed"));

    // And now update the security with the user that was deleted
    String password = "1234567";
    updateUsersInExtAuth(ImmutableMap.of(login + ".password", password));
    // check that the deleted/deactivated user "tester" has been reactivated and can now log in
    verifyAuthenticationIsOk(login, password);
    verifyUser(login);
  }

  /**
   * SONAR-7036
   */
  @Test
  public void update_password_of_technical_user() {
    // Create user in external authentication
    String login = tester.users().generateLogin();
    updateUsersInExtAuth(ImmutableMap.of(login + ".password", login));
    verifyAuthenticationIsOk(login, login);

    // Create technical user in db
    Users.CreateWsResponse.User techUser = tester.users().generate(u -> u.setPassword("old_password"));
    verifyAuthenticationIsOk(techUser.getLogin(), "old_password");

    // Updating password of technical user is allowed
    tester.users().service().changePassword(new ChangePasswordRequest().setLogin(techUser.getLogin()).setPassword("new_password"));
    verifyAuthenticationIsOk(techUser.getLogin(), "new_password");

    // But updating password of none local user is not allowed
    expectHttpError(400, () -> tester.users().service().changePassword(new ChangePasswordRequest().setLogin(login).setPassword("new_password")));
  }

  /**
   * SONAR-7640
   */
  @Test
  public void authentication_with_ws() {
    // Given clean Sonar installation and no users in external system
    String login = tester.users().generateLogin();
    String password = "1234567";

    // When user created in external system
    updateUsersInExtAuth(ImmutableMap.of(login + ".password", password));

    verifyAuthenticationIsOk(login, password);
    verifyUser(login);
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
    Users.CreateWsResponse.User user = tester.users().generate(u -> u.setName("Tester Testerovich")
      .setEmail("tester@example.org")
      .setPassword(null)
      .setLocal("false"));
    // The user is created in SonarQube but doesn't exist yet in external authentication system
    verifyAuthenticationIsNotOk(user.getLogin(), "123");

    updateUsersInExtAuth(ImmutableMap.of(
      user.getLogin() + ".password", "123",
      user.getLogin() + ".name", "Tester Testerovich",
      user.getLogin() + ".email", "tester@example.org"));

    verifyAuthenticationIsOk(user.getLogin(), "123");
    verifyUser(user.getLogin());
  }

  @Test
  public void fail_to_authenticate_user_when_email_already_exists() {
    Users.CreateWsResponse.User user = tester.users().generate();
    String username = tester.users().generateLogin();
    String password = "123";

    updateUsersInExtAuth(ImmutableMap.of(
      username + ".password", password,
      username + ".email", user.getEmail()));

    verifyAuthenticationIsNotOk(username, password);
  }

  @Test
  public void fail_to_authenticate_user_when_email_already_exists_on_several_users() {
    Users.CreateWsResponse.User user1 = tester.users().generate(u -> u.setEmail("user@email.com"));
    Users.CreateWsResponse.User user2 = tester.users().generate(u -> u.setEmail("user@email.com"));
    String username = tester.users().generateLogin();
    String password = "123";

    updateUsersInExtAuth(ImmutableMap.of(
      username + ".password", password,
      username + ".email", "user@email.com"));

    verifyAuthenticationIsNotOk(username, password);
  }

  /**
   * Updates information about users in security-plugin.
   */
  private void updateUsersInExtAuth(Map<String, String> users) {
    tester.settings().setGlobalSettings("sonar.fakeauthenticator.users", format(users));
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
    tester.as(login, password).wsClient().system().ping();
  }

  private void verifyAuthenticationIsNotOk(String login, String password) {
    expectHttpError(401, () -> tester.as(login, password).wsClient().system().ping());
  }

  private void verifyUser(String login) {
    assertThat(tester.wsClient().users().search(new SearchRequest().setQ(login)).getUsersList())
      .extracting(User::getLogin, User::getExternalIdentity, User::getExternalProvider, User::getLocal)
      .containsExactlyInAnyOrder(tuple(login, login, "sonarqube", false));
  }

}
