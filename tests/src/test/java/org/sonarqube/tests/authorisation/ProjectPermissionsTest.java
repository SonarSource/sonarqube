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
package org.sonarqube.tests.authorisation;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.pageobjects.ProjectPermissionsPage;
import org.sonarqube.tests.Category1Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.WsUsers.CreateWsResponse.User;
import org.sonarqube.ws.client.component.ShowWsRequest;
import org.sonarqube.ws.client.organization.UpdateProjectVisibilityWsRequest;
import org.sonarqube.ws.client.project.UpdateVisibilityRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectPermissionsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void change_project_visibility_from_ui() {
    Project project = tester.projects().generate(null);
    User administrator = tester.users().generateAdministrator();

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
    Project project = tester.projects().generate(null);
    assertThat(tester.wsClient().components().show(new ShowWsRequest().setKey(project.getKey())).getComponent().getVisibility()).isEqualTo("public");

    tester.wsClient().projects().updateVisibility(UpdateVisibilityRequest.builder().setProject(project.getKey()).setVisibility("private").build());

    assertThat(tester.wsClient().components().show(new ShowWsRequest().setKey(project.getKey())).getComponent().getVisibility()).isEqualTo("private");
  }

  /**
   * SONAR-10569
   */
  @Test
  public void change_default_project_visibility_from_ws() {
    try {
      tester.wsClient().organizations().updateProjectVisibility(UpdateProjectVisibilityWsRequest.builder()
        .setOrganization(tester.organizations().getDefaultOrganization().getKey())
        .setProjectVisibility("private")
        .build());

      Project project = tester.projects().generate(null);

      assertThat(tester.wsClient().components().show(new ShowWsRequest().setKey(project.getKey())).getComponent().getVisibility()).isEqualTo("private");

    } finally {
      // Restore default visibility to public to not break other tests
      tester.wsClient().organizations().updateProjectVisibility(UpdateProjectVisibilityWsRequest.builder()
        .setOrganization(tester.organizations().getDefaultOrganization().getKey())
        .setProjectVisibility("public")
        .build());
    }
  }

}
