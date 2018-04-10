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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectPermissionsPage;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.components.ShowRequest;
import org.sonarqube.ws.client.organizations.UpdateProjectVisibilityRequest;
import org.sonarqube.ws.client.projects.UpdateVisibilityRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectPermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @Test
  public void change_project_visibility_from_ui() {
    Project project = tester.projects().provision();
    Users.CreateWsResponse.User administrator = tester.users().generateAdministrator();

    ProjectPermissionsPage page = tester.openBrowser().logIn()
      .submitCredentials(administrator.getLogin())
      .openProjectPermissions(project.getKey());

    page
      .shouldBePublic()
      .turnToPrivate()
      .shouldBePrivate()
      .turnToPublic()
      .shouldBePublic();
  }

  @Test
  public void change_project_visibility_from_ws() {
    Project project = tester.projects().provision();
    assertThat(tester.wsClient().components().show(new ShowRequest().setComponent(project.getKey())).getComponent().getVisibility()).isEqualTo("public");

    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject(project.getKey()).setVisibility("private"));

    assertThat(tester.wsClient().components().show(new ShowRequest().setComponent(project.getKey())).getComponent().getVisibility()).isEqualTo("private");
  }

  /**
   * SONAR-10569
   */
  @Test
  public void change_default_project_visibility_from_ws() {
    try {
      tester.wsClient().organizations().updateProjectVisibility(new UpdateProjectVisibilityRequest()
        .setOrganization(tester.organizations().getDefaultOrganization().getKey())
        .setProjectVisibility("private"));

      Project project = tester.projects().provision();

      assertThat(tester.wsClient().components().show(new ShowRequest().setComponent(project.getKey())).getComponent().getVisibility()).isEqualTo("private");

    } finally {
      // Restore default visibility to public to not break other tests
      tester.wsClient().organizations().updateProjectVisibility(new UpdateProjectVisibilityRequest()
        .setOrganization(tester.organizations().getDefaultOrganization().getKey())
        .setProjectVisibility("public"));
    }
  }
}
