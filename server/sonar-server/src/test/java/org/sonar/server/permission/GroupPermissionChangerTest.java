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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.usergroups.ws.GroupIdOrAnyone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;

public class GroupPermissionChangerTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private GroupPermissionChanger underTest = new GroupPermissionChanger(db.getDbClient());
  private OrganizationDto org;
  private GroupDto group;
  private ComponentDto project;

  @Before
  public void setUp() throws Exception {
    org = db.organizations().insert();
    group = db.users().insertGroup(org, "a-group");
    project = db.components().insertProject(org);
  }

  @Test
  public void add_permission_to_group() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, GlobalPermissions.QUALITY_GATE_ADMIN, null, groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(GlobalPermissions.QUALITY_GATE_ADMIN);
  }

  @Test
  public void add_project_permission_to_group() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, UserRole.ISSUE_ADMIN, new ProjectId(project), groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void add_permission_to_anyone() {
    OrganizationDto defaultOrganization = db.getDefaultOrganization();
    GroupIdOrAnyone groupId = GroupIdOrAnyone.forAnyone(defaultOrganization.getUuid());

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, GlobalPermissions.QUALITY_GATE_ADMIN, null, groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectAnyonePermissions(defaultOrganization, null)).containsOnly(GlobalPermissions.QUALITY_GATE_ADMIN);
  }

  @Test
  public void add_project_permission_to_anyone() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.forAnyone(org.getUuid());

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, UserRole.ISSUE_ADMIN, new ProjectId(project), groupId));

    assertThat(db.users().selectAnyonePermissions(org, null)).isEmpty();
    assertThat(db.users().selectAnyonePermissions(org, project)).containsOnly(UserRole.ISSUE_ADMIN);
  }

  @Test
  public void do_nothing_when_adding_permission_that_already_exists() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, ADMINISTER_QUALITY_GATES.getKey(), null, groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(ADMINISTER_QUALITY_GATES.getKey());
  }

  @Test
  public void fail_to_add_global_permission_on_project() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid project permission 'gateadmin'. Valid values are [admin, codeviewer, issueadmin, scan, user]");

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, GlobalPermissions.QUALITY_GATE_ADMIN, new ProjectId(project), groupId));
  }

  @Test
  public void fail_to_add_project_permission_on_global_group() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Invalid global permission 'issueadmin'. Valid values are [admin, profileadmin, gateadmin, scan, provisioning]");

    apply(new GroupPermissionChange(PermissionChange.Operation.ADD, UserRole.ISSUE_ADMIN, null, groupId));
  }

  @Test
  public void remove_permission_from_group() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);
    db.users().insertPermissionOnGroup(group, PROVISION_PROJECTS);

    apply(new GroupPermissionChange(PermissionChange.Operation.REMOVE, ADMINISTER_QUALITY_GATES.getKey(), null, groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(PROVISION_PROJECTS.getKey());
  }

  @Test
  public void remove_project_permission_from_group() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);
    db.users().insertPermissionOnGroup(group, ADMINISTER_QUALITY_GATES);
    db.users().insertProjectPermissionOnGroup(group, UserRole.ISSUE_ADMIN, project);
    db.users().insertProjectPermissionOnGroup(group, UserRole.CODEVIEWER, project);

    apply(new GroupPermissionChange(PermissionChange.Operation.REMOVE, UserRole.ISSUE_ADMIN, new ProjectId(project), groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).containsOnly(ADMINISTER_QUALITY_GATES.getKey());
    assertThat(db.users().selectGroupPermissions(group, project)).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void do_not_fail_if_removing_a_permission_that_does_not_exist() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);

    apply(new GroupPermissionChange(PermissionChange.Operation.REMOVE, UserRole.ISSUE_ADMIN, new ProjectId(project), groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
    assertThat(db.users().selectGroupPermissions(group, project)).isEmpty();
  }

  @Test
  public void fail_to_remove_admin_permission_if_no_more_admins() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);
    db.users().insertPermissionOnGroup(group, ADMINISTER);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Last group with permission 'admin'. Permission cannot be removed.");

    underTest.apply(db.getSession(), new GroupPermissionChange(PermissionChange.Operation.REMOVE, ADMINISTER.getKey(), null, groupId));
  }

  @Test
  public void remove_admin_group_if_still_other_admins() {
    GroupIdOrAnyone groupId = GroupIdOrAnyone.from(group);
    db.users().insertPermissionOnGroup(group, ADMINISTER);
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(org, admin, ADMINISTER);

    apply(new GroupPermissionChange(PermissionChange.Operation.REMOVE, ADMINISTER.getKey(), null, groupId));

    assertThat(db.users().selectGroupPermissions(group, null)).isEmpty();
  }

  private void apply(GroupPermissionChange change) {
    underTest.apply(db.getSession(), change);
    db.commit();
  }
}
