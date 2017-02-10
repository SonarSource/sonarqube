/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.organization.ws;

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationValidationImpl;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.OrganizationFlagsImpl;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

public class EnableSupportActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private OrganizationFlags organizationFlags = new OrganizationFlagsImpl(db.getDbClient());
  private EnableSupportAction underTest = new EnableSupportAction(userSession, db.getDbClient(), defaultOrganizationProvider, organizationFlags);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void enabling_support_saves_internal_property_and_flags_caller_as_root() {
    UserDto user = db.users().insertUser();
    UserDto otherUser = db.users().insertUser();
    verifyFeatureEnabled(false);
    verifyRoot(user, false);
    verifyRoot(otherUser, false);
    logInAsSystemAdministrator(user.getLogin());

    call();

    verifyFeatureEnabled(true);
    verifyRoot(user, true);
    verifyRoot(otherUser, false);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call();
  }

  @Test
  public void do_nothing_if_support_is_already_enabled() {
    logInAsSystemAdministrator("foo");

    call();
    verifyFeatureEnabled(true);

    // the test could be improved to verify that
    // the caller user is not flagged as root
    // if he was not already root
    call();
    verifyFeatureEnabled(true);
  }

  @Test
  public void test_definition() {
    WebService.Action def = tester.getDef();
    assertThat(def.key()).isEqualTo("enable_support");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).isEmpty();
  }

  private void logInAsSystemAdministrator(String login) {
    userSession.logIn(login).addOrganizationPermission(db.getDefaultOrganization().getUuid(), SYSTEM_ADMIN);
  }

  private void call() {
    TestResponse response = tester.newRequest().setMethod("POST").execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
  }

  private void verifyFeatureEnabled(boolean enabled) {
    assertThat(organizationFlags.isEnabled(db.getSession())).isEqualTo(enabled);
  }

  private void verifyRoot(UserDto user, boolean root) {
    db.rootFlag().verify(user.getLogin(), root);
  }
}
