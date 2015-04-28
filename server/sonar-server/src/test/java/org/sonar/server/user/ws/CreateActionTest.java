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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
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
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.db.GroupDao;
import org.sonar.server.user.db.UserDao;
import org.sonar.server.user.db.UserGroupDao;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateActionTest {

  static final Settings settings = new Settings().setProperty("sonar.defaultGroup", "sonar-users");

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(settings));

  WebService.Controller controller;

  WsTester tester;

  UserIndex index;

  DbClient dbClient;

  UserIndexer userIndexer;

  DbSession session;

  @Mock
  I18n i18n;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    esTester.truncateIndices();

    System2 system2 = new System2();
    UserDao userDao = new UserDao(dbTester.myBatis(), system2);
    UserGroupDao userGroupDao = new UserGroupDao();
    GroupDao groupDao = new GroupDao();
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), userDao, userGroupDao, groupDao);
    session = dbClient.openSession(false);
    groupDao.insert(session, new GroupDto().setName("sonar-users"));
    session.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    index = new UserIndex(esTester.client());
    tester = new WsTester(new UsersWs(new CreateAction(index,
      new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2),
      i18n)));
    controller = tester.controller("api/users");

  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void create_user() throws Exception {
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    UserDoc user = index.getByLogin("john");
    assertThat(user.login()).isEqualTo("john");
    assertThat(user.name()).isEqualTo("John");
    assertThat(user.email()).isEqualTo("john@email.com");
    assertThat(user.scmAccounts()).containsOnly("jn");
  }

  @Test
  public void reactivate_user() throws Exception {
    MockUserSession.set().setLogin("admin").setLocale(Locale.FRENCH).setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    dbClient.userDao().insert(session, new UserDto()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setActive(true));
    session.commit();
    dbClient.userDao().deactivateUserByLogin("john");
    userIndexer.index();

    when(i18n.message(Locale.FRENCH, "user.reactivated", "user.reactivated", "john")).thenReturn("The user 'john' has been reactivated.");

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234").execute()
      .assertJson(getClass(), "reactivate_user.json");

    assertThat(index.getByLogin("john").active()).isTrue();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    MockUserSession.set().setLogin("not_admin");

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234")
      .setParam("password_confirmation", "1234").execute();
  }
}
