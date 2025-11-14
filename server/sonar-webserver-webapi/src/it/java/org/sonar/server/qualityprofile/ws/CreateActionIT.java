/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.qualityprofile.QProfileFactoryImpl;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse;
import org.sonarqube.ws.Qualityprofiles.CreateWsResponse.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.language.LanguageTesting.newLanguages;

class CreateActionIT {

  private static final String XOO_LANGUAGE = "xoo";

  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final EsTester es = EsTester.create();
  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(dbClient, es.client());

  private final CreateAction underTest = new CreateAction(dbClient, new QProfileFactoryImpl(dbClient, UuidFactoryFast.getInstance(), System2.INSTANCE, activeRuleIndexer),
    newLanguages(XOO_LANGUAGE), userSession);

  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key)
      .containsExactlyInAnyOrder("language", "name");
  }

  @Test
  void create_profile() {
    logInAsQProfileAdministrator();

    CreateWsResponse response = executeRequest("New Profile", XOO_LANGUAGE);

    QProfileDto dto = dbClient.qualityProfileDao().selectByNameAndLanguage(dbSession, "New Profile", XOO_LANGUAGE);
    assertThat(dto.getKee()).isNotNull();
    assertThat(dto.getLanguage()).isEqualTo(XOO_LANGUAGE);
    assertThat(dto.getName()).isEqualTo("New Profile");

    QualityProfile profile = response.getProfile();
    assertThat(profile.getKey()).isEqualTo(dto.getKee());
    assertThat(profile.getName()).isEqualTo("New Profile");
    assertThat(profile.getLanguage()).isEqualTo(XOO_LANGUAGE);
    assertThat(profile.getIsInherited()).isFalse();
    assertThat(profile.getIsDefault()).isFalse();
    assertThat(profile.hasInfos()).isFalse();
    assertThat(profile.hasWarnings()).isFalse();
  }

  @Test
  void fail_if_unsufficient_privileges() {
    userSession
      .logIn()
      .addPermission(SCAN);

    assertThatThrownBy(() -> {
      executeRequest(ws.newRequest()
        .setParam("name", "some Name")
        .setParam("language", XOO_LANGUAGE));
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  void test_json() {
    logInAsQProfileAdministrator();

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setMediaType(MediaTypes.JSON)
      .setParam("language", XOO_LANGUAGE)
      .setParam("name", "Yeehaw!")
      .execute();

    JsonAssert.assertJson(response.getInput()).isSimilarTo(getClass().getResource("CreateActionIT/test_json.json"));
    assertThat(response.getMediaType()).isEqualTo(MediaTypes.JSON);
  }

  private CreateWsResponse executeRequest(String name, String language) {
    TestRequest request = ws.newRequest()
      .setParam("name", name)
      .setParam("language", language);
    return executeRequest(request);
  }

  private CreateWsResponse executeRequest(TestRequest request) {
    return request.executeProtobuf(CreateWsResponse.class);
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }
}
