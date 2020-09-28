/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;
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
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.DefaultTemplatesResolver;
import org.sonar.server.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
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
  public void setUp() {
    DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
    GroupWsSupport groupWsSupport = new GroupWsSupport(dbClient, new DefaultGroupFinder(db.getDbClient(), defaultOrganizationProvider));
    this.underTestWithoutViews = new WsActionTester(new DeleteTemplateAction(dbClient, userSession,
      new PermissionWsSupport(dbClient, new ComponentFinder(dbClient, resourceTypes), groupWsSupport), defaultTemplatesResolver, defaultOrganizationProvider));
    this.underTestWithViews = new WsActionTester(new DeleteTemplateAction(dbClient, userSession,
      new PermissionWsSupport(dbClient, new ComponentFinder(dbClient, resourceTypes), groupWsSupport), defaultTemplatesResolverWithViews, defaultOrganizationProvider));
  }

  @Test
  public void delete_template_in_db() throws Exception {
    runOnAllUnderTests((underTest) -> {
      PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
      db.organizations().setDefaultTemplates(
        db.permissionTemplates().insertTemplate(),
        null, db.permissionTemplates().insertTemplate()
      );
      loginAsAdmin();

      TestResponse result = newRequestByUuid(underTest, template.getUuid());

      assertThat(result.getInput()).isEmpty();
      assertTemplateDoesNotExist(template);
    });
  }

  @Test
  public void delete_template_by_name_case_insensitive() throws Exception {
    runOnAllUnderTests((underTest) -> {
      db.organizations().setDefaultTemplates(
        db.permissionTemplates().insertTemplate(),
        db.permissionTemplates().insertTemplate(), db.permissionTemplates().insertTemplate()
      );
      PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
      loginAsAdmin();
      newRequestByName(underTest, template);

      assertTemplateDoesNotExist(template);
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
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_project_without_views() {
    fail_to_delete_by_uuid_if_template_is_default_template_for_project(this.underTestWithoutViews);
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_project_with_views() {
    fail_to_delete_by_uuid_if_template_is_default_template_for_project(this.underTestWithViews);
  }

  private void fail_to_delete_by_uuid_if_template_is_default_template_for_project(WsActionTester underTest) {
    PermissionTemplateDto projectTemplate = insertTemplateAndAssociatedPermissions();
    db.organizations().setDefaultTemplates(projectTemplate,
      null, db.permissionTemplates().insertTemplate());
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for projects");

    newRequestByUuid(underTest, projectTemplate.getUuid());
  }

  @Test
  public void fail_to_delete_by_name_if_template_is_default_template_for_project_without_views() {
    fail_to_delete_by_name_if_template_is_default_template_for_project(this.underTestWithoutViews);
  }

  @Test
  public void fail_to_delete_by_name_if_template_is_default_template_for_project_with_views() {
    fail_to_delete_by_name_if_template_is_default_template_for_project(this.underTestWithViews);
  }

  private void fail_to_delete_by_name_if_template_is_default_template_for_project(WsActionTester underTest) {
    PermissionTemplateDto projectTemplate = insertTemplateAndAssociatedPermissions();
    db.organizations().setDefaultTemplates(projectTemplate, null, db.permissionTemplates().insertTemplate());
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for projects");

    newRequestByName(underTest, projectTemplate.getName());
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_portfolios_with_views() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    db.organizations().setDefaultTemplates(db.permissionTemplates().insertTemplate(), null, template);
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for portfolios");

    newRequestByUuid(this.underTestWithViews, template.getUuid());
  }

  @Test
  public void fail_to_delete_by_uuid_if_template_is_default_template_for_applications_with_views() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    db.organizations().setDefaultTemplates(db.permissionTemplates().insertTemplate(), template, null);
    loginAsAdmin();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("It is not possible to delete the default permission template for applications");

    newRequestByUuid(this.underTestWithViews, template.getUuid());
  }

  @Test
  public void default_template_for_views_can_be_deleted_by_uuid_if_views_is_not_installed_and_default_template_for_views_is_reset() {
    PermissionTemplateDto projectTemplate = db.permissionTemplates().insertTemplate();
    PermissionTemplateDto viewTemplate = insertTemplateAndAssociatedPermissions();
    db.organizations().setDefaultTemplates(projectTemplate, null, viewTemplate);
    loginAsAdmin();

    newRequestByUuid(this.underTestWithoutViews, viewTemplate.getUuid());

    assertTemplateDoesNotExist(viewTemplate);

    assertThat(db.getDbClient().organizationDao().getDefaultTemplates(db.getSession(), db.getDefaultOrganization().getUuid())
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
    newRequestByName(underTestWithoutViews, "name");
  }

  @Test
  public void fail_to_delete_by_name_if_not_logged_in_with_views() {
    expectedException.expect(UnauthorizedException.class);
    newRequestByName(underTestWithViews, "name");
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_admin_without_views() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByUuid(underTestWithoutViews, template.getUuid());
  }

  @Test
  public void fail_to_delete_by_uuid_if_not_admin_with_views() {
    PermissionTemplateDto template = insertTemplateAndAssociatedPermissions();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByUuid(underTestWithViews, template.getUuid());
  }

  @Test
  public void fail_to_delete_by_name_if_not_admin_without_views() {
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByName(underTestWithoutViews, template.getName());
  }

  @Test
  public void fail_to_delete_by_name_if_not_admin_with_views() {
    PermissionTemplateDto template = db.permissionTemplates().insertTemplate(PermissionTemplateTesting.newPermissionTemplateDto()
      .setName("the name"));
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequestByName(underTestWithViews, template);
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

  private UserSessionRule loginAsAdmin() {
    return userSession.logIn().addPermission(ADMINISTER);
  }

  private void runOnAllUnderTests(ConsumerWithException<WsActionTester> consumer) throws Exception {
    for (WsActionTester underTest : Arrays.asList(underTestWithoutViews, underTestWithViews)) {
      consumer.accept(underTest);
    }
  }

  private interface ConsumerWithException<T> {
    void accept(T e) throws Exception;
  }

  private PermissionTemplateDto insertTemplateAndAssociatedPermissions() {
    PermissionTemplateDto dto = db.permissionTemplates().insertTemplate();
    UserDto user = db.getDbClient().userDao().insert(db.getSession(), UserTesting.newUserDto().setActive(true));
    GroupDto group = db.getDbClient().groupDao().insert(db.getSession(), GroupTesting.newGroupDto());
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), dto.getUuid(), user.getUuid(), UserRole.ADMIN);
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), dto.getUuid(), group.getUuid(), UserRole.CODEVIEWER);
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

  private TestResponse newRequestByName(WsActionTester actionTester, @Nullable PermissionTemplateDto permissionTemplateDto) {
    return newRequestByName(
      actionTester,
      permissionTemplateDto == null ? null : permissionTemplateDto.getName());
  }

  private TestResponse newRequestByName(WsActionTester actionTester, @Nullable String name) {
    TestRequest request = actionTester.newRequest().setMethod("POST");

    if (name != null) {
      request.setParam(PARAM_TEMPLATE_NAME, name);
    }

    return request.execute();
  }

  private void assertTemplateDoesNotExist(PermissionTemplateDto template) {
    assertThat(db.getDbClient().permissionTemplateDao().selectByUuid(db.getSession(), template.getUuid())).isNull();
  }

}
