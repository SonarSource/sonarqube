/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.db.DbTester;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.OrganizationUpdater;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.user.UserTesting.newExternalUser;
import static org.sonar.db.user.UserTesting.newLocalUser;

public class ChangePasswordActionTest {

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();

  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();
  private CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient());

  private UserUpdater userUpdater = new UserUpdater(system2, mock(NewUserNotifier.class), db.getDbClient(), new UserIndexer(db.getDbClient(), es.client()),
    organizationFlags,
    TestDefaultOrganizationProvider.from(db),
    mock(OrganizationUpdater.class),
    new DefaultGroupFinder(db.getDbClient()),
    new MapSettings().asConfig(),
    localAuthentication);

  private WsActionTester tester = new WsActionTester(new ChangePasswordAction(db.getDbClient(), userUpdater, userSessionRule, localAuthentication));

  @Before
  public void setUp() {
    db.users().insertDefaultGroup(db.getDefaultOrganization(), "sonar-users");
  }

  @Test
  public void a_user_can_update_his_password() {
    userUpdater.createAndCommit(db.getSession(), NewUser.builder()
      .setEmail("john@email.com")
      .setLogin("john")
      .setName("John")
      .setPassword("Valar Dohaeris")
      .build(), u -> {
      });
    String oldCryptedPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), "john").getCryptedPassword();
    userSessionRule.logIn("john");

    TestResponse response = tester.newRequest()
      .setParam("login", "john")
      .setParam("previousPassword", "Valar Dohaeris")
      .setParam("password", "Valar Morghulis")
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    String newCryptedPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), "john").getCryptedPassword();
    assertThat(newCryptedPassword).isNotEqualTo(oldCryptedPassword);
  }

  @Test
  public void system_administrator_can_update_password_of_user() {
    userSessionRule.logIn().setSystemAdministrator();
    createLocalUser();
    String originalPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), "john").getCryptedPassword();

    tester.newRequest()
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();

    String newPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), "john").getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
  }

  @Test
  public void fail_on_missing_permission() {
    createLocalUser();
    userSessionRule.logIn("polop");

    expectedException.expect(ForbiddenException.class);
    tester.newRequest()
      .setParam("login", "john")
      .execute();
  }

  @Test
  public void fail_on_unknown_user() {
    userSessionRule.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'polop' has not been found");

    tester.newRequest()
      .setParam("login", "polop")
      .setParam("password", "polop")
      .execute();
  }

  @Test
  public void fail_on_disabled_user() {
    db.users().insertUser(u -> u.setLogin("polop").setActive(false));
    userSessionRule.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'polop' has not been found");

    tester.newRequest()
      .setParam("login", "polop")
      .setParam("password", "polop")
      .execute();
  }

  @Test
  public void fail_to_update_password_on_self_without_old_password() {
    createLocalUser();
    userSessionRule.logIn("john");

    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest()
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test
  public void fail_to_update_password_on_self_with_bad_old_password() {
    createLocalUser();
    userSessionRule.logIn("john");

    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest()
      .setParam("login", "john")
      .setParam("previousPassword", "I dunno")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test
  public void fail_to_update_password_on_external_auth() {
    userSessionRule.logIn().setSystemAdministrator();
    db.users().insertUser(newExternalUser("john", "John", "john@email.com"));

    expectedException.expect(BadRequestException.class);

    tester.newRequest()
      .setParam("login", "john")
      .setParam("password", "Valar Morghulis")
      .execute();
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(3);
  }

  private void createLocalUser() {
    db.users().insertUser(newLocalUser("john", "John", "john@email.com"));
  }
}
