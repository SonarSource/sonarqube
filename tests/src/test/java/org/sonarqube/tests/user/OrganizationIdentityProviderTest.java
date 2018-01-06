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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category6Suite;
import org.sonarqube.ws.UserGroups.Group;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.organizations.AddMemberRequest;

public class OrganizationIdentityProviderTest {

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    // enable the fake authentication plugin
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.enabled", "true");
  }

  @After
  public void tearDown() {
    tester.settings().resetSettings("sonar.auth.fake-base-id-provider.enabled", "sonar.auth.fake-base-id-provider.user",
      "sonar.auth.fake-base-id-provider.throwUnauthorizedMessage", "sonar.auth.fake-base-id-provider.enabledGroupsSync", "sonar.auth.fake-base-id-provider.groups",
      "sonar.auth.fake-base-id-provider.allowsUsersToSignUp");
  }

  @Test
  public void default_group_is_not_added_for_new_user_when_organizations_are_enabled() {
    Group group = tester.groups().generate(null);
    enableUserCreationByAuthPlugin("aLogin");
    setGroupsReturnedByAuthPlugin(group.getName());

    authenticateWithFakeAuthProvider();

    // No default group membership
    tester.groups().assertThatUserIsOnlyMemberOf(null, "aLogin", group.getName());
  }

  @Test
  public void default_group_is_not_sync_for_existing_user_when_organizations_are_enabled() {
    Group group = tester.groups().generate(null);
    User user = tester.users().generate();
    enableUserCreationByAuthPlugin(user.getLogin());
    setGroupsReturnedByAuthPlugin(group.getName());

    authenticateWithFakeAuthProvider();

    // No default group membership
    tester.groups().assertThatUserIsOnlyMemberOf(null, user.getLogin(), group.getName());
  }

  @Test
  public void remove_default_group_when_organizations_are_enabled() {
    Group group = tester.groups().generate(null);
    User user = tester.users().generate();
    // Add user as member of default organization
    tester.wsClient().organizations().addMember(new AddMemberRequest().setOrganization("default-organization").setLogin(user.getLogin()));
    tester.groups().assertThatUserIsMemberOf(null, user.getLogin(), "Members");
    enableUserCreationByAuthPlugin(user.getLogin());
    // No group is returned by the plugin
    setGroupsReturnedByAuthPlugin();

    authenticateWithFakeAuthProvider();

    // No default group membership
    tester.groups().assertThatUserIsOnlyMemberOf(null, user.getLogin());
  }

  private void enableUserCreationByAuthPlugin(String login) {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.user", login + ",fake-john,John,john@email.com");
  }

  private void setGroupsReturnedByAuthPlugin(String... groups) {
    tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.enabledGroupsSync", "true");
    if (groups.length > 0) {
      tester.settings().setGlobalSettings("sonar.auth.fake-base-id-provider.groups", Joiner.on(",").join(groups));
    }
  }

  private void authenticateWithFakeAuthProvider() {
    tester.wsClient().wsConnector().call(
      new GetRequest("/sessions/init/fake-base-id-provider"))
      .failIfNotSuccessful();
  }

}
