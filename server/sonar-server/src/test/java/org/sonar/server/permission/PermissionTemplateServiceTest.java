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
package org.sonar.server.permission;

import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
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
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class PermissionTemplateServiceTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();
  @Rule
  public DbTester dbTester = DbTester.create(new AlwaysIncreasingSystem2());
  @Rule
  public DefaultTemplatesResolverRule defaultTemplatesResolver = DefaultTemplatesResolverRule.withGovernance();

  private ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private PermissionService permissionService = new PermissionServiceImpl(resourceTypes);

  private UserSessionRule userSession = UserSessionRule.standalone();
  private PermissionTemplateDbTester templateDb = dbTester.permissionTemplates();
  private DbSession session = dbTester.getSession();
  private ProjectIndexers projectIndexers = new TestProjectIndexers();

  private PermissionTemplateService underTest = new PermissionTemplateService(dbTester.getDbClient(), projectIndexers, userSession, defaultTemplatesResolver);

  @Test
  public void apply_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(organization, null, privateProject)).isEmpty();
  }

  @Test
  public void apply_default_does_not_insert_permission_to_group_AnyOne_when_applying_template_on_private_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
    UserDto creator = dbTester.users().insertUser();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, privateProject, creator.getId());

    assertThat(selectProjectPermissionsOfGroup(organization, null, privateProject)).isEmpty();
  }

  @Test
  public void apply_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(organization, null, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_AnyOne_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addAnyoneToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(organization, null, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(privateProject));

    assertThat(selectProjectPermissionsOfGroup(organization, group, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_group_when_applying_template_on_private_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    GroupDto group = dbTester.users().insertGroup(organization);
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, privateProject, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");

    underTest.applyAndCommit(session, permissionTemplate, singletonList(publicProject));

    assertThat(selectProjectPermissionsOfGroup(organization, group, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_group_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, perm));
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, publicProject, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_permissions_to_user_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
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
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, publicProject, null);

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_inserts_any_permissions_to_user_when_applying_template_on_private_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
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
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, perm));
    dbTester.permissionTemplates().addUserToTemplate(permissionTemplate, user, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, privateProject, null);

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_permissions_to_ProjectCreator_but_USER_and_CODEVIEWER_when_applying_template_on_public_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto publicProject = dbTester.components().insertPublicProject(organization);
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, publicProject, user.getId());

    assertThat(selectProjectPermissionsOfUser(user, publicProject))
      .containsOnly("p1", UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void applyDefault_inserts_any_permissions_to_ProjectCreator_when_applying_template_on_private_project() {
    OrganizationDto organization = dbTester.organizations().insert();
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    ComponentDto privateProject = dbTester.components().insertPrivateProject(organization);
    UserDto user = dbTester.users().insertUser();
    permissionService.getAllProjectPermissions()
      .forEach(perm -> dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, perm));
    dbTester.permissionTemplates().addProjectCreatorToTemplate(permissionTemplate, "p1");
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, privateProject, user.getId());

    assertThat(selectProjectPermissionsOfUser(user, privateProject))
      .containsOnly("p1", UserRole.CODEVIEWER, UserRole.USER, UserRole.ADMIN, UserRole.ISSUE_ADMIN, UserRole.SECURITYHOTSPOT_ADMIN, SCAN.getKey());
  }

  @Test
  public void apply_template_on_view() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto view = dbTester.components().insertView(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, view))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_template_on_application() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto view = dbTester.components().insertPublicApplication(organization);
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto appPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, projectPermissionTemplate.getUuid(), appPermissionTemplate.getUuid(), null);

    underTest.applyDefault(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, view))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_template_on_portfolio() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto view = dbTester.components().insertPublicPortfolio(organization);
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto portPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(portPermissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(portPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, projectPermissionTemplate.getUuid(), null, portPermissionTemplate.getUuid());

    underTest.applyDefault(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, view))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_project_default_template_on_view_when_no_view_default_template() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto view = dbTester.components().insertView(organization);
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(projectPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, projectPermissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, view, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, view)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_template_on_applications() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto application = dbTester.components().insertApplication(organization);
    PermissionTemplateDto permissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(permissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, permissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, application))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_default_view_template_on_application() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto application = dbTester.components().insertApplication(organization);
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto appPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    PermissionTemplateDto portPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, ADMINISTER.getKey());
    dbTester.permissionTemplates().addGroupToTemplate(appPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, projectPermissionTemplate.getUuid(), appPermissionTemplate.getUuid(), portPermissionTemplate.getUuid());

    underTest.applyDefault(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, application))
      .containsOnly(ADMINISTER.getKey(), PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_project_default_template_on_application_when_no_application_default_template() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto application = dbTester.components().insertApplication(organization);
    PermissionTemplateDto projectPermissionTemplate = dbTester.permissionTemplates().insertTemplate(organization);
    GroupDto group = dbTester.users().insertGroup(organization);
    dbTester.permissionTemplates().addGroupToTemplate(projectPermissionTemplate, group, PROVISION_PROJECTS.getKey());
    dbTester.organizations().setDefaultTemplates(organization, projectPermissionTemplate.getUuid(), null, null);

    underTest.applyDefault(session, application, null);

    assertThat(selectProjectPermissionsOfGroup(organization, group, application)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void apply_permission_template() {
    OrganizationDto organization = dbTester.organizations().insert();
    UserDto user = dbTester.users().insertUser();
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
    OrganizationDto organization = dbTester.organizations().insert();
    GroupDto group = dbTester.users().insertGroup(organization);
    UserDto user = dbTester.users().insertUser();
    dbTester.users().insertMember(group, user);
    PermissionTemplateDto template = templateDb.insertTemplate(organization);
    dbTester.organizations().setDefaultTemplates(template, null, null);
    templateDb.addProjectCreatorToTemplate(template.getId(), SCAN.getKey());
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
    dbTester.organizations().setDefaultTemplates(dbTester.getDefaultOrganization(), "UNKNOWN_TEMPLATE_UUID", null, null);

    checkWouldUserHaveScanPermission(dbTester.getDefaultOrganization(), null, false);
  }

  @Test
  public void would_user_have_scan_permission_with_empty_template() {
    PermissionTemplateDto template = templateDb.insertTemplate(dbTester.getDefaultOrganization());
    dbTester.organizations().setDefaultTemplates(template, null, null);

    checkWouldUserHaveScanPermission(dbTester.getDefaultOrganization(), null, false);
  }

  private void checkWouldUserHaveScanPermission(OrganizationDto organization, @Nullable Integer userId, boolean expectedResult) {
    assertThat(underTest.wouldUserHaveScanPermissionWithDefaultTemplate(session, organization.getUuid(), userId, "PROJECT_KEY"))
      .isEqualTo(expectedResult);
  }

}
