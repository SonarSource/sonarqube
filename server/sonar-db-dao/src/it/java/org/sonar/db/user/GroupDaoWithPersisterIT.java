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
package org.sonar.db.user;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserGroupNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GroupDaoWithPersisterIT {
  private static final long NOW = 1_500_000L;

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<UserGroupNewValue> newValueCaptor = ArgumentCaptor.forClass(UserGroupNewValue.class);

  private final System2 system2 = mock(System2.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2, auditPersister);

  private final DbClient dbClient = db.getDbClient();
  private final GroupDao underTest = db.getDbClient().groupDao();

  private final GroupDto aGroup = new GroupDto()
    .setUuid("uuid")
    .setName("the-name")
    .setDescription("the description");

  @BeforeEach
  void setUp() {
    when(system2.now()).thenReturn(NOW);
  }

  @Test
  void insertAndUpdateGroupIsPersisted() {
    dbClient.groupDao().insert(db.getSession(), aGroup);

    verify(auditPersister).addUserGroup(eq(db.getSession()), newValueCaptor.capture());

    UserGroupNewValue newValue = newValueCaptor.getValue();

    assertThat(newValue)
      .extracting(UserGroupNewValue::getGroupUuid, UserGroupNewValue::getName)
      .containsExactly(aGroup.getUuid(), aGroup.getName());
    assertThat(newValue.toString()).doesNotContain("description");

    GroupDto dto = new GroupDto()
      .setUuid(aGroup.getUuid())
      .setName("new-name")
      .setDescription("New description")
      .setCreatedAt(new Date(NOW + 1_000L));
    underTest.update(db.getSession(), dto);

    verify(auditPersister).updateUserGroup(eq(db.getSession()), newValueCaptor.capture());

    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(UserGroupNewValue::getGroupUuid, UserGroupNewValue::getName, UserGroupNewValue::getDescription)
      .containsExactly(dto.getUuid(), dto.getName(), dto.getDescription());
    assertThat(newValue.toString()).contains("description");
  }

  @Test
  void deleteGroupIsPersisted() {
    dbClient.groupDao().insert(db.getSession(), aGroup);

    verify(auditPersister).addUserGroup(eq(db.getSession()), any());

    underTest.deleteByUuid(db.getSession(), aGroup.getUuid(), aGroup.getName());

    assertThat(db.countRowsOfTable(db.getSession(), "groups")).isZero();
    verify(auditPersister).deleteUserGroup(eq(db.getSession()), newValueCaptor.capture());
    assertThat(newValueCaptor.getValue())
      .extracting(UserGroupNewValue::getGroupUuid, UserGroupNewValue::getName)
      .containsExactly(aGroup.getUuid(), aGroup.getName());
  }

  @Test
  void deleteGroupWithoutAffectedRowsIsNotPersisted() {
    underTest.deleteByUuid(db.getSession(), aGroup.getUuid(), aGroup.getName());

    verifyNoInteractions(auditPersister);
  }
}
