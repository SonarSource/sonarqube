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

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionTemplateCharacteristicDaoIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);
  private final DbSession dbSession = db.getSession();
  private final PermissionTemplateCharacteristicDao underTest = new PermissionTemplateCharacteristicDao(new NoOpAuditPersister());

  @Test
  void selectByTemplateId_filter_by_template_uuid() {
    PermissionTemplateCharacteristicDto templatePermission1 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid1")
        .setPermission(ProjectPermission.ADMIN)
        .setTemplateUuid("1")
        .setWithProjectCreator(true)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      "template");
    PermissionTemplateCharacteristicDto templatePermission2 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid2")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("2")
        .setWithProjectCreator(false)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      "template");
    PermissionTemplateCharacteristicDto templatePermission3 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid3")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("3")
        .setWithProjectCreator(false)
        .setCreatedAt(1_000_000_001L)
        .setUpdatedAt(2_000_000_000L),
      "template");
    PermissionTemplateCharacteristicDto templatePermissionForAnotherTemplate = underTest.insert(dbSession,
      new PermissionTemplateCharacteristicDto()
        .setUuid("uuid4")
        .setPermission(ProjectPermission.ADMIN)
        .setTemplateUuid("42")
        .setWithProjectCreator(true)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      "template");

    List<PermissionTemplateCharacteristicDto> result = underTest.selectByTemplateUuids(dbSession, newArrayList("1", "2", "3"));
    assertThat(result)
      .hasSize(3)
      .extracting("uuid")
      .doesNotContain(templatePermissionForAnotherTemplate.getUuid())
      .containsExactly(templatePermission1.getUuid(), templatePermission2.getUuid(), templatePermission3.getUuid());
    assertThat(result.get(0))
      .isEqualToComparingFieldByField(templatePermission1);
  }

  @Test
  void selectByTemplateId_for_empty_list_of_template_uuid() {
    List<PermissionTemplateCharacteristicDto> result = underTest.selectByTemplateUuids(dbSession, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  void selectByPermissionAndTemplateId() {
    PermissionTemplateCharacteristicDto templatePermission1 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid1")
        .setPermission(ProjectPermission.ADMIN)
        .setTemplateUuid("1")
        .setWithProjectCreator(true)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      "template");
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid2")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("1")
        .setWithProjectCreator(false)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      "template");
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid3")
        .setPermission(ProjectPermission.ADMIN)
        .setTemplateUuid("42")
        .setWithProjectCreator(true)
        .setCreatedAt(1_000_000_000L)
        .setUpdatedAt(2_000_000_000L),
      "template");

    Optional<PermissionTemplateCharacteristicDto> result = underTest.selectByPermissionAndTemplateId(dbSession, ProjectPermission.ADMIN, "1");

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualToComparingFieldByField(templatePermission1);
  }

  @Test
  void insert() {
    PermissionTemplateCharacteristicDto expectedResult = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("1")
        .setWithProjectCreator(true)
        .setCreatedAt(123_456_789L)
        .setUpdatedAt(2_000_000_000L),
      "template");

    PermissionTemplateCharacteristicDto result =
      dbSession.getMapper(PermissionTemplateCharacteristicMapper.class).selectByUuid(expectedResult.getUuid());
    assertThat(result.getUuid()).isEqualTo("uuid");
    assertThat(result).isEqualToComparingFieldByField(expectedResult);
  }

  @Test
  void update_only_change_with_project_creator_and_updated_at() {
    PermissionTemplateCharacteristicDto insertedDto = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("1")
        .setWithProjectCreator(true)
        .setCreatedAt(123_456_789L)
        .setUpdatedAt(2_000_000_000L),
      "template");

    underTest.update(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid")
        .setPermission("PERMISSION_ARE_NOT_UPDATABLE")
        .setTemplateUuid("42")
        .setCreatedAt(42L)
        .setWithProjectCreator(false)
        .setUpdatedAt(3_000_000_000L),
      "template");

    PermissionTemplateCharacteristicDto result = underTest.selectByPermissionAndTemplateId(dbSession, insertedDto.getPermission(),
      insertedDto.getTemplateUuid()).get();
    assertThat(result).extracting("uuid", "permission", "templateUuid", "createdAt")
      .containsExactly(insertedDto.getUuid(), insertedDto.getPermission(), insertedDto.getTemplateUuid(), insertedDto.getCreatedAt());
    assertThat(result).extracting("withProjectCreator", "updatedAt")
      .containsExactly(false, 3_000_000_000L);
  }

  @Test
  void fail_insert_if_created_at_is_equal_to_0() {
    PermissionTemplateCharacteristicDto characteristicDto = new PermissionTemplateCharacteristicDto()
      .setUuid("uuid")
      .setPermission(ProjectPermission.USER)
      .setTemplateUuid("1")
      .setWithProjectCreator(true)
      .setUpdatedAt(2_000_000_000L);
    assertThatThrownBy(() -> underTest.insert(dbSession, characteristicDto, "template"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fail_insert_if_updated_at_is_equal_to_0() {
    PermissionTemplateCharacteristicDto characteristicDto = new PermissionTemplateCharacteristicDto()
      .setUuid("uuid")
      .setPermission(ProjectPermission.USER)
      .setTemplateUuid("1")
      .setWithProjectCreator(true)
      .setCreatedAt(2_000_000_000L);
    assertThatThrownBy(() -> underTest.insert(dbSession, characteristicDto, "template"))
      .isInstanceOf(IllegalArgumentException.class);

  }

  @Test
  void fail_update_if_uuid_is_null() {
    PermissionTemplateCharacteristicDto characteristicDto = new PermissionTemplateCharacteristicDto()
      .setPermission(ProjectPermission.USER)
      .setTemplateUuid("1")
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L);
    assertThatThrownBy(() -> underTest.update(dbSession, characteristicDto, "template"))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void delete_by_permission_template_uuid() {
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid1")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("1")
        .setWithProjectCreator(true)
        .setCreatedAt(123_456_789L)
        .setUpdatedAt(2_000_000_000L),
      "template");
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
        .setUuid("uuid2")
        .setPermission(ProjectPermission.USER)
        .setTemplateUuid("2")
        .setWithProjectCreator(true)
        .setCreatedAt(123_456_789L)
        .setUpdatedAt(2_000_000_000L),
      "template");

    assertThat(underTest.selectByTemplateUuids(dbSession, singletonList("1"))).hasSize(1);
    assertThat(underTest.selectByTemplateUuids(dbSession, asList("1", "2"))).hasSize(2);

    dbSession.getMapper(PermissionTemplateCharacteristicMapper.class).deleteByTemplateUuid("1");

    assertThat(underTest.selectByTemplateUuids(dbSession, singletonList("1"))).isEmpty();
    assertThat(underTest.selectByTemplateUuids(dbSession, asList("1", "2"))).hasSize(1);
  }
}
