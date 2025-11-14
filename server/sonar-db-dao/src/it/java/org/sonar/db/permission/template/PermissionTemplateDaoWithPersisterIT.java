/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.permission.template;

import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PermissionTemplateNewValue;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.sonar.db.permission.ProjectPermission.ADMIN;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.db.user.GroupTesting.newGroupDto;

class PermissionTemplateDaoWithPersisterIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  private static final Date PAST = new Date(100_000_000_000L);
  private static final Date NOW = new Date(500_000_000_000L);

  @RegisterExtension
  private final DbTester db = DbTester.create(auditPersister);
  private final DbSession session = db.getSession();

  private final ArgumentCaptor<PermissionTemplateNewValue> newValueCaptor = ArgumentCaptor.forClass(PermissionTemplateNewValue.class);
  private final PermissionTemplateDao underTest = db.getDbClient().permissionTemplateDao();

  @Test
  void insertPermissionTemplateIsPersisted() {
    PermissionTemplateDto dto = insertPermissionTemplate();

    verify(auditPersister).addPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName)
      .containsExactly(dto.getUuid(), dto.getName());
    assertThat(newValue.toString()).doesNotContain("keyPattern");
  }

  @Test
  void updatePermissionTemplateIsPersisted() {
    PermissionTemplateDto dto = insertPermissionTemplate();
    underTest.update(session, dto);

    verify(auditPersister).updatePermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getDescription, PermissionTemplateNewValue::getKeyPattern)
      .containsExactly(dto.getUuid(), dto.getName(), dto.getDescription(), dto.getKeyPattern());
    assertThat(newValue.toString()).contains("keyPattern");
  }

  @Test
  void deletePermissionTemplateIsPersisted() {
    PermissionTemplateDto dto = insertPermissionTemplate();
    underTest.deleteByUuid(session, dto.getUuid(), dto.getName());

    verify(auditPersister).deletePermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName)
      .containsExactly(dto.getUuid(), dto.getName());
    assertThat(newValue.toString()).doesNotContain("keyPattern");
  }

  @Test
  void deletePermissionTemplateWithoutAffectedRowsIsPersisted() {
    underTest.deleteByUuid(session, "template-uuid", "template-name");

    verifyNoInteractions(auditPersister);
  }

  @Test
  void insertAndDeleteUserPermissionToTemplateIsPersisted() {
    PermissionTemplateDto dto = insertPermissionTemplate();
    UserDto user = db.users().insertUser();
    underTest.insertUserPermission(session, dto.getUuid(), user.getUuid(), ADMIN, dto.getName(), user.getLogin());

    verify(auditPersister).addUserToPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getUserUuid, PermissionTemplateNewValue::getUserLogin)
      .containsExactly(dto.getUuid(), dto.getName(), ADMIN.getKey(), user.getUuid(), user.getLogin());
    assertThat(newValue.toString()).doesNotContain("groupUuid");

    underTest.deleteUserPermission(session, dto.getUuid(), user.getUuid(), ADMIN, dto.getName(), user.getLogin());

    verify(auditPersister).deleteUserFromPermissionTemplate(eq(session), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getUserUuid, PermissionTemplateNewValue::getUserLogin)
      .containsExactly(dto.getUuid(), dto.getName(), ADMIN.getKey(), user.getUuid(), user.getLogin());
    assertThat(newValue.toString()).doesNotContain("groupUuid");
  }

  @Test
  void deleteUserPermissionToTemplateWithoutAffectedRowsIsNotPersisted() {
    underTest.deleteUserPermission(session, "template-uuid", "user-uuid", ADMIN,
      "template-name", "user-login");

    verifyNoInteractions(auditPersister);
  }

  @Test
  void insertAndDeleteUserPermissionByUserUuidToTemplateIsPersisted() {
    PermissionTemplateDto dto = insertPermissionTemplate();
    UserDto user = db.users().insertUser();
    underTest.insertUserPermission(session, dto.getUuid(), user.getUuid(), ADMIN, dto.getName(), user.getLogin());
    underTest.deleteUserPermissionsByUserUuid(session, user.getUuid(), user.getLogin());

    verify(auditPersister).deleteUserFromPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getUserUuid, PermissionTemplateNewValue::getUserLogin)
      .containsExactly(null, null, null, user.getUuid(), user.getLogin());
    assertThat(newValue.toString()).doesNotContain("groupUuid");
  }

  @Test
  void deleteUserPermissionByUserUuidToTemplateWithoutAffectedRowsIsNotPersisted() {
    underTest.deleteUserPermissionsByUserUuid(session, "user-uuid", "user-login");

    verifyNoInteractions(auditPersister);
  }

  @Test
  void insertAndDeleteGroupPermissionToTemplateIsPersisted() {
    PermissionTemplateDto dto = insertPermissionTemplate();
    GroupDto group = db.users().insertGroup(newGroupDto());
    underTest.insertGroupPermission(session, dto.getUuid(), group.getUuid(), ADMIN, dto.getName(), group.getName());

    verify(auditPersister).addGroupToPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getGroupUuid, PermissionTemplateNewValue::getGroupName)
      .containsExactly(dto.getUuid(), dto.getName(), ADMIN.getKey(), group.getUuid(), group.getName());
    assertThat(newValue.toString()).contains("groupUuid");

    underTest.deleteGroupPermission(session, dto.getUuid(), group.getUuid(), ADMIN, dto.getName(), group.getName());

    verify(auditPersister).deleteGroupFromPermissionTemplate(eq(session), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getGroupUuid, PermissionTemplateNewValue::getGroupName)
      .containsExactly(dto.getUuid(), dto.getName(), ADMIN.getKey(), group.getUuid(), group.getName());
    assertThat(newValue.toString()).contains("groupUuid");
  }

  @Test
  void deleteGroupPermissionToTemplateWithoutAffectedRowsIsNotPersisted() {
    underTest.deleteGroupPermission(session, "template-uuid", "group-uuid", ADMIN,
      "template-name", "group-name");

    verifyNoInteractions(auditPersister);
  }

  @Test
  void insertAndDeleteGroupPermissionByGroupUuidToTemplateIsPersisted() {
    PermissionTemplateDto templateDto = insertPermissionTemplate();
    PermissionTemplateGroupDto templateGroupDto = new PermissionTemplateGroupDto()
      .setUuid(Uuids.createFast())
      .setGroupName("group")
      .setGroupUuid("group-id")
      .setPermission(ADMIN)
      .setTemplateUuid(templateDto.getUuid())
      .setCreatedAt(new Date())
      .setUpdatedAt(new Date());
    underTest.insertGroupPermission(session, templateGroupDto, templateDto.getName());

    verify(auditPersister).addGroupToPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getGroupUuid, PermissionTemplateNewValue::getGroupName)
      .containsExactly(templateDto.getUuid(), templateDto.getName(), ADMIN.getKey(), templateGroupDto.getGroupUuid(),
        templateGroupDto.getGroupName());
    assertThat(newValue.toString()).doesNotContain("userUuid");

    underTest.deleteByGroup(session, templateGroupDto.getGroupUuid(), templateGroupDto.getGroupName());

    verify(auditPersister).deleteGroupFromPermissionTemplate(eq(session), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::getGroupUuid, PermissionTemplateNewValue::getGroupName)
      .containsExactly(null, null, null, templateGroupDto.getGroupUuid(), templateGroupDto.getGroupName());
    assertThat(newValue.toString()).doesNotContain("userUuid");
  }

  @Test
  void deleteGroupPermissionByGroupUuidToTemplateWithoutAffectedRowsIsNotPersisted() {
    underTest.deleteByGroup(session, "group-uid", "group-name");

    verifyNoInteractions(auditPersister);
  }

  private PermissionTemplateDto insertPermissionTemplate() {
    return underTest.insert(session, newPermissionTemplateDto()
      .setUuid("ABCD")
      .setName("my template")
      .setDescription("my description")
      .setKeyPattern("myregexp")
      .setCreatedAt(PAST)
      .setUpdatedAt(NOW));
  }
}
