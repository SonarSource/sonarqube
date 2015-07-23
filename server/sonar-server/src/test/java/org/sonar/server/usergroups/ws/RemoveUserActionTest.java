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

import java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.UserDao;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class RemoveUserActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private WsTester tester;
  private GroupDao groupDao;
  private UserDao userDao;
  private GroupMembershipDao groupMembershipDao;
  private UserGroupDao userGroupDao;
  private DbSession session;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    groupDao = new GroupDao(System2.INSTANCE);
    userDao = new UserDao(dbTester.myBatis(), System2.INSTANCE);
    groupMembershipDao = new GroupMembershipDao(dbTester.myBatis());
    userGroupDao = new UserGroupDao();

    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), groupDao, userDao, userGroupDao, groupMembershipDao);

    tester = new WsTester(new UserGroupsWs(new RemoveUserAction(dbClient, userSession)));

    session = dbClient.openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void remove_user_not_in_group() throws Exception {
    GroupDto group = insertGroup("admins");
    UserDto user = insertUser("my-admin");
    session.commit();

    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/usergroups", "remove_user")
      .setParam("id", group.getId().toString())
      .setParam("login", user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(groupMembershipDao.selectGroupsByLogins(session, Arrays.asList(user.getLogin())).get(user.getLogin()))
      .isEmpty();
  }

  @Test
  public void remove_user_nominal() throws Exception {
    GroupDto users = insertGroup("users");
    UserDto user = insertUser("my-admin");
    insertMember(users.getId(), user.getId());
    session.commit();

    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/usergroups", "remove_user")
      .setParam("id", users.getId().toString())
      .setParam("login", user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(groupMembershipDao.selectGroupsByLogins(session, Arrays.asList(user.getLogin())).get(user.getLogin()))
      .isEmpty();
  }

  @Test
  public void remove_user_only_from_one_group() throws Exception {
    GroupDto users = insertGroup("user");
    GroupDto admins = insertGroup("admins");
    UserDto user = insertUser("user");
    insertMember(users.getId(), user.getId());
    insertMember(admins.getId(), user.getId());
    session.commit();

    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/usergroups", "remove_user")
      .setParam("id", admins.getId().toString())
      .setParam("login", user.getLogin())
      .execute()
      .assertNoContent();

    assertThat(groupMembershipDao.selectGroupsByLogins(session, Arrays.asList(user.getLogin())).get(user.getLogin()))
      .containsOnly(users.getName());
  }

  @Test
  public void unknown_group() throws Exception {
    UserDto user = insertUser("my-admin");
    session.commit();

    expectedException.expect(NotFoundException.class);

    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/usergroups", "remove_user")
      .setParam("id", "42")
      .setParam("login", user.getLogin())
      .execute();
  }

  @Test
  public void unknown_user() throws Exception {
    GroupDto group = insertGroup("admins");
    session.commit();

    expectedException.expect(NotFoundException.class);

    userSession.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/usergroups", "remove_user")
      .setParam("id", group.getId().toString())
      .setParam("login", "my-admin")
      .execute();
  }

  private GroupDto insertGroup(String groupName) {
    return groupDao.insert(session, new GroupDto()
      .setName(groupName)
      .setDescription(StringUtils.capitalize(groupName)));
  }

  private UserDto insertUser(String login) {
    return userDao.insert(session, new UserDto().setLogin(login).setName(login).setActive(true));
  }

  private void insertMember(long groupId, long userId) {
    userGroupDao.insert(session, new UserGroupDto().setGroupId(groupId).setUserId(userId));
  }
}
