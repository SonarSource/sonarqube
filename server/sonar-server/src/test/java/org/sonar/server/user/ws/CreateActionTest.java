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

import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
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
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class CreateActionTest {

  private static final String DEFAULT_GROUP_NAME = "sonar-users";
  private Settings settings = new MapSettings().setProperty("sonar.defaultGroup", DEFAULT_GROUP_NAME);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(settings));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WsTester tester;
  private UserIndex index;
  private UserIndexer userIndexer;
  private I18n i18n = mock(I18n.class);
  private GroupDto defaultGroupInDefaultOrg;

  @Before
  public void setUp() {
    System2 system2 = new System2();
    defaultGroupInDefaultOrg = db.users().insertGroup(db.getDefaultOrganization(), DEFAULT_GROUP_NAME);
    userIndexer = new UserIndexer(db.getDbClient(), esTester.client());
    index = new UserIndex(esTester.client());
    DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
    tester = new WsTester(new UsersWs(new CreateAction(db.getDbClient(),
      new UserUpdater(mock(NewUserNotifier.class), settings, db.getDbClient(), userIndexer, system2, defaultOrganizationProvider),
      i18n, userSessionRule, new UserJsonWriter(userSessionRule))));
  }

  @Test
  public void create_user() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scmAccount", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    UserDoc user = index.getNullableByLogin("john");
    assertThat(user.login()).isEqualTo("john");
    assertThat(user.name()).isEqualTo("John");
    assertThat(user.email()).isEqualTo("john@email.com");
    assertThat(user.scmAccounts()).containsOnly("jn");

    // exists in db
    Optional<UserDto> dbUser = db.users().selectUserByLogin("john");
    assertThat(dbUser).isPresent();

    // member of default group in default organization
    assertThat(db.users().selectGroupIdsOfUser(dbUser.get())).containsOnly(defaultGroupInDefaultOrg.getId());
  }

  @Test
  public void create_user_with_comma_in_scm_account() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scmAccount", "j,n")
      .setParam("password", "1234").execute();

    UserDoc user = index.getNullableByLogin("john");
    assertThat(user.scmAccounts()).containsOnly("j,n");
  }

  @Test
  public void create_user_with_deprecated_scmAccounts_parameter() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scmAccounts", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    UserDoc user = index.getNullableByLogin("john");
    assertThat(user.scmAccounts()).containsOnly("jn");
  }

  @Test
  public void create_user_with_deprecated_scm_accounts_parameter() throws Exception {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "create_user.json");

    UserDoc user = index.getNullableByLogin("john");
    assertThat(user.scmAccounts()).containsOnly("jn");
  }

  @Test
  public void reactivate_user() throws Exception {
    userSessionRule.login("admin").setLocale(Locale.FRENCH).setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    db.users().insertUser(newUserDto("john", "John", "john@email.com"));
    db.getDbClient().userDao().deactivateUserByLogin(db.getSession(), "john");
    db.commit();
    userIndexer.index();
    when(i18n.message(Locale.FRENCH, "user.reactivated", "user.reactivated", "john")).thenReturn("The user 'john' has been reactivated.");

    tester.newPostRequest("api/users", "create")
      .setParam("login", "john")
      .setParam("name", "John")
      .setParam("email", "john@email.com")
      .setParam("scm_accounts", "jn")
      .setParam("password", "1234").execute()
      .assertJson(getClass(), "reactivate_user.json");

    assertThat(index.getNullableByLogin("john").active()).isTrue();
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
