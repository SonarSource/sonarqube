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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDao;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTokenTesting.newUserToken;

public class DeactivateActionTest {

  static final Settings settings = new Settings();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(settings));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WebService.Controller controller;
  WsTester ws;
  UserIndex index;
  DbClient dbClient;
  UserIndexer userIndexer;
  DbSession dbSession;

  @Before
  public void setUp() {
    System2 system2 = new System2();
    UserDao userDao = new UserDao(db.myBatis(), system2);
    dbClient = new DbClient(db.database(), db.myBatis(), userDao, new GroupMembershipDao(db.myBatis()), new UserTokenDao());
    dbSession = db.getSession();
    dbSession.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    index = new UserIndex(esTester.client());
    ws = new WsTester(new UsersWs(new DeactivateAction(
      new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2), userSessionRule,
      new UserJsonWriter(userSessionRule), dbClient)));
    controller = ws.controller("api/users");
  }

  @Test
  public void deactivate_user() throws Exception {
    createUser();
    assertThat(dbClient.userTokenDao().selectByLogin(dbSession, "john")).isNotEmpty();
    db.commit();

    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    ws.newPostRequest("api/users", "deactivate")
      .setParam("login", "john")
      .execute()
      .assertJson(getClass(), "deactivate_user.json");

    UserDoc user = index.getNullableByLogin("john");
    assertThat(user.active()).isFalse();
    assertThat(dbClient.userTokenDao().selectByLogin(dbSession, "john")).isEmpty();
  }

  @Test(expected = BadRequestException.class)
  public void cannot_deactivate_self() throws Exception {
    createUser();

    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    ws.newPostRequest("api/users", "deactivate")
      .setParam("login", "admin")
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    createUser();

    userSessionRule.login("not_admin");
    ws.newPostRequest("api/users", "deactivate")
      .setParam("login", "john").execute();
  }

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_user() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    ws.newPostRequest("api/users", "deactivate")
      .setParam("login", "john").execute();
  }

  private void createUser() {
    dbClient.userDao().insert(dbSession, new UserDto()
      .setActive(true)
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts("jn"));
    dbClient.userTokenDao().insert(dbSession, newUserToken().setLogin("john"));
    dbSession.commit();
    userIndexer.index();
  }

}
