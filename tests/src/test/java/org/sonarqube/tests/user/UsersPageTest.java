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

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.UsersManagementPage;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.users.GroupsRequest;
import util.user.UserRule;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class UsersPageTest {

  private User adminUser;

  @ClassRule
  public static Orchestrator orchestrator = UserSuite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void initUsers() {
    adminUser = tester.users().generateAdministrator(u -> u.setLogin("admin-user").setPassword("admin-user"));
    tester.users().generate(u -> u.setLogin("random-user").setPassword("random-user"));
  }

  @After
  public void resetUsers() {
    userRule.resetUsers();
  }

  @Test
  public void generate_and_revoke_user_token()  {
    UsersManagementPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin()).openUsersManagement();
    tester.wsClient().users().skipOnboardingTutorial();

    page
      .hasUsersCount(3)
      .getUser(adminUser.getLogin())
      .hasTokensCount(0)
      .generateToken("token-test")
      .hasTokensCount(1)
      .revokeToken("token-test")
      .hasTokensCount(0);
  }

  @Test
  public void admin_should_change_his_own_password()  {
    UsersManagementPage page = tester.openBrowser().logIn().submitCredentials(adminUser.getLogin()).openUsersManagement();
    tester.wsClient().users().skipOnboardingTutorial();
    page
      .hasUsersCount(3)
      .getUser(adminUser.getLogin())
      .changePassword(adminUser.getLogin(), "newpassword");
  }

  @Test
  public void return_groups_belonging_to_a_user()  {
    String login = randomAlphabetic(10);
    String group = randomAlphabetic(10);
    tester.users().generate(u -> u.setLogin(login).setPassword(login));
    tester.groups().generate(null, g -> g.setName(group));
    tester.groups().addMemberToGroups(tester.organizations().getDefaultOrganization(), login, group);

    List<Users.GroupsWsResponse.Group> result = tester.as(adminUser.getLogin()).wsClient().users().groups(new GroupsRequest().setLogin(login)).getGroupsList();
    assertThat(result).extracting(Users.GroupsWsResponse.Group::getName).contains(group);
  }
}
