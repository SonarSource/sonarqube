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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.ExternalGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;

public class ExternalGroupService {

  private static final Logger LOG = LoggerFactory.getLogger(ExternalGroupService.class);

  private final DbClient dbClient;
  private final GroupService groupService;

  public ExternalGroupService(DbClient dbClient, GroupService groupService) {
    this.dbClient = dbClient;
    this.groupService = groupService;
  }

  public void createOrUpdateExternalGroup(DbSession dbSession, GroupRegistration groupRegistration) {
    Optional<GroupDto> groupDto = retrieveGroupByExternalInformation(dbSession, groupRegistration);
    if (groupDto.isPresent()) {
      updateExternalGroup(dbSession, groupDto.get(), groupRegistration.name());
    } else {
      createOrMatchExistingLocalGroup(dbSession, groupRegistration);
    }
  }

  private Optional<GroupDto> retrieveGroupByExternalInformation(DbSession dbSession, GroupRegistration groupRegistration) {
    Optional<ExternalGroupDto> externalGroupDto = dbClient.externalGroupDao().selectByExternalIdAndIdentityProvider(dbSession, groupRegistration.externalId(),
      groupRegistration.externalIdentityProvider());
    return externalGroupDto.flatMap(existingExternalGroupDto -> Optional.ofNullable(dbClient.groupDao().selectByUuid(dbSession, existingExternalGroupDto.groupUuid())));
  }

  private void updateExternalGroup(DbSession dbSession, GroupDto groupDto, String newName) {
    LOG.debug("Updating external group: {} with new name {}", groupDto.getName(), newName);
    groupService.updateGroup(dbSession, groupDto, newName);
  }

  private void createOrMatchExistingLocalGroup(DbSession dbSession, GroupRegistration groupRegistration) {
    GroupDto groupDto = findOrCreateLocalGroup(dbSession, groupRegistration);
    createExternalGroup(dbSession, groupDto.getUuid(), groupRegistration);
  }

  private GroupDto findOrCreateLocalGroup(DbSession dbSession, GroupRegistration groupRegistration) {
    Optional<GroupDto> groupDto = groupService.findGroup(dbSession, groupRegistration.name());
    if (groupDto.isPresent()) {
      LOG.debug("Marking existing local group {} as managed group.", groupDto.get().getName());
      return groupDto.get();
    } else {
      LOG.debug("Creating new group {}", groupRegistration.name());
      return groupService.createGroup(dbSession, groupRegistration.name(), null);
    }
  }

  private void createExternalGroup(DbSession dbSession, String groupUuid, GroupRegistration groupRegistration) {
    dbClient.externalGroupDao().insert(dbSession, new ExternalGroupDto(groupUuid, groupRegistration.externalId(), groupRegistration.externalIdentityProvider()));
  }

}
