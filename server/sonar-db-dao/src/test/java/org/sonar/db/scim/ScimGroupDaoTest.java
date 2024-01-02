/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.stream.IntStream;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.DbTester;
import org.sonar.db.OffsetBasedPagination;
import org.sonar.db.Pagination;
import org.sonar.db.user.GroupDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Fail.fail;
import static org.assertj.core.groups.Tuple.tuple;

@RunWith(DataProviderRunner.class)
public class ScimGroupDaoTest {
  private static final String DISPLAY_NAME_FILTER = "displayName eq \"group2\"";
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
        tuple(scimGroup2.getGroupUuid(), scimGroup2.getScimGroupUuid()));
  }

  @Test
  public void countScimGroups_shouldReturnTheTotalNumberOfScimGroups() {
    int totalScimGroups = 15;
    generateScimGroups(totalScimGroups);

    assertThat(scimGroupDao.countScimGroups(db.getSession(), ScimGroupQuery.ALL)).isEqualTo(totalScimGroups);
  }

  @Test
  public void countScimGroups_shouldReturnZero_whenNoScimGroups() {
    assertThat(scimGroupDao.countScimGroups(db.getSession(), ScimGroupQuery.ALL)).isZero();
  }

  @DataProvider
  public static Object[][] paginationData() {
    return new Object[][] {
      {5, 0, 20, List.of("1", "2", "3", "4", "5")},
      {9, 0, 5, List.of("1", "2", "3", "4", "5")},
      {9, 3, 3, List.of("4", "5", "6")},
      {9, 7, 3, List.of("8", "9")},
      {5, 5, 20, List.of()}
    };
  }

  @Test
  @UseDataProvider("paginationData")
  public void findScimGroups_whenPaginationAndStartIndex_shouldReturnTheCorrectNumberOfScimGroups(int totalScimGroups, int offset, int pageSize,
    List<String> expectedScimGroupUuidSuffixes) {
    generateScimGroups(totalScimGroups);

    List<ScimGroupDto> scimGroupDtos = scimGroupDao.findScimGroups(db.getSession(), ScimGroupQuery.ALL, OffsetBasedPagination.forOffset(offset, pageSize));

    List<String> actualScimGroupsUuids = toScimGroupsUuids(scimGroupDtos);
    List<String> expectedScimGroupUuids = toExpectedscimGroupUuids(expectedScimGroupUuidSuffixes);
    assertThat(actualScimGroupsUuids).containsExactlyElementsOf(expectedScimGroupUuids);
  }

  private static List<String> toExpectedscimGroupUuids(List<String> expectedScimGroupUuidSuffixes) {
    return expectedScimGroupUuidSuffixes.stream()
      .map(expectedScimGroupUuidSuffix -> "scim_uuid_Scim Group" + expectedScimGroupUuidSuffix)
      .toList();
  }

  @Test
  public void findScimGroups_whenFilteringByDisplayName_shouldReturnTheExpectedScimGroups() {
    insertGroupAndScimGroup("group1");
    insertGroupAndScimGroup("group2");
    ScimGroupQuery query = ScimGroupQuery.fromScimFilter(DISPLAY_NAME_FILTER);

    List<ScimGroupDto> scimGroups = scimGroupDao.findScimGroups(db.getSession(), query, Pagination.all());

    assertThat(scimGroups).hasSize(1);
    assertThat(scimGroups.get(0).getScimGroupUuid()).isEqualTo(createScimGroupUuid("group2"));
  }

  @Test
  public void countScimGroups_whenFilteringByDisplayName_shouldReturnCorrectCount() {
    insertGroupAndScimGroup("group1");
    insertGroupAndScimGroup("group2");
    ScimGroupQuery query = ScimGroupQuery.fromScimFilter(DISPLAY_NAME_FILTER);

    int groupCount = scimGroupDao.countScimGroups(db.getSession(), query);

    assertThat(groupCount).isEqualTo(1);
  }

  private void insertGroupAndScimGroup(String groupName) {
    GroupDto groupDto = insertGroup(groupName);
    insertScimGroup(createScimGroupUuid(groupName), groupDto.getUuid());
  }

  @Test
  public void getManagedGroupsSqlFilter_whenFilterByManagedIsTrue_returnsCorrectQuery() {
    String filterManagedUser = scimGroupDao.getManagedGroupSqlFilter(true);
    assertThat(filterManagedUser).isEqualTo(" exists (select group_uuid from scim_groups sg where sg.group_uuid = uuid)");
  }

  @Test
  public void getManagedGroupsSqlFilter_whenFilterByManagedIsFalse_returnsCorrectQuery() {
    String filterNonManagedUser = scimGroupDao.getManagedGroupSqlFilter(false);
    assertThat(filterNonManagedUser).isEqualTo("not exists (select group_uuid from scim_groups sg where sg.group_uuid = uuid)");
  }

  private List<ScimGroupDto> generateScimGroups(int totalScimGroups) {
    return IntStream.range(1, totalScimGroups + 1)
      .mapToObj(i -> insertGroup(createGroupName(i)))
      .map(groupDto -> insertScimGroup(createScimGroupUuid(groupDto.getName()), groupDto.getUuid()))
      .toList();
  }

  private static String createGroupName(int i) {
    return "Scim Group" + i;
  }

  private GroupDto insertGroup(String name) {
    return db.users().insertGroup(name);
  }

  private static String createScimGroupUuid(String groupName) {
    return StringUtils.substring("scim_uuid_" + groupName, 0, 40);
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
      .toList();
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


  @Test
  public void deleteAll_should_remove_all_ScimGroups(){
    insertScimGroup("scim-group-uuid1", "group-uuid1");
    insertScimGroup("scim-group-uuid2", "group-uuid2");

    scimGroupDao.deleteAll(db.getSession());

    assertThat(scimGroupDao.findAll(db.getSession())).isEmpty();
  }
}
