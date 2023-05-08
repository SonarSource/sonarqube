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
package org.sonar.db.user;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ExternalGroupDaoIT {

  @Rule
  public final DbTester db = DbTester.create();

  private final DbSession dbSession = db.getSession();

  private final GroupDao groupDao = db.getDbClient().groupDao();

  private final ExternalGroupDao underTest = db.getDbClient().externalGroupDao();

  @Test
  public void insert_savesExternalGroup() {
    GroupDto localGroup = insertGroup("12345");
    insertGroup("67689");
    ExternalGroupDto externalGroupDto = externalGroup("12345", "providerId");
    underTest.insert(dbSession, externalGroupDto);
    List<ExternalGroupDto> savedGroups = underTest.selectByIdentityProvider(dbSession, "providerId");
    assertThat(savedGroups)
      .hasSize(1)
      .contains(createExternalGroupDto(localGroup.getName(), externalGroupDto));
  }

  @Test
  public void selectByIdentityProvider_returnOnlyGroupForTheIdentityProvider() {
    List<ExternalGroupDto> expectedGroups = createAndInsertExternalGroupDtos("provider1", 3);
    createAndInsertExternalGroupDtos("provider2", 1);
    List<ExternalGroupDto> savedGroup = underTest.selectByIdentityProvider(dbSession, "provider1");
    assertThat(savedGroup).containsExactlyInAnyOrderElementsOf(expectedGroups);
  }

  @Test
  public void deleteByGroupUuid_deletesTheGroup() {
    List<ExternalGroupDto> insertedGroups = createAndInsertExternalGroupDtos("provider1", 3);

    ExternalGroupDto toRemove = insertedGroups.remove(0);
    underTest.deleteByGroupUuid(dbSession, toRemove.groupUuid());
    List<ExternalGroupDto> remainingGroups = underTest.selectByIdentityProvider(dbSession, "provider1");
    assertThat(remainingGroups).containsExactlyInAnyOrderElementsOf(insertedGroups);
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
    return new ExternalGroupDto(externalGroupDto.groupUuid(), externalGroupDto.externalId(), externalGroupDto.externalIdentityProvider(), name);
  }

}
