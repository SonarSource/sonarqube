/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class PermissionTemplateCharacteristicDaoTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = db.getSession();
  private PermissionTemplateCharacteristicDao underTest = new PermissionTemplateCharacteristicDao();

  @Test
  public void selectByTemplateId_filter_by_template_id() {
    PermissionTemplateCharacteristicDto templatePermission1 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.ADMIN)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));
    PermissionTemplateCharacteristicDto templatePermission2 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(2L)
      .setWithProjectCreator(false)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));
    PermissionTemplateCharacteristicDto templatePermission3 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(3L)
      .setWithProjectCreator(false)
      .setCreatedAt(1_000_000_001L)
      .setUpdatedAt(2_000_000_000L));
    PermissionTemplateCharacteristicDto templatePermissionForAnotherTemplate = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.ADMIN)
      .setTemplateId(42L)
      .setWithProjectCreator(true)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));

    List<PermissionTemplateCharacteristicDto> result = underTest.selectByTemplateIds(dbSession, newArrayList(1L, 2L, 3L));
    assertThat(result)
      .hasSize(3)
      .extracting("id")
      .doesNotContain(templatePermissionForAnotherTemplate.getId())
      .containsExactly(templatePermission1.getId(), templatePermission2.getId(), templatePermission3.getId());
    assertThat(result.get(0))
      .isEqualToComparingFieldByField(templatePermission1);
  }

  @Test
  public void selectByTemplateId_for_empty_list_of_template_id() {
    List<PermissionTemplateCharacteristicDto> result = underTest.selectByTemplateIds(dbSession, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  public void selectByPermissionAndTemplateId() {
    PermissionTemplateCharacteristicDto templatePermission1 = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.ADMIN)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(false)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.ADMIN)
      .setTemplateId(42L)
      .setWithProjectCreator(true)
      .setCreatedAt(1_000_000_000L)
      .setUpdatedAt(2_000_000_000L));

    Optional<PermissionTemplateCharacteristicDto> result = underTest.selectByPermissionAndTemplateId(dbSession, UserRole.ADMIN, 1L);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualToComparingFieldByField(templatePermission1);
  }

  @Test
  public void insert() {
    PermissionTemplateCharacteristicDto expectedResult = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L));

    PermissionTemplateCharacteristicDto result = dbSession.getMapper(PermissionTemplateCharacteristicMapper.class).selectById(expectedResult.getId());
    assertThat(result.getId()).isNotNull();
    assertThat(result).isEqualToComparingFieldByField(expectedResult);
  }

  @Test
  public void update_only_change_with_project_creator_and_updated_at() {
    PermissionTemplateCharacteristicDto insertedDto = underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L));

    underTest.update(dbSession, new PermissionTemplateCharacteristicDto()
      .setId(insertedDto.getId())
      .setPermission("PERMISSION_ARE_NOT_UPDATABLE")
      .setTemplateId(42L)
      .setCreatedAt(42L)
      .setWithProjectCreator(false)
      .setUpdatedAt(3_000_000_000L));

    PermissionTemplateCharacteristicDto result = underTest.selectByPermissionAndTemplateId(dbSession, insertedDto.getPermission(), insertedDto.getTemplateId()).get();
    assertThat(result).extracting("id", "permission", "templateId", "createdAt")
      .containsExactly(insertedDto.getId(), insertedDto.getPermission(), insertedDto.getTemplateId(), insertedDto.getCreatedAt());
    assertThat(result).extracting("withProjectCreator", "updatedAt")
      .containsExactly(false, 3_000_000_000L);
  }

  @Test
  public void fail_insert_if_created_at_is_equal_to_0() {
    expectedException.expect(IllegalArgumentException.class);

    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setUpdatedAt(2_000_000_000L));
  }

  @Test
  public void fail_insert_if_updated_at_is_equal_to_0() {
    expectedException.expect(IllegalArgumentException.class);

    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(2_000_000_000L));
  }

  @Test
  public void fail_update_if_id_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.update(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L));
  }

  @Test
  public void delete_by_permission_template_id() {
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(1L)
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L));
    underTest.insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(UserRole.USER)
      .setTemplateId(2L)
      .setWithProjectCreator(true)
      .setCreatedAt(123_456_789L)
      .setUpdatedAt(2_000_000_000L));

    assertThat(underTest.selectByTemplateIds(dbSession, asList(1L))).hasSize(1);
    assertThat(underTest.selectByTemplateIds(dbSession, asList(1L, 2L))).hasSize(2);

    dbSession.getMapper(PermissionTemplateCharacteristicMapper.class).deleteByTemplateId(1L);

    assertThat(underTest.selectByTemplateIds(dbSession, asList(1L))).hasSize(0);
    assertThat(underTest.selectByTemplateIds(dbSession, asList(1L, 2L))).hasSize(1);
  }
}
