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
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
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
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
    authenticateAsAdmin();

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
    authenticateAsAdmin();

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
    authenticateAsAdmin();

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
    authenticateAsAdmin();

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

  @Test
  public void create_user_with_root_flag_to_false_if_default_group_is_unset() throws Exception {
    unsetDefaultGroupProperty();
    authenticateAsAdmin();

    executeRequest("john");

    db.rootFlag().verify("john", false);
  }

  @Test
  public void create_user_with_root_flag_to_false_if_default_group_is_non_admin_on_default_organization() throws Exception {
    GroupDto adminGroup = db.users().insertGroup(db.getDefaultOrganization());
    setDefaultGroupProperty(adminGroup);
    authenticateAsAdmin();

    executeRequest("foo");

    db.rootFlag().verify("foo", false);
  }

  @Test
  public void request_fails_with_ServerException_when_default_group_belongs_to_another_organization() throws Exception {
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto group = db.users().insertGroup(otherOrganization);
    setDefaultGroupProperty(group);
    authenticateAsAdmin();

    expectedException.expect(ServerException.class);
    expectedException.expectMessage("The default group '" + group.getName() + "' for new users does not exist. " +
      "Please update the general security settings to fix this issue");

    executeRequest("bar");
  }

  @Test
  public void create_user_with_root_flag_to_true_if_default_group_is_admin_on_default_organization() throws Exception {
    GroupDto adminGroup = db.users().insertAdminGroup(db.getDefaultOrganization());
    setDefaultGroupProperty(adminGroup);
    authenticateAsAdmin();

    executeRequest("doh");

    db.rootFlag().verify("doh", true);
  }

  private void unsetDefaultGroupProperty() {
    settings.setProperty("sonar.defaultGroup", (String) null);
  }

  private void setDefaultGroupProperty(GroupDto adminGroup) {
    settings.setProperty("sonar.defaultGroup", adminGroup.getName());
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

  private void authenticateAsAdmin() {
    userSessionRule.login("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
  }

  private void executeRequest(String login) throws Exception {
    tester.newPostRequest("api/users", "create")
      .setParam("login", login)
      .setParam("name", "name of " + login)
      .setParam("email", login + "@email.com")
      .setParam("scm_accounts", login.substring(0, 2))
      .setParam("password", "pwd_" + login)
      .execute();
  }
}
