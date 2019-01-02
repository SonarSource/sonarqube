/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.permission.ws.template;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateActionTest {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UserSessionRule userSession = UserSessionRule.standalone();
  private DbClient dbClient = db.getDbClient();
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final ResourceTypesRule resourceTypesWithViews = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW);

  private DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(resourceTypes);
  private DefaultTemplatesResolver defaultTemplatesResolverWithViews = new DefaultTemplatesResolverImpl(resourceTypesWithViews);

  private WsActionTester underTestWithoutViews;
  private WsActionTester underTestWithViews;

  @Before
  public void setUp() throws Exception {
    GroupWsSupport groupWsSupport = new GroupWsSupport(dbClient, TestDefaultOrganizationProvider.from(db), new DefaultGroupFinder(db.getDbClient()));
    this.underTestWithoutViews = new WsActionTester(new DeleteTemplateAction(dbClient, userSession,
      new PermissionWsSupport(dbClient, new ComponentFinder(dbClient, resourceTypes), groupWsSupport), defaultTemplatesResolver));
    this.underTestWithViews = new WsActionTester(new DeleteTemplateAction(dbClient, userSession,
      new PermissionWsSupport(dbClient, new ComponentFinder(dbClient, resourceTypes), groupWsSupport), defaultTemplatesResolverWithViews));
  }

  @Test
  public void delete_template_in_db() throws Exception {
    runOnAllUnderTests((underTest) -> {
      OrganizationDto organization = db.organizations().insert();
      PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
      db.organizations().setDefaultTemplates(
        db.permissionTemplates().insertTemplate(organization),
        null, db.permissionTemplates().insertTemplate(organization)
      );
      loginAsAdmin(organization);

      TestResponse result = newRequestByUuid(underTest, template.getUuid());

      assertThat(result.getInput()).isEmpty();
      assertTemplateDoesNotExist(template);
    });
  }

  @Test
  public void delete_template_by_name_case_insensitive() throws Exception {
    runOnAllUnderTests((underTest) -> {
      OrganizationDto organization = db.organizations().insert();
      db.organizations().setDefaultTemplates(
        db.permissionTemplates().insertTemplate(organization),
        db.permissionTemplates().insertTemplate(organization), db.permissionTemplates().insertTemplate(organization)
      );
      PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
      loginAsAdmin(organization);
      newRequestByName(underTest, organization, template);

      assertTemplateDoesNotExist(template);
    });
  }

  @Test
  public void delete_template_by_name_returns_empty_when_no_organization_is_provided_and_templates_does_not_belong_to_default_organization() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    db.organizations().setDefaultTemplates(
      db.permissionTemplates().insertTemplate(organization),
      db.permissionTemplates().insertTemplate(organization), db.permissionTemplates().insertTemplate(organization)
    );
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
    loginAsAdmin(organization);

    runOnAllUnderTests((underTest) -> {
      try {
        newRequestByName(underTest, null, template);
        fail("NotFoundException should have been raised");
      } catch (NotFoundException e) {
        assertThat(e).hasMessage(
          "Permission template with name '" + template.getName() + "' is not found (case insensitive) in organization with key '" + db.getDefaultOrganization().getKey() + "'");
      }
    });
  }

  @Test
  public void delete_template_by_name_returns_empty_when_wrong_organization_is_provided() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    db.organizations().setDefaultTemplates(
      db.permissionTemplates().insertTemplate(organization),
      db.permissionTemplates().insertTemplate(organization), db.permissionTemplates().insertTemplate(organization)
    );
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    loginAsAdmin(organization);

    runOnAllUnderTests((underTest) -> {
      try {
        newRequestByName(underTest, otherOrganization, template);
        fail("NotFoundException should have been raised");
      } catch (NotFoundException e) {
        assertThat(e)
          .hasMessage("Permission template with name '" + template.getName() + "' is not found (case insensitive) in organization with key '" + otherOrganization.getKey() + "'");
      }
    });
  }

  @Test
  public void fail_if_uuid_is_not_known_without_views() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);

    newRequestByUuid(underTestWithoutViews, "unknown-template-uuid");
  }

  @Test
  public void fail_if_uuid_is_not_known_with_views() {
    userSession.logIn();

    expectedException.expect(NotFoundException.class);

    newRequestByUuid(underTestWithViews, "unknown-template-uuid");
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_project_without_views() throws Exception {
    fail_to_delete_by_uuid_if_template_is_default_template_for_project(this.underTestWithoutViews);
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_project_with_views() throws Exception {
    fail_to_delete_by_uuid_if_template_is_default_template_for_project(this.underTestWithViews);
  }

  private void fail_to_delete_by_uuid_if_template_is_default_template_for_project(WsActionTester underTest) {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto projectTemplate = insertTemplateAndAssociatedPermissions(organization);
    db.organizations().setDefaultTemplates(projectTemplate,
      null, db.permissionTemplates().insertTemplate(organization));
    loginAsAdmin(organization);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for projects");

    newRequestByUuid(underTest, projectTemplate.getUuid());
  }

  @Test
  public void fail_to_delete_by_name_if_template_is_default_template_for_project_without_views() throws Exception {
    fail_to_delete_by_name_if_template_is_default_template_for_project(this.underTestWithoutViews);
  }

  @Test
  public void fail_to_delete_by_name_if_template_is_default_template_for_project_with_views() throws Exception {
    fail_to_delete_by_name_if_template_is_default_template_for_project(this.underTestWithViews);
  }

  private void fail_to_delete_by_name_if_template_is_default_template_for_project(WsActionTester underTest) {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto projectTemplate = insertTemplateAndAssociatedPermissions(organization);
    db.organizations().setDefaultTemplates(projectTemplate, null, db.permissionTemplates().insertTemplate(organization));
    loginAsAdmin(organization);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for projects");

    newRequestByName(underTest, organization.getKey(), projectTemplate.getName());
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_portfolios_with_views() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
    db.organizations().setDefaultTemplates(db.permissionTemplates().insertTemplate(organization), null, template);
    loginAsAdmin(organization);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for portfolios");

    newRequestByUuid(this.underTestWithViews, template.getUuid());
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_applications_with_views() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
    db.organizations().setDefaultTemplates(db.permissionTemplates().insertTemplate(organization), template, null);
    loginAsAdmin(organization);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for applications");

    newRequestByUuid(this.underTestWithViews, template.getUuid());
  }

  @Test
  public void default_template_for_views_can_be_deleted_by_uuid_if_views_is_not_installed_and_default_template_for_views_is_reset() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto projectTemplate = db.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto viewTemplate = insertTemplateAndAssociatedPermissions(organization);
    db.organizations().setDefaultTemplates(projectTemplate, null, viewTemplate);
    loginAsAdmin(organization);

    newRequestByUuid(this.underTestWithoutViews, viewTemplate.getUuid());

    assertTemplateDoesNotExist(viewTemplate);

    assertThat(db.getDbClient().organizationDao().getDefaultTemplates(db.getSession(), organization.getUuid())
      .get().getApplicationsUuid())
        .isNull();
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_logged_in_without_views() {
    expectedException.expect(UnauthorizedException.class);

    newRequestByUuid(underTestWithoutViews, "uuid");
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_logged_in_with_views() {
    expectedException.expect(UnauthorizedException.class);

    newRequestByUuid(underTestWithViews, "uuid");
  }

  @Test
  public void fail_to_delete_by_name_if_not_logged_in_without_views() {
    expectedException.expect(UnauthorizedException.class);

    newRequestByName(underTestWithoutViews, "whatever", "name");
  }

  @Test
  public void fail_to_delete_by_name_if_not_logged_in_with_views() {
    expectedException.expect(UnauthorizedException.class);

    newRequestByName(underTestWithViews, "whatever", "name");
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_admin_without_views() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByUuid(underTestWithoutViews, template.getUuid());
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_admin_with_views() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions(organization);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByUuid(underTestWithViews, template.getUuid());
  }

  @Test
  public void fail_to_delete_by_name_if_not_admin_without_views() {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(organization);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByName(underTestWithoutViews, organization.getKey(), template.getName());
  }

  @Test
  public void fail_to_delete_by_name_if_not_admin_with_views() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(PermissionTemplateTesting.newPermissionTemplateDto()
      .setOrganizationUuid(organization.getUuid())
      .setName("the name"));
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByName(underTestWithViews, organization, template);
  }

  @Test
  public void fail_if_neither_uuid_nor_name_is_provided_without_views() {
    userSession.logIn();

    expectedException.expect(BadRequestException.class);

    newRequestByUuid(underTestWithoutViews, null);
  }

  @Test
  public void fail_if_neither_uuid_nor_name_is_provided_with_views() {
    userSession.logIn();

    expectedException.expect(BadRequestException.class);

    newRequestByUuid(underTestWithViews, null);
  }

  @Test
  public void fail_if_both_uuid_and_name_are_provided_without_views() {
    userSession.logIn();

    expectedException.expect(BadRequestException.class);

    underTestWithoutViews.newRequest().setMethod("POST")
      .setParam(PARAM_TEMPLATE_ID, "uuid")
      .setParam(PARAM_TEMPLATE_NAME, "name")
      .execute();
  }

  @Test
  public void fail_if_both_uuid_and_name_are_provided_with_views() {
    userSession.logIn();

    expectedException.expect(BadRequestException.class);

    underTestWithViews.newRequest().setMethod("POST")
      .setParam(PARAM_TEMPLATE_ID, "uuid")
      .setParam(PARAM_TEMPLATE_NAME, "name")
      .execute();
  }

  // @Test
  // public void delete_perm_tpl_characteristic_when_delete_template() throws Exception {
  // db.getDbClient().permissionTemplateCharacteristicDao().insert(db.getSession(), new PermissionTemplateCharacteristicDto()
  // .setPermission(UserRole.USER)
  // .setTemplateId(template.getId())
  // .setWithProjectCreator(true)
  // .setCreatedAt(new Date().getTime())
  // .setUpdatedAt(new Date().getTime()));
  // db.commit();
  //
  // newRequest(template.getUuid());
  //
  // assertThat(db.getDbClient().permissionTemplateCharacteristicDao().selectByTemplateIds(db.getSession(),
  // asList(template.getId()))).isEmpty();
  // }

  private UserSessionRule loginAsAdmin(OrganizationDto organization) {
    return userSession.logIn().addPermission(ADMINISTER, organization);
  }

  private void runOnAllUnderTests(ConsumerWithException<WsActionTester> consumer) throws Exception {
    for (WsActionTester underTest : Arrays.asList(underTestWithoutViews, underTestWithViews)) {
      consumer.accept(underTest);
    }
  }

  private interface ConsumerWithException<T> {
    void accept(T e) throws Exception;
  }

  private PermissionTemplateDto insertTemplateAndAssociatedPermissions(OrganizationDto organization) {
    PermissionTemplateDto dto = db.permissionTemplates().insertTemplate(organization);
    UserDto user = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto().setActive(true));
    GroupDto group = db.getDbClient().groupDao().insert(db.getSession(), GroupTesting.newGroupDto());
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), dto.getId(), user.getId(), UserRole.ADMIN);
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), dto.getId(), group.getId(), UserRole.CODEVIEWER);
    db.commit();
    return dto;
  }

  private TestResponse newRequestByUuid(WsActionTester actionTester, @Nullable String id) {
    TestRequest request = actionTester.newRequest().setMethod("POST");
    if (id != null) {
      request.setParam(PARAM_TEMPLATE_ID, id);
    }
    return request.execute();
  }

  private TestResponse newRequestByName(WsActionTester actionTester, @Nullable OrganizationDto organizationDto, @Nullable PermissionTemplateDto permissionTemplateDto)
    throws Exception {
    return newRequestByName(
      actionTester,
      organizationDto == null ? null : organizationDto.getKey(),
      permissionTemplateDto == null ? null : permissionTemplateDto.getName());
  }

  private TestResponse newRequestByName(WsActionTester actionTester, @Nullable String organizationKey, @Nullable String name) {
    TestRequest request = actionTester.newRequest().setMethod("POST");
    if (organizationKey != null) {
      request.setParam(PARAM_ORGANIZATION, organizationKey);
    }
    if (name != null) {
      request.setParam(PARAM_TEMPLATE_NAME, name);
    }

    return request.execute();
  }

  private void assertTemplateDoesNotExist(PermissionTemplateDto template) {
    assertThat(db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid())).isNull();
  }

}
