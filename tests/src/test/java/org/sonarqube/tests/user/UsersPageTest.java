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
package org.sonarqube.tests.user;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category1Suite;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsUsers;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.user.GroupsRequest;
import util.selenium.Selenese;
import util.user.UserRule;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;

public class UsersPageTest {

  private static final String ADMIN_USER_LOGIN = "admin-user";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private WsClient adminClient = newAdminWsClient(orchestrator);

  @Before
  public void initAdminUser() throws Exception {
    userRule.createAdminUser(ADMIN_USER_LOGIN, ADMIN_USER_LOGIN);
  }

  @After
  public void deleteAdminUser() {
    userRule.resetUsers();
  }

  @Test
  public void generate_and_revoke_user_token()  {
    Selenese.runSelenese(orchestrator, "/user/UsersPageTest/generate_and_revoke_user_token.html");
  }

  @Test
  public void admin_should_change_his_own_password()  {
    Selenese.runSelenese(orchestrator, "/user/UsersPageTest/admin_should_change_its_own_password.html");
  }

  @Test
  public void return_groups_belonging_to_a_user()  {
    String login = randomAlphabetic(10);
    String group = randomAlphabetic(10);
    userRule.createUser(login, login);
    userRule.createGroup(group);
    userRule.associateGroupsToUser(login, group);

    List<WsUsers.GroupsWsResponse.Group> result = adminClient.users().groups(GroupsRequest.builder().setLogin(login).build()).getGroupsList();

    assertThat(result).extracting(WsUsers.GroupsWsResponse.Group::getName).contains(group);
  }
}
