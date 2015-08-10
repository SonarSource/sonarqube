/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

public class PermissionWsCommons {

  private final DbClient dbClient;

  public PermissionWsCommons(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public String searchGroupName(DbSession dbSession, @Nullable String groupNameParam, @Nullable Long groupId) {
    checkParameters(groupNameParam, groupId);
    if (groupNameParam != null) {
      return groupNameParam;
    }

    GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
    if (group == null) {
      throw new NotFoundException(String.format("Group with id '%d' is not found", groupId));
    }

    return group.getName();
  }

  private static void checkParameters(@Nullable String groupName, @Nullable Long groupId) {
    if (groupName != null ^ groupId != null) {
      return;
    }

    throw new BadRequestException("Group name or group id must be provided, not both");
  }
}
