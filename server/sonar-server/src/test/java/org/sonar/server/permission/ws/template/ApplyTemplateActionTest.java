/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.index.ComponentIndexDefinition;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.measure.index.ProjectMeasuresIndexDefinition;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.ws.BasePermissionWsTest;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class ApplyTemplateActionTest extends BasePermissionWsTest<ApplyTemplateAction> {

  @Rule
  public EsTester esTester = new EsTester(
    new IssueIndexDefinition(new MapSettings()),
    new ProjectMeasuresIndexDefinition(new MapSettings()),
    new ComponentIndexDefinition(new MapSettings()));

  private UserDto user1;
  private UserDto user2;
  private GroupDto group1;
  private GroupDto group2;
  private ComponentDto project;
  private PermissionTemplateDto template1;
  private PermissionTemplateDto template2;
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(esTester);
  private PermissionIndexer permissionIndexer = new PermissionIndexer(db.getDbClient(), esTester.client());

  @Override
  protected ApplyTemplateAction buildWsAction() {
    PermissionTemplateService permissionTemplateService = new PermissionTemplateService(db.getDbClient(),
      new MapSettings(), permissionIndexer, userSession);
    return new ApplyTemplateAction(db.getDbClient(), userSession, permissionTemplateService, newPermissionWsSupport());
  }

  @Before
  public void setUp() {
    user1 = db.users().insertUser("user-login-1");
    user2 = db.users().insertUser("user-login-2");
    OrganizationDto defaultOrg = db.getDefaultOrganization();
    group1 = db.users().insertGroup(defaultOrg, "group-name-1");
    group2 = db.users().insertGroup(defaultOrg, "group-name-2");

    // template 1
    template1 = insertTemplate();
    addUserToTemplate(user1, template1, UserRole.CODEVIEWER);
    addUserToTemplate(user2, template1, UserRole.ISSUE_ADMIN);
    addGroupToTemplate(group1, template1, UserRole.ADMIN);
    addGroupToTemplate(group2, template1, UserRole.USER);
    // template 2
    template2 = insertTemplate();
    addUserToTemplate(user1, template2, UserRole.USER);
    addUserToTemplate(user2, template2, UserRole.USER);
    addGroupToTemplate(group1, template2, UserRole.USER);
    addGroupToTemplate(group2, template2, UserRole.USER);

    project = db.components().insertComponent(newProjectDto(defaultOrg, "project-uuid-1"));
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project);
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, project);
    db.users().insertProjectPermissionOnGroup(group1, UserRole.ADMIN, project);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ADMIN, project);
  }

  @Test
  public void apply_template_with_project_uuid() throws Exception {
    loginAsAdminOnDefaultOrganization();

    newRequest(template1.getUuid(), project.uuid(), null);

    assertTemplate1AppliedToProject();
  }

  @Test
  public void apply_template_with_project_uuid_by_template_name() throws Exception {
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template1.getName().toUpperCase())
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute();

    assertTemplate1AppliedToProject();
  }

  @Test
  public void apply_template_with_project_key() throws Exception {
    loginAsAdminOnDefaultOrganization();

    newRequest(template1.getUuid(), null, project.key());

    assertTemplate1AppliedToProject();
  }

  @Test
  public void fail_when_unknown_template() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Permission template with id 'unknown-template-uuid' is not found");

    newRequest("unknown-template-uuid", project.uuid(), null);
  }

  @Test
  public void fail_when_unknown_project_uuid() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project id 'unknown-project-uuid' not found");

    newRequest(template1.getUuid(), "unknown-project-uuid", null);
  }

  @Test
  public void fail_when_unknown_project_key() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Project key 'unknown-project-key' not found");

    newRequest(template1.getUuid(), null, "unknown-project-key");
  }

  @Test
  public void fail_when_template_is_not_provided() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);

    newRequest(null, project.uuid(), null);
  }

  @Test
  public void fail_when_project_uuid_and_key_not_provided() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Project id or project key can be provided, not both.");

    newRequest(template1.getUuid(), null, null);
  }

  @Test
  public void fail_when_not_admin_of_organization() throws Exception {
    userSession.login().addOrganizationPermission("otherOrg", SYSTEM_ADMIN);

    expectedException.expect(ForbiddenException.class);
    userSession.login().setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    newRequest(template1.getUuid(), project.uuid(), null);
  }

  private void assertTemplate1AppliedToProject() {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).containsExactly(user1.getId());
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).containsExactly(user2.getId());

    authorizationIndexerTester.verifyProjectExistsWithPermission(project.uuid(), singletonList(group2.getName()), Collections.emptyList());
  }

  private TestResponse newRequest(@Nullable String templateUuid, @Nullable String projectUuid, @Nullable String projectKey) throws Exception {
    TestRequest request = newRequest();
    if (templateUuid != null) {
      request.setParam(PARAM_TEMPLATE_ID, templateUuid);
    }
    if (projectUuid != null) {
      request.setParam(PARAM_PROJECT_ID, projectUuid);
    }
    if (projectKey != null) {
      request.setParam(PARAM_PROJECT_KEY, projectKey);
    }

    return request.execute();
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
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setComponentUuid(project.uuid()).build();
    return db.getDbClient().groupPermissionDao().selectGroupNamesByQuery(db.getSession(), db.getDefaultOrganization().getUuid(), query);
  }

  private List<Long> selectProjectPermissionUsers(ComponentDto project, String permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setComponentUuid(project.uuid()).build();
    return db.getDbClient().userPermissionDao().selectUserIds(db.getSession(), db.getDefaultOrganization().getUuid(), query);
  }
}
