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

import org.assertj.core.api.Fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SetDefaultActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient;

  private QualityProfileDao qualityProfileDao;

  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";

  private WsTester tester;

  private DbSession session;

  @Before
  public void setUp() {
    qualityProfileDao = new QualityProfileDao(dbTester.myBatis(), mock(System2.class));
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), qualityProfileDao);
    session = dbClient.openSession(false);

    createProfiles();

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new SetDefaultAction(LanguageTesting.newLanguages(xoo1Key, xoo2Key), new QProfileLookup(dbClient), new QProfileFactory(dbClient), userSessionRule)));
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void set_default_profile_using_key() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
    checkDefaultProfile(xoo2Key, "my-sonar-way-xoo2-34567");

    tester.newPostRequest("api/qualityprofiles", "set_default").setParam("profileKey", "sonar-way-xoo2-23456").execute().assertNoContent();

    checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
    checkDefaultProfile(xoo2Key, "sonar-way-xoo2-23456");
    assertThat(dbClient.qualityProfileDao().selectByKey(session, "sonar-way-xoo2-23456").isDefault()).isTrue();
    assertThat(dbClient.qualityProfileDao().selectByKey(session, "my-sonar-way-xoo2-34567").isDefault()).isFalse();

    // One more time!
    tester.newPostRequest("api/qualityprofiles", "set_default").setParam("profileKey", "sonar-way-xoo2-23456").execute().assertNoContent();
    checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
    checkDefaultProfile(xoo2Key, "sonar-way-xoo2-23456");
  }

  @Test
  public void set_default_profile_using_language_and_name() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    tester.newPostRequest("api/qualityprofiles", "set_default").setParam("language", xoo2Key).setParam("profileName", "Sonar way").execute().assertNoContent();

    checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
    checkDefaultProfile(xoo2Key, "sonar-way-xoo2-23456");
  }

  @Test
  public void fail_to_set_default_profile_using_key() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    try {
      tester.newPostRequest("api/qualityprofiles", "set_default").setParam("profileKey", "unknown-profile-666").execute();
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (NotFoundException nfe) {
      assertThat(nfe).hasMessage("Quality profile not found: unknown-profile-666");
      checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
      checkDefaultProfile(xoo2Key, "my-sonar-way-xoo2-34567");
    }
  }

  @Test
  public void fail_to_set_default_profile_using_language_and_name() throws Exception {
    userSessionRule.login("obiwan").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    try {
      tester.newPostRequest("api/qualityprofiles", "set_default").setParam("language", xoo2Key).setParam("profileName", "Unknown").execute();
      Fail.failBecauseExceptionWasNotThrown(NotFoundException.class);
    } catch (NotFoundException nfe) {
      assertThat(nfe).hasMessage("Unable to find a profile for language 'xoo2' with name 'Unknown'");
      checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
      checkDefaultProfile(xoo2Key, "my-sonar-way-xoo2-34567");
    }
  }

  @Test
  public void fail_on_missing_permission() throws Exception {
    userSessionRule.login("obiwan");

    try {
      tester.newPostRequest("api/qualityprofiles", "set_default").setParam("profileKey", "sonar-way-xoo2-23456").execute().assertNoContent();
      Fail.failBecauseExceptionWasNotThrown(ForbiddenException.class);
    } catch (ForbiddenException forbidden) {
      checkDefaultProfile(xoo1Key, "sonar-way-xoo1-12345");
      checkDefaultProfile(xoo2Key, "my-sonar-way-xoo2-34567");
    }
  }

  private void createProfiles() {
    qualityProfileDao.insert(session,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1Key).setName("Sonar way").setDefault(true),
      QualityProfileDto.createFor("sonar-way-xoo2-23456").setLanguage(xoo2Key).setName("Sonar way"),
      QualityProfileDto.createFor("my-sonar-way-xoo2-34567").setLanguage(xoo2Key).setName("My Sonar way").setParentKee("sonar-way-xoo2-23456").setDefault(true));
    session.commit();
  }

  private void checkDefaultProfile(String language, String key) {
    assertThat(dbClient.qualityProfileDao().selectDefaultProfile(language).getKey()).isEqualTo(key);
  }
}
