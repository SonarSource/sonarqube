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
package org.sonar.server.user.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.test.JsonAssert.assertJson;

public class CurrentActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();

  private WsActionTester ws;

  @Before
  public void before() {
    ws = new WsActionTester(new CurrentAction(userSessionRule, dbClient));
  }

  @Test
  public void json_example() throws Exception {
    userSessionRule.login("obiwan.kenobi").setName("Obiwan Kenobi")
      .setGlobalPermissions(GlobalPermissions.ALL.toArray(new String[0]));
    UserDto obiwan = db.users().insertUser(
      newUserDto("obiwan.kenobi", "Obiwan Kenobi", "obiwan.kenobi@starwars.com")
        .setLocal(true)
        .setExternalIdentity("obiwan.kenobi")
        .setExternalIdentityProvider("sonarqube")
        .setScmAccounts(newArrayList("obiwan:github", "obiwan:bitbucket")));
    GroupDto jedi = db.users().insertGroup(newGroupDto().setName("Jedi"));
    GroupDto rebel = db.users().insertGroup(newGroupDto().setName("Rebel"));
    db.users().insertGroup(newGroupDto().setName("Sith"));
    dbClient.userGroupDao().insert(db.getSession(), new UserGroupDto()
      .setUserId(obiwan.getId())
      .setGroupId(jedi.getId()));
    dbClient.userGroupDao().insert(db.getSession(), new UserGroupDto()
      .setUserId(obiwan.getId())
      .setGroupId(rebel.getId()));
    db.commit();

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("current-example.json"));
  }

  @Test
  public void anonymous() throws Exception {
    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("CurrentActionTest/anonymous.json"));
  }
}
