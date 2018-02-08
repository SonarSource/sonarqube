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
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.users.CreateRequest;
import util.user.UserRule;
import util.user.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;
import static util.selenium.Selenese.runSelenese;

/**
 * TODO : Add missing ITs
 * - display multiple identity provider plugins (probably in another class)
 */
public class BaseIdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = UserSuite.ORCHESTRATOR;

  private static UserRule userRule = UserRule.from(ORCHESTRATOR);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(ORCHESTRATOR).around(userRule);

  static String FAKE_PROVIDER_KEY = "fake-base-id-provider";

  static String USER_LOGIN = "john";
  static String USER_PROVIDER_ID = "fake-john";
  static String USER_NAME = "John";
  static String USER_EMAIL = "john@email.com";

  static String USER_NAME_UPDATED = "John Doe";
  static String USER_EMAIL_UPDATED = "john.doe@email.com";

  static String GROUP1 = "group1";
  static String GROUP2 = "group2";
  static String GROUP3 = "group3";

  static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    adminWsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @Before
  @After
  public void resetData() {
    userRule.resetUsers();
    userRule.removeGroups(GROUP1, GROUP2, GROUP3);
    resetSettings(ORCHESTRATOR, null,
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
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL, false);
  }

  @Test
  public void authenticate_user_through_ui() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    Navigation.create(ORCHESTRATOR).openLogin().useOAuth2().shouldBeLoggedIn();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void display_unauthorized_page_when_authentication_failed() {
    enablePlugin();
    // As this property is null, the plugin will throw an exception
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", null);

    runSelenese(ORCHESTRATOR, "/user/BaseIdentityProviderTest/display_unauthorized_page_when_authentication_failed.html");

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void fail_when_email_already_exists() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    userRule.createUser("another", "Another", USER_EMAIL, "another");

    runSelenese(ORCHESTRATOR, "/user/BaseIdentityProviderTest/fail_when_email_already_exists.html");

    File logFile = ORCHESTRATOR.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile))
      .doesNotContain("You can't sign up because email 'john@email.com' is already used by an existing user. This means that you probably already registered with another account");
  }

  @Test
  public void fail_to_authenticate_when_not_allowed_to_sign_up() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.allowsUsersToSignUp", "false");

    runSelenese(ORCHESTRATOR, "/user/BaseIdentityProviderTest/fail_to_authenticate_when_not_allowed_to_sign_up.html");

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void update_existing_user_when_authenticate() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME_UPDATED, USER_EMAIL_UPDATED);

    // Second connection, user should be updated
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME_UPDATED, USER_EMAIL_UPDATED);
  }

  @Test
  public void reactivate_disabled_user() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    Optional<Users.User> user = userRule.getUserByLogin(USER_LOGIN);
    assertThat(user).isPresent();

    // Disable user
    userRule.deactivateUsers(USER_LOGIN);

    // Second connection, user is reactivated
    authenticateWithFakeAuthProvider();
    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void not_authenticate_when_plugin_is_disabled() {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "false");
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    authenticateWithFakeAuthProvider();

    // User is not created as nothing plugin is disabled
    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // TODO Add Selenium test to check login form
  }

  @Test
  public void display_message_in_ui_but_not_in_log_when_unauthorized_exception() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.throwUnauthorizedMessage", "true");

    runSelenese(ORCHESTRATOR,
      "/user/BaseIdentityProviderTest/display_message_in_ui_but_not_in_log_when_unauthorized_exception.html");

    File logFile = ORCHESTRATOR.getServer().getWebLogs();
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("A functional error has happened");
    assertThat(FileUtils.readFileToString(logFile)).doesNotContain("UnauthorizedException");

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void synchronize_groups_for_new_user() {
    enablePlugin();
    userRule.createGroup(GROUP1);
    userRule.createGroup(GROUP2);
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    // Group3 doesn't exist in DB, user won't belong to this group
    setGroupsReturnedByAuthPlugin(GROUP1, GROUP2, GROUP3);

    authenticateWithFakeAuthProvider();

    userRule.verifyUserGroupMembership(USER_LOGIN, GROUP1, GROUP2, "sonar-users");
  }

  @Test
  public void synchronize_groups_for_existing_user() {
    enablePlugin();
    userRule.createGroup(GROUP1);
    userRule.createGroup(GROUP2);
    userRule.createGroup(GROUP3);
    userRule.createUser(USER_LOGIN, "password");
    userRule.associateGroupsToUser(USER_LOGIN, GROUP1, GROUP2);
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    // Group1 is not returned by the plugin, user won't belong anymore to this group
    setGroupsReturnedByAuthPlugin(GROUP2, GROUP3);

    authenticateWithFakeAuthProvider();

    userRule.verifyUserGroupMembership(USER_LOGIN, GROUP2, GROUP3, "sonar-users");
  }

  @Test
  public void remove_user_groups_when_groups_provided_by_plugin_are_empty() {
    enablePlugin();
    userRule.createGroup(GROUP1);
    userRule.createUser(USER_LOGIN, "password");
    userRule.associateGroupsToUser(USER_LOGIN, GROUP1);
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    // No group is returned by the plugin
    setGroupsReturnedByAuthPlugin();

    authenticateWithFakeAuthProvider();

    // User is not member to any group
    userRule.verifyUserGroupMembership(USER_LOGIN, "sonar-users");
  }

  @Test
  public void allow_user_login_with_2_characters() {
    enablePlugin();
    String login = "jo";
    setUserCreatedByAuthPlugin(login, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    userRule.verifyUserDoesNotExist(login);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(login, USER_NAME, USER_EMAIL, false);
  }

  @Test
  public void provision_user_before_authentication() {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    // Provision none local user in database
    newAdminWsClient(ORCHESTRATOR).users().create(new CreateRequest()
      .setLogin(USER_LOGIN)
      .setName(USER_NAME)
      .setEmail(USER_EMAIL)
      .setLocal("false"));
    assertThat(userRule.getUserByLogin(USER_LOGIN).get())
      .extracting(Users.User::isLocal, Users.User::getExternalIdentity, Users.User::getExternalProvider)
      .containsOnly(false, USER_LOGIN, "sonarqube");

    // Authenticate with external system -> It will update external provider info
    authenticateWithFakeAuthProvider();

    assertThat(userRule.getUserByLogin(USER_LOGIN).get())
      .extracting(Users.User::isLocal, Users.User::getExternalIdentity, Users.User::getExternalProvider)
      .containsOnly(false, USER_PROVIDER_ID, FAKE_PROVIDER_KEY);
  }

  private static void enablePlugin() {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "true");
  }

  private static void setUserCreatedByAuthPlugin(String login, String providerId, String name, String email) {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", login + "," + providerId + "," + name + "," + email);
  }

  private static void setGroupsReturnedByAuthPlugin(String... groups) {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabledGroupsSync", "true");
    if (groups.length > 0) {
      setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.groups", Joiner.on(",").join(groups));
    }
  }

  private static void authenticateWithFakeAuthProvider() {
    adminWsClient.wsConnector().call(
      new GetRequest("/sessions/init/" + FAKE_PROVIDER_KEY))
      .failIfNotSuccessful();
  }

}
