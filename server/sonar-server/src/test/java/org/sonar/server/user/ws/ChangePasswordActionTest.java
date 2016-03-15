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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.SecurityRealmFactory;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangePasswordActionTest {

  static final Settings settings = new Settings().setProperty("sonar.defaultGroup", "sonar-users");

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static final EsTester esTester = new EsTester().addDefinitions(new UserIndexDefinition(settings));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

  WebService.Controller controller;

  WsTester tester;

  UserIndex index;

  DbClient dbClient;

  UserUpdater userUpdater;

  UserIndexer userIndexer;

  DbSession session;

  SecurityRealmFactory realmFactory = mock(SecurityRealmFactory.class);

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
    userUpdater = new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2);
    tester = new WsTester(new UsersWs(new ChangePasswordAction(userUpdater, userSessionRule)));
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
    String originalPassword = dbClient.userDao().selectOrFailByLogin(session, "john").getCryptedPassword();

    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute()
      .assertNoContent();

    session.clearCache();
    String newPassword = dbClient.userDao().selectOrFailByLogin(session, "john").getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
  }

  @Test
  public void update_password_on_self() throws Exception {
    createUser();
    session.clearCache();
    String originalPassword = dbClient.userDao().selectOrFailByLogin(session, "john").getCryptedPassword();

    userSessionRule.login("john");
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("previousPassword", "Valar Dohaeris")
      .setParam("password", "Valar Morghulis")
      .execute()
      .assertNoContent();

    session.clearCache();
    String newPassword = dbClient.userDao().selectOrFailByLogin(session, "john").getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_update_password_on_self_without_old_password() throws Exception {
    createUser();
    session.clearCache();

    userSessionRule.login("john");
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_update_password_on_self_with_bad_old_password() throws Exception {
    createUser();
    session.clearCache();

    userSessionRule.login("john");
    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("previousPassword", "I dunno")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test(expected = BadRequestException.class)
  public void fail_to_update_password_on_external_auth() throws Exception {
    userUpdater.create(NewUser.create()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setExternalIdentity(new ExternalIdentity("gihhub", "john")));
    session.clearCache();
    when(realmFactory.hasExternalAuthentication()).thenReturn(true);

    tester.newPostRequest("api/users", "change_password")
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  private void createUser() {
    userUpdater.create(NewUser.create()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setPassword("Valar Dohaeris"));
  }
}
