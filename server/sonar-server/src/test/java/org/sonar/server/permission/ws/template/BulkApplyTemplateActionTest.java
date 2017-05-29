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
package org.sonar.server.permission.ws.template;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.i18n.I18nRule;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.ws.BasePermissionWsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class BulkApplyTemplateActionTest extends BasePermissionWsTest<BulkApplyTemplateAction> {

  @org.junit.Rule
  public DefaultTemplatesResolverRule defaultTemplatesResolver = DefaultTemplatesResolverRule.withoutGovernance();

  private UserDto user1;
  private UserDto user2;
  private GroupDto group1;
  private GroupDto group2;
  private OrganizationDto organization;
  private PermissionTemplateDto template1;
  private PermissionTemplateDto template2;
  private PermissionIndexer issuePermissionIndexer = mock(PermissionIndexer.class);

  @Override
  protected BulkApplyTemplateAction buildWsAction() {
    PermissionTemplateService permissionTemplateService = new PermissionTemplateService(db.getDbClient(),
      issuePermissionIndexer, userSession, defaultTemplatesResolver);
    return new BulkApplyTemplateAction(db.getDbClient(), userSession, permissionTemplateService, newPermissionWsSupport(), new I18nRule(), newRootResourceTypes());
  }

  @Before
  public void setUp() {
    organization = db.organizations().insert();

    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    group1 = db.users().insertGroup(organization);
    group2 = db.users().insertGroup(organization);

    db.organizations().addMember(organization, user1);
    db.organizations().addMember(organization, user2);

    // template 1 for org 1
    template1 = db.permissionTemplates().insertTemplate(organization);
    addUserToTemplate(user1, template1, UserRole.CODEVIEWER);
    addUserToTemplate(user2, template1, UserRole.ISSUE_ADMIN);
    addGroupToTemplate(group1, template1, UserRole.ADMIN);
    addGroupToTemplate(group2, template1, UserRole.USER);
    // template 2
    template2 = db.permissionTemplates().insertTemplate(organization);
    addUserToTemplate(user1, template2, UserRole.USER);
    addUserToTemplate(user2, template2, UserRole.USER);
    addGroupToTemplate(group1, template2, UserRole.USER);
    addGroupToTemplate(group2, template2, UserRole.USER);
  }

  @Test
  public void bulk_apply_template_by_template_uuid() throws Exception {
    // this project should not be applied the template
    OrganizationDto otherOrganization = db.organizations().insert();
    db.components().insertPrivateProject(otherOrganization);

    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    ComponentDto view = db.components().insertView(organization);
    loginAsAdmin(organization);

    newRequest().setParam(PARAM_TEMPLATE_ID, template1.getUuid()).execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
    assertTemplate1AppliedToPublicProject(view);
  }

  @Test
  public void request_throws_NotFoundException_if_template_with_specified_name_does_not_exist_in_specified_organization() throws Exception {
    OrganizationDto otherOrganization = db.organizations().insert();
    loginAsAdmin(otherOrganization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with name '" + template1.getName()
      + "' is not found (case insensitive) in organization with key '" + otherOrganization.getKey() + "'");

    newRequest()
      .setParam(PARAM_ORGANIZATION, otherOrganization.getKey())
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute();
  }

  @Test
  public void bulk_apply_template_by_template_name() throws Exception {
    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    loginAsAdmin(organization);

    newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
  }

  @Test
  public void apply_template_by_qualifier() throws Exception {
    ComponentDto publicProject = db.components().insertPublicProject(organization);
    ComponentDto privateProject = db.components().insertPrivateProject(organization);
    ComponentDto view = db.components().insertComponent(newView(organization));
    loginAsAdmin(organization);

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_QUALIFIER, privateProject.qualifier()).execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
    assertNoPermissionOnProject(view);
  }

  @Test
  public void apply_template_by_query_on_name_and_key_public_project() throws Exception {
    ComponentDto publicProjectFoundByKey = ComponentTesting.newPublicProjectDto(organization).setKey("sonar");
    db.components().insertProjectAndSnapshot(publicProjectFoundByKey);
    ComponentDto publicProjectFoundByName = ComponentTesting.newPublicProjectDto(organization).setName("name-sonar-name");
    db.components().insertProjectAndSnapshot(publicProjectFoundByName);
    // match must be exact on key
    ComponentDto projectUntouched = ComponentTesting.newPublicProjectDto(organization).setKey("new-sonar").setName("project-name");
    db.components().insertProjectAndSnapshot(projectUntouched);
    loginAsAdmin(organization);

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "sonar")
      .execute();

    assertTemplate1AppliedToPublicProject(publicProjectFoundByKey);
    assertTemplate1AppliedToPublicProject(publicProjectFoundByName);
    assertNoPermissionOnProject(projectUntouched);
  }

  @Test
  public void apply_template_by_query_on_name_and_key() throws Exception {
    ComponentDto privateProjectFoundByKey = ComponentTesting.newPrivateProjectDto(organization).setKey("sonar");
    db.components().insertProjectAndSnapshot(privateProjectFoundByKey);
    ComponentDto privateProjectFoundByName = ComponentTesting.newPrivateProjectDto(organization).setName("name-sonar-name");
    db.components().insertProjectAndSnapshot(privateProjectFoundByName);
    // match must be exact on key
    ComponentDto projectUntouched = ComponentTesting.newPublicProjectDto(organization).setKey("new-sonar").setName("project-name");
    db.components().insertProjectAndSnapshot(projectUntouched);
    loginAsAdmin(organization);

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "sonar")
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProjectFoundByKey);
    assertTemplate1AppliedToPrivateProject(privateProjectFoundByName);
    assertNoPermissionOnProject(projectUntouched);
  }

  @Test
  public void fail_if_no_template_parameter() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Template name or template id must be provided, not both.");

    newRequest().execute();
  }

  @Test
  public void fail_if_template_name_is_incorrect() throws Exception {
    loginAsAdmin(db.getDefaultOrganization());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-template-uuid' is not found");

    newRequest().setParam(PARAM_TEMPLATE_ID, "unknown-template-uuid").execute();
  }

  private void assertTemplate1AppliedToPublicProject(ComponentDto project) throws Exception {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).containsExactly(user2.getId());
  }

  private void assertTemplate1AppliedToPrivateProject(ComponentDto project) throws Exception {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).containsExactly(user1.getId());
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).containsExactly(user2.getId());
  }

  private void assertNoPermissionOnProject(ComponentDto project) throws Exception {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionGroups(project, UserRole.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionGroups(project, UserRole.ISSUE_ADMIN)).isEmpty();
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.USER)).isEmpty();
  }

  private void addUserToTemplate(UserDto user, PermissionTemplateDto permissionTemplate, String permission) {
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), permissionTemplate.getId(), user.getId(), permission);
    db.commit();
  }

  private void addGroupToTemplate(GroupDto group, PermissionTemplateDto permissionTemplate, String permission) {
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), permissionTemplate.getId(), group.getId(), permission);
    db.commit();
  }

  private List<String> selectProjectPermissionGroups(ComponentDto project, String permission) {
    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(project.getOrganizationUuid()).setPermission(permission).setComponentUuid(project.uuid()).build();
    return db.getDbClient().groupPermissionDao().selectGroupNamesByQuery(db.getSession(), query);
  }

  private List<Integer> selectProjectPermissionUsers(ComponentDto project, String permission) {
    PermissionQuery query = PermissionQuery.builder().setOrganizationUuid(project.getOrganizationUuid()).setPermission(permission).setComponentUuid(project.uuid()).build();
    return db.getDbClient().userPermissionDao().selectUserIdsByQuery(db.getSession(), query);
  }
}
