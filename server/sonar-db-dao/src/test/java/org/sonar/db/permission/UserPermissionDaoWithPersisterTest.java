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

import java.util.function.Consumer;
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
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

public class UserPermissionDaoWithPersisterTest {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<PermissionNewValue> newValueCaptor = ArgumentCaptor.forClass(PermissionNewValue.class);
  private final DbSession dbSession = db.getSession();
  private final UserPermissionDao underTest = db.getDbClient().userPermissionDao();

  @Test
  public void userGlobalPermissionInsertAndDeleteArePersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), null);
    underTest.insert(dbSession, dto, null);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getUserUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(dto.getUuid(), user.getUuid(), null, dto.getPermission(), null);
    assertThat(newValue.toString()).doesNotContain("projectUuid");

    underTest.deleteGlobalPermission(dbSession, user.getUuid(), SYSTEM_ADMIN);

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getUserUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, user.getUuid(), null, dto.getPermission(), null);
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void userProjectPermissionInsertAndDeleteArePersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project);

    verify(auditPersister).addUserPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getUserUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(dto.getUuid(), user.getUuid(), project.uuid(), dto.getPermission(), project.name());
    assertThat(newValue.toString()).contains("projectUuid");

    underTest.deleteProjectPermission(dbSession, user.getUuid(), SYSTEM_ADMIN, project.projectUuid(), project.name());

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getUserUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, user.getUuid(), project.uuid(), dto.getPermission(), project.name());
    assertThat(newValue.toString()).doesNotContain("permissionUuid");
  }

  @Test
  public void deleteUserPermissionOfAnyUserIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SCAN_EXECUTION, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project);
    underTest.deleteProjectPermissionOfAnyUser(dbSession, project.projectUuid(), SCAN_EXECUTION, project.name());

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getUserUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, null, project.uuid(), SCAN_EXECUTION, project.name());
    assertThat(newValue.toString()).doesNotContain("userUuid");
  }

  @Test
  public void deleteUserPermissionByUserUuidIsPersisted() {
    UserDto user = insertUser(u -> u.setLogin("login1").setName("Marius").setEmail("email1@email.com"));
    ComponentDto project = db.components().insertPrivateProject();
    UserPermissionDto dto = new UserPermissionDto(Uuids.create(), SYSTEM_ADMIN, user.getUuid(), project.uuid());
    underTest.insert(dbSession, dto, project);
    underTest.deleteByUserUuid(dbSession, user.getUuid());

    verify(auditPersister).deleteUserPermission(eq(dbSession), newValueCaptor.capture());
    PermissionNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionNewValue::getPermissionUuid, PermissionNewValue::getUserUuid, PermissionNewValue::getProjectUuid,
        PermissionNewValue::getRole, PermissionNewValue::getProjectName)
      .containsExactly(null, user.getUuid(), null, null, null);
    assertThat(newValue.toString()).contains("userUuid");
  }

  private UserDto insertUser(Consumer<UserDto> populateUserDto) {
    UserDto user = db.users().insertUser(populateUserDto);
    return user;
  }
}
