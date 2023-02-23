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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    ScimGroupDto scimGroup1 = db.users().insertScimGroup(db.users().insertGroup());
    ScimGroupDto scimGroup2 = db.users().insertScimGroup(db.users().insertGroup());

    List<ScimGroupDto> underTest = scimGroupDao.findAll(db.getSession());

    assertThat(underTest).hasSize(2)
      .extracting(ScimGroupDto::getGroupUuid, ScimGroupDto::getScimGroupUuid)
      .containsExactlyInAnyOrder(
        tuple(scimGroup1.getGroupUuid(), scimGroup1.getScimGroupUuid()),
        tuple(scimGroup2.getGroupUuid(), scimGroup2.getScimGroupUuid())
      );
  }

  @Test
  public void countScimGroups_shouldReturnTheTotalNumberOfScimGroups() {
    int totalScimGroups = 15;
    generateScimGroups(totalScimGroups);

    assertThat(scimGroupDao.countScimGroups(db.getSession())).isEqualTo(totalScimGroups);
  }

  @Test
  public void countScimGroups_shouldReturnZero_whenNoScimGroups() {
    assertThat(scimGroupDao.countScimGroups(db.getSession())).isZero();
  }

  @DataProvider
  public static Object[][] paginationData() {
    return new Object[][] {
      {5, 0, 20, List.of("1", "2", "3", "4", "5")},
      {9, 0, 5, List.of("1", "2", "3", "4", "5")},
      {9, 3, 3, List.of("4", "5", "6")},
      {9, 7, 3, List.of("8", "9")},
      {5, 5, 20, List.of()},
      {5, 0, 0, List.of()}
    };
  }

  @Test
  @UseDataProvider("paginationData")
  public void findScimGroups_whenPaginationAndStartIndex_shouldReturnTheCorrectNumberOfScimGroups(int totalScimGroups, int offset, int pageSize, List<String> expectedScimGroupUuids) {
    generateScimGroups(totalScimGroups);

    List<ScimGroupDto> scimUserDtos = scimGroupDao.findScimGroups(db.getSession(), offset, pageSize);

    List<String> scimGroupsUuids = toScimGroupsUuids(scimUserDtos);
    assertThat(scimGroupsUuids).containsExactlyElementsOf(expectedScimGroupUuids);
  }

  private void generateScimGroups(int totalScimGroups) {
    List<ScimGroupDto> allScimGroups = Stream.iterate(1, i -> i + 1)
      .map(i -> insertScimGroup(i.toString()))
      .limit(totalScimGroups)
      .collect(Collectors.toList());
    assertThat(allScimGroups).hasSize(totalScimGroups);
  }

  private ScimGroupDto insertScimGroup(String scimGroupUuid) {
    return insertScimGroup(scimGroupUuid, randomAlphanumeric(40));
  }

  private ScimGroupDto insertScimGroup(String scimGroupUuid, String groupUuid) {
    ScimGroupDto scimGroupDto = new ScimGroupDto(scimGroupUuid, groupUuid);
    Map<String, Object> data = Map.of("scim_uuid", scimGroupDto.getScimGroupUuid(), "group_uuid", scimGroupDto.getGroupUuid());
    db.executeInsert("scim_groups", data);

    return scimGroupDto;
  }

  private static List<String> toScimGroupsUuids(Collection<ScimGroupDto> scimGroupDtos) {
    return scimGroupDtos.stream()
      .map(ScimGroupDto::getScimGroupUuid)
      .collect(Collectors.toList());
  }

  @Test
  public void findByScimUuid_whenScimUuidNotFound_shouldReturnEmptyOptional() {
    assertThat(scimGroupDao.findByScimUuid(db.getSession(), "unknownId")).isEmpty();
  }

  @Test
  public void findByScimUuid_whenScimUuidFound_shouldReturnDto() {
    ScimGroupDto scimGroupDto = db.users().insertScimGroup(db.users().insertGroup());
    db.users().insertScimGroup(db.users().insertGroup());

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
    ScimGroupDto scimGroupDto = db.users().insertScimGroup(db.users().insertGroup());
    db.users().insertScimGroup(db.users().insertGroup());

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
    ScimGroupDto scimGroupDto = db.users().insertScimGroup(db.users().insertGroup());

    scimGroupDao.deleteByGroupUuid(db.getSession(), scimGroupDto.getGroupUuid());

    assertThat(scimGroupDao.findAll(db.getSession())).isEmpty();
  }

  @Test
  public void deleteByScimUuid_shouldDeleteScimGroup() {
    ScimGroupDto scimGroupDto1 = db.users().insertScimGroup(db.users().insertGroup());
    ScimGroupDto scimGroupDto2 = db.users().insertScimGroup(db.users().insertGroup());

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

}
