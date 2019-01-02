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
package org.sonar.server.usergroups;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;

import static com.google.common.base.Preconditions.checkArgument;

public class DefaultGroupCreatorImpl implements DefaultGroupCreator {

  static final String DEFAULT_GROUP_NAME = "Members";
  private final DbClient dbClient;

  public DefaultGroupCreatorImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public GroupDto create(DbSession dbSession, String organizationUuid) {
    Optional<GroupDto> existingMembersGroup = dbClient.groupDao().selectByName(dbSession, organizationUuid, DEFAULT_GROUP_NAME);
    checkArgument(!existingMembersGroup.isPresent(), "The group '%s' already exist on organization '%s'", DEFAULT_GROUP_NAME, organizationUuid);

    GroupDto defaultGroup = new GroupDto()
      .setName(DEFAULT_GROUP_NAME)
      .setDescription("All members of the organization")
      .setOrganizationUuid(organizationUuid);
    dbClient.groupDao().insert(dbSession, defaultGroup);
    dbClient.organizationDao().setDefaultGroupId(dbSession, organizationUuid, defaultGroup);
    return defaultGroup;
  }

}
