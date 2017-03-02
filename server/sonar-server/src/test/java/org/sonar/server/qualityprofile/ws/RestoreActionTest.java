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

import java.io.Reader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

@RunWith(MockitoJUnitRunner.class)
public class RestoreActionTest {
  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  // TODO Replace with proper DbTester + EsTester medium test once DaoV2 is removed
  @Mock
  private QProfileBackuper backuper;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("ORG1");
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSessionRule, defaultOrganizationProvider);
  private WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new RestoreAction(backuper, LanguageTesting.newLanguages("xoo"), wsSupport)));
  }

  @Test
  public void restore_profile() throws Exception {
    logInAsQProfileAdministrator();

    QualityProfileDto profile = QualityProfileDto.createFor("xoo-sonar-way-12345")
      .setDefault(false).setLanguage("xoo").setName("Sonar way");
    BulkChangeResult restoreResult = new BulkChangeResult(profile);
    when(backuper.restore(any(Reader.class), (QProfileName) eq(null))).thenReturn(restoreResult);

    tester.newPostRequest("api/qualityprofiles", "restore").setParam("backup", "<polop><palap/></polop>").execute()
      .assertJson(getClass(), "restore_profile.json");
    verify(backuper).restore(any(Reader.class), (QProfileName) eq(null));
  }

  @Test
  public void fail_on_missing_backup() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A backup file must be provided");

    tester.newPostRequest("api/qualityprofiles", "restore").execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newPostRequest("api/qualityprofiles", "restore").execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newPostRequest("api/qualityprofiles", "restore").execute();
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }
}
