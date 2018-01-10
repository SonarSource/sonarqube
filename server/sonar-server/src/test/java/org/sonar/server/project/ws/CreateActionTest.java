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
package org.sonar.server.project.ws;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidations.BillingValidationsException;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.ws.CreateAction.CreateRequest;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.server.project.Visibility.PRIVATE;
import static org.sonar.server.project.ws.ProjectsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class CreateActionTest {

  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String DEFAULT_PROJECT_NAME = "project-name";

  private System2 system2 = System2.INSTANCE;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public I18nRule i18n = new I18nRule().put("qualifier.TRK", "Project");

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private BillingValidationsProxy billingValidations = mock(BillingValidationsProxy.class);
  private TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private WsActionTester ws = new WsActionTester(
    new CreateAction(
      new ProjectsWsSupport(db.getDbClient(), defaultOrganizationProvider, billingValidations),
      db.getDbClient(), userSession,
      new ComponentUpdater(db.getDbClient(), i18n, system2, mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()),
        projectIndexers)));

  @Test
  public void create_project() {
    userSession.addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    CreateWsResponse response = call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());

    assertThat(response.getProject())
      .extracting(Project::getKey, Project::getName, Project::getQualifier, Project::getVisibility)
      .containsOnly(DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_NAME, "TRK", "public");
    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get())
      .extracting(ComponentDto::getDbKey, ComponentDto::name, ComponentDto::qualifier, ComponentDto::scope, ComponentDto::isPrivate, ComponentDto::getMainBranchProjectUuid)
      .containsOnly(DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_NAME, "TRK", "PRJ", false, null);
  }

  @Test
  public void create_project_with_branch() {
    userSession.addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    CreateWsResponse response = call(CreateRequest.builder()
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setBranch("origin/master")
      .build());

    assertThat(response.getProject())
      .extracting(Project::getKey, Project::getName, Project::getQualifier, Project::getVisibility)
      .containsOnly(DEFAULT_PROJECT_KEY + ":origin/master", DEFAULT_PROJECT_NAME, "TRK", "public");
  }

  @Test
  public void create_project_with_deprecated_parameter() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(PROVISION_PROJECTS, organization);

    CreateWsResponse response = ws.newRequest()
      .setMethod(POST.name())
      .setParam("organization", organization.getKey())
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam(PARAM_NAME, DEFAULT_PROJECT_NAME)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(response.getProject())
      .extracting(Project::getKey, Project::getName, Project::getQualifier, Project::getVisibility)
      .containsOnly(DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_NAME, "TRK", "public");
  }

  @Test
  public void apply_project_visibility_public() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(PROVISION_PROJECTS, organization);

    CreateWsResponse result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("organization", organization.getKey())
      .setParam("visibility", "public")
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("public");
  }

  @Test
  public void apply_project_visibility_private() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(PROVISION_PROJECTS, organization);

    CreateWsResponse result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("organization", organization.getKey())
      .setParam("visibility", PRIVATE.getLabel())
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("private");
  }

  @Test
  public void apply_default_project_visibility_public() {
    OrganizationDto organization = db.organizations().insert();
    db.organizations().setNewProjectPrivate(organization, false);
    userSession.addPermission(PROVISION_PROJECTS, organization);

    CreateWsResponse result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("organization", organization.getKey())
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("public");
  }

  @Test
  public void apply_default_project_visibility_private() {
    OrganizationDto organization = db.organizations().insert();
    db.organizations().setNewProjectPrivate(organization, true);
    userSession.addPermission(PROVISION_PROJECTS, organization);

    CreateWsResponse result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("organization", organization.getKey())
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("private");
  }

  @Test
  public void does_not_fail_to_create_public_projects_when_organization_is_not_allowed_to_use_private_projects() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(PROVISION_PROJECTS, organization);
    doThrow(new BillingValidationsException("This organization cannot use project private")).when(billingValidations)
      .checkCanUpdateProjectVisibility(any(BillingValidations.Organization.class), eq(true));

    CreateWsResponse result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("organization", organization.getKey())
      .setParam("visibility", "public")
      .executeProtobuf(CreateWsResponse.class);

    AssertionsForClassTypes.assertThat(result.getProject().getVisibility()).isEqualTo("public");
  }

  @Test
  public void fail_to_create_private_projects_when_organization_is_not_allowed_to_use_private_projects() {
    OrganizationDto organization = db.organizations().insert();
    userSession.addPermission(PROVISION_PROJECTS, organization);
    doThrow(new BillingValidationsException("This organization cannot use project private")).when(billingValidations)
      .checkCanUpdateProjectVisibility(any(BillingValidations.Organization.class), eq(true));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("This organization cannot use project private");

    ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("organization", organization.getKey())
      .setParam("visibility", "private")
      .executeProtobuf(CreateWsResponse.class);
  }

  @Test
  public void fail_when_project_already_exists() {
    OrganizationDto organization = db.organizations().insert();
    db.components().insertPublicProject(project -> project.setDbKey(DEFAULT_PROJECT_KEY));
    userSession.addPermission(PROVISION_PROJECTS, organization);

    expectedException.expect(BadRequestException.class);

    call(CreateRequest.builder()
      .setOrganization(organization.getKey())
      .setKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build());
  }

  @Test
  public void properly_fail_when_project_key_contains_percent_character() {
    userSession.addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Malformed key for Project: project%Key. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least one non-digit.");

    call(CreateRequest.builder()
      .setKey("project%Key")
      .setName(DEFAULT_PROJECT_NAME)
      .build());
  }

  @Test
  public void fail_when_missing_project_parameter() {
    userSession.addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'project' parameter is missing");

    call(CreateRequest.builder().setName(DEFAULT_PROJECT_NAME).build());
  }

  @Test
  public void fail_when_missing_name_parameter() {
    userSession.addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    call(CreateRequest.builder().setKey(DEFAULT_PROJECT_KEY).build());
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    expectedException.expect(ForbiddenException.class);

    call(CreateRequest.builder().setKey(DEFAULT_PROJECT_KEY).setName(DEFAULT_PROJECT_NAME).build());
  }

  @Test
  public void test_example() {
    userSession.addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    String result = ws.newRequest()
      .setParam("key", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("create");
    assertThat(definition.since()).isEqualTo("4.0");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();

    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder(
      PARAM_VISIBILITY,
      PARAM_ORGANIZATION,
      PARAM_NAME,
      PARAM_PROJECT,
      PARAM_BRANCH);

    WebService.Param organization = definition.param(PARAM_ORGANIZATION);
    assertThat(organization.description()).isEqualTo("The key of the organization");
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.since()).isEqualTo("6.3");

    WebService.Param isPrivate = definition.param(PARAM_VISIBILITY);
    assertThat(isPrivate.description()).isNotEmpty();
    assertThat(isPrivate.isInternal()).isTrue();
    assertThat(isPrivate.isRequired()).isFalse();
    assertThat(isPrivate.since()).isEqualTo("6.4");
    assertThat(isPrivate.possibleValues()).containsExactlyInAnyOrder("private", "public");

    WebService.Param project = definition.param(PARAM_PROJECT);
    assertThat(project.isRequired()).isTrue();
    assertThat(project.maximumLength()).isEqualTo(400);

    WebService.Param name = definition.param(PARAM_NAME);
    assertThat(name.isRequired()).isTrue();
    assertThat(name.maximumLength()).isEqualTo(2000);
  }

  private CreateWsResponse call(CreateRequest request) {
    TestRequest httpRequest = ws.newRequest()
      .setMethod(POST.name());
    setNullable(request.getOrganization(), e -> httpRequest.setParam("organization", e));
    setNullable(request.getKey(), e -> httpRequest.setParam("project", e));
    setNullable(request.getName(), e -> httpRequest.setParam("name", e));
    setNullable(request.getBranch(), e -> httpRequest.setParam("branch", e));
    return httpRequest.executeProtobuf(CreateWsResponse.class);
  }

}
