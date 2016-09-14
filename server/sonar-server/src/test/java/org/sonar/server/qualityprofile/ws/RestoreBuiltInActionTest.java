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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileReset;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;

public class RestoreBuiltInActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private QProfileReset reset = mock(QProfileReset.class);
  private Languages languages = LanguageTesting.newLanguages("xoo");

  private WsActionTester tester = new WsActionTester(new RestoreBuiltInAction(reset, languages, userSession));

  @Test
  public void return_empty_result_when_no_info_or_warning() {
    userSession.login("himself").setGlobalPermissions(QUALITY_PROFILE_ADMIN);
    TestResponse response = tester.newRequest().setParam("language", "xoo").execute();

    verify(reset).resetLanguage("xoo");
    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void fail_on_unknown_language() throws Exception {
    userSession.login("himself").setGlobalPermissions(QUALITY_PROFILE_ADMIN);
    expectedException.expect(IllegalArgumentException.class);
    tester.newRequest().setParam("language", "unknown").execute();
  }

  @Test
  public void fail_if_not_profile_administrator() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    tester.newRequest().setParam("language", "xoo").execute();
  }
}
