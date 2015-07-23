/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.usergroups.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.UserDao;
import org.sonar.server.ws.WsTester;
import org.sonar.server.ws.WsTester.TestRequest;
import org.sonar.test.DbTests;

@Category(DbTests.class)
public class UsersActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  WsTester wsTester;
  DbClient dbClient;
  DbSession session;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    System2 system2 = System2.INSTANCE;
    UserDao userDao = new UserDao(dbTester.myBatis(), system2);
    GroupDao groupDao = new GroupDao(system2);
    UserGroupDao userGroupDao = new UserGroupDao();
    GroupMembershipDao groupMembershipDao = new GroupMembershipDao(dbTester.myBatis());

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, groupDao, userGroupDao, groupMembershipDao);
    session = dbClient.openSession(false);
    session.commit();

    wsTester = new WsTester(new UserGroupsWs(new UsersAction(dbClient, userSession)));
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

  }

  @After
  public void tearDown() {
    session.close();
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

  private TestRequest newUsersRequest() {
    return wsTester.newGetRequest("api/usergroups", "users");
  }

  @Test
  public void empty_users() throws Exception {
    GroupDto group = insertGroup();
    session.commit();

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
    session.commit();

    newUsersRequest()
      .setParam("id", group.getId().toString())
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all.json");
  }

  @Test
  public void selected_users() throws Exception {
    GroupDto group = insertGroup();
    UserDto groupUser = insertUser("ada", "Ada Lovelace");
    insertUser("grace", "Grace Hopper");
    addUserToGroup(groupUser, group);
    session.commit();

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
    session.commit();

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
    session.commit();

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
    session.commit();

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

  private GroupDto insertGroup() {
    return dbClient.groupDao().insert(session, new GroupDto()
      .setName("sonar-users"));
  }

  private UserDto insertUser(String login, String name) {
    return dbClient.userDao().insert(session, new UserDto().setLogin(login).setName(name));
  }

  private void addUserToGroup(UserDto user, GroupDto usersGroup) {
    dbClient.userGroupDao().insert(session, new UserGroupDto().setUserId(user.getId()).setGroupId(usersGroup.getId()));
  }
}
