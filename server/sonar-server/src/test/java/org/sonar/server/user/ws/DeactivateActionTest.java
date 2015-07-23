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
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserUpdater;
import org.sonar.db.user.UserDao;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class DeactivateActionTest {

  static final Settings settings = new Settings();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(settings));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WebService.Controller controller;

  WsTester tester;

  UserIndex index;

  DbClient dbClient;

  UserIndexer userIndexer;

  DbSession session;

  I18n i18n = mock(I18n.class);

  @Before
  public void setUp() {
    dbTester.truncateTables();
    esTester.truncateIndices();

    System2 system2 = new System2();
    UserDao userDao = new UserDao(dbTester.myBatis(), system2);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, new GroupMembershipDao(dbTester.myBatis()));
    session = dbClient.openSession(false);
    session.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    index = new UserIndex(esTester.client());
    tester = new WsTester(new UsersWs(new DeactivateAction(index,
      new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2, mock(SecurityRealmFactory.class)), userSessionRule,
      new UserJsonWriter(userSessionRule), dbClient)));
    controller = tester.controller("api/users");

  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void deactivate_user() throws Exception {
    createUser();

    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/users", "deactivate")
      .setParam("login", "john")
      .execute()
      .assertJson(getClass(), "deactivate_user.json");

    UserDoc user = index.getByLogin("john");
    assertThat(user.active()).isFalse();
  }

  @Test(expected = BadRequestException.class)
  public void cannot_deactivate_self() throws Exception {
    createUser();

    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/users", "deactivate")
      .setParam("login", "admin")
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    createUser();

    userSessionRule.login("not_admin");
    tester.newPostRequest("api/users", "deactivate")
      .setParam("login", "john").execute();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_user() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.newPostRequest("api/users", "deactivate")
      .setParam("login", "john").execute();
  }

  private void createUser() {
    dbClient.userDao().insert(session, new UserDto()
      .setActive(true)
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts("jn"));
    session.commit();
    userIndexer.index();
  }

}
