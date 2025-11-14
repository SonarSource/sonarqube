/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UpdateLoginActionIT {

  private final System2 system2 = new System2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();

  private final ManagedInstanceChecker managedInstanceChecker = mock(ManagedInstanceChecker.class);
  private final WsActionTester ws = new WsActionTester(new UpdateLoginAction(db.getDbClient(), userSession,
    new UserUpdater(mock(NewUserNotifier.class), db.getDbClient(), null, null, null, null), managedInstanceChecker));

  @Test
  public void update_login_from_sonarqube_account_when_user_is_local() {
    userSession.logIn().setSystemAdministrator();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("old_login")
      .setLocal(true)
      .setExternalIdentityProvider("sonarqube")
      .setExternalLogin("old_login")
      .setExternalId("old_login"));

    ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam("newLogin", "new_login")
      .execute();

    assertThat(db.getDbClient().userDao().selectByLogin(db.getSession(), "old_login")).isNull();
    UserDto userReloaded = db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid());
    assertThat(userReloaded.getLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalId()).isEqualTo("new_login");
    assertThat(userReloaded.isLocal()).isTrue();
    assertThat(userReloaded.getCryptedPassword()).isNotNull().isEqualTo(user.getCryptedPassword());
    assertThat(userReloaded.getSalt()).isNotNull().isEqualTo(user.getSalt());
  }

  @Test
  public void update_login_from_sonarqube_account_when_user_is_not_local() {
    userSession.logIn().setSystemAdministrator();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("old_login")
      .setLocal(false)
      .setExternalIdentityProvider("sonarqube")
      .setExternalLogin("old_login")
      .setExternalId("old_login"));

    ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam("newLogin", "new_login")
      .execute();

    assertThat(db.getDbClient().userDao().selectByLogin(db.getSession(), "old_login")).isNull();
    UserDto userReloaded = db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid());
    assertThat(userReloaded.getLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalId()).isEqualTo("new_login");
    assertThat(userReloaded.isLocal()).isFalse();
    assertThat(userReloaded.getCryptedPassword()).isNotNull().isEqualTo(user.getCryptedPassword());
    assertThat(userReloaded.getSalt()).isNotNull().isEqualTo(user.getSalt());
  }

  @Test
  public void update_login_from_external_account() {
    userSession.logIn().setSystemAdministrator();
    UserDto user = db.users().insertUser(u -> u
      .setLogin("old_login")
      .setLocal(false)
      .setExternalIdentityProvider("github")
      .setExternalLogin("github_login")
      .setExternalId("github_id")
      .setCryptedPassword(null)
      .setSalt(null));

    ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam("newLogin", "new_login")
      .execute();

    UserDto userReloaded = db.getDbClient().userDao().selectByUuid(db.getSession(), user.getUuid());
    assertThat(userReloaded.getLogin()).isEqualTo("new_login");
    assertThat(userReloaded.getExternalLogin()).isEqualTo("github_login");
    assertThat(userReloaded.getExternalId()).isEqualTo("github_id");
    assertThat(userReloaded.isLocal()).isFalse();
    assertThat(userReloaded.getCryptedPassword()).isNull();
    assertThat(userReloaded.getSalt()).isNull();
  }

  @Test
  public void fail_with_IAE_when_new_login_is_already_used() {
    userSession.logIn().setSystemAdministrator();
    UserDto user = db.users().insertUser();
    UserDto user2 = db.users().insertUser();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", user.getLogin())
        .setParam("newLogin", user2.getLogin())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("A user with login '%s' already exists", user2.getLogin()));
  }

  @Test
  public void fail_with_NFE_when_login_does_not_match_active_user() {
    userSession.logIn().setSystemAdministrator();
    UserDto user = db.users().insertDisabledUser();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", user.getLogin())
        .setParam("newLogin", "new_login")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("User '%s' doesn't exist", user.getLogin()));
  }

  @Test
  public void fail_with_NFE_when_login_does_not_match_existing_user() {
    userSession.logIn().setSystemAdministrator();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", "unknown")
        .setParam("newLogin", "new_login")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'unknown' doesn't exist");
  }

  @Test
  public void fail_when_not_system_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("login", "old_login")
        .setParam("newLogin", "new_login")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void response_has_no_content() {
    UserDto user = db.users().insertUser();
    userSession.logIn().setSystemAdministrator();

    TestResponse response = ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam("newLogin", "new_login")
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void handle_whenInstanceManaged_shouldThrowBadRequestException() {
    BadRequestException badRequestException = BadRequestException.create("message");
    doThrow(badRequestException).when(managedInstanceChecker).throwIfInstanceIsManaged();

    TestRequest request = ws.newRequest();

    assertThatThrownBy(request::execute)
      .isEqualTo(badRequestException);
  }

  @Test
  public void handle_whenInstanceManagedAndNotSystemAdministrator_shouldThrowUnauthorizedException() {
    userSession.anonymous();

    TestRequest request = ws.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
    verify(managedInstanceChecker, never()).throwIfInstanceIsManaged();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("update_login");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.since()).isEqualTo("7.6");

    assertThat(def.params())
      .extracting(Param::key, Param::isRequired, Param::maximumLength, Param::minimumLength)
      .containsExactlyInAnyOrder(
        tuple("login", true, null, null),
        tuple("newLogin", true, 255, 2));
  }
}
