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
package org.sonar.server.organization.ws;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;

public class DeleteActionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession session = dbTester.getSession();
  private ComponentCleanerService componentCleanerService = mock(ComponentCleanerService.class);
  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone().setEnabled(true);
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(dbTester);
  private DeleteAction underTest = new DeleteAction(userSession, dbTester.getDbClient(), defaultOrganizationProvider, componentCleanerService, organizationFlags);
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo("delete");
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isEqualTo("Delete an organization.<br/>" +
      "Require 'Administer System' permission on the specified organization. Organization support must be enabled.");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.2");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.params()).hasSize(1);
    assertThat(action.responseExample()).isNull();

    assertThat(action.param("key"))
      .matches(param -> param.isRequired())
      .matches(param -> "foo-company".equals(param.exampleValue()))
      .matches(param -> "Organization key".equals(param.description()));
  }

  @Test
  public void request_fails_with_IllegalStateException_if_organization_support_is_disabled() {
    organizationFlags.setEnabled(false);
    userSession.logIn();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Organization support is disabled");

    wsTester.newRequest().execute();
  }

  @Test
  public void request_fails_with_UnauthorizedException_if_user_is_not_logged_in() {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    wsTester.newRequest()
      .execute();
  }

  @Test
  public void request_fails_with_IAE_if_key_param_is_missing() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter is missing");

    wsTester.newRequest().execute();
  }

  @Test
  public void request_fails_with_IAE_if_key_is_the_one_of_default_organization() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default Organization can't be deleted");

    sendRequest(dbTester.getDefaultOrganization());
  }

  @Test
  public void request_fails_with_NotFoundException_if_organization_with_specified_key_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization with key 'foo' not found");

    sendRequest("foo");
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_administrator_of_specified_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    sendRequest(organization);
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_system_administrator() {
    OrganizationDto organization = dbTester.organizations().insert();
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    sendRequest(organization);
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_administrator_of_other_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    logInAsAdministrator(dbTester.getDefaultOrganization());

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    sendRequest(organization);
  }

  @Test
  public void request_deletes_specified_organization_if_exists_and_user_is_administrator_of_it() {
    OrganizationDto organization = dbTester.organizations().insert();
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
  }

  @Test
  public void request_deletes_specified_organization_if_exists_and_user_is_organization_administrator() {
    OrganizationDto organization = dbTester.organizations().insert();
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
  }

  @Test
  public void request_deletes_specified_guarded_organization_if_exists_and_user_is_system_administrator() {
    OrganizationDto organization = dbTester.organizations().insert(dto -> dto.setGuarded(true));
    logInAsSystemAdministrator();

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
  }

  @Test
  public void request_also_deletes_components_of_specified_organization() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertProject(organization);
    ComponentDto module = dbTester.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto directory = dbTester.components().insertComponent(ComponentTesting.newDirectory(module, "a/b"));
    ComponentDto file = dbTester.components().insertComponent(ComponentTesting.newFileDto(module, directory));
    ComponentDto view = dbTester.components().insertView(organization, (dto) -> {
    });
    ComponentDto subview1 = dbTester.components().insertComponent(ComponentTesting.newSubView(view, "v1", "ksv1"));
    ComponentDto subview2 = dbTester.components().insertComponent(ComponentTesting.newSubView(subview1, "v2", "ksv2"));
    ComponentDto projectCopy = dbTester.components().insertComponent(ComponentTesting.newProjectCopy("pc1", project, subview1));
    logInAsAdministrator(organization);

    sendRequest(organization);

    verifyOrganizationDoesNotExist(organization);
    ArgumentCaptor<List<ComponentDto>> arg = (ArgumentCaptor<List<ComponentDto>>) ((ArgumentCaptor) ArgumentCaptor.forClass(List.class));
    verify(componentCleanerService).delete(any(DbSession.class), arg.capture());
    assertThat(arg.getValue()).containsOnly(project, view);
  }

  @Test
  public void request_also_deletes_permissions_templates_and_permissions_and_groups_of_specified_organization() {
    OrganizationDto org = dbTester.organizations().insert();
    OrganizationDto otherOrg = dbTester.organizations().insert();

    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    GroupDto group1 = dbTester.users().insertGroup(org);
    GroupDto group2 = dbTester.users().insertGroup(org);
    GroupDto otherGroup1 = dbTester.users().insertGroup(otherOrg);
    GroupDto otherGroup2 = dbTester.users().insertGroup(otherOrg);

    ComponentDto projectDto = dbTester.components().insertProject(org);
    ComponentDto otherProjectDto = dbTester.components().insertProject(otherOrg);

    dbTester.users().insertPermissionOnAnyone(org, "u1");
    dbTester.users().insertPermissionOnAnyone(otherOrg, "not deleted u1");
    dbTester.users().insertPermissionOnUser(org, user1, "u2");
    dbTester.users().insertPermissionOnUser(otherOrg, user1, "not deleted u2");
    dbTester.users().insertPermissionOnGroup(group1, "u3");
    dbTester.users().insertPermissionOnGroup(otherGroup1, "not deleted u3");
    dbTester.users().insertProjectPermissionOnAnyone("u4", projectDto);
    dbTester.users().insertProjectPermissionOnAnyone("not deleted u4", otherProjectDto);
    dbTester.users().insertProjectPermissionOnGroup(group1, "u5", projectDto);
    dbTester.users().insertProjectPermissionOnGroup(otherGroup1, "not deleted u5", otherProjectDto);
    dbTester.users().insertProjectPermissionOnUser(user1, "u6", projectDto);
    dbTester.users().insertProjectPermissionOnUser(user1, "not deleted u6", otherProjectDto);

    PermissionTemplateDto templateDto = dbTester.permissionTemplates().insertTemplate(org);
    PermissionTemplateDto otherTemplateDto = dbTester.permissionTemplates().insertTemplate(otherOrg);
    logInAsAdministrator(org);

    sendRequest(org);

    verifyOrganizationDoesNotExist(org);
    assertThat(dbClient.groupDao().selectByIds(session, of(group1.getId(), otherGroup1.getId(), group2.getId(), otherGroup2.getId())))
      .extracting(GroupDto::getId)
      .containsOnly(otherGroup1.getId(), otherGroup2.getId());
    assertThat(dbClient.permissionTemplateDao().selectByUuid(session, templateDto.getUuid()))
      .isNull();
    assertThat(dbClient.permissionTemplateDao().selectByUuid(session, otherTemplateDto.getUuid()))
      .isNotNull();
    assertThat(dbTester.select("select role as \"role\" from USER_ROLES"))
      .extracting(row -> (String) row.get("role"))
      .doesNotContain("u2", "u6")
      .contains("not deleted u2", "not deleted u6");
    assertThat(dbTester.select("select role as \"role\" from GROUP_ROLES"))
      .extracting(row -> (String) row.get("role"))
      .doesNotContain("u1", "u3", "u4", "u5")
      .contains("not deleted u1", "not deleted u3", "not deleted u4", "not deleted u5");
  }

  private void verifyOrganizationDoesNotExist(OrganizationDto organization) {
    assertThat(dbTester.getDbClient().organizationDao().selectByKey(session, organization.getKey()))
      .isEmpty();
  }

  private void sendRequest(OrganizationDto organization) {
    sendRequest(organization.getKey());
  }

  private void sendRequest(String organizationKey) {
    wsTester.newRequest()
      .setParam(PARAM_KEY, organizationKey)
      .execute();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void logInAsAdministrator(OrganizationDto organization) {
    userSession.logIn().addOrganizationPermission(organization.getUuid(), SYSTEM_ADMIN);
  }
}
