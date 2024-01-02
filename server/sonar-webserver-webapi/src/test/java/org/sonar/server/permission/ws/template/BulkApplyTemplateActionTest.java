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

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.DefaultTemplatesResolver;
import org.sonar.server.permission.DefaultTemplatesResolverImpl;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.ws.BasePermissionWsTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.db.component.ComponentTesting.newApplication;
import static org.sonar.db.component.ComponentTesting.newPortfolio;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ANALYZED_BEFORE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_ON_PROVISIONED_ONLY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class BulkApplyTemplateActionTest extends BasePermissionWsTest<BulkApplyTemplateAction> {

  private UserDto user1;
  private UserDto user2;
  private GroupDto group1;
  private GroupDto group2;
  private PermissionTemplateDto template1;
  private final ProjectIndexers projectIndexers = new TestProjectIndexers();
  private final ResourceTypesRule resourceTypesRule = new ResourceTypesRule().setRootQualifiers(PROJECT, VIEW, APP);
  private final DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(db.getDbClient(), resourceTypesRule);

  @Override
  protected BulkApplyTemplateAction buildWsAction() {
    PermissionTemplateService permissionTemplateService = new PermissionTemplateService(db.getDbClient(),
      projectIndexers, userSession, defaultTemplatesResolver, new SequenceUuidFactory());
    return new BulkApplyTemplateAction(db.getDbClient(), userSession, permissionTemplateService, newPermissionWsSupport(), new I18nRule(), newRootResourceTypes());
  }

  @Before
  public void setUp() {
    user1 = db.users().insertUser();
    user2 = db.users().insertUser();
    group1 = db.users().insertGroup();
    group2 = db.users().insertGroup();

    // template 1 for org 1
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
  }

  @Test
  public void bulk_apply_template_by_template_uuid() {
    // this project should not be applied the template
    db.components().insertPrivateProject();

    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto publicProject = db.components().insertPublicProject();
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
      .hasMessage("'projects' can contains only 1000 values, got 1001");
  }

  @Test
  public void bulk_apply_template_by_template_name() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto publicProject = db.components().insertPublicProject();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_NAME, template1.getName())
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
  }

  @Test
  public void apply_template_by_qualifiers() {
    ComponentDto publicProject = db.components().insertPublicProject();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto view = db.components().insertComponent(newPortfolio());
    ComponentDto application = db.components().insertComponent(newApplication());
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_QUALIFIERS, String.join(",", Qualifiers.PROJECT, Qualifiers.APP))
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject);
    assertTemplate1AppliedToPublicProject(publicProject);
    assertTemplate1AppliedToPublicProject(application);
    assertNoPermissionOnProject(view);
  }

  @Test
  public void apply_template_by_query_on_name_and_key_public_project() {
    ComponentDto publicProjectFoundByKey = ComponentTesting.newPublicProjectDto().setKey("sonar");
    db.components().insertProjectAndSnapshot(publicProjectFoundByKey);
    ComponentDto publicProjectFoundByName = ComponentTesting.newPublicProjectDto().setName("name-sonar-name");
    db.components().insertProjectAndSnapshot(publicProjectFoundByName);
    ComponentDto projectUntouched = ComponentTesting.newPublicProjectDto().setKey("new-sona").setName("project-name");
    db.components().insertProjectAndSnapshot(projectUntouched);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "SONAR")
      .execute();

    assertTemplate1AppliedToPublicProject(publicProjectFoundByKey);
    assertTemplate1AppliedToPublicProject(publicProjectFoundByName);
    assertNoPermissionOnProject(projectUntouched);
  }

  @Test
  public void apply_template_by_query_on_name_and_key() {
    // partial match on key
    ComponentDto privateProjectFoundByKey = ComponentTesting.newPrivateProjectDto().setKey("sonarqube");
    db.components().insertProjectAndSnapshot(privateProjectFoundByKey);
    ComponentDto privateProjectFoundByName = ComponentTesting.newPrivateProjectDto().setName("name-sonar-name");
    db.components().insertProjectAndSnapshot(privateProjectFoundByName);
    ComponentDto projectUntouched = ComponentTesting.newPublicProjectDto().setKey("new-sona").setName("project-name");
    db.components().insertProjectAndSnapshot(projectUntouched);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(Param.TEXT_QUERY, "SONAR")
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProjectFoundByKey);
    assertTemplate1AppliedToPrivateProject(privateProjectFoundByName);
    assertNoPermissionOnProject(projectUntouched);
  }

  @Test
  public void apply_template_by_project_keys() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto untouchedProject = db.components().insertPrivateProject();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_PROJECTS, String.join(",", project1.getKey(), project2.getKey()))
      .execute();

    assertTemplate1AppliedToPrivateProject(project1);
    assertTemplate1AppliedToPrivateProject(project2);
    assertNoPermissionOnProject(untouchedProject);
  }

  @Test
  public void apply_template_by_provisioned_only() {
    ComponentDto provisionedProject1 = db.components().insertPrivateProject();
    ComponentDto provisionedProject2 = db.components().insertPrivateProject();
    ComponentDto analyzedProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(analyzedProject));
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_ON_PROVISIONED_ONLY, "true")
      .execute();

    assertTemplate1AppliedToPrivateProject(provisionedProject1);
    assertTemplate1AppliedToPrivateProject(provisionedProject2);
    assertNoPermissionOnProject(analyzedProject);
  }

  @Test
  public void apply_template_by_analyzed_before() {
    ComponentDto oldProject1 = db.components().insertPrivateProject();
    ComponentDto oldProject2 = db.components().insertPrivateProject();
    ComponentDto recentProject = db.components().insertPrivateProject();
    db.components().insertSnapshot(oldProject1, a -> a.setCreatedAt(parseDate("2015-02-03").getTime()));
    db.components().insertSnapshot(oldProject2, a -> a.setCreatedAt(parseDate("2016-12-11").getTime()));
    db.components().insertSnapshot(recentProject, a -> a.setCreatedAt(System.currentTimeMillis()));
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_ANALYZED_BEFORE, "2017-09-07")
      .execute();

    assertTemplate1AppliedToPrivateProject(oldProject1);
    assertTemplate1AppliedToPrivateProject(oldProject2);
    assertNoPermissionOnProject(recentProject);
  }

  @Test
  public void apply_template_by_visibility() {
    ComponentDto privateProject1 = db.components().insertPrivateProject();
    ComponentDto privateProject2 = db.components().insertPrivateProject();
    ComponentDto publicProject = db.components().insertPublicProject();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_TEMPLATE_ID, template1.getUuid())
      .setParam(PARAM_VISIBILITY, "private")
      .execute();

    assertTemplate1AppliedToPrivateProject(privateProject1);
    assertTemplate1AppliedToPrivateProject(privateProject2);
    assertNoPermissionOnProject(publicProject);
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

  private void assertTemplate1AppliedToPublicProject(ComponentDto project) {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).containsExactly(user2.getUuid());
  }

  private void assertTemplate1AppliedToPrivateProject(ComponentDto project) {
    assertThat(selectProjectPermissionGroups(project, UserRole.ADMIN)).containsExactly(group1.getName());
    assertThat(selectProjectPermissionGroups(project, UserRole.USER)).containsExactly(group2.getName());
    assertThat(selectProjectPermissionUsers(project, UserRole.ADMIN)).isEmpty();
    assertThat(selectProjectPermissionUsers(project, UserRole.CODEVIEWER)).containsExactly(user1.getUuid());
    assertThat(selectProjectPermissionUsers(project, UserRole.ISSUE_ADMIN)).containsExactly(user2.getUuid());
  }

  private void assertNoPermissionOnProject(ComponentDto project) {
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
    db.getDbClient().permissionTemplateDao().insertUserPermission(db.getSession(), permissionTemplate.getUuid(), user.getUuid(),
      permission, permissionTemplate.getName(), user.getLogin());
    db.commit();
  }

  private void addGroupToTemplate(GroupDto group, PermissionTemplateDto permissionTemplate, String permission) {
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), permissionTemplate.getUuid(), group.getUuid(),
      permission, permissionTemplate.getName(), group.getName());
    db.commit();
  }

  private List<String> selectProjectPermissionGroups(ComponentDto project, String permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setComponent(project).build();
    return db.getDbClient().groupPermissionDao().selectGroupNamesByQuery(db.getSession(), query);
  }

  private List<String> selectProjectPermissionUsers(ComponentDto project, String permission) {
    PermissionQuery query = PermissionQuery.builder().setPermission(permission).setComponent(project).build();
    return db.getDbClient().userPermissionDao().selectUserUuidsByQuery(db.getSession(), query);
  }
}
