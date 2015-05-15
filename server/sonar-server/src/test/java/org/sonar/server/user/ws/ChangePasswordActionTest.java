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
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.user.db.UserGroupDao;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ChangePasswordActionTest {

  static final Settings settings = new Settings().setProperty("sonar.defaultGroup", "sonar-users");

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(settings));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

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
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, userGroupDao, groupDao);
    session = dbClient.openSession(false);
    groupDao.insert(session, new GroupDto().setName("sonar-users"));
    session.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    index = new UserIndex(esTester.client());
    tester = new WsTester(new UsersWs(new ChangePasswordAction(new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2), userSessionRule)));
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
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .execute();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_user() throws Exception {
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "polop")
      .setParam("password", "polop")
      .execute();
  }

  @Test
  public void update_password() throws Exception {
    createUser();
    session.clearCache();
    String originalPassword = dbClient.userDao().selectByLogin(session, "john").getCryptedPassword();

    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute()
      .assertNoContent();

    session.clearCache();
    String newPassword = dbClient.userDao().selectByLogin(session, "john").getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
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
