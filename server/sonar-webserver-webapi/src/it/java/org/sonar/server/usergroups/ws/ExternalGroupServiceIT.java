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
package org.sonar.server.usergroups.ws;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.ExternalGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ExternalGroupServiceIT {

  private static final String GROUP_NAME = "GROUP_NAME";
  private static final String EXTERNAL_ID = "EXTERNAL_ID";
  private static final String EXTERNAL_IDENTITY_PROVIDER = "EXTERNAL_IDENTITY_PROVIDER";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbTester.getSession();

  private final DefaultGroupFinder defaultGroupFinder = new DefaultGroupFinder(dbClient);

  private final ManagedInstanceService managedInstanceService = mock();
  private final GroupService groupService = new GroupService(dbClient, UuidFactoryFast.getInstance(), defaultGroupFinder, managedInstanceService);

  private final ExternalGroupService externalGroupService = new ExternalGroupService(dbClient, groupService);

  @Test
  public void createOrUpdateExternalGroup_whenNewGroup_shouldCreateIt() {
    externalGroupService.createOrUpdateExternalGroup(dbSession, new GroupRegistration(EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER, GROUP_NAME));

    assertGroupAndExternalGroup();
  }

  @Test
  public void createOrUpdateExternalGroup_whenExistingLocalGroup_shouldMatchAndMakeItExternal() {
    dbTester.users().insertGroup(GROUP_NAME);

    externalGroupService.createOrUpdateExternalGroup(dbSession, new GroupRegistration(EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER, GROUP_NAME));

    assertThat(dbTester.users().countAllGroups()).isEqualTo(1);
    assertGroupAndExternalGroup();
  }

  @Test
  public void createOrUpdateExternalGroup_whenExistingExternalGroup_shouldUpdate() {
    dbTester.users().insertDefaultGroup();
    GroupDto existingGroupDto = dbTester.users().insertGroup(GROUP_NAME);
    dbTester.users().insertExternalGroup(new ExternalGroupDto(existingGroupDto.getUuid(), EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER));

    String updatedGroupName = "updated_" + GROUP_NAME;
    externalGroupService.createOrUpdateExternalGroup(dbSession, new GroupRegistration(EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER, updatedGroupName));

    Optional<GroupDto> groupDto = dbTester.users().selectGroup(updatedGroupName);
    assertThat(groupDto)
      .isPresent().get()
      .extracting(GroupDto::getName)
      .isEqualTo(updatedGroupName);
  }

  private void assertGroupAndExternalGroup() {
    Optional<GroupDto> groupDto = dbTester.users().selectGroup(GROUP_NAME);
    assertThat(groupDto)
      .isPresent().get()
      .extracting(GroupDto::getName).isEqualTo(GROUP_NAME);

    assertThat((dbTester.users().selectExternalGroupByGroupUuid(groupDto.get().getUuid())))
      .isPresent().get()
      .extracting(ExternalGroupDto::externalId, ExternalGroupDto::externalIdentityProvider)
      .containsExactly(EXTERNAL_ID, EXTERNAL_IDENTITY_PROVIDER);
  }

}
