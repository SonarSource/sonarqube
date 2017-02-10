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
package org.sonar.server.user.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static java.util.Collections.singletonList;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupsActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WsTester tester;
  private DbClient dbClient;
  private DbSession session;

  @Before
  public void setUp() {
    System2 system2 = new System2();
    UserDao userDao = new UserDao(dbTester.myBatis(), system2);
    GroupDao groupDao = new GroupDao(system2);
    UserGroupDao userGroupDao = new UserGroupDao();
    GroupMembershipDao groupMembershipDao = new GroupMembershipDao();

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, groupDao, userGroupDao, groupMembershipDao);
    session = dbClient.openSession(false);
    session.commit();

    tester = new WsTester(new UsersWs(new GroupsAction(dbClient, userSession)));
    userSession.logIn().setSystemAdministrator();
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
    userSession.logIn("not-admin");
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
      .setScmAccounts(singletonList("jn")));
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
    return dbClient.groupDao().insert(session, newGroupDto().setName(name).setDescription(description));
  }

  private void addUserToGroup(UserDto user, GroupDto usersGroup) {
    dbClient.userGroupDao().insert(session, new UserGroupDto().setUserId(user.getId()).setGroupId(usersGroup.getId()));
  }
}
