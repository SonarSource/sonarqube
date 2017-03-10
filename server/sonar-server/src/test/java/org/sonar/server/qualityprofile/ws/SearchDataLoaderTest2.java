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

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;


public class SearchDataLoaderTest2 {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private Languages languages;
  private QProfileLookup profileLookup;
  private QProfileFactory profileFactory;
  private ComponentFinder componentFinder;
  private ActiveRuleIndex activeRuleIndex;
  private QProfileWsSupport qProfileWsSupport;
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = dbTester.organizations().insert();
    languages = new Languages();
    DbClient dbClient = dbTester.getDbClient();
    profileLookup = new QProfileLookup(dbClient);
    TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
    qProfileWsSupport = new QProfileWsSupport(dbClient, null, defaultOrganizationProvider);
    profileFactory = new QProfileFactory(dbClient);
    componentFinder = mock(ComponentFinder.class);
  }

  @Test
  public void find_no_profiles_if_database_is_empty() throws Exception {
    assertThat(run(
      new SearchWsRequest()
    )).isEmpty();
  }

  @Test
  public void findAll_in_default_organization() throws Exception {
    insertQualityProfile(dbTester.getDefaultOrganization());
    assertThat(run(
      new SearchWsRequest()
    )).hasSize(1);
  }

  @Test
  public void findAll_in_specific_organization() throws Exception {
    insertQualityProfile(organization);
    assertThat(run(
      new SearchWsRequest()
        .setOrganizationKey(organization.getKey())
    )).hasSize(1);
  }

  @Test
  public void findDefaults_in_specific_organization() throws Exception {
    insertQualityProfile(organization, dto -> dto.setDefault(true));
    assertThat(run(
      new SearchWsRequest()
        .setOrganizationKey(organization.getKey())
        .setDefaults(true)
    )).hasSize(1);
  }

  @Test
  public void findForProject_in_specific_organization() throws Exception {
    insertQualityProfile(organization, dto -> dto.setDefault(true));
    ComponentDto project1 = insertProject();
    assertThat(run(
      new SearchWsRequest()
        .setOrganizationKey(organization.getKey())
        .setProjectKey(project1.getKey())
    )).hasSize(1);
  }

  @Test
  public void findAllForLanguage_in_specific_organization() throws Exception {
    QualityProfileDto qualityProfile = insertQualityProfile(organization, dto -> dto.setDefault(true));
    assertThat(run(
      new SearchWsRequest()
        .setOrganizationKey(organization.getKey())
        .setLanguage(qualityProfile.getLanguage())
    )).hasSize(1);
    assertThat(run(
      new SearchWsRequest()
        .setOrganizationKey(organization.getKey())
        .setLanguage("other language")
    )).hasSize(0);
  }

  private List<QProfile> run(SearchWsRequest request) {
    return createLoader().findProfiles(dbTester.getSession(), request);
  }

  private SearchDataLoader createLoader() {
    return new SearchDataLoader(languages, profileLookup, profileFactory, dbTester.getDbClient(), componentFinder, activeRuleIndex, qProfileWsSupport);
  }

  private QualityProfileDto insertQualityProfile(OrganizationDto organization, Consumer<QualityProfileDto>... specials) {
    Language language = insertLanguage();
    QualityProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage(language.getKey());
    Arrays.stream(specials)
      .forEachOrdered(c -> c.accept(qualityProfile));
    dbTester.qualityProfiles().insertQualityProfile(qualityProfile);
    return qualityProfile;
  }

  private Language insertLanguage() {
    Language language = LanguageTesting.newLanguage(randomAlphanumeric(20));
    languages.add(language);
    return language;
  }

  private ComponentDto insertProject() {
    ComponentDto project = dbTester.components().insertProject(organization);
    doReturn(project).when(componentFinder).getByKey(any(DbSession.class), eq(project.getKey()));
    return project;
  }
}
