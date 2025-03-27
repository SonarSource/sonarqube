/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PermissionTemplateNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PermissionTemplateCharacteristicDaoWithPersisterIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);
  private final DbSession session = db.getSession();
  private final PermissionTemplateCharacteristicDao underTest = db.getDbClient().permissionTemplateCharacteristicDao();
  private final ArgumentCaptor<PermissionTemplateNewValue> newValueCaptor = ArgumentCaptor.forClass(PermissionTemplateNewValue.class);

  @Test
  void insertPermissionTemplateCharacteristicIsPersisted() {
    PermissionTemplateCharacteristicDto dto = getPermissionTemplateCharacteristic(ProjectPermission.USER);
    underTest.insert(session, dto, "template");

    verify(auditPersister).addCharacteristicToPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::isWithProjectCreator)
      .containsExactly(dto.getTemplateUuid(), "template", dto.getPermission(), dto.getWithProjectCreator());
    assertThat(newValue.toString()).contains("withProjectCreator");
  }

  @Test
  void updatePermissionTemplateCharacteristicIsPersisted() {
    underTest.insert(session, getPermissionTemplateCharacteristic(ProjectPermission.USER),
      "template");
    PermissionTemplateCharacteristicDto updated = getPermissionTemplateCharacteristic(ProjectPermission.ADMIN);
    underTest.update(session, updated, "template");

    verify(auditPersister).updateCharacteristicInPermissionTemplate(eq(session), newValueCaptor.capture());
    PermissionTemplateNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PermissionTemplateNewValue::getTemplateUuid, PermissionTemplateNewValue::getName,
        PermissionTemplateNewValue::getPermission, PermissionTemplateNewValue::isWithProjectCreator)
      .containsExactly(updated.getTemplateUuid(), "template", updated.getPermission(), updated.getWithProjectCreator());
    assertThat(newValue.toString()).contains("withProjectCreator");
  }

  private PermissionTemplateCharacteristicDto getPermissionTemplateCharacteristic(ProjectPermission role) {
    return new PermissionTemplateCharacteristicDto()
      .setUuid("uuid")
      .setPermission(role.getKey())
      .setTemplateUuid("1")
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L);
  }
}
