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

import com.google.common.base.Joiner;
import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.Users.SearchWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.usergroups.DeleteRequest;
import org.sonarqube.ws.client.users.CreateRequest;
import org.sonarqube.ws.client.users.DeactivateRequest;
import org.sonarqube.ws.client.users.GroupsRequest;
import org.sonarqube.ws.client.users.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.ws.UserGroups.Group;
import static util.selenium.Selenese.runSelenese;

public class BaseIdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = UserSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(ORCHESTRATOR).disableOrganizations();

  private static String FAKE_PROVIDER_KEY = "fake-base-id-provider";

  private static String USER_LOGIN = "john";
  private static String USER_PROVIDER_ID = "ABCD";
  private static String USER_PROVIDER_LOGIN = "fake-john";
  private static String USER_NAME = "John";
  private static String USER_EMAIL = "john@email.com";

  @After
  public void resetData() {
    tester.settings().resetSettings(
      "sonar.auth.fake-base-id-provider.enabled",
      "sonar.auth.fake-base-id-provider.user",
      "sonar.auth.fake-base-id-provider.throwUnauthorizedMessage",
      "sonar.auth.fake-base-id-provider.enabledGroupsSync",
      "sonar.auth.fake-base-id-provider.groups",
      "sonar.auth.fake-base-id-provider.allowsUsersToSignUp");
  }

  @Test
  public void create_new_user_when_authenticate() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
    assertThat(tester.users().getByLogin(USER_LOGIN)).isNotPresent();

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    assertThat(tester.users().service().search(new SearchRequest().setQ(USER_LOGIN)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(USER_LOGIN, USER_NAME, USER_EMAIL, FAKE_PROVIDER_KEY, USER_PROVIDER_LOGIN, false));
  }

  @Test
  public void authenticate_user_through_ui() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);

    Navigation.create(ORCHESTRATOR).openLogin().useOAuth2().shouldBeLoggedIn();

    assertThat(tester.users().service().search(new SearchRequest().setQ(USER_LOGIN)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(USER_LOGIN, USER_NAME, USER_EMAIL, FAKE_PROVIDER_KEY, USER_PROVIDER_LOGIN, false));
  }

  @Test
  public void display_unauthorized_page_when_authentication_failed() {
    enablePlugin();
    // As this property is null, the plugin will throw an exception
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.user", null);

    runSelenese(ORCHESTRATOR, "/user/BaseIdentityProviderTest/display_unauthorized_page_when_authentication_failed.html");

    assertThat(tester.users().getByLogin(USER_LOGIN)).isNotPresent();
  }

  @Test
  public void fail_when_email_already_exists() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
    tester.users().generate(u -> u.setLogin("another").setName("Another").setEmail(USER_EMAIL).setPassword("another"));

    runSelenese(ORCHESTRATOR, "/user/BaseIdentityProviderTest/fail_when_email_already_exists.html");

    File logFile = ORCHESTRATOR.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile))
      .doesNotContain("You can't sign up because email 'john@email.com' is already used by an existing user. This means that you probably already registered with another account");
  }

  @Test
  public void fail_to_authenticate_when_not_allowed_to_sign_up() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.allowsUsersToSignUp", "false");

    runSelenese(ORCHESTRATOR, "/user/BaseIdentityProviderTest/fail_to_authenticate_when_not_allowed_to_sign_up.html");

    assertThat(tester.users().getByLogin(USER_LOGIN)).isNotPresent();
  }

  @Test
  public void update_existing_user_when_authenticate() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    String newName = "John Doe";
    String newEmail = "john.doe@email.com";
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, newName, newEmail);

    // Second connection, user should be updated
    authenticateWithFakeAuthProvider();

    assertThat(tester.users().service().search(new SearchRequest().setQ(USER_LOGIN)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(USER_LOGIN, newName, newEmail, FAKE_PROVIDER_KEY, USER_PROVIDER_LOGIN, false));
  }

  @Test
  public void reactivate_disabled_user() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
    // First connection, user is created
    authenticateWithFakeAuthProvider();
    // Disable user
    tester.users().service().deactivate(new DeactivateRequest().setLogin(USER_LOGIN));

    // Second connection, user is reactivated
    authenticateWithFakeAuthProvider();

    assertThat(tester.users().service().search(new SearchRequest().setQ(USER_LOGIN)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(USER_LOGIN, USER_NAME, USER_EMAIL, FAKE_PROVIDER_KEY, USER_PROVIDER_LOGIN, false));
  }

  @Test
  public void not_authenticate_when_plugin_is_disabled() {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.enabled", "false");
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);

    authenticateWithFakeAuthProvider();

    // User is not created as nothing plugin is disabled
    assertThat(tester.users().getByLogin(USER_LOGIN)).isNotPresent();

    // TODO Add Selenium test to check login form
  }

  @Test
  public void display_message_in_ui_but_not_in_log_when_unauthorized_exception() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.throwUnauthorizedMessage", "true");

    runSelenese(ORCHESTRATOR,
      "/user/BaseIdentityProviderTest/display_message_in_ui_but_not_in_log_when_unauthorized_exception.html");

    File logFile = ORCHESTRATOR.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("A functional error has happened");
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("UnauthorizedException");

    assertThat(tester.users().getByLogin(USER_LOGIN)).isNotPresent();
  }

  @Test
  public void synchronize_groups_for_new_user() {
    enablePlugin();
    Group group1 = tester.groups().generate();
    Group group2 = tester.groups().generate();
    Group group3 = tester.groups().generate();
    try {
      setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
      setGroupsReturnedByAuthPlugin(group1.getName(), group2.getName(), "Another group");

      authenticateWithFakeAuthProvider();

      assertThat(tester.users().service().groups(new GroupsRequest().setLogin(USER_LOGIN)).getGroupsList())
        .extracting(org.sonarqube.ws.Users.GroupsWsResponse.Group::getName)
        .containsExactlyInAnyOrder(group1.getName(), group2.getName(), "sonar-users");
    } finally {
      deleteGroups(group1, group2, group3);
    }
  }

  @Test
  public void synchronize_groups_for_existing_user() {
    enablePlugin();
    Group group1 = tester.groups().generate();
    Group group2 = tester.groups().generate();
    Group group3 = tester.groups().generate();
    try {
      tester.users().generate(u -> u.setLogin(USER_LOGIN).setPassword("password"));
      tester.groups().addMemberToGroups(tester.organizations().getDefaultOrganization(), USER_LOGIN, group1.getName(), group2.getName());
      setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
      // Group1 is not returned by the plugin, user won't belong anymore to this group
      setGroupsReturnedByAuthPlugin(group2.getName(), group3.getName());

      authenticateWithFakeAuthProvider();

      assertThat(tester.users().service().groups(new GroupsRequest().setLogin(USER_LOGIN)).getGroupsList())
        .extracting(org.sonarqube.ws.Users.GroupsWsResponse.Group::getName)
        .containsExactlyInAnyOrder(group2.getName(), group3.getName(), "sonar-users");
    } finally {
      deleteGroups(group1, group2, group3);
    }
  }

  @Test
  public void remove_user_groups_when_groups_provided_by_plugin_are_empty() {
    enablePlugin();
    Group group = tester.groups().generate();
    try {
      tester.users().generate(u -> u.setLogin(USER_LOGIN).setPassword("password"));
      tester.groups().addMemberToGroups(tester.organizations().getDefaultOrganization(), USER_LOGIN, group.getName());
      setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);
      // No group is returned by the plugin
      setGroupsReturnedByAuthPlugin();

      authenticateWithFakeAuthProvider();

      // User is not member to any group
      assertThat(tester.users().service().groups(new GroupsRequest().setLogin(USER_LOGIN)).getGroupsList())
        .extracting(org.sonarqube.ws.Users.GroupsWsResponse.Group::getName)
        .containsExactlyInAnyOrder("sonar-users");
    } finally {
      deleteGroups(group);
    }
  }

  @Test
  public void allow_user_login_with_2_characters() {
    enablePlugin();
    String login = "jo";
    setUserCreatedByAuthPlugin(login, login, login, USER_NAME, USER_EMAIL);
    assertThat(tester.users().getByLogin(login)).isNotPresent();

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    assertThat(tester.users().getByLogin(login)).isPresent();
  }

  @Test
  public void provision_user_before_authentication() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_PROVIDER_LOGIN, USER_NAME, USER_EMAIL);

    // Provision none local user in database
    tester.users().service().create(new CreateRequest()
      .setLogin(USER_LOGIN)
      .setName(USER_NAME)
      .setEmail(USER_EMAIL)
      .setLocal("false"));
    assertThat(tester.users().service().search(new SearchRequest().setQ(USER_LOGIN)).getUsersList())
      .extracting(User::getLogin, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(USER_LOGIN, "sonarqube", USER_LOGIN, false));

    // Authenticate with external system -> It will update external provider info
    authenticateWithFakeAuthProvider();

    assertThat(tester.users().service().search(new SearchRequest().setQ(USER_LOGIN)).getUsersList())
      .extracting(User::getLogin, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(USER_LOGIN, FAKE_PROVIDER_KEY, USER_PROVIDER_LOGIN, false));
  }

  @Test
  public void update_login() {
    enablePlugin();
    String oldLogin = tester.users().generateLogin();
    String providerId = tester.users().generateProviderId();
    setUserCreatedByAuthPlugin(oldLogin, providerId, tester.users().generateLogin(), USER_NAME, USER_EMAIL);
    assertThat(tester.users().getByLogin(USER_LOGIN)).isNotPresent();
    authenticateWithFakeAuthProvider();

    // Login is updated
    String newLogin = tester.users().generateLogin();
    String newProviderLogin = tester.users().generateLogin();
    setUserCreatedByAuthPlugin(newLogin, providerId, newProviderLogin, USER_NAME, USER_EMAIL);
    authenticateWithFakeAuthProvider();

    assertThat(tester.users().getByLogin(oldLogin)).isNotPresent();
    assertThat(tester.users().service().search(new SearchRequest().setQ(newLogin)).getUsersList())
      .extracting(User::getLogin, User::getName, User::getEmail, User::getExternalProvider, User::getExternalIdentity, User::getLocal)
      .containsExactlyInAnyOrder(tuple(newLogin, USER_NAME, USER_EMAIL, FAKE_PROVIDER_KEY, newProviderLogin, false));
    // Check that searching for old login return nothing
    assertThat(tester.users().service().search(new SearchRequest().setQ(oldLogin)).getPaging().getTotal()).isZero();
  }

  @Test
  public void authenticate_with_external_id_null() {
    enablePlugin();
    String login = tester.users().generateLogin();
    setUserCreatedByAuthPlugin(login, null, login, USER_NAME, USER_EMAIL);

    // First authentication
    authenticateWithFakeAuthProvider();
    assertThat(tester.users().getByLogin(login)).isPresent();

    // De-activate and re-authenticate to check everything is ok
    tester.users().service().deactivate(new DeactivateRequest().setLogin(login));
    authenticateWithFakeAuthProvider();
    assertThat(tester.users().getByLogin(login)).isPresent();
  }

  @Test
  public void update_external_id() {
    enablePlugin();
    String login = tester.users().generateLogin();
    setUserCreatedByAuthPlugin(login, tester.users().generateProviderId(), login, USER_NAME, USER_EMAIL);
    authenticateWithFakeAuthProvider();
    assertThat(tester.users().getByLogin(login)).isPresent();

    setUserCreatedByAuthPlugin(login, tester.users().generateProviderId(), login, USER_NAME, USER_EMAIL);
    authenticateWithFakeAuthProvider();
    assertThat(tester.users().getByLogin(login)).isPresent();
  }

  private void enablePlugin() {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.enabled", "true");
  }

  private void setUserCreatedByAuthPlugin(String login, @Nullable String providerId, String providerLogin, String name, String email) {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.user",
      login + "," + (providerId == null ? "" : providerId) + "," + providerLogin + "," + name + "," + email);
  }

  private void setGroupsReturnedByAuthPlugin(String... groups) {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.enabledGroupsSync", "true");
    if (groups.length > 0) {
      tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.groups", Joiner.on(",").join(groups));
    }
  }

  private void authenticateWithFakeAuthProvider() {
    tester.wsClient().wsConnector().call(
      new GetRequest("/sessions/init/" + FAKE_PROVIDER_KEY))
      .failIfNotSuccessful();
  }

  private void deleteGroups(Group... groups) {
    List<String> allGroups = tester.wsClient().userGroups().search(new org.sonarqube.ws.client.usergroups.SearchRequest()).getGroupsList().stream().map(Group::getName)
      .collect(Collectors.toList());
    Arrays.stream(groups)
      .filter(g -> allGroups.contains(g.getName()))
      .forEach(g -> tester.wsClient().userGroups().delete(new DeleteRequest().setName(g.getName())));
  }

}
