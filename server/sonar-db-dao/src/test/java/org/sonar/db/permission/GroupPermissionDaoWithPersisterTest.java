/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.permission;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupPermissionNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.sonar.api.web.UserRole.ADMIN;

public class GroupPermissionDaoWithPersisterTest {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<GroupPermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(GroupPermissionNewValue.class);
  private final DbSession dbSession = db.getSession();
  private final GroupPermissionDao underTest = db.getDbClient().groupPermissionDao();

  @Test
  public void groupGlobalPermissionInsertAndDeleteArePersisted() {
    GroupDto group = db.users().insertGroup();
    GroupPermissionDto dto = new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(ADMIN);
    underTest.insert(dbSession, dto, null);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName, GroupPermissionNewValue::getQualifier)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), null, dto.getRole(), null, null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");

    underTest.delete(dbSession, ADMIN, group.getUuid(), group.getName(), null, null);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName)
      .containsExactly(null, group.getUuid(), group.getName(), null, ADMIN, null);
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupGlobalPermissionDeleteWithoutAffectedRowsIsNotPersisted() {
    underTest.delete(dbSession, ADMIN, "group-uuid", "group-name", null, null);

    verifyNoInteractions(auditPersister);
  }

  @Test
  public void groupProjectPermissionDeleteByComponentIsPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);
    underTest.insert(dbSession, dto, project);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName, GroupPermissionNewValue::getQualifier)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.name(), "project");
    assertThat(newValue.toString()).contains("componentUuid");

    underTest.deleteByRootComponentUuid(dbSession, project);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName)
      .containsExactly(null, null, null, project.uuid(), null, project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentWithoutAffectedRowsIsNotPersisted() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.deleteByRootComponentUuid(dbSession, project);

    verify(auditPersister).addComponent(any(), any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndGroupIsPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(project);
    underTest.insert(dbSession, dto, project);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName, GroupPermissionNewValue::getQualifier)
      .containsExactly(dto.getUuid(), null, null, project.uuid(), dto.getRole(), project.name(), "project");
    assertThat(newValue.toString()).contains("componentUuid");

    underTest.deleteByRootComponentUuidForAnyOne(dbSession, project);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName)
      .containsExactly(null, null, null, project.uuid(), null, project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndGroupWithoutAffectedRowsIsNotPersisted() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.deleteByRootComponentUuidForAnyOne(dbSession, project);

    verify(auditPersister).addComponent(any(), any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndPermissionIsPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);
    underTest.insert(dbSession, dto, project);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    GroupPermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid,
        GroupPermissionNewValue::getGroupName, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName, GroupPermissionNewValue::getQualifier)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.name(), "project");
    assertThat(newValue.toString()).contains("componentUuid");

    underTest.deleteByRootComponentUuidAndPermission(dbSession, dto.getRole(), project);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(GroupPermissionNewValue::getPermissionUuid, GroupPermissionNewValue::getGroupUuid, GroupPermissionNewValue::getComponentUuid,
        GroupPermissionNewValue::getRole, GroupPermissionNewValue::getComponentName)
      .containsExactly(null, null, project.uuid(), ADMIN, project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndPermissionWithoutAffectedRowsIsNotPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);

    underTest.deleteByRootComponentUuidAndPermission(dbSession, dto.getRole(), project);

    verify(auditPersister).addComponent(any(), any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  private GroupPermissionDto getGroupPermission(GroupDto group, ComponentDto project) {
    return new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setGroupUuid(group.getUuid())
      .setGroupName(group.getName())
      .setRole(ADMIN)
      .setComponentUuid(project.uuid())
      .setComponentName(project.name());
  }

  private GroupPermissionDto getGroupPermission(ComponentDto project) {
    return new GroupPermissionDto()
      .setUuid(Uuids.createFast())
      .setRole(ADMIN)
      .setComponentUuid(project.uuid())
      .setComponentName(project.name());
  }
}
