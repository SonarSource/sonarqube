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
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;

public class UpdateActionTest {

  static final Settings settings = new Settings().setProperty("sonar.defaultGroup", "sonar-users");

  @ClassRule
  public static final DbTester dbTester = new DbTester();
  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(settings));
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone().login("admin")
      .setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

  WebService.Controller controller;

  WsTester tester;

  UserIndex index;

  DbClient dbClient;

  UserIndexer userIndexer;

  DbSession session;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    esTester.truncateIndices();

    System2 system2 = new System2();
    UserDao userDao = new UserDao(dbTester.myBatis(), system2);
    UserGroupDao userGroupDao = new UserGroupDao();
    GroupDao groupDao = new GroupDao(system2);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, userGroupDao, groupDao, new GroupMembershipDao(dbTester.myBatis()));
    session = dbClient.openSession(false);
    groupDao.insert(session, new GroupDto().setName("sonar-users"));
    session.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    index = new UserIndex(esTester.client());
    tester = new WsTester(new UsersWs(new UpdateAction(index,
      new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2, mock(SecurityRealmFactory.class)), userSessionRule,
      new UserJsonWriter(userSessionRule), dbClient)));
    controller = tester.controller("api/users");
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    createUser();

    userSessionRule.login("polop");
    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .execute();
  }

  @Test
  public void update_user() throws Exception {
    createUser();

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("name", "Jon Snow")
      .setParam("email", "jon.snow@thegreatw.all")
      .setParam("scmAccounts", "jon.snow")
      .execute()
      .assertJson(getClass(), "update_user.json");
  }

  @Test
  public void update_only_name() throws Exception {
    createUser();

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("name", "Jon Snow")
      .execute()
      .assertJson(getClass(), "update_name.json");
  }

  @Test
  public void update_only_email() throws Exception {
    createUser();

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("email", "jon.snow@thegreatw.all")
      .execute()
      .assertJson(getClass(), "update_email.json");
  }

  @Test
  public void update_only_scm_accounts() throws Exception {
    createUser();

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("scmAccounts", "jon.snow")
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");
  }

  @Test
  public void update_only_scm_accounts_with_deprecated_parameter() throws Exception {
    createUser();

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("scm_accounts", "jon.snow")
      .execute()
      .assertJson(getClass(), "update_scm_accounts.json");
  }

  private void createUser() {
    dbClient.userDao().insert(session, new UserDto()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setActive(true));
    session.commit();
    userIndexer.index();
  }
}
