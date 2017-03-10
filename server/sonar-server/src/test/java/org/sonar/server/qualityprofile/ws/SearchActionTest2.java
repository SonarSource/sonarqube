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

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;

public class SearchActionTest2 {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private QProfileWsSupport qProfileWsSupport = new QProfileWsSupport(dbTester.getDbClient(), userSession, defaultOrganizationProvider);
  private ActiveRuleIndex activeRuleIndex = mock(ActiveRuleIndex.class);

  private Language xoo1 = LanguageTesting.newLanguage("xoo1");
  private Language xoo2 = LanguageTesting.newLanguage("xoo2");
  private Languages languages = new Languages(xoo1, xoo2);

  @Test
  public void search_default_profile_by_profile_name_and_org() {
    OrganizationDto org1 = dbTester.organizations().insert();
    QualityProfileDto profile1 = QualityProfileDto.createFor("ORG1-SONAR-XOO1")
      .setOrganizationUuid(org1.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar Xoo1 way")
      .setDefault(false);
    QualityProfileDto profile2 = QualityProfileDto.createFor("ORG1-SONAR-XOO2")
      .setOrganizationUuid(org1.getUuid())
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar Xoo2 way")
      .setDefault(false);
    QualityProfileDto profile3 = QualityProfileDto.createFor("ORG1-MYWAY-XOO1")
      .setOrganizationUuid(org1.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("My Xoo1 way")
      .setDefault(true);
    QualityProfileDto profile4 = QualityProfileDto.createFor("ORG1-MYWAY-XOO2")
      .setOrganizationUuid(org1.getUuid())
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("My Xoo2 way")
      .setDefault(true);
    OrganizationDto org2 = dbTester.organizations().insert();
    QualityProfileDto profile5 = QualityProfileDto.createFor("ORG2-SONAR-XOO1")
      .setOrganizationUuid(org2.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar Xoo1 way")
      .setDefault(false);
    QualityProfileDto profile6 = QualityProfileDto.createFor("ORG2-SONAR-XOO2")
      .setOrganizationUuid(org2.getUuid())
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar Xoo2 way")
      .setDefault(false);
    QualityProfileDto profile7 = QualityProfileDto.createFor("ORG2-MYWAY-XOO1")
      .setOrganizationUuid(org2.getUuid())
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("My Xoo1 way")
      .setDefault(true);
    QualityProfileDto profile8 = QualityProfileDto.createFor("ORG2-MYWAY-XOO2")
      .setOrganizationUuid(org2.getUuid())
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("My Xoo2 way")
      .setDefault(true);
    dbTester.qualityProfiles().insertQualityProfiles(profile1, profile2, profile3, profile4, profile5, profile6);

    DbClient dbClient = dbTester.getDbClient();
    SearchAction underTest = new SearchAction(
      new SearchDataLoader(
        languages,
        new QProfileLookup(dbClient),
        new QProfileFactory(dbClient),
        dbClient,
        new ComponentFinder(dbClient), activeRuleIndex, qProfileWsSupport),
      languages);

    SearchWsRequest request = new SearchWsRequest().setDefaults(true).setProfileName("Sonar Xoo1 way").setOrganizationKey(org1.getKey());
    QualityProfiles.SearchWsResponse response = underTest.doHandle(request);

    assertThat(response.getProfilesList())
      .extracting(QualityProfiles.SearchWsResponse.QualityProfile::getKey)
      .containsExactlyInAnyOrder(
  
        // name match for xoo1
        "ORG1-SONAR-XOO1",
  
        // default for xoo2
        "ORG1-MYWAY-XOO2"
      );
    assertThat(response.getProfilesList())
      .extracting(QualityProfiles.SearchWsResponse.QualityProfile::getOrganization)
      .containsOnly(org1.getKey());
  }
}
