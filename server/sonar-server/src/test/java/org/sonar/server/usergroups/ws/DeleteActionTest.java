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
package org.sonar.server.usergroups.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.internal.AlwaysIncreasingSystem2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
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
  private GroupDto defaultGroup;
  private WsTester ws;

  @Before
  public void setUp() {
    defaultGroup = db.users().insertGroup(db.getDefaultOrganization(), CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
    Settings settings = new MapSettings().setProperty(CoreProperties.CORE_DEFAULT_GROUP, CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);

    ws = new WsTester(new UserGroupsWs(
      new DeleteAction(
        db.getDbClient(),
        userSession,
        newGroupWsSupport(),
        settings, defaultOrganizationProvider)));
  }

  @Test
  public void delete_by_id() throws Exception {
    addAdmin(db.getDefaultOrganization());
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_on_default_organization() throws Exception {
    addAdminToDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_and_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    addAdmin(org);
    GroupDto group = db.users().insertGroup(org, "to-delete");
    loginAsAdmin(org);

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_fails_if_organization_is_not_correct() throws Exception {
    OrganizationDto org = db.organizations().insert();
    loginAsAdmin(org);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'missing'");

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, "missing")
      .setParam(PARAM_GROUP_NAME, "a-group")
      .execute();
  }

  @Test
  public void delete_members() throws Exception {
    addAdminToDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group, user);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertNoContent();

    assertThat(db.countRowsOfTable("groups_users")).isEqualTo(0);
  }

  @Test
  public void delete_permissions() throws Exception {
    addAdminToDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    ComponentDto project = componentTester.insertComponent(ComponentTesting.newProjectDto());
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, project);
    loginAsAdminOnDefaultOrganization();

    newRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertNoContent();

    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(0);
  }

  @Test
  public void delete_group_from_permission_templates() throws Exception {
    addAdminToDefaultOrganization();
    GroupDto group = db.users().insertGroup();
    PermissionTemplateDto template = db.getDbClient().permissionTemplateDao().insert(db.getSession(), PermissionTemplateTesting.newPermissionTemplateDto());
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), template.getId(), group.getId(), "perm");
    db.commit();
    loginAsAdminOnDefaultOrganization();
    assertThat(db.countRowsOfTable("perm_templates_groups")).isEqualTo(1);

    newRequest()
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(db.countRowsOfTable("perm_templates_groups")).isEqualTo(0);
  }

  @Test
  public void fail_if_id_does_not_exist() throws Exception {
    addAdminToDefaultOrganization();
    loginAsAdminOnDefaultOrganization();
    long groupId = defaultGroup.getId() + 123;

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No group with id '" + groupId + "'");

    newRequest()
      .setParam("id", String.valueOf(groupId))
      .execute();
  }

  @Test
  public void cannot_delete_default_group_of_default_organization() throws Exception {
    loginAsAdminOnDefaultOrganization();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Default group 'sonar-users' cannot be deleted");

    newRequest()
      .setParam("id", defaultGroup.getId().toString())
      .execute();
  }

  @Test
  public void delete_group_of_an_organization_even_if_name_is_default_group_of_default_organization() throws Exception {
    OrganizationDto org = db.organizations().insert();
    addAdmin(org);
    GroupDto group = db.users().insertGroup(org, defaultGroup.getName());
    loginAsAdmin(org);

    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.users().selectGroupById(defaultGroup.getId())).isNotNull();
    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void cannot_delete_last_system_admin_group() throws Exception {
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

  @Test
  public void deleting_a_group_of_default_organization_with_admin_permissions_updates_root_flag_of_its_members() throws Exception {
    UserDto rootByUserPermission1 = db.users().insertRootByUserPermission("root1");
    UserDto rootByUserPermission2 = db.users().insertRootByUserPermission("root2");
    UserDto rootByUserPermission3 = db.users().insertRootByUserPermission("root3");
    GroupDto adminGroup1 = db.users().insertAdminGroup(db.getDefaultOrganization());
    GroupDto adminGroup2 = db.users().insertAdminGroup(db.getDefaultOrganization());
    // member of admin group 1
    UserDto rootByGroupPermission1 = db.users().insertRootByGroupPermission(adminGroup1);
    UserDto rootByGroupPermission2 = db.users().insertRootByGroupPermission(adminGroup1);
    db.users().insertMembers(adminGroup1, rootByUserPermission1);
    db.users().insertMembers(adminGroup1, rootByUserPermission3);
    // members of admin group 2
    UserDto rootByGroupPermission3 = db.users().insertRootByGroupPermission(adminGroup2);
    db.users().insertMembers(adminGroup2, rootByUserPermission2);
    db.users().insertMembers(adminGroup2, rootByUserPermission3);
    db.users().insertMembers(adminGroup2, rootByGroupPermission2);
    loginAsAdmin(db.getDefaultOrganization());

    executeDeleteGroupRequest(adminGroup1);
    db.rootFlag().verify(rootByUserPermission1, true);
    db.rootFlag().verifyUnchanged(rootByUserPermission2);
    db.rootFlag().verify(rootByUserPermission3, true);
    db.rootFlag().verify(rootByGroupPermission1, false);
    db.rootFlag().verify(rootByGroupPermission2, true);
    db.rootFlag().verifyUnchanged(rootByGroupPermission3);

    executeDeleteGroupRequest(adminGroup2);
    db.rootFlag().verify(rootByUserPermission1, true);
    db.rootFlag().verify(rootByUserPermission2, true);
    db.rootFlag().verify(rootByUserPermission3, true);
    db.rootFlag().verify(rootByGroupPermission1, false);
    db.rootFlag().verify(rootByGroupPermission2, false);
    db.rootFlag().verify(rootByGroupPermission3, false);
  }

  @Test
  public void deleting_a_group_of_other_organization_with_admin_permissions_does_not_update_root_flag_of_its_members() throws Exception {
    OrganizationDto otherOrganization = db.organizations().insert();
    UserDto rootByUserPermission1 = db.users().insertRootByUserPermission("root1");
    UserDto rootByUserPermission2 = db.users().insertRootByUserPermission("root2");
    UserDto rootByUserPermission3 = db.users().insertRootByUserPermission("root3");
    db.users().insertPermissionOnUser(otherOrganization, rootByUserPermission1, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(otherOrganization, rootByUserPermission2, SYSTEM_ADMIN);
    db.users().insertPermissionOnUser(otherOrganization, rootByUserPermission3, SYSTEM_ADMIN);
    GroupDto adminGroup1 = db.users().insertAdminGroup(otherOrganization);
    GroupDto adminGroup2 = db.users().insertAdminGroup(otherOrganization);
    // member of admin group 1
    UserDto falselyRootByGroupPermission1 = db.users().makeRoot(db.users().insertUser());
    UserDto falselyRootByGroupPermission2 = db.users().makeRoot(db.users().insertUser());
    db.users().insertMembers(adminGroup1, falselyRootByGroupPermission1);
    db.users().insertMembers(adminGroup1, falselyRootByGroupPermission2);
    db.users().insertMembers(adminGroup1, rootByUserPermission1);
    db.users().insertMembers(adminGroup1, rootByUserPermission3);
    // members of admin group 2
    UserDto falselyRootByGroupPermission3 = db.users().makeRoot(db.users().insertUser());
    db.users().insertMembers(adminGroup2, falselyRootByGroupPermission3);
    db.users().insertMembers(adminGroup2, rootByUserPermission2);
    db.users().insertMembers(adminGroup2, rootByUserPermission3);
    db.users().insertMembers(adminGroup2, falselyRootByGroupPermission2);
    loginAsAdmin(otherOrganization);

    executeDeleteGroupRequest(adminGroup1);
    db.rootFlag().verify(rootByUserPermission1, true);
    db.rootFlag().verifyUnchanged(rootByUserPermission2);
    db.rootFlag().verify(rootByUserPermission3, true);
    db.rootFlag().verify(falselyRootByGroupPermission1, false);
    db.rootFlag().verify(falselyRootByGroupPermission2, false);
    db.rootFlag().verifyUnchanged(falselyRootByGroupPermission3);

    executeDeleteGroupRequest(adminGroup2);
    db.rootFlag().verify(rootByUserPermission1, true);
    db.rootFlag().verify(rootByUserPermission2, true);
    db.rootFlag().verify(rootByUserPermission3, true);
    db.rootFlag().verify(falselyRootByGroupPermission1, false);
    db.rootFlag().verify(falselyRootByGroupPermission2, false);
    db.rootFlag().verify(falselyRootByGroupPermission3, false);
  }

  private WsTester.Result executeDeleteGroupRequest(GroupDto adminGroup1) throws Exception {
    return newRequest()
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
    userSession.login().addOrganizationPermission(org.getUuid(), GlobalPermissions.SYSTEM_ADMIN);
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "delete");
  }

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider);
  }

}
