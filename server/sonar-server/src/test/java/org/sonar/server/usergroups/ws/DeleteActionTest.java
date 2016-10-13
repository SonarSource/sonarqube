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
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProviderRule;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;

public class DeleteActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentDbTester componentTester = new ComponentDbTester(db);
  private DefaultOrganizationProviderRule defaultOrganizationProvider = DefaultOrganizationProviderRule.create(db);
  private GroupDto defaultGroup;
  private WsTester ws;

  @Before
  public void setUp() {
    defaultGroup = db.users().insertGroup(defaultOrganizationProvider.getDto(), CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE);
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
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "to-delete");

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_on_default_organization() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "to-delete");

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_and_organization() throws Exception {
    OrganizationDto org = OrganizationTesting.insert(db, newOrganizationDto());
    GroupDto group = db.users().insertGroup(org, "to-delete");

    loginAsAdmin();
    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, org.getKey())
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute()
      .assertNoContent();

    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void delete_by_name_fails_if_organization_is_not_correct() throws Exception {
    OrganizationDto org = newOrganizationDto().setUuid("org1");
    OrganizationTesting.insert(db, org);

    loginAsAdmin();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization with key 'org1'");

    newRequest()
      .setParam(PARAM_ORGANIZATION_KEY, "org1")
      .setParam(PARAM_GROUP_NAME, "a-group")
      .execute();
  }

  @Test
  public void delete_members() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "to-be-deleted");
    UserDto user = db.users().insertUser("a-user");
    db.users().insertMember(group, user);

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertNoContent();

    assertThat(db.countRowsOfTable("groups_users")).isEqualTo(0);
  }

  @Test
  public void delete_permissions() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "to-be-deleted");
    ComponentDto project = componentTester.insertComponent(ComponentTesting.newProjectDto());
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, project);

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute()
      .assertNoContent();

    assertThat(db.countRowsOfTable("group_roles")).isEqualTo(0);
  }

  @Test
  public void delete_permission_templates() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "to-be-deleted");
    // TODO

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute().assertNoContent();

    assertThat(db.countRowsOfTable("perm_templates_groups")).isEqualTo(0);
  }

  @Test(expected = NotFoundException.class)
  public void fail_if_id_does_not_exist() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("id", String.valueOf(defaultGroup.getId() + 123))
      .execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void cannot_delete_default_group_of_default_organization() throws Exception {
    loginAsAdmin();
    newRequest()
      .setParam("id", defaultGroup.getId().toString())
      .execute();
  }

  @Test
  public void delete_group_of_an_organization_even_if_name_is_default_group_of_default_organization() throws Exception {
    OrganizationDto org = OrganizationTesting.insert(db, newOrganizationDto());
    GroupDto group = db.users().insertGroup(org, defaultGroup.getName());

    loginAsAdmin();
    newRequest()
      .setParam("id", group.getId().toString())
      .execute();

    assertThat(db.users().selectGroupById(defaultGroup.getId())).isNotNull();
    assertThat(db.users().selectGroupById(group.getId())).isNull();
  }

  @Test
  public void cannot_delete_last_system_admin_group() throws Exception {
    GroupDto group = db.users().insertGroup(defaultOrganizationProvider.getDto(), "system-admins");
    db.users().insertPermissionOnGroup(group, SYSTEM_ADMIN);

    loginAsAdmin();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The last system admin group cannot be deleted");

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();
  }

  @Test
  public void delete_system_admin_group_if_not_last() throws Exception {
    OrganizationDto defaultOrg = defaultOrganizationProvider.getDto();
    GroupDto funkyAdmins = db.users().insertGroup(defaultOrg, "funky-admins");
    db.users().insertPermissionOnGroup(funkyAdmins, SYSTEM_ADMIN);
    GroupDto boringAdmins = db.users().insertGroup(defaultOrg, "boring-admins");
    db.users().insertPermissionOnGroup(boringAdmins, SYSTEM_ADMIN);

    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, boringAdmins.getName())
      .execute();

    assertThat(db.getDbClient().groupPermissionDao().countGroups(db.getSession(), SYSTEM_ADMIN, null)).isEqualTo(1);
  }

  @Test
  public void delete_last_system_admin_group_if_admin_user_left() throws Exception {
    GroupDto lastGroup = db.users().insertGroup(defaultOrganizationProvider.getDto(), "last-group");
    db.users().insertPermissionOnGroup(lastGroup, SYSTEM_ADMIN);
    UserDto bigBoss = db.users().insertUser("big.boss");
    db.users().insertPermissionOnUser(bigBoss, SYSTEM_ADMIN);

    loginAsAdmin();
    newRequest().setParam(PARAM_GROUP_NAME, lastGroup.getName()).execute();

    assertThat(db.users().selectGroupById(lastGroup.getId())).isNull();
  }

  private void loginAsAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest("api/user_groups", "delete");
  }

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), defaultOrganizationProvider);
  }

}
