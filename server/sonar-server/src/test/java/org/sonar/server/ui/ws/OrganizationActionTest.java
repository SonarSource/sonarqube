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
package org.sonar.server.ui.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.organization.BillingValidations;
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ui.PageRepository;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.web.page.Page.Scope.ORGANIZATION;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.test.JsonAssert.assertJson;

public class OrganizationActionTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private PageRepository pageRepository = mock(PageRepository.class);
  private BillingValidationsProxy billingValidations = mock(BillingValidationsProxy.class);

  private WsActionTester ws = new WsActionTester(new OrganizationAction(dbClient, defaultOrganizationProvider, userSession, pageRepository, billingValidations));

  @Test
  public void verify_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.isInternal()).isTrue();
    assertThat(def.description()).isEqualTo("Get information concerning organization navigation for the current user");
    assertThat(def.since()).isEqualTo("6.3");
    assertThat(def.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("6.4", "The field 'projectVisibility' is added"));

    assertThat(def.params()).hasSize(1);
    WebService.Param organization = def.param("organization");
    assertThat(organization.description()).isEqualTo("the organization key");
    assertThat(organization.isRequired()).isTrue();
    assertThat(organization.exampleValue()).isEqualTo("my-org");
  }

  @Test
  public void fails_with_IAE_if_parameter_organization_is_not_specified() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'organization' parameter is missing");

    executeRequest(null);
  }

  @Test
  public void json_example() {
    initWithPages(
      Page.builder("my-plugin/org-page").setName("Organization page").setScope(ORGANIZATION).build(),
      Page.builder("my-plugin/org-admin-page").setName("Organization admin page").setScope(ORGANIZATION).setAdmin(true).build());
    OrganizationDto organization = dbTester.organizations().insert(dto -> dto.setGuarded(true));
    userSession.logIn()
      .addPermission(ADMINISTER, organization)
      .addPermission(PROVISION_PROJECTS, organization);

    TestResponse response = executeRequest(organization);

    assertJson(response.getInput())
      .isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void filter_out_admin_pages_when_user_is_not_admin() {
    initWithPages(
      Page.builder("my-plugin/org-page").setName("Organization page").setScope(ORGANIZATION).build(),
      Page.builder("my-plugin/org-admin-page").setName("Organization admin page").setScope(ORGANIZATION).setAdmin(true).build());
    OrganizationDto organization = dbTester.organizations().insert(dto -> dto.setGuarded(true));
    userSession.logIn()
      .addPermission(PROVISION_PROJECTS, organization);

    TestResponse response = executeRequest(organization);

    assertThat(response.getInput())
      .contains("my-plugin/org-page")
      .doesNotContain("my-plugin/org-admin-page");
  }

  @Test
  public void returns_non_admin_and_canDelete_false_when_user_not_logged_in_and_key_is_the_default_organization() {
    TestResponse response = executeRequest(dbTester.getDefaultOrganization());

    verifyResponse(response, false, false, false);
  }

  @Test
  public void returns_non_admin_and_canDelete_false_when_user_logged_in_but_not_admin_and_key_is_the_default_organization() {
    userSession.logIn();

    TestResponse response = executeRequest(dbTester.getDefaultOrganization());

    verifyResponse(response, false, false, false);
  }

  @Test
  public void returns_admin_and_canDelete_true_when_user_logged_in_and_admin_and_key_is_the_default_organization() {
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();
    userSession.logIn().addPermission(ADMINISTER, defaultOrganization);

    TestResponse response = executeRequest(defaultOrganization);

    verifyResponse(response, true, false, true);
  }

  @Test
  public void returns_non_admin_and_canDelete_false_when_user_not_logged_in_and_key_is_not_the_default_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    TestResponse response = executeRequest(organization);

    verifyResponse(response, false, false, false);
  }

  @Test
  public void returns_non_admin_and_canDelete_false_when_user_logged_in_but_not_admin_and_key_is_not_the_default_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.logIn();

    TestResponse response = executeRequest(organization);

    verifyResponse(response, false, false, false);
  }

  @Test
  public void returns_admin_and_canDelete_true_when_user_logged_in_and_admin_and_key_is_not_the_default_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.logIn().addPermission(ADMINISTER, organization);

    TestResponse response = executeRequest(organization);

    verifyResponse(response, true, false, true);
  }

  @Test
  public void returns_admin_and_canDelete_false_when_user_logged_in_and_admin_and_key_is_guarded_organization() {
    OrganizationDto organization = dbTester.organizations().insert(dto -> dto.setGuarded(true));
    userSession.logIn().addPermission(ADMINISTER, organization);

    TestResponse response = executeRequest(organization);

    verifyResponse(response, true, false, false);
  }

  @Test
  public void returns_only_canDelete_true_when_user_is_system_administrator_and_key_is_guarded_organization() {
    OrganizationDto organization = dbTester.organizations().insert(dto -> dto.setGuarded(true));
    userSession.logIn().setSystemAdministrator();

    TestResponse response = executeRequest(organization);

    verifyResponse(response, false, false, true);
  }

  @Test
  public void returns_provisioning_true_when_user_can_provision_projects_in_organization() {
    // user can provision projects in org2 but not in org1
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    userSession.logIn().addPermission(PROVISION_PROJECTS, org2);

    verifyResponse(executeRequest(org1), false, false, false);
    verifyResponse(executeRequest(org2), false, true, false);
  }

  @Test
  public void returns_project_visibility_private() {
    OrganizationDto organization = dbTester.organizations().insert();
    dbTester.organizations().setNewProjectPrivate(organization, true);
    userSession.logIn().addPermission(PROVISION_PROJECTS, organization);
    assertJson(executeRequest(organization).getInput()).isSimilarTo("{\"organization\": {\"projectVisibility\": \"private\"}}");
  }

  @Test
  public void returns_project_visibility_public() {
    OrganizationDto organization = dbTester.organizations().insert();
    dbTester.organizations().setNewProjectPrivate(organization, false);
    userSession.logIn().addPermission(PROVISION_PROJECTS, organization);
    assertJson(executeRequest(organization).getInput()).isSimilarTo("{\"organization\": {\"projectVisibility\": \"public\"}}");
  }

  @Test
  public void returns_non_admin_and_canUpdateProjectsVisibilityToPrivate_false_when_user_logged_in_but_not_admin_and_extension_returns_true() {
    OrganizationDto defaultOrganization = dbTester.getDefaultOrganization();

    userSession.logIn();
    when(billingValidations.canUpdateProjectVisibilityToPrivate(any(BillingValidations.Organization.class))).thenReturn(true);
    verifyCanUpdateProjectsVisibilityToPrivateResponse(executeRequest(dbTester.getDefaultOrganization()), false);

    userSession.logIn().addPermission(ADMINISTER, defaultOrganization);
    when(billingValidations.canUpdateProjectVisibilityToPrivate(any(BillingValidations.Organization.class))).thenReturn(false);
    verifyCanUpdateProjectsVisibilityToPrivateResponse(executeRequest(dbTester.getDefaultOrganization()), false);

    userSession.logIn().addPermission(ADMINISTER, defaultOrganization);
    when(billingValidations.canUpdateProjectVisibilityToPrivate(any(BillingValidations.Organization.class))).thenReturn(true);
    verifyCanUpdateProjectsVisibilityToPrivateResponse(executeRequest(dbTester.getDefaultOrganization()), true);
  }

  private void initWithPages(Page... pages) {
    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.hasPlugin(anyString())).thenReturn(true);
    PageRepository pageRepository = new PageRepository(pluginRepository, new PageDefinition[] {context -> {
      for (Page page : pages) {
        context.addPage(page);
      }
    }});
    pageRepository.start();
    ws = new WsActionTester(new OrganizationAction(dbClient, defaultOrganizationProvider, userSession, pageRepository, billingValidations));
  }

  private TestResponse executeRequest(@Nullable OrganizationDto organization) {
    TestRequest request = ws.newRequest();
    if (organization != null) {
      request.setParam("organization", organization.getKey());
    }
    return request.execute();
  }

  private static void verifyResponse(TestResponse response, boolean canAdmin, boolean canProvisionProjects, boolean canDelete) {
    assertJson(response.getInput())
      .isSimilarTo("{" +
        "  \"organization\": {" +
        "    \"canAdmin\": " + canAdmin + "," +
        "    \"canProvisionProjects\": " + canProvisionProjects + "," +
        "    \"canDelete\": " + canDelete +
        "    \"pages\": []" +
        "  }" +
        "}");
  }

  private static void verifyCanUpdateProjectsVisibilityToPrivateResponse(TestResponse response, boolean canUpdateProjectsVisibilityToPrivate) {
    assertJson(response.getInput())
      .isSimilarTo("{" +
        "  \"organization\": {" +
        "    \"canUpdateProjectsVisibilityToPrivate\": " + canUpdateProjectsVisibilityToPrivate + "," +
        "  }" +
        "}");
  }
}
