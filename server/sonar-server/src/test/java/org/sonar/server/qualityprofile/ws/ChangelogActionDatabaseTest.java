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
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;

public class ChangelogActionDatabaseTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private WsActionTester ws;
  private ChangelogLoader changelogLoader;
  private QProfileWsSupport wsSupport;
  private OrganizationDto organization;
  private DefaultOrganizationProvider defaultOrganizationProvider;

  @Before
  public void before() {
    defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
    wsSupport = new QProfileWsSupport(dbTester.getDbClient(), userSession, defaultOrganizationProvider);
    changelogLoader = new ChangelogLoader(dbTester.getDbClient());
    ws = new WsActionTester(
      new ChangelogAction(changelogLoader, wsSupport, new Languages(), dbTester.getDbClient()));
    organization = dbTester.organizations().insert();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("profile", "profileName", "language", "organization", "since", "to", "p", "ps");
    WebService.Param profile = definition.param("profile");
    assertThat(profile.deprecatedKey()).isEqualTo("profileKey");
    WebService.Param profileName = definition.param("profileName");
    assertThat(profileName.deprecatedSince()).isEqualTo("6.5");
    WebService.Param language = definition.param("language");
    assertThat(language.deprecatedSince()).isEqualTo("6.5");
  }

  @Test
  public void find_changelog_by_profile_key() throws Exception {
    QProfileDto profile = dbTester.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_PROFILE, profile.getKee())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void find_changelog_by_language_and_name() throws Exception {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(dbTester.getDefaultOrganization());

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_PROFILE_NAME, qualityProfile.getName())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void find_changelog_by_organization_and_language_and_name() throws Exception {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_PROFILE_NAME, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute()
      .getInput();

    assertThat(response).isNotEmpty();
  }

  @Test
  public void do_not_find_changelog_by_wrong_organization_and_language_and_name() throws Exception {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();

    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization1);

    TestRequest request = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_LANGUAGE, qualityProfile.getLanguage())
      .setParam(PARAM_PROFILE_NAME, qualityProfile.getName())
      .setParam(PARAM_ORGANIZATION, organization2.getKey());

    thrown.expect(NotFoundException.class);

    request.execute();
  }

  @Test
  public void changelog_empty() throws Exception {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_PROFILE, qualityProfile.getKee())
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":0");
    assertThat(response).contains("\"events\":[]");
  }

  @Test
  public void changelog_not_empty() throws Exception {
    QProfileDto qualityProfile = dbTester.qualityProfiles().insert(organization);
    QProfileChangeDto change = QualityProfileTesting.newQProfileChangeDto()
      .setUuid(null)
      .setCreatedAt(0)
      .setRulesProfileUuid(qualityProfile.getRulesProfileUuid());
    DbSession session = dbTester.getSession();
    dbTester.getDbClient().qProfileChangeDao().insert(session, change);
    session.commit();

    String response = ws.newRequest()
      .setMethod("GET")
      .setParam(PARAM_PROFILE, qualityProfile.getKee())
      .execute()
      .getInput();

    assertThat(response).contains("\"total\":1");
  }
}
