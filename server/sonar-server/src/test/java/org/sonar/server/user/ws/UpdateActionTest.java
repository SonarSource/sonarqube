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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.config.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UpdateActionTest {

  static final Settings settings = new MapSettings().setProperty("sonar.defaultGroup", "sonar-users");

  System2 system2 = new System2();

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(settings));
  @Rule
  public final UserSessionRule userSessionRule = UserSessionRule.standalone().login("admin")
    .setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

  DbClient dbClient = dbTester.getDbClient();

  DbSession session = dbTester.getSession();

  WebService.Controller controller;

  WsTester tester;

  UserIndexer userIndexer;

  @Before
  public void setUp() {
    dbClient.groupDao().insert(session, new GroupDto().setName("sonar-users"));
    session.commit();

    userIndexer = (UserIndexer) new UserIndexer(dbClient, esTester.client()).setEnabled(true);
    tester = new WsTester(new UsersWs(new UpdateAction(
      new UserUpdater(mock(NewUserNotifier.class), settings, dbClient, userIndexer, system2), userSessionRule,
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
  public void blank_email_is_updated_to_null() throws Exception {
    createUser();

    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .setParam("email", "")
      .execute()
      .assertJson(getClass(), "blank_email_is_updated_to_null.json");

    UserDto userDto = dbClient.userDao().selectByLogin(session, "john");
    assertThat(userDto.getEmail()).isNull();
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

  @Test(expected = NotFoundException.class)
  public void fail_on_unknown_user() throws Exception {
    tester.newPostRequest("api/users", "update")
      .setParam("login", "john")
      .execute();
  }

  private void createUser() {
    dbClient.userDao().insert(session, new UserDto()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setScmAccounts(newArrayList("jn"))
      .setActive(true))
      .setExternalIdentity("jo")
      .setExternalIdentityProvider("sonarqube");
    session.commit();
    userIndexer.index();
  }
}
