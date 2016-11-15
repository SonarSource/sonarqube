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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.es.ProjectMeasuresIndexDefinition;
import org.sonar.server.component.es.ProjectMeasuresIndexer;
import org.sonar.server.es.EsTester;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.QProfileName;
import org.sonar.server.qualityprofile.QProfileProjectOperations;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.server.qualityprofile.QProfileTesting.newQProfileDto;

public class AddProjectActionTest {

  static final String LANGUAGE_1 = "xoo";
  static final String LANGUAGE_2 = "foo";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new ProjectMeasuresIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  DbClient dbClient = dbTester.getDbClient();
  DbSession session = dbTester.getSession();

  ComponentDbTester componentDb = new ComponentDbTester(dbTester);
  QualityProfileDbTester qualityProfileDbTester = new QualityProfileDbTester(dbTester);
  QProfileProjectOperations qProfileProjectOperations = new QProfileProjectOperations(dbClient);
  Languages languages = LanguageTesting.newLanguages(LANGUAGE_1, LANGUAGE_2);
  ProjectAssociationParameters projectAssociationParameters = new ProjectAssociationParameters(languages);

  ComponentDto project;

  WsActionTester ws = new WsActionTester(new AddProjectAction(projectAssociationParameters,
    qProfileProjectOperations, new ProjectAssociationFinder(new QProfileLookup(dbClient),
      new ComponentService(dbClient, null, userSession, null, new ComponentFinder(dbClient), new ProjectMeasuresIndexer(system2, dbClient, es.client()))),
    userSession));

  @Before
  public void setUp() throws Exception {
    project = componentDb.insertComponent(ComponentTesting.newProjectDto());
  }

  @Test
  public void add_project() throws Exception {
    setUserAsQualityProfileAdmin();
    QualityProfileDto profile = qualityProfileDbTester.insertQualityProfile(newQProfileDto(QProfileName.createFor(LANGUAGE_1, "profile1"), "Profile"));
    session.commit();

    executeRequest(project, profile);

    assertProjectIsAssociatedToProfile(project.key(), LANGUAGE_1, profile.getKey());
  }

  @Test
  public void change_project_association() throws Exception {
    setUserAsQualityProfileAdmin();
    QualityProfileDto profile1 = newQProfileDto(QProfileName.createFor(LANGUAGE_1, "profile1"), "Profile 1");
    QualityProfileDto profile2 = newQProfileDto(QProfileName.createFor(LANGUAGE_1, "profile2"), "Profile 2");
    qualityProfileDbTester.insertQualityProfiles(profile1, profile2);
    qualityProfileDbTester.associateProjectWithQualityProfile(project, profile1);
    session.commit();

    executeRequest(project, profile2);

    assertProjectIsAssociatedToProfile(project.key(), LANGUAGE_1, profile2.getKey());
  }

  @Test
  public void change_project_association_when_project_is_linked_on_many_profiles() throws Exception {
    setUserAsQualityProfileAdmin();
    QualityProfileDto profile1Language1 = newQProfileDto(QProfileName.createFor(LANGUAGE_1, "profile1"), "Profile 1");
    QualityProfileDto profile2Language2 = newQProfileDto(QProfileName.createFor(LANGUAGE_2, "profile2"), "Profile 2");
    QualityProfileDto profile3Language1 = newQProfileDto(QProfileName.createFor(LANGUAGE_1, "profile3"), "Profile 3");
    qualityProfileDbTester.insertQualityProfiles(profile1Language1, profile2Language2, profile3Language1);
    qualityProfileDbTester.associateProjectWithQualityProfile(project, profile1Language1, profile2Language2);
    session.commit();

    executeRequest(project, profile3Language1);

    assertProjectIsAssociatedToProfile(project.key(), LANGUAGE_1, profile3Language1.getKey());
    assertProjectIsAssociatedToProfile(project.key(), LANGUAGE_2, profile2Language2.getKey());
  }

  private void assertProjectIsAssociatedToProfile(String projectKey, String language, String expectedProfileKey) {
    assertThat(dbClient.qualityProfileDao().selectByProjectAndLanguage(session, projectKey, language).getKey()).isEqualTo(expectedProfileKey);
  }

  private void setUserAsQualityProfileAdmin() {
    userSession.login("admin").setGlobalPermissions(QUALITY_PROFILE_ADMIN);
  }

  private void executeRequest(ComponentDto project, QualityProfileDto qualityProfile) {
    TestRequest request = ws.newRequest()
      .setParam("projectUuid", project.uuid())
      .setParam("profileKey", qualityProfile.getKey());
    request.execute();
  }

}
