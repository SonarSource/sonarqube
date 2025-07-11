/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.common.permission.DefaultTemplatesResolver;
import org.sonar.server.common.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.permission.ws.BasePermissionWsIT;
import org.sonar.server.ws.TestRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class ApplyTemplateActionIT extends BasePermissionWsIT<ApplyTemplateAction> {

  @Rule
  public DbTester dbTester = DbTester.create();

  private UserDto user1;
  private UserDto user2;
  private GroupDto group1;
  private GroupDto group2;
  private ProjectDto project;
  private PermissionTemplateDto template1;

  private final ComponentTypesRule resourceTypesRule = new ComponentTypesRule().setRootQualifiers(PROJECT, VIEW, APP);
  private final DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(dbTester.getDbClient(), resourceTypesRule);
  private final PermissionTemplateService permissionTemplateService = new PermissionTemplateService(db.getDbClient(),
    new TestIndexers(), userSession, defaultTemplatesResolver, new SequenceUuidFactory());

  private final ManagedProjectService managedProjectService = mock(ManagedProjectService.class);
  private final ManagedInstanceChecker managedInstanceChecker = new ManagedInstanceChecker(null, managedProjectService);

  @Override
  protected ApplyTemplateAction buildWsAction() {
    return new ApplyTemplateAction(db.getDbClient(), userSession, permissionTemplateService, newPermissionWsSupport(), managedInstanceChecker);
  }

  @Before
  public void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    group1 = db.users().insertGroup();
    group2 = db.users().insertGroup();

    // template 1
    template1 = db.permissionTemplates().insertTemplate();
    addUserToTemplate(user1, template1, UserRole.CODEVIEWER);
    addUserToTemplate(user2, template1, UserRole.ISSUE_ADMIN);
    addGroupToTemplate(group1, template1, UserRole.ADMIN);
    addGroupToTemplate(group2, template1, UserRole.USER);
    // template 2
    PermissionTemplateDto template2 = db.permissionTemplates().insertTemplate();
    addUserToTemplate(user1, template2, UserRole.USER);
    addUserToTemplate(user2, template2, UserRole.USER);
    addGroupToTemplate(group1, template2, UserRole.USER);
    addGroupToTemplate(group2, template2, UserRole.USER);

    project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertProjectPermissionOnUser(user1, UserRole.ADMIN, project);
    db.users().insertProjectPermissionOnUser(user2, UserRole.ADMIN, project);
    db.users().insertEntityPermissionOnGroup(group1, UserRole.ADMIN, project);
    db.users().insertEntityPermissionOnGroup(group2, UserRole.ADMIN, project);
  }

  @Test
  public void apply_template_with_project_uuid() {
    loginAsAdmin();

    newRequest(template1.getUuid(), project.getUuid(), null);

    assertTemplate1AppliedToProject();
  }

  @Test
  public void apply_template_with_project_uuid_by_template_name() {
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template1.getName().toUpperCase())
      .setParam(PARAM_PROJECT_ID, project.getUuid())
      .execute();

    assertTemplate1AppliedToProject();
  }

  @Test
  public void apply_template_with_project_key() {
    loginAsAdmin();

    newRequest(template1.getUuid(), null, project.getKey());

    assertTemplate1AppliedToProject();
  }

  @Test
  public void fail_when_unknown_template() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest("unknown-template-uuid", project.getUuid(), null);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Permission template with id 'unknown-template-uuid' is not found");
  }

  @Test
  public void fail_when_unknown_project_uuid() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest(template1.getUuid(), "unknown-project-uuid", null);
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void fail_when_unknown_project_key() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest(template1.getUuid(), null, "unknown-project-key");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Entity not found");
  }

  @Test
  public void fail_when_template_is_not_provided() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest(null, project.getUuid(), null))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_when_project_uuid_and_key_not_provided() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest(template1.getUuid(), null, null);
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Project id or project key can be provided, not both.");
  }

  @Test
  public void fail_when_project_is_managed() {
    loginAsAdmin();

    when(managedProjectService.isProjectManaged(any(), eq(project.getUuid()))).thenReturn(true);

    String templateUuid = template1.getUuid();
    String projectUuid = project.getUuid();
    assertThatThrownBy(() -> newRequest(templateUuid, projectUuid, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Operation not allowed when the project is externally managed.");
  }

  @Test
  public void fail_when_not_admin() {
    userSession.logIn().addPermission(SCAN);

    assertThatThrownBy(() -> newRequest(template1.getUuid(), project.getUuid(), null))
      .isInstanceOf(ForbiddenException.class);
  }

  private void assertTemplate1AppliedToProject() {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).containsExactly(user1.getUuid());
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).containsExactly(user2.getUuid());
  }

  private void newRequest(@Nullable String templateUuid, @Nullable String projectUuid, @Nullable String projectKey) {
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
    request.execute();
  }

  private void addUserToTemplate(UserDto user, PermissionTemplateDto permissionTemplate, String permission) {
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), permissionTemplate.getUuid(), user.getUuid(),
      permission, permissionTemplate.getName(), user.getLogin());
    db.commit();
  }

  private void addGroupToTemplate(GroupDto group, PermissionTemplateDto permissionTemplate, String permission) {
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), permissionTemplate.getUuid(), group.getUuid(),
      permission, permissionTemplate.getName(), group.getName(), permissionTemplate.getOrganizationUuid());
    db.commit();
  }

  private List<String> selectProjectPermissionGroups(EntityDto entity, String permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setEntity(entity).build();
    return db.getDbClient().groupPermissionDao().selectGroupNamesByQuery(db.getSession(), query);
  }

  private List<String> selectProjectPermissionUsers(EntityDto entity, String permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setEntity(entity).build();
    return db.getDbClient().userPermissionDao().selectUserUuidsByQuery(db.getSession(), query);
  }
}
