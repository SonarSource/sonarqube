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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileBackuperImpl;
import org.sonar.server.qualityprofile.QProfileParser;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BackupActionIT {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final QProfileBackuper backuper = new QProfileBackuperImpl(db.getDbClient(), null, null, null, new QProfileParser());
  private final QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession);
  private final Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private final WsActionTester tester = new WsActionTester(new BackupAction(db.getDbClient(), backuper, wsSupport, languages));

  @Test
  public void returns_backup_of_profile_with_specified_key() {
    QProfileDto profile = db.qualityProfiles().insert(qp -> qp.setLanguage("xoo"));

    TestResponse response = tester.newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .execute();
    assertThat(response.getMediaType()).isEqualTo("application/xml");
    assertThat(response.getInput()).isXmlEqualTo(xmlForProfileWithoutRules(profile));
    assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=" + profile.getKee() + ".xml");
  }

  @Test
  public void returns_backup_of_profile_with_specified_name() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage(A_LANGUAGE));

    TestResponse response = tester.newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .execute();
    assertThat(response.getInput()).isXmlEqualTo(xmlForProfileWithoutRules(profile));
  }

  @Test
  public void throws_IAE_if_profile_reference_is_not_set() {
    TestRequest request = tester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();

    assertThat(definition.key()).isEqualTo("backup");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.isPost()).isFalse();

    // parameters
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("qualityProfile", "language");
    Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
  }

  private static String xmlForProfileWithoutRules(QProfileDto profile) {
    return "<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile>" +
      "  <name>" + profile.getName() + "</name>" +
      "  <language>" + profile.getLanguage() + "</language>" +
      "  <rules/>" +
      "</profile>";
  }

}
