/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SetDefaultActionTest {

  private static final String XOO_1_KEY = "xoo1";
  private static final String XOO_2_KEY = "xoo2";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient;
  private QProfileWsSupport wsSupport;

  private SetDefaultAction underTest;
  private WsActionTester ws;

  /** Single, default quality profile for language xoo1 */
  private QProfileDto xoo1Profile;
  /** Parent quality profile for language xoo2 (not a default) */
  private QProfileDto xoo2Profile;
  /** Child quality profile for language xoo2, set as default */
  private QProfileDto xoo2Profile2;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    wsSupport = new QProfileWsSupport(dbClient, userSessionRule);
    underTest = new SetDefaultAction(LanguageTesting.newLanguages(XOO_1_KEY, XOO_2_KEY), dbClient, userSessionRule, wsSupport);

    xoo1Profile = QualityProfileTesting.newQualityProfileDto().setLanguage(XOO_1_KEY);
    xoo2Profile = QualityProfileTesting.newQualityProfileDto().setLanguage(XOO_2_KEY);
    xoo2Profile2 = QualityProfileTesting.newQualityProfileDto().setLanguage(XOO_2_KEY).setParentKee(xoo2Profile.getKee());
    dbClient.qualityProfileDao().insert(db.getSession(), xoo1Profile, xoo2Profile, xoo2Profile2);
    db.commit();
    db.qualityProfiles().setAsDefault(xoo1Profile, xoo2Profile2);

    ws = new WsActionTester(underTest);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition).isNotNull();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("qualityProfile", "language");
  }

  @Test
  public void set_default_profile_using_language_and_name() {
    logInAsQProfileAdministrator();

    checkDefaultProfile(XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(XOO_2_KEY, xoo2Profile2.getKee());

    TestResponse response = ws.newRequest().setMethod("POST")
      .setParam("language", xoo2Profile.getLanguage())
      .setParam("qualityProfile", xoo2Profile.getName())
      .execute();

    assertThat(response.getInput()).isEmpty();

    checkDefaultProfile(XOO_1_KEY, xoo1Profile.getKee());
    checkDefaultProfile(XOO_2_KEY, xoo2Profile.getKee());

  }

  @Test
  public void fail_to_set_default_profile_using_language_and_invalid_name() {
    logInAsQProfileAdministrator();

    try {
      TestResponse response = ws.newRequest().setMethod("POST")
        .setParam("language", XOO_2_KEY)
        .setParam("qualityProfile", "Unknown")
        .execute();
      Fail.failBecauseExceptionWasNotThrown(NotFoundException.class);
    } catch (NotFoundException nfe) {
      assertThat(nfe).hasMessage("Quality Profile for language 'xoo2' and name 'Unknown' does not exist");
      checkDefaultProfile(XOO_1_KEY, xoo1Profile.getKee());
      checkDefaultProfile(XOO_2_KEY, xoo2Profile2.getKee());
    }
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSessionRule.logIn();

    assertThatThrownBy(() -> {
      ws.newRequest().setMethod("POST")
        .setParam(PARAM_QUALITY_PROFILE, xoo2Profile.getName())
        .setParam(PARAM_LANGUAGE, xoo2Profile.getLanguage())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    assertThatThrownBy(() -> {
      ws.newRequest().setMethod("POST")
        .setParam(PARAM_KEY, xoo2Profile.getKee())
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private void checkDefaultProfile(String language, String key) {
    assertThat(dbClient.qualityProfileDao().selectDefaultProfile(db.getSession(), language).getKee()).isEqualTo(key);
  }
}
