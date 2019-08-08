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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;

public class DeleteActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private ComponentDbTester componentTester = new ComponentDbTester(db);
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private WsActionTester ws = new WsActionTester(new DeleteAction(db.getDbClient(), userSession, newGroupWsSupport()));

  @Test
  public void response_has_no_content() {
    addAdmin(db.getDefaultOrganization());
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    TestResponse response = newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void delete_by_id() {
    addAdmin(db.getDefaultOrganization());
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_on_default_organization() {
    addAdminToDefaultOrganization();
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_and_organization() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertDefaultGroup(org);
    addAdmin(org);
    GroupDto group = db.users().insertGroup(org, "to-delete");
    loginAsAdmin(org);

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_fails_if_organization_is_not_correct() {
    OrganizationDto org = db.organizations().insert();
    db.users().insertDefaultGroup(org);
    loginAsAdmin(org);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'missing'");

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, "missing")
      .setParam(PARAM_GROUP_NAME, "a-group")
      .execute();
  }

  @Test
  public void delete_members() {
    addAdminToDefaultOrganization();
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group, user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.countRowsOfTable("groups_users")).isEqualTo(0);
  }

  @Test
  public void delete_permissions() {
    addAdminToDefaultOrganization();
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    ComponentDto project = componentTester.insertComponent(ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()));
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, project);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(0);
  }

  @Test
  public void delete_group_from_permission_templates() {
    addAdminToDefaultOrganization();
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    PermissionTemplateDto template = db.getDbClient().permissionTemplateDao().insert(db.getSession(), PermissionTemplateTesting.newPermissionTemplateDto());
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), template.getId(), group.getId(), "perm");
    db.commit();
    loginAsAdminOnDefaultOrganization();
    assertThat(db.countRowsOfTable("perm_templates_groups")).isEqualTo(1);

    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.countRowsOfTable("perm_templates_groups")).isEqualTo(0);
  }

  @Test
  public void delete_qprofile_permissions() {
    addAdminToDefaultOrganization();
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    QProfileDto profile = db.qualityProfiles().insert(db.getDefaultOrganization());
    db.qualityProfiles().addGroupPermission(profile, group);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.countRowsOfTable("qprofile_edit_groups")).isZero();
  }

  @Test
  public void fail_if_id_does_not_exist() {
    addAdminToDefaultOrganization();
    loginAsAdminOnDefaultOrganization();
    int groupId = 123;

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '" + groupId + "'");

    newRequest()
      .setParam("id", String.valueOf(groupId))
      .execute();
  }

  @Test
  public void fail_to_delete_default_group_of_default_organization() {
    loginAsAdminOnDefaultOrganization();
    GroupDto defaultGroup = db.users().insertDefaultGroup(db.getDefaultOrganization(), "default");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default group 'default' cannot be used to perform this action");

    newRequest()
      .setParam("id", defaultGroup.getId().toString())
      .execute();
  }

  @Test
  public void fail_to_delete_group_of_a_none_default_organization() {
    OrganizationDto org = db.organizations().insert();
    GroupDto defaultGroup = db.users().insertDefaultGroup(org, "default");
    addAdmin(org);
    loginAsAdmin(org);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default group 'default' cannot be used to perform this action");

    newRequest()
      .setParam("id", defaultGroup.getId().toString())
      .execute();
  }

  @Test
  public void cannot_delete_last_system_admin_group() {
    insertDefaultGroupOnDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    db.users().insertPermissionOnGroup(group, SYSTEM_ADMIN);
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The last system admin group cannot be deleted");

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();
  }

  @Test
  public void delete_admin_group_fails_if_no_admin_users_left() throws Exception {
    // admin users are part of the group to be deleted
    OrganizationDto org = db.organizations().insert();
    db.users().insertDefaultGroup(org);
    GroupDto adminGroup = db.users().insertGroup(org, "admins");
    db.users().insertPermissionOnGroup(adminGroup, SYSTEM_ADMIN);
    UserDto bigBoss = db.users().insertUser();
    db.users().insertMember(adminGroup, bigBoss);
    loginAsAdmin(org);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The last system admin group cannot be deleted");

    executeDeleteGroupRequest(adminGroup);
  }

  @Test
  public void delete_admin_group_succeeds_if_other_groups_have_administrators() throws Exception {
    OrganizationDto org = db.organizations().insert();
    db.users().insertDefaultGroup(org);
    GroupDto adminGroup1 = db.users().insertGroup(org, "admins");
    db.users().insertPermissionOnGroup(adminGroup1, SYSTEM_ADMIN);
    GroupDto adminGroup2 = db.users().insertGroup(org, "admins");
    db.users().insertPermissionOnGroup(adminGroup2, SYSTEM_ADMIN);
    UserDto bigBoss = db.users().insertUser();
    db.users().insertMember(adminGroup2, bigBoss);
    loginAsAdmin(org);

    executeDeleteGroupRequest(adminGroup1);

    assertThat(db.users().selectGroupPermissions(adminGroup2, null)).hasSize(1);
  }

  private void executeDeleteGroupRequest(GroupDto adminGroup1) {
    newRequest()
      .setParam(PARAM_GROUP_ID, adminGroup1.getId().toString())
      .execute();
  }

  private void addAdminToDefaultOrganization() {
    addAdmin(db.getDefaultOrganization());
  }

  private void addAdmin(OrganizationDto org) {
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(org, admin, SYSTEM_ADMIN);
  }

  private void loginAsAdminOnDefaultOrganization() {
    loginAsAdmin(db.getDefaultOrganization());
  }

  private void loginAsAdmin(OrganizationDto org) {
    userSession.logIn().addPermission(ADMINISTER, org);
  }

  private void insertDefaultGroupOnDefaultOrganization() {
    db.users().insertDefaultGroup(db.getDefaultOrganization());
  }

  private TestRequest newRequest() {
    return ws.newRequest();
  }

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider, new DefaultGroupFinder(db.getDbClient()));
  }

}
