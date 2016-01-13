/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;

import static org.sonar.server.ws.WsUtils.checkFound;

public class UserGroupFinder {
  private final DbClient dbClient;

  public UserGroupFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public GroupDto getGroup(DbSession dbSession, WsGroupRef group) {
    Long groupId = group.id();
    String groupName = group.name();

    GroupDto groupDto = null;

    if (groupId != null) {
      groupDto = checkFound(dbClient.groupDao().selectById(dbSession, groupId),
        "Group with id '%d' is not found", groupId);
    }

    if (groupName != null) {
      groupDto = checkFound(dbClient.groupDao().selectByName(dbSession, groupName),
        "Group with name '%s' is not found", groupName);
    }

    return groupDto;
  }
}
