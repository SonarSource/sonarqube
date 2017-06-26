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
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.qualityprofile.QProfileLookup;

import static org.mockito.Mockito.mock;

public class SearchDataLoaderTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Languages languages;
  private QProfileLookup profileLookup;
  private ComponentFinder componentFinder;
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = dbTester.organizations().insert();
    languages = new Languages();
    DbClient dbClient = dbTester.getDbClient();
    profileLookup = new QProfileLookup(dbClient);
    componentFinder = mock(ComponentFinder.class);
  }

//  @Test
//  public void find_no_profiles_if_database_is_empty() throws Exception {
//    assertThat(findProfiles(
//      new SearchWsRequest(),
//      null)).isEmpty();
//  }
//
//  @Test
//  public void findAll() throws Exception {
//    insertQualityProfile(organization);
//    assertThat(findProfiles(
//      new SearchWsRequest()
//        .setOrganizationKey(organization.getKey()),
//      null)).hasSize(1);
//  }
//
//  @Test
//  public void findDefaults() throws Exception {
//    QProfileDto profile = insertQualityProfile(organization);
//    dbTester.qualityProfiles().setAsDefault(profile);
//    assertThat(findProfiles(
//      new SearchWsRequest()
//        .setOrganizationKey(organization.getKey())
//        .setDefaults(true),
//      null)).hasSize(1);
//  }
//
//  @Test
//  public void findForProject() throws Exception {
//    QProfileDto profile = insertQualityProfile(organization);
//    dbTester.qualityProfiles().setAsDefault(profile);
//    ComponentDto project1 = insertProject();
//    assertThat(findProfiles(
//      new SearchWsRequest()
//        .setOrganizationKey(organization.getKey()),
//      project1)).hasSize(1);
//  }
//
//  @Test
//  public void findAllForLanguage() throws Exception {
//    QProfileDto profile = insertQualityProfile(organization);
//    dbTester.qualityProfiles().setAsDefault(profile);
//    assertThat(findProfiles(
//      new SearchWsRequest()
//        .setOrganizationKey(organization.getKey())
//        .setLanguage(profile.getLanguage()),
//      null)).hasSize(1);
//    assertThat(findProfiles(
//      new SearchWsRequest()
//        .setOrganizationKey(organization.getKey())
//        .setLanguage("other language"),
//      null)).hasSize(0);
//  }
//
//  private List<QProfileDto> findProfiles(SearchWsRequest request, @Nullable ComponentDto project) {
//    return new SearchDataLoader(languages, profileLookup, dbTester.getDbClient())
//      .findProfiles(dbTester.getSession(), request, organization, project);
//  }
//
//  private QProfileDto insertQualityProfile(OrganizationDto organization) {
//    Language language = insertLanguage();
//    return dbTester.qualityProfiles().insert(organization, p -> p.setLanguage(language.getKey()));
//  }
//
//  private Language insertLanguage() {
//    Language language = LanguageTesting.newLanguage(randomAlphanumeric(20));
//    languages.add(language);
//    return language;
//  }
//
//  private ComponentDto insertProject() {
//    ComponentDto project = dbTester.components().insertPrivateProject(organization);
//    doReturn(project).when(componentFinder).getByKey(any(DbSession.class), eq(project.getKey()));
//    return project;
//  }
}
