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

import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateTesting;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;

public class DeleteActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private final ManagedInstanceService managedInstanceService = mock();
  private final DefaultGroupFinder defaultGroupFinder = new DefaultGroupFinder(db.getDbClient());
  private final GroupService groupService = new GroupService(db.getDbClient(), UuidFactoryImpl.INSTANCE, defaultGroupFinder, managedInstanceService);
  private final WsActionTester ws = new WsActionTester(new DeleteAction(db.getDbClient(), userSession, groupService, managedInstanceService));

  @Test
  public void verify_definition() {
    Action wsDef = ws.getDef();

    assertThat(wsDef.isInternal()).isFalse();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isTrue();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsOnly(
      tuple("10.4", "Deprecated. Use DELETE /api/v2/authorizations/groups instead"),
      tuple("10.0", "Parameter 'id' is removed. Use 'name' instead."),
      tuple("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));
  }

  @Test
  public void response_has_no_content() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    TestResponse response = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
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
  public void delete_ifNotGroupFound_throwsNotFoundException() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    loginAsAdmin();

    TestRequest groupDeletionRequest = newRequest().setParam(PARAM_GROUP_NAME, group.getName() + "_toto");
    assertThatExceptionOfType(NotFoundException.class)
      .isThrownBy(groupDeletionRequest::execute)
      .withMessageStartingWith("No group with name ");
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
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(db.countRowsOfTable("groups_users")).isZero();
  }

  @Test
  public void delete_permissions() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, project);
    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
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
      template.getName(), group.getName(), template.getOrganizationUuid());
    db.commit();
    loginAsAdmin();
    assertThat(db.countRowsOfTable("perm_templates_groups")).isOne();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
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
      .setParam(PARAM_GROUP_NAME, group.getName())
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
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(db.countRowsOfTable("qgate_group_permissions")).isZero();
  }

  @Test
  public void delete_scim_group() {
    addAdmin();
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    db.users().insertScimGroup(group);

    loginAsAdmin();

    newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(db.countRowsOfTable("scim_groups")).isZero();
  }

  @Test
  public void fail_to_delete_default_group() {
    loginAsAdmin();
    GroupDto defaultGroup = db.users().insertDefaultGroup();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_GROUP_NAME, defaultGroup.getName())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Default group 'sonar-users' cannot be used to perform this action");
  }

  @Test
  public void cannot_delete_last_system_admin_group() {
    insertDefaultGroup();
    GroupDto group = db.users().insertGroup();
    db.users().insertPermissionOnGroup(group, ADMINISTER.getKey());
    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The last system admin group cannot be deleted");
  }

  @Test
  public void delete_admin_group_fails_if_no_admin_users_left() {
    // admin users are part of the group to be deleted
    db.users().insertDefaultGroup();
    GroupDto adminGroup = db.users().insertGroup("admins");
    db.users().insertPermissionOnGroup(adminGroup, ADMINISTER.getKey());
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
    db.users().insertPermissionOnGroup(adminGroup1, ADMINISTER.getKey());
    GroupDto adminGroup2 = db.users().insertGroup("admins2");
    db.users().insertPermissionOnGroup(adminGroup2, ADMINISTER.getKey());
    UserDto bigBoss = db.users().insertUser();
    db.users().insertMember(adminGroup2, bigBoss);
    loginAsAdmin();

    executeDeleteGroupRequest(adminGroup1);

    assertThat(db.users().selectGroupPermissions(adminGroup2, null)).hasSize(1);
  }

  @Test
  public void delete_local_group_when_instance_is_managed_shouldSucceed() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    addAdmin();
    insertDefaultGroup();
    GroupDto group = insertGroupAndMockIsManaged(false);

    loginAsAdmin();
    TestResponse response = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName())
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void fail_to_delete_managed_group_when_instance_is_managed() {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);
    addAdmin();
    insertDefaultGroup();
    GroupDto group = insertGroupAndMockIsManaged(true);

    loginAsAdmin();
    TestRequest request = newRequest()
      .setParam(PARAM_GROUP_NAME, group.getName());

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Deleting managed groups is not allowed.");

  }

  private GroupDto insertGroupAndMockIsManaged(boolean isManaged) {
    GroupDto group = db.users().insertGroup();
    when(managedInstanceService.getGroupUuidToManaged(any(DbSession.class), eq(Set.of(group.getUuid()))))
      .thenReturn(Map.of(group.getUuid(), isManaged));
    return group;
  }

  private void executeDeleteGroupRequest(GroupDto adminGroup1) {
    newRequest()
      .setParam(PARAM_GROUP_NAME, adminGroup1.getName())
      .execute();
  }

  private void addAdmin() {
    UserDto admin = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(admin, ADMINISTER);
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
}
