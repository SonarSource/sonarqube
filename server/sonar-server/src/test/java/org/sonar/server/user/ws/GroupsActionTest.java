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

package org.sonar.server.user.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.server.ws.WsTester;

public class GroupsActionTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  WsTester tester;
  DbClient dbClient;
  DbSession session;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    System2 system2 = new System2();
    UserDao userDao = new UserDao(dbTester.myBatis(), system2);
    GroupDao groupDao = new GroupDao(system2);
    UserGroupDao userGroupDao = new UserGroupDao();
    GroupMembershipDao groupMembershipDao = new GroupMembershipDao(dbTester.myBatis());

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, groupDao, userGroupDao, groupMembershipDao);
    session = dbClient.openSession(false);
    session.commit();

    tester = new WsTester(new UsersWs(new GroupsAction(dbClient, userSession)));
    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_user() throws Exception {
    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    userSession.login("not-admin");
    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john").execute();
  }

  @Test
  public void empty_groups() throws Exception {
    createUser();
    session.commit();

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .execute()
      .assertJson(getClass(), "empty.json");
  }

  @Test
  public void all_groups() throws Exception {
    UserDto user = createUser();
    GroupDto usersGroup = createGroup("sonar-users", "Sonar Users");
    createGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);
    session.commit();

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all.json");
  }

  @Test
  public void selected_groups() throws Exception {
    UserDto user = createUser();
    GroupDto usersGroup = createGroup("sonar-users", "Sonar Users");
    createGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);
    session.commit();

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .execute()
      .assertJson(getClass(), "selected.json");

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam(Param.SELECTED, SelectionMode.SELECTED.value())
      .execute()
      .assertJson(getClass(), "selected.json");
  }

  @Test
  public void deselected_groups() throws Exception {
    UserDto user = createUser();
    GroupDto usersGroup = createGroup("sonar-users", "Sonar Users");
    createGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);
    session.commit();

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam(Param.SELECTED, SelectionMode.DESELECTED.value())
      .execute()
      .assertJson(getClass(), "deselected.json");
  }

  private UserDto createUser() {
    return dbClient.userDao().insert(session, new UserDto()
      .setActive(true)
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts("jn"));
  }

  @Test
  public void paging() throws Exception {
    UserDto user = createUser();
    GroupDto usersGroup = createGroup("sonar-users", "Sonar Users");
    createGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);
    session.commit();

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam(Param.PAGE_SIZE, "1")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all_page1.json");

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam(Param.PAGE_SIZE, "1")
      .setParam(Param.PAGE, "2")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all_page2.json");
  }

  @Test
  public void filtering() throws Exception {
    UserDto user = createUser();
    GroupDto usersGroup = createGroup("sonar-users", "Sonar Users");
    createGroup("sonar-admins", "Sonar Admins");
    addUserToGroup(user, usersGroup);
    session.commit();

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam("q", "users")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all_users.json");

    tester.newGetRequest("api/users", "groups")
      .setParam("login", "john")
      .setParam("q", "admin")
      .setParam(Param.SELECTED, SelectionMode.ALL.value())
      .execute()
      .assertJson(getClass(), "all_admin.json");
  }

  private GroupDto createGroup(String name, String description) {
    return dbClient.groupDao().insert(session, new GroupDto().setName(name).setDescription(description));
  }

  private void addUserToGroup(UserDto user, GroupDto usersGroup) {
    dbClient.userGroupDao().insert(session, new UserGroupDto().setUserId(user.getId()).setGroupId(usersGroup.getId()));
  }
}
