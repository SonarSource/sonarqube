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
package org.sonar.server.usergroups.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_GROUP_NAME;


public class UsersActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  WsTester wsTester;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();

    wsTester = new WsTester(new UserGroupsWs(
      new UsersAction(
        dbClient,
        new UserGroupFinder(dbClient),
        userSession)));
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_user() throws Exception {
    newUsersRequest()
      .setParam("id", "42")
      .setParam("login", "john").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    userSession.login("not-admin");
    newUsersRequest()
      .setParam("id", "42")
      .setParam("login", "john").execute();
  }

  @Test
  public void empty_users() throws Exception {
    GroupDto group = insertGroup();
    dbSession.commit();

    newUsersRequest()
      .setParam("login", "john")
      .setParam("id", group.getId().toString())
      .execute()
      .assertJson(getClass(), "empty.json");
  }

  @Test
  public void all_users() throws Exception {
    GroupDto group = insertGroup();
    UserDto groupUser = insertUser("ada", "Ada Lovelace");
    insertUser("grace", "Grace Hopper");
    addUserToGroup(groupUser, group);
    dbSession.commit();

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all.json");
  }

  @Test
  public void all_users_by_group_name() throws Exception {
    GroupDto group = insertGroup();
    UserDto adaLovelace = insertUser("ada", "Ada Lovelace");
    UserDto graceHopper = insertUser("grace", "Grace Hopper");
    addUserToGroup(adaLovelace, group);
    addUserToGroup(graceHopper, group);
    dbSession.commit();

    String response = newUsersRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute().outputAsString();

    assertThat(response).contains("Ada Lovelace", "Grace Hopper");
  }

  @Test
  public void selected_users() throws Exception {
    GroupDto group = insertGroup();
    UserDto groupUser = insertUser("ada", "Ada Lovelace");
    insertUser("grace", "Grace Hopper");
    addUserToGroup(groupUser, group);
    dbSession.commit();

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertJson(getClass(), "selected.json");

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.SELECTED.value())
      .execute()
      .assertJson(getClass(), "selected.json");
  }

  @Test
  public void deselected_users() throws Exception {
    GroupDto group = insertGroup();
    UserDto groupUser = insertUser("ada", "Ada Lovelace");
    insertUser("grace", "Grace Hopper");
    addUserToGroup(groupUser, group);
    dbSession.commit();

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.DESELECTED.value())
      .execute()
      .assertJson(getClass(), "deselected.json");
  }

  @Test
  public void paging() throws Exception {
    GroupDto group = insertGroup();
    UserDto groupUser = insertUser("ada", "Ada Lovelace");
    insertUser("grace", "Grace Hopper");
    addUserToGroup(groupUser, group);
    dbSession.commit();

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("ps", "1")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all_page1.json");

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("ps", "1")
      .setParam("p", "2")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all_page2.json");
  }

  @Test
  public void filtering() throws Exception {
    GroupDto group = insertGroup();
    UserDto groupUser = insertUser("ada", "Ada Lovelace");
    insertUser("grace", "Grace Hopper");
    addUserToGroup(groupUser, group);
    dbSession.commit();

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("q", "ace")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all.json");

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam("q", "love")
      .execute()
      .assertJson(getClass(), "all_ada.json");
  }

  private TestRequest newUsersRequest() {
    return wsTester.newGetRequest("api/user_groups", "users");
  }

  private GroupDto insertGroup() {
    return dbClient.groupDao().insert(dbSession, new GroupDto()
      .setName("sonar-users"));
  }

  private UserDto insertUser(String login, String name) {
    return dbClient.userDao().insert(dbSession, new UserDto().setLogin(login).setName(name));
  }

  private void addUserToGroup(UserDto user, GroupDto usersGroup) {
    dbClient.userGroupDao().insert(dbSession, new UserGroupDto().setUserId(user.getId()).setGroupId(usersGroup.getId()));
  }
}
