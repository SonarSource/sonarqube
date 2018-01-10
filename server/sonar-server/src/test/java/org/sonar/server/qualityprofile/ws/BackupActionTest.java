/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileBackuper;
import org.sonar.server.qualityprofile.QProfileBackuperImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;

public class BackupActionTest {

  private static final String A_LANGUAGE = "xoo";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private QProfileBackuper backuper = new QProfileBackuperImpl(db.getDbClient(), null, null);
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, defaultOrganizationProvider);
  private Languages languages = LanguageTesting.newLanguages(A_LANGUAGE);
  private WsActionTester tester = new WsActionTester(new BackupAction(db.getDbClient(), backuper, wsSupport, languages));

  @Test
  public void test_definition() {
    WebService.Action definition = tester.getDef();

    assertThat(definition.key()).isEqualTo("backup");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.isPost()).isFalse();

    // parameters
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("key", "organization", "qualityProfile", "language");
    Param key = definition.param("key");
    assertThat(key.deprecatedKey()).isEqualTo("profileKey");
    assertThat(key.deprecatedSince()).isEqualTo("6.6");
    Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isNullOrEmpty();
    Param profileName = definition.param("qualityProfile");
    Param orgParam = definition.param("organization");
    assertThat(orgParam.since()).isEqualTo("6.4");
  }

  @Test
  public void returns_backup_of_profile_with_specified_key() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());

    TestResponse response = tester.newRequest().setParam(PARAM_KEY, profile.getKee()).execute();
    assertThat(response.getMediaType()).isEqualTo("application/xml");
    assertThat(response.getInput()).isXmlEqualTo(xmlForProfileWithoutRules(profile));
    assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=" + profile.getKee() + ".xml");
  }

  @Test
  public void returns_backup_of_profile_with_specified_name_on_default_organization() {
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(A_LANGUAGE));

    TestResponse response = tester.newRequest()
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .execute();
    assertThat(response.getInput()).isXmlEqualTo(xmlForProfileWithoutRules(profile));
  }

  @Test
  public void returns_backup_of_profile_with_specified_name_and_organization() {
    OrganizationDto org = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(org, p -> p.setLanguage(A_LANGUAGE));

    TestResponse response = tester.newRequest()
      .setParam("organization", org.getKey())
      .setParam("language", profile.getLanguage())
      .setParam("qualityProfile", profile.getName())
      .execute();
    assertThat(response.getInput()).isXmlEqualTo(xmlForProfileWithoutRules(profile));
  }

  @Test
  public void throws_NotFoundException_if_profile_with_specified_key_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'missing' does not exist");

    tester.newRequest().setParam(PARAM_KEY, "missing").execute();
  }

  @Test
  public void throws_NotFoundException_if_specified_organization_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'the-missing-org'");

    tester.newRequest()
      .setParam("organization", "the-missing-org")
      .setParam("language", A_LANGUAGE)
      .setParam("qualityProfile", "the-name")
      .execute();
  }

  @Test
  public void throws_NotFoundException_if_profile_name_exists_but_in_another_organization() {
    OrganizationDto org1 = db.organizations().insert();
    QProfileDto profileInOrg1 = db.qualityProfiles().insert(org1, p -> p.setLanguage(A_LANGUAGE));
    OrganizationDto org2 = db.organizations().insert();
    QProfileDto profileInOrg2 = db.qualityProfiles().insert(org2, p -> p.setLanguage(A_LANGUAGE));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile for language 'xoo' and name '" + profileInOrg1.getName() + "' does not exist in organization '" + org2.getKey() + "'");

    tester.newRequest()
      .setParam("organization", org2.getKey())
      .setParam("language", profileInOrg1.getLanguage())
      .setParam("qualityProfile", profileInOrg1.getName())
      .execute();
  }

  @Test
  public void throws_IAE_if_profile_reference_is_not_set() {
    expectedException.expect(IllegalArgumentException.class);

    tester.newRequest().execute();
  }

  private static String xmlForProfileWithoutRules(QProfileDto profile) {
    return "<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile>" +
      "  <name>" + profile.getName() + "</name>" +
      "  <language>" + profile.getLanguage() + "</language>" +
      "  <rules/>" +
      "</profile>";
  }

  private static QProfileDto newProfile(OrganizationDto org) {
    return QualityProfileTesting.newQualityProfileDto()
      .setLanguage(A_LANGUAGE)
      .setOrganizationUuid(org.getUuid());
  }
}
