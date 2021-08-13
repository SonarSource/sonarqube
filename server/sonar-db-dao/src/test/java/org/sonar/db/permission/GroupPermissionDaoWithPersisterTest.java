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
import org.sonar.db.audit.model.PermissionNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.web.UserRole.ADMIN;

public class GroupPermissionDaoWithPersisterTest {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<PermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(PermissionNewValue.class);
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
    underTest.insert(dbSession, dto);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getGroupName, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), null, dto.getRole(), null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");

    underTest.delete(dbSession, ADMIN, group.getUuid(), group.getName(), null, null);

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getGroupName, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, group.getUuid(), group.getName(), null, ADMIN, null);
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentIsPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);
    underTest.insert(dbSession, dto);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getGroupName, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.name());
    assertThat(newValue.toString()).contains("projectUuid");

    underTest.deleteByRootComponentUuid(dbSession, project.uuid(), project.name());

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getGroupName, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, null, null, project.uuid(), null, project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndGroupIsPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);
    underTest.insert(dbSession, dto);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getGroupName, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.name());
    assertThat(newValue.toString()).contains("projectUuid");

    underTest.deleteByRootComponentUuidForAnyOne(dbSession, project.uuid(), project.name());

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, project.uuid(), null, project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void groupProjectPermissionDeleteByComponentAndPermissionIsPersisted() {
    GroupDto group = db.users().insertGroup();
    ComponentDto project = db.components().insertPrivateProject();
    GroupPermissionDto dto = getGroupPermission(group, project);
    underTest.insert(dbSession, dto);

    verify(auditPersister).addGroupPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getGroupName, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(dto.getUuid(), group.getUuid(), group.getName(), project.uuid(), dto.getRole(), project.name());
    assertThat(newValue.toString()).contains("projectUuid");

    underTest.deleteByRootComponentUuidAndPermission(dbSession, project.uuid(), dto.getRole(), project.name());

    verify(auditPersister).deleteGroupPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getGroupUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, null, project.uuid(), ADMIN, project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
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
}
