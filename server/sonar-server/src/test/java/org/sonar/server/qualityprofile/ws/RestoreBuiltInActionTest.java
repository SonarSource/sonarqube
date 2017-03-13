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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileReset;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RestoreBuiltInActionTest {
  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private QProfileReset reset = mock(QProfileReset.class);
  private Languages languages = LanguageTesting.newLanguages("xoo");
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("ORG1");
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);

  private WsActionTester tester = new WsActionTester(new RestoreBuiltInAction(db.getDbClient(), reset, languages, wsSupport));

  @Test
  public void return_empty_result_when_no_info_or_warning() {
    logInAsQProfileAdministrator();
    TestResponse response = tester.newRequest().setParam("language", "xoo").execute();

    verify(reset).resetLanguage(any(DbSession.class), eq("xoo"));
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void fail_on_unknown_language() throws Exception {
    logInAsQProfileAdministrator();
    expectedException.expect(IllegalArgumentException.class);
    tester.newRequest().setParam("language", "unknown").execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newRequest().setParam("language", "xoo").execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newRequest().setParam("language", "xoo").execute();
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }
}
