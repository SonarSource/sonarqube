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

import com.google.common.base.Joiner;
import com.sonar.orchestrator.Orchestrator;
import it.Category6Suite;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class OrganizationIdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category6Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(ORCHESTRATOR);

  private static String USER_LOGIN = "john";

  private static String GROUP = "group";

  private static WsClient adminWsClient;

  @BeforeClass
  public static void enableOrganizations() {
    enableOrganizationsSupport();
    adminWsClient = newAdminWsClient(ORCHESTRATOR);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "true");
  }

  private static void purgeSettings(){
    resetSettings(ORCHESTRATOR, null, "sonar.auth.fake-base-id-provider.enabled", "sonar.auth.fake-base-id-provider.user",
      "sonar.auth.fake-base-id-provider.throwUnauthorizedMessage", "sonar.auth.fake-base-id-provider.enabledGroupsSync", "sonar.auth.fake-base-id-provider.groups",
      "sonar.auth.fake-base-id-provider.allowsUsersToSignUp");
  }

  @AfterClass
  public static void cleanUp() throws Exception {
    purgeSettings();
  }

  @Before
  public void setUp() throws Exception {
    userRule.deactivateUsers(USER_LOGIN);
    userRule.removeGroups(GROUP);
    purgeSettings();
  }

  @Test
  public void default_group_is_not_added_for_new_user_when_organizations_are_enabled() throws Exception {
    enablePlugin();
    userRule.createGroup(GROUP);
    enableUserCreationByAuthPlugin();
    setGroupsReturnedByAuthPlugin(GROUP);

    authenticateWithFakeAuthProvider();

    // No default group membership
    userRule.verifyUserGroupMembership(USER_LOGIN, GROUP);
  }

  @Test
  public void default_group_is_not_sync_for_existing_user_when_organizations_are_enabled() throws Exception {
    enablePlugin();
    userRule.createGroup(GROUP);
    userRule.createUser(USER_LOGIN, "password");
    enableUserCreationByAuthPlugin();
    setGroupsReturnedByAuthPlugin(GROUP);

    authenticateWithFakeAuthProvider();

    // No default group membership
    userRule.verifyUserGroupMembership(USER_LOGIN, GROUP);
  }

  @Test
  public void remove_default_group_when_organizations_are_enabled() throws Exception {
    enablePlugin();
    userRule.createGroup(GROUP);
    userRule.createUser(USER_LOGIN, "password");
    // Add user as member of default organization
    adminWsClient.organizations().addMember("default-organization", USER_LOGIN);
    userRule.verifyUserGroupMembership(USER_LOGIN, "Members");
    enableUserCreationByAuthPlugin();
    // No group is returned by the plugin
    setGroupsReturnedByAuthPlugin();

    authenticateWithFakeAuthProvider();

    // No default group membership
    userRule.verifyUserGroupMembership(USER_LOGIN);
  }

  private static void enablePlugin() {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "true");
  }

  private static void enableUserCreationByAuthPlugin() {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", USER_LOGIN + ",fake-john,John,john@email.com");
  }

  private static void setGroupsReturnedByAuthPlugin(String... groups) {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabledGroupsSync", "true");
    if (groups.length > 0) {
      setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.groups", Joiner.on(",").join(groups));
    }
  }

  private static void authenticateWithFakeAuthProvider() {
    adminWsClient.wsConnector().call(
      new GetRequest("/sessions/init/fake-base-id-provider"))
      .failIfNotSuccessful();
  }

}
