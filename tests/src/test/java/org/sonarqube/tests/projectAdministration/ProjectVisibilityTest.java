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
package org.sonarqube.tests.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.client.component.SearchProjectsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.project.UpdateVisibilityRequest;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ProjectsManagementPage;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class ProjectVisibilityTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private Navigation nav = Navigation.create(orchestrator);

  private String adminUser;

  @Before
  public void initData() throws SQLException {
    orchestrator.resetData();
    adminUser = userRule.createAdminUser();
  }

  @Test
  public void return_all_projects_even_when_no_permission() throws Exception {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProperties("sonar.projectKey", "sample1"));
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProperties("sonar.projectKey", "sample2"));
    newAdminWsClient(orchestrator).projects().updateVisibility(UpdateVisibilityRequest.builder().setProject("sample2").setVisibility("private").build());
    // Remove 'Admin' permission for admin group on project 2 -> No one can access or admin this project, expect System Admin
    newAdminWsClient(orchestrator).permissions().removeGroup(new RemoveGroupWsRequest().setProjectKey("sample2").setGroupName("sonar-administrators").setPermission("admin"));

    nav.logIn().submitCredentials(adminUser).openProjectsManagement()
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
    ProjectsManagementPage page = nav.logIn().submitCredentials(adminUser, adminUser).openProjectsManagement();
    page
      .shouldHaveProjectsCount(0)
      .createProject("foo", "foo", visibility)
      .shouldHaveProjectsCount(1);

    WsComponents.SearchProjectsWsResponse response = newAdminWsClient(orchestrator).components().searchProjects(
      SearchProjectsRequest.builder().build());
    assertThat(response.getComponentsCount()).isEqualTo(1);
    assertThat(response.getComponents(0).getKey()).isEqualTo("foo");
    assertThat(response.getComponents(0).getName()).isEqualTo("foo");
    assertThat(response.getComponents(0).getVisibility()).isEqualTo(visibility);
  }

}
