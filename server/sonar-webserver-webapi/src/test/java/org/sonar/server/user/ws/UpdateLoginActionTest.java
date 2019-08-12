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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.OrganizationUpdater;
import org.sonar.server.organization.OrganizationUpdaterImpl;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.NewUserNotifier;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

public class UpdateLoginActionTest {

  private System2 system2 = new System2();

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private OrganizationUpdater organizationUpdater = new OrganizationUpdaterImpl(db.getDbClient(), mock(System2.class), UuidFactoryFast.getInstance(),
    new OrganizationValidationImpl(), null, null, null, null);

  private WsActionTester ws = new WsActionTester(new UpdateLoginAction(db.getDbClient(), userSession,
    new UserUpdater(system2, mock(NewUserNotifier.class), db.getDbClient(), new UserIndexer(db.getDbClient(), es.client()),
      null, null, null, null, null),
    organizationUpdater));

  @Test
  public void update_login_from_sonarqube_account() {
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

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(format("A user with login '%s' already exists", user2.getLogin()));

    ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam("newLogin", user2.getLogin())
      .execute();
  }

  @Test
  public void fail_with_NFE_when_login_does_not_match_active_user() {
    userSession.logIn().setSystemAdministrator();
    UserDto user = db.users().insertDisabledUser();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("User '%s' doesn't exist", user.getLogin()));

    ws.newRequest()
      .setParam("login", user.getLogin())
      .setParam("newLogin", "new_login")
      .execute();
  }

  @Test
  public void fail_with_NFE_when_login_does_not_match_existing_user() {
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'unknown' doesn't exist");

    ws.newRequest()
      .setParam("login", "unknown")
      .setParam("newLogin", "new_login")
      .execute();
  }

  @Test
  public void fail_when_not_system_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam("login", "old_login")
      .setParam("newLogin", "new_login")
      .execute();
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
