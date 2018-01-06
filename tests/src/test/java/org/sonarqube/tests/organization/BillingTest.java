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
package org.sonarqube.tests.organization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.Navigation;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.ce.TaskRequest;
import org.sonarqube.ws.client.organizations.UpdateProjectVisibilityRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.projects.UpdateVisibilityRequest;
import util.ItUtils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.ws.Ce.TaskResponse;
import static util.ItUtils.expectHttpError;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.projectDir;

public class BillingTest {

  private static final String PROPERTY_PREVENT_ANALYSIS = "sonar.billing.preventProjectAnalysis";

  @ClassRule
  public static Orchestrator orchestrator = OrganizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private Organizations.Organization organization;
  private User orgAdministrator;

  @Before
  @After
  public void reset() {
    tester.settings().resetSettings(PROPERTY_PREVENT_ANALYSIS, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate");
  }

  @Before
  public void setUp() {
    organization = tester.organizations().generate();
    orgAdministrator = tester.users().generateAdministrator(organization);
  }

  @Test
  public void execute_successfully_ce_analysis_on_organization() {
    tester.settings().setGlobalSettings(PROPERTY_PREVENT_ANALYSIS, "false");

    String taskUuid = executeAnalysis(newProjectKey());

    TaskResponse taskResponse = tester.wsClient().ce().task(new TaskRequest().setId(taskUuid));
    assertThat(taskResponse.getTask().hasErrorMessage()).isFalse();
  }

  @Test
  public void fail_to_execute_ce_analysis_on_organization() {
    tester.settings().setGlobalSettings(PROPERTY_PREVENT_ANALYSIS, "true");

    String taskUuid = executeAnalysis(newProjectKey());

    TaskResponse taskResponse = tester.wsClient().ce().task(new TaskRequest().setId(taskUuid));
    assertThat(taskResponse.getTask().hasErrorMessage()).isTrue();
    assertThat(taskResponse.getTask().getErrorMessage()).contains(format("Organization %s cannot perform analysis", organization.getKey()));
  }

  @Test
  public void api_navigation_organization_returns_canUpdateProjectsVisibilityToPrivate() {
    User user = tester.users().generate();
    tester.organizations().addMember(organization, user);

    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/organization").setParam("organization", organization.getKey()),
      "\"canUpdateProjectsVisibilityToPrivate\":true");

    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/organization").setParam("organization", organization.getKey()),
      "\"canUpdateProjectsVisibilityToPrivate\":false");

    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");
    assertWsResponseAsUser(new GetRequest("api/navigation/organization").setParam("organization", organization.getKey()),
      "\"canUpdateProjectsVisibilityToPrivate\":false", user);
  }

  @Test
  public void api_navigation_component_returns_canUpdateProjectVisibilityToPrivate() {
    String projectKey = createPublicProject();

    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey),
      "\"canUpdateProjectVisibilityToPrivate\":true");

    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey),
      "\"canUpdateProjectVisibilityToPrivate\":false");
  }

  @Test
  public void does_not_fail_to_update_default_projects_visibility_to_private() {
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    tester.wsClient().organizations().updateProjectVisibility(new UpdateProjectVisibilityRequest()
      .setOrganization(organization.getKey())
      .setProjectVisibility("private"));

    assertWsResponseAsAdmin(new GetRequest("api/navigation/organization").setParam("organization", organization.getKey()),
      "\"projectVisibility\":\"private\"");
  }

  @Test
  public void fail_to_update_organization_default_visibility_to_private() {
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    expectHttpError(400,
      format("Organization %s cannot use private project", organization.getKey()),
      () -> tester.wsClient().organizations()
        .updateProjectVisibility(new UpdateProjectVisibilityRequest().setOrganization(organization.getKey()).setProjectVisibility("private")));
  }

  @Test
  public void does_not_fail_to_update_project_visibility_to_private() {
    String projectKey = createPublicProject();
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject(projectKey).setVisibility("private"));

    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey), "\"visibility\":\"private\"");
  }

  @Test
  public void fail_to_update_project_visibility_to_private() {
    String projectKey = createPublicProject();
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    expectHttpError(400,
      format("Organization %s cannot use private project", organization.getKey()),
      () -> tester.wsClient().projects().updateVisibility(new UpdateVisibilityRequest().setProject(projectKey).setVisibility("private")));
  }

  @Test
  public void does_not_fail_to_create_private_project() {
    String projectKey = newProjectKey();
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    tester.wsClient().projects().create(new CreateRequest().setProject(projectKey).setName(projectKey).setOrganization(organization.getKey()).setVisibility("public"));

    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey), "\"visibility\":\"public\"");
  }

  @Test
  public void fail_to_create_private_project() {
    String projectKey = newProjectKey();
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    expectHttpError(400,
      format("Organization %s cannot use private project", organization.getKey()),
      () -> tester.wsClient().projects()
        .create(new CreateRequest().setProject(projectKey).setName(projectKey).setOrganization(organization.getKey()).setVisibility("private")));
  }

  @Test
  public void ui_does_not_allow_to_turn_project_to_private() {
    String projectKey = createPublicProject();
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    Navigation.create(orchestrator)
      .logIn().submitCredentials(orgAdministrator.getLogin())
      .openProjectPermissions(projectKey)
      .shouldBePublic()
      .shouldNotAllowPrivate();
  }

  @Test
  public void ui_allows_to_turn_project_to_private() {
    String projectKey = createPublicProject();
    tester.settings().setGlobalSettings("sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    tester.openBrowser()
      .logIn().submitCredentials(orgAdministrator.getLogin())
      .openProjectPermissions(projectKey)
      .shouldBePublic()
      .turnToPrivate();
  }

  private String createPublicProject() {
    return tester.projects().provision(organization).getKey();
  }

  private String executeAnalysis(String projectKey) {
    BuildResult buildResult = orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organization.getKey(),
      "sonar.projectKey", projectKey,
      "sonar.login", orgAdministrator.getLogin(),
      "sonar.password", orgAdministrator.getLogin()));
    return ItUtils.extractCeTaskId(buildResult);
  }

  private void assertWsResponseAsAdmin(GetRequest request, String expectedContent) {
    WsResponse response = tester.wsClient().wsConnector().call(request).failIfNotSuccessful();
    assertThat(response.content()).contains(expectedContent);
  }

  private void assertWsResponseAsUser(GetRequest request, String expectedContent, User user) {
    WsResponse response = tester.as(user.getLogin()).wsClient().wsConnector().call(request).failIfNotSuccessful();
    assertThat(response.content()).contains(expectedContent);
  }
}
