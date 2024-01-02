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
package org.sonar.server.usergroups.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;

public class DeleteActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private final ComponentDbTester componentTester = new ComponentDbTester(db);
  private final WsActionTester ws = new WsActionTester(new DeleteAction(db.getDbClient(), userSession, newGroupWsSupport()));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));
  }

  @Test
  public void response_has_no_content() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    TestResponse response = newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void delete_by_id() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(db.users().selectGroupByUuid(group.getUuid())).isNull();
  }

  @Test
  public void delete_by_name() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(db.users().selectGroupByUuid(group.getUuid())).isNull();
  }

  @Test
  public void delete_members() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    UserDto user = db.users().insertUser();
    db.users().insertMember(group, user);
    loginAsAdmin();

    newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(db.countRowsOfTable("groups_users")).isZero();
  }

  @Test
  public void delete_permissions() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    ComponentDto project = componentTester.insertPrivateProject();
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(db.countRowsOfTable("group_roles")).isZero();
  }

  @Test
  public void delete_group_from_permission_templates() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    PermissionTemplateDto template = db.getDbClient().permissionTemplateDao().insert(db.getSession(),
      PermissionTemplateTesting.newPermissionTemplateDto());
    db.getDbClient().permissionTemplateDao().insertGroupPermission(db.getSession(), template.getUuid(), group.getUuid(), "perm",
      template.getName(), group.getName());
    db.commit();
    loginAsAdmin();
    assertThat(db.countRowsOfTable("perm_templates_groups")).isOne();

    newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(db.countRowsOfTable("perm_templates_groups")).isZero();
  }

  @Test
  public void delete_qprofile_permissions() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    QProfileDto profile = db.qualityProfiles().insert();
    db.qualityProfiles().addGroupPermission(profile, group);
    loginAsAdmin();

    newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(db.countRowsOfTable("qprofile_edit_groups")).isZero();
  }

  @Test
  public void delete_qgate_permissions() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    db.qualityGates().addGroupPermission(qualityGate, group);
    loginAsAdmin();

    newRequest()
      .setParam("id", group.getUuid())
      .execute();

    assertThat(db.countRowsOfTable("qgate_group_permissions")).isZero();
  }

  @Test
  public void fail_if_id_does_not_exist() {
    addAdmin();
    loginAsAdmin();
    int groupId = 123;

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", String.valueOf(groupId))
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No group with id '" + groupId + "'");
  }

  @Test
  public void fail_to_delete_default_group() {
    loginAsAdmin();
    GroupDto defaultGroup = db.users().insertDefaultGroup();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam("id", defaultGroup.getUuid())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void cannot_delete_last_system_admin_group() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    db.users().insertPermissionOnGroup(group, SYSTEM_ADMIN);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, group.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The last system admin group cannot be deleted");
  }

  @Test
  public void delete_admin_group_fails_if_no_admin_users_left() {
    // admin users are part of the group to be deleted
    db.users().insertDefaultGroup();
    GroupDto adminGroup = db.users().insertGroup("admins");
    db.users().insertPermissionOnGroup(adminGroup, SYSTEM_ADMIN);
    UserDto bigBoss = db.users().insertUser();
    db.users().insertMember(adminGroup, bigBoss);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      executeDeleteGroupRequest(adminGroup);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The last system admin group cannot be deleted");
  }

  @Test
  public void delete_admin_group_succeeds_if_other_groups_have_administrators() {
    db.users().insertDefaultGroup();
    GroupDto adminGroup1 = db.users().insertGroup("admins");
    db.users().insertPermissionOnGroup(adminGroup1, SYSTEM_ADMIN);
    GroupDto adminGroup2 = db.users().insertGroup("admins2");
    db.users().insertPermissionOnGroup(adminGroup2, SYSTEM_ADMIN);
    UserDto bigBoss = db.users().insertUser();
    db.users().insertMember(adminGroup2, bigBoss);
    loginAsAdmin();

    executeDeleteGroupRequest(adminGroup1);

    assertThat(db.users().selectGroupPermissions(adminGroup2, null)).hasSize(1);
  }

  private void executeDeleteGroupRequest(GroupDto adminGroup1) {
    newRequest()
      .setParam(PARAM_GROUP_ID, adminGroup1.getUuid())
      .execute();
  }

  private void addAdmin() {
    UserDto admin = db.users().insertUser();
    db.users().insertPermissionOnUser(admin, SYSTEM_ADMIN);
  }

  private void loginAsAdmin() {
    userSession.logIn().addPermission(ADMINISTER);
  }

  private void insertDefaultGroup() {
    db.users().insertDefaultGroup();
  }

  private TestRequest newRequest() {
    return ws.newRequest();
  }

  private GroupWsSupport newGroupWsSupport() {
    return new GroupWsSupport(db.getDbClient(), new DefaultGroupFinder(db.getDbClient()));
  }

}
