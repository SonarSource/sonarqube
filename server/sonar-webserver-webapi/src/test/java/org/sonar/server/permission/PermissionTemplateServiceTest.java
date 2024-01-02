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
package org.sonar.server.permission;

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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.template.PermissionTemplateDbTester;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.TemplateMatchingKeyException;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class PermissionTemplateServiceTest {

  @Rule
  public DbTester dbTester = DbTester.create();

  private final ResourceTypesRule resourceTypesRule = new ResourceTypesRule().setRootQualifiers(PROJECT, VIEW, APP);
  private final DefaultTemplatesResolver defaultTemplatesResolver = new DefaultTemplatesResolverImpl(dbTester.getDbClient(), resourceTypesRule);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypesRule);
  private final UserSessionRule userSession = UserSessionRule.standalone();
  private final PermissionTemplateDbTester templateDb = dbTester.permissionTemplates();
  private final DbSession session = dbTester.getSession();
  private final ProjectIndexers projectIndexers = new TestProjectIndexers();
  private final PermissionTemplateService underTest = new PermissionTemplateService(dbTester.getDbClient(), projectIndexers, userSession, defaultTemplatesResolver,
    new SequenceUuidFactory());
  private ComponentDto privateProject;

  @Test
  public void apply_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(null, privateProject)).isEmpty();
  }

  @Test
  public void apply_default_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    UserDto creator = dbTester.users().insertUser();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, creator.getUuid());

    assertThat(selectProjectPermissionsOfGroup(null, privateProject)).isEmpty();
  }

  @Test
  public void apply_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(null, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(null, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    GroupDto group = dbTester.users().insertGroup();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(group, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    GroupDto group = dbTester.users().insertGroup();
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, null);

    assertThat(selectProjectPermissionsOfGroup(group, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    GroupDto group = dbTester.users().insertGroup();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(group, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    GroupDto group = dbTester.users().insertGroup();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(group, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, null);

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, null);

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_ProjectCreator_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto publicProject = dbTester.components().insertPublicProject();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, publicProject, user.getUuid());

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_ProjectCreator_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    ComponentDto privateProject = dbTester.components().insertPrivateProject();
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, privateProject, user.getUuid());

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_template_on_view() {
    ComponentDto portfolio = dbTester.components().insertPrivatePortfolio();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, portfolio, null);

    assertThat(selectProjectPermissionsOfGroup(group, portfolio))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_template_on_application() {
    ComponentDto view = dbTester.components().insertPublicApplication();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto appPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, appPermissionTemplate, null);

    underTest.applyDefaultToNewComponent(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(group, view))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_template_on_portfolio() {
    ComponentDto view = dbTester.components().insertPublicPortfolio();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto portPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(portPermissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(portPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, portPermissionTemplate);

    underTest.applyDefaultToNewComponent(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(group, view))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_project_default_template_on_view_when_no_view_default_template() {
    ComponentDto view = dbTester.components().insertPrivatePortfolio();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(projectPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(group, view)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_template_on_applications() {
    ComponentDto application = dbTester.components().insertPublicApplication();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(permissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_view_template_on_application() {
    ComponentDto application = dbTester.components().insertPublicApplication();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto appPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    PermissionTemplateDto portPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, appPermissionTemplate, portPermissionTemplate);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_project_default_template_on_application_when_no_application_default_template() {
    ComponentDto application = dbTester.components().insertPublicApplication();
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate();
    GroupDto group = dbTester.users().insertGroup();
    dbTester.permissionTemplates().addGroupToTemplate(projectPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.permissionTemplates().setDefaultTemplates(projectPermissionTemplate, null, null);

    underTest.applyDefaultToNewComponent(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(group, application)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_permission_template() {
    UserDto user = dbTester.users().insertUser();
    ComponentDto project = dbTester.components().insertPrivateProject();
    GroupDto adminGroup = dbTester.users().insertGroup();
    GroupDto userGroup = dbTester.users().insertGroup();
    dbTester.users().insertPermissionOnGroup(adminGroup, "admin");
    dbTester.users().insertPermissionOnGroup(userGroup, "user");
    dbTester.users().insertPermissionOnUser(user, "admin");
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate();
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, adminGroup, "admin");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, adminGroup, "issueadmin");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, userGroup, "user");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, userGroup, "codeviewer");
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "user");
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "codeviewer");
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "admin");

    assertThat(selectProjectPermissionsOfGroup(adminGroup, project)).isEmpty();
    assertThat(selectProjectPermissionsOfGroup(userGroup, project)).isEmpty();
    assertThat(selectProjectPermissionsOfGroup(null, project)).isEmpty();
    assertThat(selectProjectPermissionsOfUser(user, project)).isEmpty();

    underTest.applyAndCommit(session, permissionTemplate, singletonList(project));

    assertThat(selectProjectPermissionsOfGroup(adminGroup, project)).containsOnly("admin", "issueadmin");
    assertThat(selectProjectPermissionsOfGroup(userGroup, project)).containsOnly("user", "codeviewer");
    assertThat(selectProjectPermissionsOfGroup(null, project)).isEmpty();
    assertThat(selectProjectPermissionsOfUser(user, project)).containsOnly("admin");
  }

  private List<String> selectProjectPermissionsOfGroup(@Nullable GroupDto groupDto, ComponentDto project) {
    return dbTester.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(session, groupDto != null ? groupDto.getUuid() : null, project.uuid());
  }

  private List<String> selectProjectPermissionsOfUser(UserDto userDto, ComponentDto project) {
    return dbTester.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(session,
      userDto.getUuid(), project.uuid());
  }

  @Test
  public void would_user_have_scan_permission_with_default_permission_template() {
    GroupDto group = dbTester.users().insertGroup();
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate();
    dbTester.permissionTemplates().setDefaultTemplates(template, null, null);
    templateDb.addProjectCreatorToTemplate(template.getUuid(), SCAN.getKey(), template.getName());
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

    ComponentDto project = dbTester.components().insertPrivateProject(p -> p.setKey(key));

    assertThatThrownBy(() -> underTest.applyDefaultToNewComponent(session, project, null))
      .isInstanceOf(TemplateMatchingKeyException.class)
      .hasMessageContaining("The \"%s\" key matches multiple permission templates: %s.", key, templateNames);
  }

  private void checkWouldUserHaveScanPermission(@Nullable String userUuid, boolean expectedResult) {
    assertThat(underTest.wouldUserHaveScanPermissionWithDefaultTemplate(session, userUuid, "PROJECT_KEY"))
      .isEqualTo(expectedResult);
  }

}
