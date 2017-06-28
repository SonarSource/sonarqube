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
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.QualityProfiles.ShowWsResponse;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.QualityProfiles.ShowWsResponse.QualityProfile;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;

public class ShowActionTest {

  private static Language XOO1 = newLanguage("xoo1");
  private static Language XOO2 = newLanguage("xoo2");
  private static Languages LANGUAGES = new Languages(XOO1, XOO2);

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private WsActionTester ws = new WsActionTester(
    new ShowAction(db.getDbClient(), new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db)), LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("show");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isFalse();
    assertThat(action.since()).isEqualTo("6.5");

    WebService.Param profile = action.param("profile");
    assertThat(profile.isRequired()).isTrue();
    assertThat(profile.isInternal()).isFalse();
    assertThat(profile.description()).isNotEmpty();

    WebService.Param compareToSonarWay = action.param("compareToSonarWay");
    assertThat(compareToSonarWay.isRequired()).isFalse();
    assertThat(compareToSonarWay.isInternal()).isTrue();
    assertThat(compareToSonarWay.description()).isNotEmpty();
    assertThat(compareToSonarWay.possibleValues()).contains("true", "false");
  }

  @Test
  public void profile_info() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));

    ShowWsResponse result = call(ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()));

    assertThat(result.getProfile())
      .extracting(QualityProfile::getKey, QualityProfile::getName, QualityProfile::getIsBuiltIn, QualityProfile::getLanguage, QualityProfile::getLanguageName,
        QualityProfile::getIsInherited)
      .containsExactly(profile.getKee(), profile.getName(), profile.isBuiltIn(), profile.getLanguage(), XOO1.getName(), false);
  }

  @Test
  public void default_profile() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    db.qualityProfiles().setAsDefault(profile);

    ShowWsResponse result = call(ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()));

    assertThat(result.getProfile().getIsDefault()).isTrue();
  }

  @Test
  public void non_default_profile() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    QProfileDto defaultProfile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    db.qualityProfiles().setAsDefault(defaultProfile);

    ShowWsResponse result = call(ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()));

    assertThat(result.getProfile().getIsDefault()).isFalse();
  }

  @Test
  public void map_dates() {
    long time = DateUtils.parseDateTime("2016-12-22T19:10:03+0100").getTime();
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p
      .setLanguage(XOO1.getKey())
      .setRulesUpdatedAt("2016-12-21T19:10:03+0100")
      .setLastUsed(time)
      .setUserUpdatedAt(time));

    ShowWsResponse result = call(ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()));

    assertThat(result.getProfile().getRulesUpdatedAt()).isEqualTo("2016-12-21T19:10:03+0100");
    assertThat(parseDateTime(result.getProfile().getLastUsed()).getTime()).isEqualTo(time);
    assertThat(parseDateTime(result.getProfile().getUserUpdatedAt()).getTime()).isEqualTo(time);
  }

  @Test
  public void statistics() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(XOO1.getKey()));
    // Active rules
    range(0, 10)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(XOO1.getKey())).getDefinition())
      .forEach(r -> db.qualityProfiles().activateRule(profile, r));
    // Deprecated rules
    range(0, 3)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(XOO1.getKey()).setStatus(DEPRECATED)).getDefinition())
      .forEach(r -> db.qualityProfiles().activateRule(profile, r));
    // Projects
    range(0, 7)
      .mapToObj(i -> db.components().insertPrivateProject())
      .forEach(project -> db.qualityProfiles().associateWithProject(project, profile));

    ShowWsResponse result = call(ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()));

    assertThat(result.getProfile())
      .extracting(QualityProfile::getActiveRuleCount, QualityProfile::getActiveDeprecatedRuleCount, QualityProfile::getProjectCount)
      .containsExactly(13L, 3L, 7L);
  }

  @Test
  public void fail_if_profile_language_is_not_supported() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setKee("unknown-profile").setLanguage("kotlin"));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'unknown-profile' does not exist");

    call(ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()));
  }

  @Test
  public void fail_if_profile_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'unknown-profile' does not exist");

    call(ws.newRequest().setParam(PARAM_PROFILE, "unknown-profile"));
  }

  @Test
  public void json_example() {
    Language cs = newLanguage("cs", "C#");
    QProfileDto parentProfile = db.qualityProfiles().insert(db.getDefaultOrganization(),
      p -> p.setKee("AU-TpxcA-iU5OvuD2FL1")
        .setName("Parent Company Profile")
        .setLanguage(cs.getKey()));
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p
      .setKee("AU-TpxcA-iU5OvuD2FL3")
      .setName("My Company Profile")
      .setLanguage(cs.getKey())
      .setIsBuiltIn(false)
      .setRulesUpdatedAt("2016-12-22T19:10:03+0100")
      .setParentKee(parentProfile.getKee()));
    // Active rules
    range(0, 10)
      .mapToObj(i -> db.rules().insertRule(r -> r.setLanguage(cs.getKey())).getDefinition())
      .forEach(r -> db.qualityProfiles().activateRule(profile, r));
    // Projects
    range(0, 7)
      .mapToObj(i -> db.components().insertPrivateProject())
      .forEach(project -> db.qualityProfiles().associateWithProject(project, profile));

    ws = new WsActionTester(new ShowAction(db.getDbClient(), new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db)), new Languages(cs)));
    String result = ws.newRequest().setParam(PARAM_PROFILE, profile.getKee()).execute().getInput();

    assertJson(result).ignoreFields("rulesUpdatedAt", "lastUsed", "userUpdatedAt", "compareToSonarWay").isSimilarTo(ws.getDef().responseExampleAsString());
  }

  private ShowWsResponse call(TestRequest request) {
    TestRequest wsRequest = request.setMediaType(MediaTypes.PROTOBUF);
    return wsRequest.executeProtobuf(ShowWsResponse.class);
  }
}
