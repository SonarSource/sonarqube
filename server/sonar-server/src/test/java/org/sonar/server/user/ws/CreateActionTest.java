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

import java.util.Locale;
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
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserUpdater;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.UserDao;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class CreateActionTest {

  static final Settings settings = new Settings().setProperty("sonar.defaultGroup", "sonar-users");

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
    UserGroupDao userGroupDao = new UserGroupDao();
    GroupDao groupDao = new GroupDao(system2);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, userGroupDao, groupDao);
    session = dbClient.openSession(false);
    groupDao.insert(session, new GroupDto().setName("sonar-users"));
    session.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    index = new UserIndex(esTester.client());
    tester = new WsTester(new UsersWs(new CreateAction(index,
      new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2, mock(SecurityRealmFactory.class)),
      i18n, userSessionRule, new UserJsonWriter(userSessionRule))));
    controller = tester.controller("api/users");

  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void create_user() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scmAccounts", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    UserDoc user = index.getByLogin("john");
    assertThat(user.login()).isEqualTo("john");
    assertThat(user.name()).isEqualTo("John");
    assertThat(user.email()).isEqualTo("john@email.com");
    assertThat(user.scmAccounts()).containsOnly("jn");
  }

  @Test
  public void create_user_with_deprecated_scm_parameter() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    UserDoc user = index.getByLogin("john");
    assertThat(user.login()).isEqualTo("john");
    assertThat(user.name()).isEqualTo("John");
    assertThat(user.email()).isEqualTo("john@email.com");
    assertThat(user.scmAccounts()).containsOnly("jn");
  }

  @Test
  public void reactivate_user() throws Exception {
    userSessionRule.login("admin").setLocale(Locale.FRENCH).setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    dbClient.userDao().insert(session, new UserDto()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setActive(true));
    session.commit();
    dbClient.userDao().deactivateUserByLogin(session, "john");
    userIndexer.index();

    when(i18n.message(Locale.FRENCH, "user.reactivated", "user.reactivated", "john")).thenReturn("The user 'john' has been reactivated.");

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "reactivate_user.json");

    assertThat(index.getByLogin("john").active()).isTrue();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    userSessionRule.login("not_admin");

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234").execute();
  }
}
