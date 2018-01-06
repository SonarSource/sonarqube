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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category3Suite;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Favorites;
import org.sonarqube.ws.Favorites.Favorite;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.favorites.SearchRequest;
import org.sonarqube.ws.client.permissions.AddProjectCreatorToTemplateRequest;
import org.sonarqube.ws.client.permissions.RemoveProjectCreatorFromTemplateRequest;
import org.sonarqube.ws.client.permissions.SearchTemplatesRequest;

import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static com.sonar.orchestrator.container.Server.ADMIN_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class FavoriteTest {
  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;

  private static final String PROJECT_KEY = "sample";

  @Before
  public void setUp() {
    orchestrator.resetData();
  }

  @After
  public void tearDown() {
    removeProjectCreatorPermission();
  }

  @BeforeClass
  public static void classSetUp() {
    adminWsClient = newAdminWsClient(orchestrator);
  }

  @Test
  public void project_as_favorite_when_authenticated_and_first_analysis_and_a_project_creator_permission() {
    SonarScanner sampleProject = createScannerWithUserCredentials();
    addProjectCreatorPermission();

    orchestrator.executeBuild(sampleProject);

    Favorites.SearchResponse response = adminWsClient.favorites().search(new SearchRequest());
    assertThat(response.getFavoritesList()).extracting(Favorite::getKey).contains(PROJECT_KEY);
  }

  @Test
  public void no_project_as_favorite_when_no_project_creator_permission() {
    SonarScanner sampleProject = createScannerWithUserCredentials();

    orchestrator.executeBuild(sampleProject);

    Favorites.SearchResponse response = adminWsClient.favorites().search(new SearchRequest());
    assertThat(response.getFavoritesList()).extracting(Favorite::getKey).doesNotContain(PROJECT_KEY);
  }

  @Test
  public void no_project_as_favorite_when_second_analysis() {
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(sampleProject);
    sampleProject = createScannerWithUserCredentials();
    addProjectCreatorPermission();

    orchestrator.executeBuild(sampleProject);

    Favorites.SearchResponse response = adminWsClient.favorites().search(new SearchRequest());
    assertThat(response.getFavoritesList()).extracting(Favorite::getKey).doesNotContain(PROJECT_KEY);
  }

  private static SonarScanner createScannerWithUserCredentials() {
    return SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperty("sonar.login", ADMIN_LOGIN)
      .setProperty("sonar.password", ADMIN_PASSWORD);
  }

  private void addProjectCreatorPermission() {
    Permissions.SearchTemplatesWsResponse permissionTemplates = adminWsClient.permissions().searchTemplates(new SearchTemplatesRequest());
    assertThat(permissionTemplates.getDefaultTemplatesCount()).isEqualTo(1);
    adminWsClient.permissions().addProjectCreatorToTemplate(new AddProjectCreatorToTemplateRequest()
      .setTemplateId(permissionTemplates.getDefaultTemplates(0).getTemplateId())
      .setPermission("admin"));
  }

  private void removeProjectCreatorPermission() {
    Permissions.SearchTemplatesWsResponse permissionTemplates = adminWsClient.permissions().searchTemplates(new SearchTemplatesRequest());
    assertThat(permissionTemplates.getDefaultTemplatesCount()).isEqualTo(1);
    adminWsClient.permissions().removeProjectCreatorFromTemplate(new RemoveProjectCreatorFromTemplateRequest()
      .setTemplateId(permissionTemplates.getDefaultTemplates(0).getTemplateId())
      .setPermission("admin"));
  }
}
