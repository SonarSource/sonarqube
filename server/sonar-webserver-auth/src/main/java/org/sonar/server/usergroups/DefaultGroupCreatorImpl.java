/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;

import static com.google.common.base.Preconditions.checkArgument;

public class DefaultGroupCreatorImpl implements DefaultGroupCreator {

  public static final String DEFAULT_GROUP_NAME = "Members";
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final DefaultOrganizationProvider organizationProvider;

  public DefaultGroupCreatorImpl(DbClient dbClient, UuidFactory uuidFactory, DefaultOrganizationProvider organizationProvider) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.organizationProvider = organizationProvider;
  }

  public GroupDto create(DbSession dbSession) {
    Optional<GroupDto> existingMembersGroup = dbClient.groupDao().selectByName(dbSession, DEFAULT_GROUP_NAME);
    checkArgument(!existingMembersGroup.isPresent(), "The group '%s' already exists", DEFAULT_GROUP_NAME);

    GroupDto defaultGroup = new GroupDto()
      .setUuid(uuidFactory.create())
      .setName(DEFAULT_GROUP_NAME)
      .setDescription("All members of the organization");
    dbClient.groupDao().insert(dbSession, defaultGroup);
    dbClient.organizationDao().setDefaultGroupUuid(dbSession, organizationProvider.get().getUuid(), defaultGroup);
    return defaultGroup;
  }

}
