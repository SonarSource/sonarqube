/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import java.io.Reader;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RestoreActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  // TODO Replace with proper DbTester + EsTester medium test once DaoV2 is removed
  @Mock
  private QProfileBackuper backuper;

  private WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      mock(ProjectAssociationActions.class),
      new RestoreAction(backuper, LanguageTesting.newLanguages("xoo"), userSessionRule)));
  }

  @Test
  public void restore_profile() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    QualityProfileDto profile = QualityProfileDto.createFor("xoo-sonar-way-12345")
      .setDefault(false).setLanguage("xoo").setName("Sonar way");
    BulkChangeResult restoreResult = new BulkChangeResult(profile);
    when(backuper.restore(any(Reader.class), (QProfileName) eq(null))).thenReturn(restoreResult);

    tester.newPostRequest("api/qualityprofiles", "restore").setParam("backup", "<polop><palap/></polop>").execute()
      .assertJson(getClass(), "restore_profile.json");
    verify(backuper).restore(any(Reader.class), (QProfileName) eq(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_backup() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "restore").execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_misssing_permission() throws Exception {
    userSessionRule.login("obiwan");

    tester.newPostRequest("api/qualityprofiles", "restore").setParam("backup", "<polop><palap/></polop>").execute();
  }
}
