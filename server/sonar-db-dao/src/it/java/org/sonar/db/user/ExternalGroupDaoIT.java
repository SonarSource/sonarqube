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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalGroupDaoIT {

  private static final String GROUP_UUID = "uuid";
  private static final String EXTERNAL_ID = "external_id";
  private static final String EXTERNAL_IDENTITY_PROVIDER = "external_identity_provider";
  private static final String PROVIDER = "provider1";

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final DbSession dbSession = db.getSession();

  private final GroupDao groupDao = db.getDbClient().groupDao();

  private final ExternalGroupDao underTest = db.getDbClient().externalGroupDao();

  @Test
  void insert_savesExternalGroup() {
    GroupDto localGroup = insertGroup(GROUP_UUID);
    insertGroup("67689");
    ExternalGroupDto externalGroupDto = externalGroup(GROUP_UUID, EXTERNAL_IDENTITY_PROVIDER);
    underTest.insert(dbSession, externalGroupDto);
    List<ExternalGroupDto> savedGroups = underTest.selectByIdentityProvider(dbSession, EXTERNAL_IDENTITY_PROVIDER);
    assertThat(savedGroups)
      .hasSize(1)
      .contains(createExternalGroupDto(localGroup.getName(), externalGroupDto));
  }

  @Test
  void selectByGroupUuid_shouldReturnExternalGroup() {
    ExternalGroupDto expectedExternalGroupDto = new ExternalGroupDto(GROUP_UUID, EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER);
    underTest.insert(dbSession, expectedExternalGroupDto);

    Optional<ExternalGroupDto> actualExternalGroupDto = underTest.selectByGroupUuid(dbSession, GROUP_UUID);

    assertThat(actualExternalGroupDto).isPresent();
    compareExpectedAndActualExternalGroupDto(expectedExternalGroupDto, actualExternalGroupDto.get());
  }

  @Test
  void selectByIdentityProvider_returnOnlyGroupForTheIdentityProvider() {
    List<ExternalGroupDto> expectedGroups = createAndInsertExternalGroupDtos(PROVIDER, 3);
    createAndInsertExternalGroupDtos("provider2", 1);
    List<ExternalGroupDto> savedGroup = underTest.selectByIdentityProvider(dbSession, PROVIDER);
    assertThat(savedGroup).containsExactlyInAnyOrderElementsOf(expectedGroups);
  }

  @Test
  void selectByExternalIdAndIdentityProvider_shouldReturnOnlyMatchingExternalGroup() {
    ExternalGroupDto expectedExternalGroupDto = new ExternalGroupDto(GROUP_UUID, EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER);
    underTest.insert(dbSession, expectedExternalGroupDto);
    underTest.insert(dbSession, new ExternalGroupDto(GROUP_UUID + "1", EXTERNAL_ID, "another_external_identity_provider"));
    underTest.insert(dbSession, new ExternalGroupDto(GROUP_UUID + "2", "another_external_id", EXTERNAL_IDENTITY_PROVIDER));
    underTest.insert(dbSession, new ExternalGroupDto(GROUP_UUID + "3", "whatever", "whatever"));

    Optional<ExternalGroupDto> actualExternalGroupDto = underTest.selectByExternalIdAndIdentityProvider(dbSession, EXTERNAL_ID,
      EXTERNAL_IDENTITY_PROVIDER);

    compareExpectedAndActualExternalGroupDto(expectedExternalGroupDto, actualExternalGroupDto.get());
  }

  private void compareExpectedAndActualExternalGroupDto(ExternalGroupDto expectedExternalGroupDto,
    ExternalGroupDto actualExternalGroupDto) {
    assertThat(actualExternalGroupDto)
      .usingRecursiveComparison()
      .isEqualTo(expectedExternalGroupDto);
  }

  @Test
  void deleteByGroupUuid_deletesTheGroup() {
    List<ExternalGroupDto> insertedGroups = createAndInsertExternalGroupDtos(PROVIDER, 3);

    ExternalGroupDto toRemove = insertedGroups.remove(0);
    underTest.deleteByGroupUuid(dbSession, toRemove.groupUuid());
    List<ExternalGroupDto> remainingGroups = underTest.selectByIdentityProvider(dbSession, PROVIDER);
    assertThat(remainingGroups).containsExactlyInAnyOrderElementsOf(insertedGroups);
  }

  @Test
  void deleteByExternalIdentityProvider_onlyDeletesGroupOfTheRequestedProvider() {
    createAndInsertExternalGroupDtos(PROVIDER, 3);
    List<ExternalGroupDto> groupsProvider2 = createAndInsertExternalGroupDtos("provider2", 3);

    underTest.deleteByExternalIdentityProvider(dbSession, PROVIDER);

    assertThat(underTest.selectByIdentityProvider(dbSession, PROVIDER)).isEmpty();
    assertThat(underTest.selectByIdentityProvider(dbSession, "provider2")).containsExactlyInAnyOrderElementsOf(groupsProvider2);
  }

  @Test
  void getManagedGroupsSqlFilter_whenFilterByManagedIsTrue_returnsCorrectQuery() {
    String filterManagedUser = underTest.getManagedGroupSqlFilter(true);
    assertThat(filterManagedUser).isEqualTo(
      "(exists (select group_uuid from external_groups eg where eg.group_uuid = uuid) "
        + "or exists (select group_uuid from github_orgs_groups gog where gog.group_uuid = uuid))");
  }

  @Test
  void getManagedGroupsSqlFilter_whenFilterByManagedIsFalse_returnsCorrectQuery() {
    String filterNonManagedUser = underTest.getManagedGroupSqlFilter(false);
    assertThat(filterNonManagedUser).isEqualTo(
      "(not exists (select group_uuid from external_groups eg where eg.group_uuid = uuid) "
        + "and not exists (select group_uuid from github_orgs_groups gog where gog.group_uuid = uuid))");
  }

  private List<ExternalGroupDto> createAndInsertExternalGroupDtos(String provider, int numberOfGroups) {
    List<ExternalGroupDto> expectedExternalGroupDtos = new ArrayList<>();
    for (int i = 1; i <= numberOfGroups; i++) {
      ExternalGroupDto externalGroupDto = externalGroup(provider + "_" + i, provider);
      GroupDto localGroup = insertGroup(externalGroupDto.groupUuid());
      underTest.insert(dbSession, externalGroupDto);
      expectedExternalGroupDtos.add(createExternalGroupDto(localGroup.getName(), externalGroupDto));
    }
    return expectedExternalGroupDtos;
  }

  private static ExternalGroupDto externalGroup(String groupUuid, String identityProvider) {
    return new ExternalGroupDto(groupUuid, "external_" + groupUuid, identityProvider);
  }

  private GroupDto insertGroup(String groupUuid) {
    GroupDto group = new GroupDto();
    group.setUuid(groupUuid);
    group.setName("name" + groupUuid);
    return groupDao.insert(dbSession, group);
  }

  private ExternalGroupDto createExternalGroupDto(String name, ExternalGroupDto externalGroupDto) {
    return new ExternalGroupDto(externalGroupDto.groupUuid(), externalGroupDto.externalId(), externalGroupDto.externalIdentityProvider(),
      name);
  }

}
