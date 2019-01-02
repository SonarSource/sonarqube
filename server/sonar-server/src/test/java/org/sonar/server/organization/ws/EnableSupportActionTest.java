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
package org.sonar.server.organization.ws;

import java.net.HttpURLConnection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganisationSupport;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;

public class EnableSupportActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private OrganisationSupport organisationSupport = mock(OrganisationSupport.class);
  private EnableSupportAction underTest = new EnableSupportAction(userSession, defaultOrganizationProvider, organisationSupport);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void enabling_support_saves_internal_property_and_flags_caller_as_root() {
    UserDto user = dbTester.users().insertUser();
    UserDto otherUser = dbTester.users().insertUser();
    logInAsSystemAdministrator(user.getLogin());

    call();

    verify(organisationSupport).enable(user.getLogin());
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
  public void call_delegate_even_if_support_is_already_enabled() {
    dbTester.users().insertDefaultGroup(dbTester.getDefaultOrganization(), "sonar-users");
    logInAsSystemAdministrator("foo");

    call();
    verify(organisationSupport).enable("foo");
    reset(organisationSupport);

    call();
    verify(organisationSupport).enable("foo");
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
    userSession.logIn(login).addPermission(ADMINISTER, dbTester.getDefaultOrganization());
  }

  private void call() {
    TestResponse response = tester.newRequest().setMethod("POST").execute();
    assertThat(response.getStatus()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT);
  }

}
