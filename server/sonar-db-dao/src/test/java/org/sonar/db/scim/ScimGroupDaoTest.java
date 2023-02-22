/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.scim;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.DbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Fail.fail;
import static org.assertj.core.groups.Tuple.tuple;

@RunWith(DataProviderRunner.class)
public class ScimGroupDaoTest {
  @Rule
  public DbTester db = DbTester.create();
  private final ScimGroupDao scimGroupDao = db.getDbClient().scimGroupDao();

  @Test
  public void findAll_ifNoData_returnsEmptyList() {
    assertThat(scimGroupDao.findAll(db.getSession())).isEmpty();
  }

  @Test
  public void findAll_returnsAllEntries() {
    ScimGroupDto scimGroup1 = insertScimGroup();
    ScimGroupDto scimGroup2 = insertScimGroup();

    List<ScimGroupDto> underTest = scimGroupDao.findAll(db.getSession());

    assertThat(underTest).hasSize(2)
      .extracting(ScimGroupDto::getGroupUuid, ScimGroupDto::getScimGroupUuid)
      .containsExactlyInAnyOrder(
        tuple(scimGroup1.getGroupUuid(), scimGroup1.getScimGroupUuid()),
        tuple(scimGroup2.getGroupUuid(), scimGroup2.getScimGroupUuid())
      );
  }

  @Test
  public void findByScimUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimGroupDao.findByScimUuid(db.getSession(), "unknownId")).isEmpty();
  }

  @Test
  public void findByScimUuid_whenScimUuidFound_shouldReturnDto() {
    ScimGroupDto scimGroupDto = insertScimGroup();
    insertScimGroup();

    ScimGroupDto underTest = scimGroupDao.findByScimUuid(db.getSession(), scimGroupDto.getScimGroupUuid())
      .orElseGet(() -> fail("Group not found"));

    assertThat(underTest.getScimGroupUuid()).isEqualTo(scimGroupDto.getScimGroupUuid());
    assertThat(underTest.getGroupUuid()).isEqualTo(scimGroupDto.getGroupUuid());
  }

  @Test
  public void findByGroupUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimGroupDao.findByGroupUuid(db.getSession(), "unknownId")).isEmpty();
  }

  @Test
  public void findByGroupUuid_whenScimUuidFound_shouldReturnDto() {
    ScimGroupDto scimGroupDto = insertScimGroup();
    insertScimGroup();

    ScimGroupDto underTest = scimGroupDao.findByGroupUuid(db.getSession(), scimGroupDto.getGroupUuid())
      .orElseGet(() -> fail("Group not found"));

    assertThat(underTest.getScimGroupUuid()).isEqualTo(scimGroupDto.getScimGroupUuid());
    assertThat(underTest.getGroupUuid()).isEqualTo(scimGroupDto.getGroupUuid());
  }

  @Test
  public void enableScimForGroup_addsGroupToScimGroups() {
    ScimGroupDto underTest = scimGroupDao.enableScimForGroup(db.getSession(), "sqGroup1");

    assertThat(underTest.getScimGroupUuid()).isNotBlank();
    ScimGroupDto scimGroupDto = scimGroupDao.findByScimUuid(db.getSession(), underTest.getScimGroupUuid()).orElseThrow();
    assertThat(underTest.getScimGroupUuid()).isEqualTo(scimGroupDto.getScimGroupUuid());
    assertThat(underTest.getGroupUuid()).isEqualTo(scimGroupDto.getGroupUuid());
  }

  @Test
  public void deleteByGroupUuid_shouldDeleteScimGroup() {
    ScimGroupDto scimGroupDto = insertScimGroup();

    scimGroupDao.deleteByGroupUuid(db.getSession(), scimGroupDto.getGroupUuid());

    assertThat(scimGroupDao.findAll(db.getSession())).isEmpty();
  }

  @Test
  public void deleteByScimUuid_shouldDeleteScimGroup() {
    ScimGroupDto scimGroupDto1 = insertScimGroup();
    ScimGroupDto scimGroupDto2 = insertScimGroup();

    scimGroupDao.deleteByScimUuid(db.getSession(), scimGroupDto1.getScimGroupUuid());

    List<ScimGroupDto> remainingGroups = scimGroupDao.findAll(db.getSession());
    assertThat(remainingGroups).hasSize(1);

    ScimGroupDto remainingGroup = remainingGroups.get(0);
    assertThat(remainingGroup.getScimGroupUuid()).isEqualTo(scimGroupDto2.getScimGroupUuid());
    assertThat(remainingGroup.getGroupUuid()).isEqualTo(scimGroupDto2.getGroupUuid());
  }

  @Test
  public void deleteFromGroupUuid_shouldNotFail_whenNoGroup() {
    assertThatCode(() -> scimGroupDao.deleteByGroupUuid(db.getSession(), randomAlphanumeric(6))).doesNotThrowAnyException();
  }

  private ScimGroupDto insertScimGroup() {
    return scimGroupDao.enableScimForGroup(db.getSession(), randomAlphanumeric(40));
  }
}
