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
package org.sonar.server.common.permission;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDbTester;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.TemplateMatchingKeyException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;

public class PermissionTemplateServiceIT {

  @Rule
  public DbTester dbTester = DbTester.create();

  private final ComponentTypesRule resourceTypesRule = new ComponentTypesRule().setRootQualifiers(PROJECT, VIEW, APP);
  private final DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(dbTester.getDbClient(), resourceTypesRule);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypesRule);
  private final UserSessionRule userSession = UserSessionRule.standalone();
  private final PermissionTemplateDbTester templateDb = dbTester.permissionTemplates();
  private final DbSession session = dbTester.getSession();
  private final Indexers indexers = new TestIndexers();
  private final PermissionTemplateService underTest = new PermissionTemplateService(dbTester.getDbClient(), indexers, userSession, defaultTemplatesResolver,
    new SequenceUuidFactory());

  @Test
  public void apply_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(null, privateProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_default_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    UserDto creator = dbTester.users().insertUser();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, creator.getUuid());

    assertThat(selectProjectPermissionsOfGroup(null, privateProject.getUuid())).isEmpty();
  }

  @Test
  public void apply_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(null, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(null, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    GroupDto group = dbTester.users().insertGroup();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(group, privateProject.getUuid()))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    GroupDto group = dbTester.users().insertGroup();
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, null);

    assertThat(selectProjectPermissionsOfGroup(group, privateProject.getUuid()))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    GroupDto group = dbTester.users().insertGroup();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(group, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    GroupDto group = dbTester.users().insertGroup();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(group, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfUser(user, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, null);

    assertThat(selectProjectPermissionsOfUser(user, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfUser(user, privateProject.getUuid()))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, null);

    assertThat(selectProjectPermissionsOfUser(user, privateProject.getUuid()))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_ProjectCreator_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto publicProject = dbTester.components().insertPublicProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, user.getUuid());

    assertThat(selectProjectPermissionsOfUser(user, publicProject.getUuid()))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_ProjectCreator_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ProjectDto privateProject = dbTester.components().insertPrivateProject().getProjectDto();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, user.getUuid());

    assertThat(selectProjectPermissionsOfUser(user, privateProject.getUuid()))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, GlobalPermission.SCAN.getKey());
  }

  @Test
  public void apply_template_on_view() {
    PortfolioDto portfolio = dbTester.components().insertPrivatePortfolioDto();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, GlobalPermission.ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, portfolio, null);

    assertThat(selectProjectPermissionsOfGroup(group, portfolio.getUuid()))
      .containsOnly(GlobalPermission.ADMINISTER.getKey(), GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_template_on_application() {
    ProjectDto application = dbTester.components().insertPublicApplication().getProjectDto();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto appPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, GlobalPermission.ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, appPermissionTemplate, null);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application.getUuid()))
      .containsOnly(GlobalPermission.ADMINISTER.getKey(), GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_template_on_portfolio() {
    PortfolioDto portfolio = dbTester.components().insertPublicPortfolioDto();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto portPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(portPermissionTemplate, group, GlobalPermission.ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(portPermissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, portPermissionTemplate);

    underTest.applyDefaultToNewComponent(session, portfolio, null);

    assertThat(selectProjectPermissionsOfGroup(group, portfolio.getUuid()))
      .containsOnly(GlobalPermission.ADMINISTER.getKey(), GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_project_default_template_on_view_when_no_view_default_template() {
    PortfolioDto portfolio = dbTester.components().insertPrivatePortfolioDto();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(projectPermissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, portfolio, null);

    assertThat(selectProjectPermissionsOfGroup(group, portfolio.getUuid())).containsOnly(GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_template_on_applications() {
    ProjectDto application = dbTester.components().insertPublicApplication().getProjectDto();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, GlobalPermission.ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application.getUuid()))
      .containsOnly(GlobalPermission.ADMINISTER.getKey(), GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_view_template_on_application() {
    ProjectDto application = dbTester.components().insertPublicApplication().getProjectDto();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto appPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto portPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, GlobalPermission.ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, appPermissionTemplate, portPermissionTemplate);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application.getUuid()))
      .containsOnly(GlobalPermission.ADMINISTER.getKey(), GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_project_default_template_on_application_when_no_application_default_template() {
    ProjectDto application = dbTester.components().insertPublicApplication().getProjectDto();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(projectPermissionTemplate, group, GlobalPermission.PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application.getUuid())).containsOnly(GlobalPermission.PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_permission_template() {
    UserDto user = dbTester.users().insertUser();
    ProjectDto project = dbTester.components().insertPrivateProject().getProjectDto();
    GroupDto adminGroup = dbTester.users().insertGroup();
    GroupDto userGroup = dbTester.users().insertGroup();
    dbTester.users().insertPermissionOnGroup(adminGroup, GlobalPermission.ADMINISTER.getKey());
    dbTester.users().insertPermissionOnGroup(userGroup, UserRole.USER);
    dbTester.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, adminGroup, GlobalPermission.ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, adminGroup, UserRole.ISSUE_ADMIN);
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, userGroup, UserRole.USER);
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, userGroup, UserRole.CODEVIEWER);
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, UserRole.USER);
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, UserRole.CODEVIEWER);
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, GlobalPermission.ADMINISTER.getKey());

    assertThat(selectProjectPermissionsOfGroup(adminGroup, project.getUuid())).isEmpty();
    assertThat(selectProjectPermissionsOfGroup(userGroup, project.getUuid())).isEmpty();
    assertThat(selectProjectPermissionsOfGroup(null, project.getUuid())).isEmpty();
    assertThat(selectProjectPermissionsOfUser(user, project.getUuid())).isEmpty();

    underTest.applyAndCommit(session, permissionTemplate, singletonList(project));

    assertThat(selectProjectPermissionsOfGroup(adminGroup, project.getUuid())).containsOnly(GlobalPermission.ADMINISTER.getKey(), UserRole.ISSUE_ADMIN);
    assertThat(selectProjectPermissionsOfGroup(userGroup, project.getUuid())).containsOnly(UserRole.USER, UserRole.CODEVIEWER);
    assertThat(selectProjectPermissionsOfGroup(null, project.getUuid())).isEmpty();
    assertThat(selectProjectPermissionsOfUser(user, project.getUuid())).containsOnly(GlobalPermission.ADMINISTER.getKey());
  }

  private List<String> selectProjectPermissionsOfGroup(@Nullable GroupDto groupDto, String projectUuid) {
    return dbTester.getDbClient().groupPermissionDao().selectEntityPermissionsOfGroup(session, groupDto != null ? groupDto.getUuid() : null, projectUuid);
  }

  private List<String> selectProjectPermissionsOfUser(UserDto userDto, String projectUuid) {
    return dbTester.getDbClient().userPermissionDao().selectEntityPermissionsOfUser(session, userDto.getUuid(), projectUuid);
  }

  @Test
  public void would_user_have_scan_permission_with_default_permission_template() {
    GroupDto group = dbTester.users().insertGroup();
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate();
    dbTester.permissionTemplates().setDefaultTemplates(template, null, null);
    templateDb.addProjectCreatorToTemplate(template.getUuid(), GlobalPermission.SCAN.getKey(), template.getName());
    templateDb.addUserToTemplate(template.getUuid(), user.getUuid(), UserRole.USER, template.getName(), user.getLogin());
    templateDb.addGroupToTemplate(template.getUuid(), group.getUuid(), UserRole.CODEVIEWER, template.getName(), group.getName());
    templateDb.addGroupToTemplate(template.getUuid(), null, UserRole.ISSUE_ADMIN, template.getName(), null);

    // authenticated user
    checkWouldUserHaveScanPermission(user.getUuid(), true);

    // anonymous user
    checkWouldUserHaveScanPermission(null, false);
  }

  @Test
  public void would_user_have_scan_permission_with_unknown_default_permission_template() {
    dbTester.permissionTemplates().setDefaultTemplates("UNKNOWN_TEMPLATE_UUID", null, null);

    checkWouldUserHaveScanPermission(null, false);
  }

  @Test
  public void would_user_have_scan_permission_with_empty_template() {
    PermissionTemplateDto template = templateDb.insertTemplate();
    dbTester.permissionTemplates().setDefaultTemplates(template, null, null);

    checkWouldUserHaveScanPermission(null, false);
  }

  @Test
  public void apply_permission_template_with_key_pattern_collision() {
    final String key = "hi-test";
    final String keyPattern = ".*-test";

    Stream<PermissionTemplateDto> templates = Stream.of(
      templateDb.insertTemplate(t -> t.setKeyPattern(keyPattern)),
      templateDb.insertTemplate(t -> t.setKeyPattern(keyPattern))
    );

    String templateNames = templates
      .map(PermissionTemplateDto::getName)
      .sorted(String.CASE_INSENSITIVE_ORDER)
      .map(x -> String.format("\"%s\"", x))
      .collect(Collectors.joining(", "));

    ProjectDto project = dbTester.components().insertPrivateProject(p -> p.setKey(key)).getProjectDto();

    assertThatThrownBy(() -> underTest.applyDefaultToNewComponent(session, project, null))
      .isInstanceOf(TemplateMatchingKeyException.class)
      .hasMessageContaining("The \"%s\" key matches multiple permission templates: %s.", key, templateNames);
  }

  private void checkWouldUserHaveScanPermission(@Nullable String userUuid, boolean expectedResult) {
    assertThat(underTest.wouldUserHaveScanPermissionWithDefaultTemplate(session, userUuid, "PROJECT_KEY"))
      .isEqualTo(expectedResult);
  }

}
