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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

@RunWith(MockitoJUnitRunner.class)
public class CopyActionTest {

  private static final String DEFAULT_ORG_UUID = "U1";

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid(DEFAULT_ORG_UUID);
  private WsTester tester;

  // TODO Replace with proper DbTester + EsTester medium test during removal of DaoV2
  @Mock
  private QProfileCopier qProfileCopier;

  @Before
  public void setUp() {
    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new CopyAction(db.getDbClient(), qProfileCopier, LanguageTesting.newLanguages("xoo"), new QProfileWsSupport(db.getDbClient(), userSessionRule, defaultOrganizationProvider))));
  }

  @Test
  public void copy_nominal() throws Exception {
    logInAsQProfileAdministrator();

    String fromProfileKey = "xoo-sonar-way-23456";
    String toName = "Other Sonar Way";

    when(qProfileCopier.copyToName(any(DbSession.class), eq(fromProfileKey), eq(toName))).thenReturn(
      QualityProfileDto.createFor("xoo-other-sonar-way-12345")
        .setName(toName)
        .setLanguage("xoo"));

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("fromKey", fromProfileKey)
      .setParam("toName", toName)
      .execute().assertJson(getClass(), "copy_nominal.json");

    verify(qProfileCopier).copyToName(any(DbSession.class), eq(fromProfileKey), eq(toName));
  }

  @Test
  public void copy_with_parent() throws Exception {
    logInAsQProfileAdministrator();

    String fromProfileKey = "xoo-sonar-way-23456";
    String toName = "Other Sonar Way";

    when(qProfileCopier.copyToName(any(DbSession.class), eq(fromProfileKey), eq(toName))).thenReturn(
      QualityProfileDto.createFor("xoo-other-sonar-way-12345")
        .setName(toName)
        .setLanguage("xoo")
        .setParentKee("xoo-parent-profile-01324"));

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("fromKey", fromProfileKey)
      .setParam("toName", toName)
      .execute().assertJson(getClass(), "copy_with_parent.json");

    verify(qProfileCopier).copyToName(any(DbSession.class), eq(fromProfileKey), eq(toName));
  }

  @Test
  public void fail_if_parameter_fromKey_is_missing() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'fromKey' parameter is missing");

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("toName", "Other Sonar Way")
      .execute();
  }

  @Test
  public void fail_if_parameter_toName_is_missing() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'toName' parameter is missing");

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("fromKey", "sonar-way-xoo1-13245")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newPostRequest("api/qualityprofiles", "copy").execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newPostRequest("api/qualityprofiles", "copy").execute();
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }
}
