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
package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectsManagementPage;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.client.components.SearchProjectsRequest;
import org.sonarqube.ws.client.permissions.RemoveGroupRequest;
import org.sonarqube.ws.client.projects.UpdateVisibilityRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectVisibilityPageTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private String adminUser;

  @Before
  public void setUp() {
    adminUser = tester.users().generateAdministratorOnDefaultOrganization().getLogin();
  }

  @Test
  public void return_all_projects_even_when_no_permission() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProperties("sonar.projectKey", "sample1"));
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProperties("sonar.projectKey", "sample2"));
    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject("sample2").setVisibility("private"));
    // Remove 'Admin' permission for admin group on project 2 -> No one can access or admin this project, expect System Admin
    tester.wsClient().permissions().removeGroup(new RemoveGroupRequest().setProjectKey("sample2").setGroupName("sonar-administrators").setPermission("admin"));

    tester.openBrowser().logIn().submitCredentials(adminUser)
      .openProjectsManagement("default-organization")
      .shouldHaveProject("sample1")
      .shouldHaveProject("sample2");
  }

  @Test
  public void create_public_project() {
    createProjectAndVerify("public");
  }

  @Test
  public void create_private_project() {
    createProjectAndVerify("private");
  }

  private void createProjectAndVerify(String visibility) {
    ProjectsManagementPage page = tester.openBrowser().logIn()
      .submitCredentials(adminUser, adminUser)
      .openProjectsManagement("default-organization");
    page
      .shouldHaveProjectsCount(0)
      .createProject("foo", "foo", visibility)
      .shouldHaveProjectsCount(1);

    Components.SearchProjectsWsResponse response = tester.wsClient().components().searchProjects(
      new SearchProjectsRequest());
    assertThat(response.getComponentsCount()).isEqualTo(1);
    assertThat(response.getComponents(0).getKey()).isEqualTo("foo");
    assertThat(response.getComponents(0).getName()).isEqualTo("foo");
    assertThat(response.getComponents(0).getVisibility()).isEqualTo(visibility);
  }

}
