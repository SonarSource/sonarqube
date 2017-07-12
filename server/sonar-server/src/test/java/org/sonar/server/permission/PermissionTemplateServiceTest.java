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
package org.sonar.server.permission;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDbTester;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.ProjectIndexers;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.permission.ws.template.DefaultTemplatesResolverRule;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;

public class PermissionTemplateServiceTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public DefaultTemplatesResolverRule defaultTemplatesResolver = DefaultTemplatesResolverRule.withGovernance();

  private UserSessionRule userSession = UserSessionRule.standalone();
  private PermissionTemplateDbTester templateDb = dbTester.permissionTemplates();
  private DbSession session = dbTester.getSession();
  private ProjectIndexers projectIndexers = new TestProjectIndexers();

  private OrganizationDto organization;
  private ComponentDto privateProject;
  private ComponentDto publicProject;
  private GroupDto group;
  private UserDto user;
  private UserDto creator;

  private PermissionTemplateService underTest = new PermissionTemplateService(dbTester.getDbClient(), projectIndexers, userSession, defaultTemplatesResolver);

  @Before
  public void setUp() throws Exception {
    organization = dbTester.organizations().insert();
    privateProject = dbTester.components().insertPrivateProject(organization);
    publicProject = dbTester.components().insertPublicProject(organization);
    group = dbTester.users().insertGroup(organization);
    user = dbTester.users().insertUser();
    creator = dbTester.users().insertUser();
  }

  @Test
  public void apply_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(organization, null, privateProject)).isEmpty();
  }

  @Test
  public void apply_default_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), privateProject, creator.getId());

    assertThat(selectProjectPermissionsOfGroup(organization, null, privateProject)).isEmpty();
  }

  @Test
  public void apply_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(organization, null, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(organization, null, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void apply_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(organization, group, privateProject))
      .containsOnly("p1", UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), privateProject, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, privateProject))
      .containsOnly("p1", UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void apply_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(organization, group, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void apply_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), publicProject, null);

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void apply_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), privateProject, null);

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_permissions_to_ProjectCreator_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
        .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), publicProject, user.getId());

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
        .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_ProjectCreator_when_applying_template_on_private_project() {
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ProjectPermissions.ALL
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null);

    underTest.applyDefault(session, organization.getUuid(), privateProject, user.getId());

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.USER, UserRole.CODEVIEWER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, GlobalPermissions.SCAN_EXECUTION);
  }

  @Test
  public void apply_permission_template() {
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    GroupDto adminGroup = dbTester.users().insertGroup(organization);
    GroupDto userGroup = dbTester.users().insertGroup(organization);
    dbTester.users().insertPermissionOnGroup(adminGroup, "admin");
    dbTester.users().insertPermissionOnGroup(userGroup, "user");
    dbTester.users().insertPermissionOnUser(organization, user, "admin");
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, adminGroup, "admin");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, adminGroup, "issueadmin");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, userGroup, "user");
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, userGroup, "codeviewer");
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "user");
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "codeviewer");
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "admin");

    assertThat(selectProjectPermissionsOfGroup(organization, adminGroup, project)).isEmpty();
    assertThat(selectProjectPermissionsOfGroup(organization, userGroup, project)).isEmpty();
    assertThat(selectProjectPermissionsOfGroup(organization, null, project)).isEmpty();
    assertThat(selectProjectPermissionsOfUser(user, project)).isEmpty();

    underTest.applyAndCommit(session, permissionTemplate, singletonList(project));

    assertThat(selectProjectPermissionsOfGroup(organization, adminGroup, project)).containsOnly("admin", "issueadmin");
    assertThat(selectProjectPermissionsOfGroup(organization, userGroup, project)).containsOnly("user", "codeviewer");
    assertThat(selectProjectPermissionsOfGroup(organization, null, project)).isEmpty();
    assertThat(selectProjectPermissionsOfUser(user, project)).containsOnly("admin");

    checkAuthorizationUpdatedAtIsUpdated(project);
  }

  private List<String> selectProjectPermissionsOfGroup(OrganizationDto organizationDto, @Nullable GroupDto groupDto, ComponentDto project) {
    return dbTester.getDbClient().groupPermissionDao().selectProjectPermissionsOfGroup(session,
      organizationDto.getUuid(), groupDto != null ? groupDto.getId() : null, project.getId());
  }

  private List<String> selectProjectPermissionsOfUser(UserDto userDto, ComponentDto project) {
    return dbTester.getDbClient().userPermissionDao().selectProjectPermissionsOfUser(session,
      userDto.getId(), project.getId());
  }

  @Test
  public void would_user_have_scan_permission_with_default_permission_template() {
    dbTester.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate(organization);
    dbTester.organizations().setDefaultTemplates(template, null);
    templateDb.addProjectCreatorToTemplate(template.getId(), SCAN_EXECUTION);
    templateDb.addUserToTemplate(template.getId(), user.getId(), UserRole.USER);
    templateDb.addGroupToTemplate(template.getId(), group.getId(), UserRole.CODEVIEWER);
    templateDb.addGroupToTemplate(template.getId(), null, UserRole.ISSUE_ADMIN);

    // authenticated user
    checkWouldUserHaveScanPermission(organization, user.getId(), true);

    // anonymous user
    checkWouldUserHaveScanPermission(organization, null, false);
  }

  @Test
  public void would_user_have_scan_permission_with_unknown_default_permission_template() {
    dbTester.organizations().setDefaultTemplates(dbTester.getDefaultOrganization(), "UNKNOWN_TEMPLATE_UUID", null);

    checkWouldUserHaveScanPermission(dbTester.getDefaultOrganization(), null, false);
  }

  @Test
  public void would_user_have_scann_permission_with_empty_template() {
    PermissionTemplateDto template = templateDb.insertTemplate(dbTester.getDefaultOrganization());
    dbTester.organizations().setDefaultTemplates(template, null);

    checkWouldUserHaveScanPermission(dbTester.getDefaultOrganization(), null, false);
  }

  private void checkWouldUserHaveScanPermission(OrganizationDto organization, @Nullable Integer userId, boolean expectedResult) {
    assertThat(underTest.wouldUserHaveScanPermissionWithDefaultTemplate(session, organization.getUuid(), userId, null, "PROJECT_KEY", Qualifiers.PROJECT))
      .isEqualTo(expectedResult);
  }

  private void checkAuthorizationUpdatedAtIsUpdated(ComponentDto project) {
    assertThat(dbTester.getDbClient().componentDao().selectOrFailById(session, project.getId()).getAuthorizationUpdatedAt())
      .isNotNull();
  }

}
