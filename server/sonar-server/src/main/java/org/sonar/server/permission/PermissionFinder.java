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
package org.sonar.server.permission;

import com.google.common.collect.Ordering;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;

import static java.util.Collections.emptyList;

@ServerSide
public class PermissionFinder {

  private final PermissionDao permissionDao;
  private final GroupDao groupDao;

  public PermissionFinder(DbClient dbClient) {
    this.permissionDao = dbClient.permissionDao();
    this.groupDao = dbClient.groupDao();
  }

  public List<GroupDto> findGroups(DbSession dbSession, PermissionQuery.Builder dbQuery) {
    List<String> orderedNames = permissionDao.selectGroupNamesByPermissionQuery(dbSession, dbQuery.build());

    List<GroupDto> groups = groupDao.selectByNames(dbSession, orderedNames);
    if (orderedNames.contains(DefaultGroups.ANYONE)) {
      groups.add(0, new GroupDto().setId(0L).setName(DefaultGroups.ANYONE));
    }

    return Ordering.explicit(orderedNames).onResultOf(GroupDto::getName).immutableSortedCopy(groups);
  }

  public List<GroupRoleDto> findGroupPermissions(DbSession dbSession, PermissionQuery.Builder dbQuery, List<GroupDto> groups) {
    if (groups.isEmpty()) {
      return emptyList();
    }

    List<String> names = groups.stream().map(GroupDto::getName).collect(Collectors.toList());
    return permissionDao.selectGroupPermissionsByQuery(dbSession, dbQuery
      .setGroupNames(names)
      .withPermissionOnly()
      .build());
  }
}
