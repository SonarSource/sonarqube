/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.permission.DefaultTemplatesResolver;
import org.sonar.server.common.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.ws.BasePermissionWsIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class BulkApplyTemplateActionIT extends BasePermissionWsIT<BulkApplyTemplateAction> {

  private UserDto user1;
  private UserDto user2;
  private GroupDto group1;
  private GroupDto group2;
  private PermissionTemplateDto template1;
  private final Indexers indexers = new TestIndexers();
  private final ComponentTypesRule resourceTypesRule = new ComponentTypesRule().setRootQualifiers(PROJECT, VIEW, APP);
  private final DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(db.getDbClient(), resourceTypesRule);

  private final ManagedProjectService managedProjectService = mock(ManagedProjectService.class);

  @Override
  protected BulkApplyTemplateAction buildWsAction() {
    PermissionTemplateService permissionTemplateService = new PermissionTemplateService(db.getDbClient(),
      indexers, userSession, defaultTemplatesResolver, new SequenceUuidFactory());
    return new BulkApplyTemplateAction(db.getDbClient(), userSession, permissionTemplateService, newPermissionWsSupport(), new I18nRule(), newRootResourceTypes(),
      managedProjectService);
  }

  @Before
  public void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    group1 = db.users().insertGroup();
    group2 = db.users().insertGroup();

    // template 1 for org 1
    template1 = db.permissionTemplates().insertTemplate();
    addUserToTemplate(user1, template1, ProjectPermission.CODEVIEWER);
    addUserToTemplate(user2, template1, ProjectPermission.ISSUE_ADMIN);
    addGroupToTemplate(group1, template1, ProjectPermission.ADMIN);
    addGroupToTemplate(group2, template1, ProjectPermission.USER);
    // template 2
    PermissionTemplateDto template2 = db.permissionTemplates().insertTemplate();
    addUserToTemplate(user1, template2, ProjectPermission.USER);
    addUserToTemplate(user2, template2, ProjectPermission.USER);
    addGroupToTemplate(group1, template2, ProjectPermission.USER);
    addGroupToTemplate(group2, template2, ProjectPermission.USER);
  }

  @Test
  public void bulk_apply_template_by_template_uuid() {
    // this project should not be applied the template
    db.components().insertPrivateProject().getProjectDto();

    ProjectDto privateProject = db.components().insertPrivateProject().getProjectDto();
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
  }

  @Test
  public void request_throws_NotFoundException_if_template_with_specified_name_does_not_exist() {
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_TEMPLATE_NAME, "unknown")
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Permission template with name 'unknown' is not found (case insensitive)");
  }

  @Test
  public void request_throws_IAE_if_more_than_1000_projects() {
    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_TEMPLATE_NAME, template1.getName())
        .setParam(PARAM_PROJECTS, StringUtils.join(Collections.nCopies(1_001, "foo"), ","))
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'projects' can contain only 1000 values, got 1001");
  }

  @Test
  public void bulk_apply_template_by_template_name() {
    ProjectDto privateProject = db.components().insertPrivateProject().getProjectDto();
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
  }

  @Test
  public void apply_template_by_qualifiers() {
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    ProjectDto privateProject = db.components().insertPrivateProject().getProjectDto();
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();
    ProjectDto application = db.components().insertPublicApplication().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_QUALIFIERS, String.join(",", ComponentQualifiers.PROJECT, ComponentQualifiers.APP))
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
    assertTemplate1AppliedToPublicProject(application);
    assertNoPermissionOnEntity(portfolio);
  }

  @Test
  public void apply_template_by_query_on_name_and_key_public_project() {
    ComponentDto publicProjectFoundByKey = ComponentTesting.newPublicProjectDto().setKey("sonar");
    ProjectDto publicProjectDtoFoundByKey = db.components().insertProjectDataAndSnapshot(publicProjectFoundByKey).getProjectDto();
    ComponentDto publicProjectFoundByName = ComponentTesting.newPublicProjectDto().setName("name-sonar-name");
    ProjectDto publicProjectDtoFoundByName = db.components().insertProjectDataAndSnapshot(publicProjectFoundByName).getProjectDto();
    ComponentDto projectUntouched = ComponentTesting.newPublicProjectDto().setKey("new-sona").setName("project-name");
    ProjectDto projectDtoUntouched = db.components().insertProjectDataAndSnapshot(projectUntouched).getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "SONAR")
      .execute();

    assertTemplate1AppliedToPublicProject(publicProjectDtoFoundByKey);
    assertTemplate1AppliedToPublicProject(publicProjectDtoFoundByName);
    assertNoPermissionOnEntity(projectDtoUntouched);
  }

  @Test
  public void apply_template_by_query_on_name_and_key() {
    // partial match on key
    ComponentDto privateProjectFoundByKey = ComponentTesting.newPrivateProjectDto().setKey("sonarqube");
    ProjectDto privateProjectDtoFoundByKey = db.components().insertProjectDataAndSnapshot(privateProjectFoundByKey).getProjectDto();
    ComponentDto privateProjectFoundByName = ComponentTesting.newPrivateProjectDto().setName("name-sonar-name");
    ProjectDto privateProjectDtoFoundByName = db.components().insertProjectDataAndSnapshot(privateProjectFoundByName).getProjectDto();
    ComponentDto projectUntouched = ComponentTesting.newPublicProjectDto().setKey("new-sona").setName("project-name");
    ProjectDto projectDtoUntouched = db.components().insertProjectDataAndSnapshot(projectUntouched).getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "SONAR")
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProjectDtoFoundByKey);
    assertTemplate1AppliedToPrivateProject(privateProjectDtoFoundByName);
    assertNoPermissionOnEntity(projectDtoUntouched);
  }

  @Test
  public void apply_template_by_project_keys() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto untouchedProject = db.components().insertPrivateProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_PROJECTS, String.join(",", project1.getKey(), project2.getKey()))
      .execute();

    assertTemplate1AppliedToPrivateProject(project1);
    assertTemplate1AppliedToPrivateProject(project2);
    assertNoPermissionOnEntity(untouchedProject);
  }

  @Test
  public void apply_template_by_provisioned_only() {
    ProjectDto provisionedProject1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto provisionedProject2 = db.components().insertPrivateProject().getProjectDto();
    ProjectData analyzedProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(analyzedProject.getMainBranchDto().getUuid()));
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_ON_PROVISIONED_ONLY, "true")
      .execute();

    assertTemplate1AppliedToPrivateProject(provisionedProject1);
    assertTemplate1AppliedToPrivateProject(provisionedProject2);
    assertNoPermissionOnEntity(analyzedProject.getProjectDto());
  }

  @Test
  public void apply_template_by_analyzed_before() {
    ProjectData oldProject1 = db.components().insertPrivateProject();
    ProjectData oldProject2 = db.components().insertPrivateProject();
    ProjectData recentProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(oldProject1.getMainBranchComponent(), a -> a.setCreatedAt(parseDate("2015-02-03").getTime()));
    db.components().insertSnapshot(oldProject2.getMainBranchComponent(), a -> a.setCreatedAt(parseDate("2016-12-11").getTime()));
    db.components().insertSnapshot(recentProject.getMainBranchComponent(), a -> a.setCreatedAt(System.currentTimeMillis()));
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_ANALYZED_BEFORE, "2017-09-07")
      .execute();

    assertTemplate1AppliedToPrivateProject(oldProject1.getProjectDto());
    assertTemplate1AppliedToPrivateProject(oldProject2.getProjectDto());
    assertNoPermissionOnEntity(recentProject.getProjectDto());
  }

  @Test
  public void apply_template_by_visibility() {
    ProjectDto privateProject1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto privateProject2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_VISIBILITY, "private")
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject1);
    assertTemplate1AppliedToPrivateProject(privateProject2);
    assertNoPermissionOnEntity(publicProject);
  }

  @Test
  public void apply_template_filters_out_managed_projects() {
    ProjectDto managedProject = db.components().insertPrivateProject().getProjectDto();
    ProjectDto nonManagedProject = db.components().insertPrivateProject().getProjectDto();
    when(managedProjectService.isProjectManaged(any(), eq(managedProject.getUuid()))).thenReturn(true);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_VISIBILITY, "private")
      .execute();

    assertNoPermissionOnEntity(managedProject);
    assertTemplate1AppliedToPrivateProject(nonManagedProject);
  }

  @Test
  public void fail_if_no_template_parameter() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest().execute())
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Template name or template id must be provided, not both.");
  }

  @Test
  public void fail_if_template_name_is_incorrect() {
    loginAsAdmin();

    assertThatThrownBy(() -> newRequest().setParam(PARAM_TEMPLATE_ID, "unknown-template-uuid").execute())
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Permission template with id 'unknown-template-uuid' is not found");
  }

  private void assertTemplate1AppliedToPublicProject(ProjectDto project) {
    assertThat(selectProjectPermissionGroups(project, ProjectPermission.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, ProjectPermission.USER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, ProjectPermission.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, ProjectPermission.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, ProjectPermission.ISSUE_ADMIN)).containsExactly(user2.getUuid());
  }

  private void assertTemplate1AppliedToPrivateProject(ProjectDto project) {
    assertThat(selectProjectPermissionGroups(project, ProjectPermission.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, ProjectPermission.USER)).containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, ProjectPermission.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, ProjectPermission.CODEVIEWER)).containsExactly(user1.getUuid());
    assertThat(selectProjectPermissionUsers(project, ProjectPermission.ISSUE_ADMIN)).containsExactly(user2.getUuid());
  }

  private void assertNoPermissionOnEntity(EntityDto entity) {
    assertThat(selectProjectPermissionGroups(entity, ProjectPermission.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionGroups(entity, ProjectPermission.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionGroups(entity, ProjectPermission.ISSUE_ADMIN)).isEmpty();
    assertThat(selectProjectPermissionGroups(entity, ProjectPermission.USER)).isEmpty();
    assertThat(selectProjectPermissionUsers(entity, ProjectPermission.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(entity, ProjectPermission.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionUsers(entity, ProjectPermission.ISSUE_ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(entity, ProjectPermission.USER)).isEmpty();
  }

  private void addUserToTemplate(UserDto user, PermissionTemplateDto permissionTemplate, ProjectPermission permission) {
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), permissionTemplate.getUuid(), user.getUuid(),
      permission, permissionTemplate.getName(), user.getLogin());
    db.commit();
  }

  private void addGroupToTemplate(GroupDto group, PermissionTemplateDto permissionTemplate, ProjectPermission permission) {
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), permissionTemplate.getUuid(), group.getUuid(),
      permission, permissionTemplate.getName(), group.getName());
    db.commit();
  }

  private List<String> selectProjectPermissionGroups(EntityDto project, ProjectPermission permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setEntity(project).build();
    return db.getDbClient().groupPermissionDao().selectGroupNamesByQuery(db.getSession(), query);
  }

  private List<String> selectProjectPermissionUsers(EntityDto project, ProjectPermission permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setEntity(project).build();
    return db.getDbClient().userPermissionDao().selectUserUuidsByQuery(db.getSession(), query);
  }
}
