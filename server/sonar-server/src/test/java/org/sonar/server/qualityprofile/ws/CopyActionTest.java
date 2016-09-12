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
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CopyActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WsTester tester;

  // TODO Replace with proper DbTester + EsTester medium test during removal of DaoV2
  @Mock
  private QProfileCopier qProfileCopier;

  @Before
  public void setUp() {
    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new CopyAction(qProfileCopier, LanguageTesting.newLanguages("xoo"), userSessionRule)));
  }

  @Test
  public void copy_nominal() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    String fromProfileKey = "xoo-sonar-way-23456";
    String toName = "Other Sonar Way";

    when(qProfileCopier.copyToName(fromProfileKey, toName)).thenReturn(
      QualityProfileDto.createFor("xoo-other-sonar-way-12345")
        .setName(toName)
        .setLanguage("xoo"));

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("fromKey", fromProfileKey)
      .setParam("toName", toName)
      .execute().assertJson(getClass(), "copy_nominal.json");

    verify(qProfileCopier).copyToName(fromProfileKey, toName);
  }

  @Test
  public void copy_with_parent() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    String fromProfileKey = "xoo-sonar-way-23456";
    String toName = "Other Sonar Way";

    when(qProfileCopier.copyToName(fromProfileKey, toName)).thenReturn(
      QualityProfileDto.createFor("xoo-other-sonar-way-12345")
        .setName(toName)
        .setLanguage("xoo")
        .setParentKee("xoo-parent-profile-01324"));

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("fromKey", fromProfileKey)
      .setParam("toName", toName)
      .execute().assertJson(getClass(), "copy_with_parent.json");

    verify(qProfileCopier).copyToName(fromProfileKey, toName);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_key() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("name", "Other Sonar Way")
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_missing_name() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("key", "sonar-way-xoo1-13245")
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    userSessionRule.login("obiwan");

    tester.newPostRequest("api/qualityprofiles", "copy")
      .setParam("key", "sonar-way-xoo1-13245")
      .setParam("name", "Hey look I am not quality profile admin!")
      .execute();
  }
}
