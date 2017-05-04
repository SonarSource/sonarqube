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
package it.organization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category6Suite;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.organization.UpdateProjectVisibilityWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.project.UpdateVisibilityRequest;
import pageobjects.Navigation;
import util.ItUtils;
import util.user.UserRule;

import static it.Category6Suite.enableOrganizationsSupport;
import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonarqube.ws.WsCe.TaskResponse;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newOrganizationKey;
import static util.ItUtils.newProjectKey;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.projectDir;
import static util.ItUtils.resetSettings;
import static util.ItUtils.setServerProperty;

public class BillingTest {

  private static final String USER_LOGIN = "USER_LOGIN";

  @ClassRule
  public static Orchestrator orchestrator = Category6Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  private static WsClient adminClient;

  @BeforeClass
  public static void prepare() throws Exception {
    adminClient = newAdminWsClient(orchestrator);
    enableOrganizationsSupport();
  }

  @Before
  public void setUp() throws Exception {
    userRule.deactivateUsers(USER_LOGIN);
    resetSettings(orchestrator, null, "sonar.billing.preventProjectAnalysis", "sonar.billing.preventUpdatingProjectsVisibilityToPrivate");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    resetSettings(orchestrator, null, "sonar.billing.preventProjectAnalysis", "sonar.billing.preventUpdatingProjectsVisibilityToPrivate");
    userRule.deactivateUsers(USER_LOGIN);
  }

  @Test
  public void execute_successfully_ce_analysis_on_organization() {
    String organizationKey = createOrganization();
    setServerProperty(orchestrator, "sonar.billing.preventProjectAnalysis", "false");

    String taskUuid = executeAnalysis(organizationKey);

    TaskResponse taskResponse = adminClient.ce().task(taskUuid);
    assertThat(taskResponse.getTask().hasErrorMessage()).isFalse();
  }

  @Test
  public void fail_to_execute_ce_analysis_on_organization() {
    String organizationKey = createOrganization();
    setServerProperty(orchestrator, "sonar.billing.preventProjectAnalysis", "true");

    String taskUuid = executeAnalysis(organizationKey);

    TaskResponse taskResponse = adminClient.ce().task(taskUuid);
    assertThat(taskResponse.getTask().hasErrorMessage()).isTrue();
    assertThat(taskResponse.getTask().getErrorMessage()).contains(format("Organization %s cannot perform analysis", organizationKey));
  }

  @Test
  public void api_navigation_organization_returns_canUpdateProjectsVisibilityToPrivate() {
    String organizationKey = createOrganization();
    userRule.createUser(USER_LOGIN, USER_LOGIN);
    adminClient.organizations().addMember(organizationKey, USER_LOGIN);

    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/organization").setParam("organization", organizationKey), "\"canUpdateProjectsVisibilityToPrivate\":true");

    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/organization").setParam("organization", organizationKey), "\"canUpdateProjectsVisibilityToPrivate\":false");

    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");
    assertWsResponseAsUser(new GetRequest("api/navigation/organization").setParam("organization", organizationKey), "\"canUpdateProjectsVisibilityToPrivate\":false");
  }

  @Test
  public void api_navigation_component_returns_canUpdateProjectVisibilityToPrivate() {
    String organizationKey = createOrganization();
    String projectKey = createPublicProject(organizationKey);

    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey), "\"canUpdateProjectVisibilityToPrivate\":true");

    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");
    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey), "\"canUpdateProjectVisibilityToPrivate\":false");
  }

  @Test
  public void does_not_fail_to_update_default_projects_visibility_to_private() {
    String organizationKey = createOrganization();
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    adminClient.organizations().updateProjectVisibility(UpdateProjectVisibilityWsRequest.builder().setOrganization(organizationKey).setProjectVisibility("private").build());

    assertWsResponseAsAdmin(new GetRequest("api/navigation/organization").setParam("organization", organizationKey), "\"projectVisibility\":\"private\"");
  }

  @Test
  public void fail_to_update_organization_default_visibility_to_private() {
    String organizationKey = createOrganization();
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    try {
      adminClient.organizations().updateProjectVisibility(UpdateProjectVisibilityWsRequest.builder().setOrganization(organizationKey).setProjectVisibility("private").build());
      fail();
    } catch (HttpException ex) {
      assertThat(ex.code()).isEqualTo(400);
      assertThat(ex.content()).contains(format("Organization %s cannot use private project", organizationKey));
    }
  }

  @Test
  public void does_not_fail_to_update_project_visibility_to_private() {
    String organizationKey = createOrganization();
    String projectKey = createPublicProject(organizationKey);
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    adminClient.projects().updateVisibility(UpdateVisibilityRequest.builder().setProject(projectKey).setVisibility("private").build());

    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey), "\"visibility\":\"private\"");
  }

  @Test
  public void fail_to_update_project_visibility_to_private() {
    String organizationKey = createOrganization();
    String projectKey = createPublicProject(organizationKey);
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    try {
      adminClient.projects().updateVisibility(UpdateVisibilityRequest.builder().setProject(projectKey).setVisibility("private").build());
      fail();
    } catch (HttpException ex) {
      assertThat(ex.code()).isEqualTo(400);
      assertThat(ex.content()).contains(format("Organization %s cannot use private project", organizationKey));
    }
  }

  @Test
  public void does_not_fail_to_create_private_project() {
    String organizationKey = createOrganization();
    String projectKey = newProjectKey();
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    adminClient.projects().create(CreateRequest.builder().setKey(projectKey).setName(projectKey).setOrganization(organizationKey).setVisibility("public").build());

    assertWsResponseAsAdmin(new GetRequest("api/navigation/component").setParam("componentKey", projectKey), "\"visibility\":\"public\"");
  }

  @Test
  public void fail_to_create_private_project() {
    String organizationKey = createOrganization();
    String projectKey = newProjectKey();
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    try {
      adminClient.projects().create(CreateRequest.builder().setKey(projectKey).setName(projectKey).setOrganization(organizationKey).setVisibility("private").build());
      fail();
    } catch (HttpException ex) {
      assertThat(ex.code()).isEqualTo(400);
      assertThat(ex.content()).contains(format("Organization %s cannot use private project", organizationKey));
    }
  }

  @Test
  public void ui_does_not_allow_to_turn_project_to_private() {
    String projectKey = createPublicProject(createOrganization());
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "true");

    nav.logIn().asAdmin().openProjectPermissions(projectKey)
      .shouldBePublic()
      .shouldNotAllowPrivate();
  }

  @Test
  public void ui_allows_to_turn_project_to_private() {
    String projectKey = createPublicProject(createOrganization());
    setServerProperty(orchestrator, "sonar.billing.preventUpdatingProjectsVisibilityToPrivate", "false");

    nav.logIn().asAdmin().openProjectPermissions(projectKey)
      .shouldBePublic()
      .turnToPrivate();
  }

  private static String createOrganization() {
    String key = newOrganizationKey();
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(key).setName(key).build()).getOrganization();
    return key;
  }

  private static String createPublicProject(String organizationKey) {
    String projectKey = newProjectKey();
    adminClient.projects().create(CreateRequest.builder().setKey(projectKey).setName(projectKey).setOrganization(organizationKey).setVisibility("public").build());
    return projectKey;
  }

  private static String executeAnalysis(String organizationKey) {
    return executeAnalysis(newProjectKey(), organizationKey);
  }

  private static String executeAnalysis(String projectKey, String organizationKey) {
    BuildResult buildResult = orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.organization", organizationKey,
      "sonar.projectKey", projectKey,
      "sonar.login", "admin",
      "sonar.password", "admin"));
    return ItUtils.extractCeTaskId(buildResult);
  }

  private void assertWsResponseAsAdmin(GetRequest request, String expectedContent) {
    WsResponse response = adminClient.wsConnector().call(request).failIfNotSuccessful();
    assertThat(response.content()).contains(expectedContent);
  }

  private void assertWsResponseAsUser(GetRequest request, String expectedContent) {
    WsResponse response = newUserWsClient(orchestrator, USER_LOGIN, USER_LOGIN).wsConnector().call(request).failIfNotSuccessful();
    assertThat(response.content()).contains(expectedContent);
  }
}
