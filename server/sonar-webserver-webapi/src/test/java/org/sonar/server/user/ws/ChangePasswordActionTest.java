/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexDefinition;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ChangePasswordActionTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.createCustom(UserIndexDefinition.createForTest());
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn();
  private final MapSettings settings = new MapSettings().setProperty("sonar.internal.pbkdf2.iterations", "1");
  private final CredentialsLocalAuthentication localAuthentication = new CredentialsLocalAuthentication(db.getDbClient(), settings.asConfig());

  private final UserUpdater userUpdater = new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(),
    new UserIndexer(db.getDbClient(), es.client()), new DefaultGroupFinder(db.getDbClient()),
    new MapSettings().asConfig(), new NoOpAuditPersister(), localAuthentication);

  private final WsActionTester tester = new WsActionTester(new ChangePasswordAction(db.getDbClient(), userUpdater, userSessionRule, localAuthentication));

  @Before
  public void setUp() {
    db.users().insertDefaultGroup();
  }

  @Test
  public void a_user_can_update_his_password() {
    String oldPassword = "Valar Dohaeris";
    UserDto user = createLocalUser(oldPassword);
    String oldCryptedPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin()).getCryptedPassword();
    userSessionRule.logIn(user);

    TestResponse response = tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("previousPassword", "Valar Dohaeris")
      .setParam("password", "Valar Morghulis")
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    String newCryptedPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin()).getCryptedPassword();
    assertThat(newCryptedPassword).isNotEqualTo(oldCryptedPassword);
  }

  @Test
  public void system_administrator_can_update_password_of_user() {
    UserDto admin = createLocalUser();
    userSessionRule.logIn(admin).setSystemAdministrator();
    UserDto user = createLocalUser();
    String originalPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin()).getCryptedPassword();

    tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("password", "Valar Morghulis")
      .execute();

    String newPassword = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin()).getCryptedPassword();
    assertThat(newPassword).isNotEqualTo(originalPassword);
  }

  @Test
  public void fail_to_update_someone_else_password_if_not_admin() {
    UserDto user = createLocalUser();
    userSessionRule.logIn(user);
    UserDto anotherUser = createLocalUser();

    TestRequest request = tester.newRequest()
      .setParam("login", anotherUser.getLogin())
      .setParam("previousPassword", "I dunno")
      .setParam("password", "Valar Morghulis");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_to_update_unknown_user() {
    UserDto admin = createLocalUser();
    userSessionRule.logIn(admin).setSystemAdministrator();

    TestRequest request = tester.newRequest()
      .setParam("login", "polop")
      .setParam("password", "polop");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User with login 'polop' has not been found");
  }

  @Test
  public void fail_on_disabled_user() {
    UserDto user = db.users().insertUser(u -> u.setActive(false));
    userSessionRule.logIn(user);

    TestRequest request = tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("password", "polop");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("User with login '%s' has not been found", user.getLogin()));
  }

  @Test
  public void fail_to_update_password_on_self_without_old_password() {
    UserDto user = createLocalUser();
    userSessionRule.logIn(user);

    TestRequest request = tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("password", "Valar Morghulis");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'previousPassword' parameter is missing");
  }

  @Test
  public void fail_to_update_password_on_self_with_bad_old_password() {
    UserDto user = createLocalUser();
    userSessionRule.logIn(user);

    TestRequest request = tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("previousPassword", "I dunno")
      .setParam("password", "Valar Morghulis");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Incorrect password");
  }

  @Test
  public void fail_to_update_password_on_external_auth() {
    UserDto admin = db.users().insertUser();
    userSessionRule.logIn(admin).setSystemAdministrator();
    UserDto user = db.users().insertUser(u -> u.setLocal(false));

    TestRequest request = tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("previousPassword", "I dunno")
      .setParam("password", "Valar Morghulis");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Password cannot be changed when external authentication is used");
  }

  @Test
  public void fail_to_update_to_same_password() {
    String oldPassword = "Valar Dohaeris";
    UserDto user = createLocalUser(oldPassword);
    userSessionRule.logIn(user);

    TestRequest request = tester.newRequest()
      .setParam("login", user.getLogin())
      .setParam("previousPassword", oldPassword)
      .setParam("password", oldPassword);

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Password must be different from old password");
  }

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(3);
  }

  private UserDto createLocalUser() {
    return db.users().insertUser(u -> u.setLocal(true));
  }

  private UserDto createLocalUser(String password) {
    UserDto user = createLocalUser();
    localAuthentication.storeHashPassword(user, password);
    db.getDbClient().userDao().update(db.getSession(), user);
    db.commit();
    return user;
  }

}
