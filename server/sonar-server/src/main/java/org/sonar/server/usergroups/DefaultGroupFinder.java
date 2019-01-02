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

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class DefaultGroupFinder {

  private final DbClient dbClient;

  public DefaultGroupFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public GroupDto findDefaultGroup(DbSession dbSession, String organizationUuid) {
    int defaultGroupId = dbClient.organizationDao().getDefaultGroupId(dbSession, organizationUuid)
      .orElseThrow(() -> new IllegalStateException(format("Default group cannot be found on organization '%s'", organizationUuid)));
    return requireNonNull(dbClient.groupDao().selectById(dbSession, defaultGroupId), format("Group '%s' cannot be found", defaultGroupId));
  }

}
