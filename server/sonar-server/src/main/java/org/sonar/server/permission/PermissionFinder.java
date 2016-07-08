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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Paging;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceQuery;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.OldPermissionQuery;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPermissionDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.sonar.api.utils.Paging.forPageIndex;

@ServerSide
public class PermissionFinder {

  private final PermissionDao permissionDao;
  private final ResourceDao resourceDao;
  private final UserDao userDao;

  public PermissionFinder(DbClient dbClient) {
    this.resourceDao = dbClient.resourceDao();
    this.permissionDao = dbClient.permissionDao();
    this.userDao = dbClient.userDao();
  }

  public List<UserWithPermissionDto> findUsersWithPermission(DbSession dbSession, OldPermissionQuery query) {
    Long componentId = componentId(query.component());
    int limit = query.pageSize();
    return permissionDao.selectUsers(dbSession, query, componentId, offset(query), limit);
  }

  public List<UserDto> findUsers(DbSession dbSession, PermissionQuery.Builder dbQuery) {
    List<String> orderedLogins = permissionDao.selectLoginsByPermissionQuery(dbSession, dbQuery.build());

    return Ordering.explicit(orderedLogins).onResultOf(UserDto::getLogin).immutableSortedCopy(userDao.selectByLogins(dbSession, orderedLogins));
  }

  public List<UserPermissionDto> findUserPermissions(DbSession dbSession, PermissionQuery.Builder dbQuery, List<UserDto> users) {
    if (users.isEmpty()) {
      return emptyList();
    }

    List<String> logins = users.stream().map(UserDto::getLogin).collect(Collectors.toList());
    return permissionDao.selectUserPermissionsByQuery(dbSession, dbQuery
      .setLogins(logins)
      .withPermissionOnly()
      .build());
  }

  /**
   * Paging for groups search is done in Java in order to correctly handle the 'Anyone' group
   */
  public List<GroupWithPermissionDto> findGroupsWithPermission(DbSession dbSession, OldPermissionQuery query) {
    Long componentId = componentId(query.component());
    return toGroupQueryResult(permissionDao.selectGroups(dbSession, query, componentId), query);
  }

  @Nullable
  private Long componentId(@Nullable String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ResourceDto resourceDto = resourceDao.selectResource(ResourceQuery.create().setKey(componentKey));
      if (resourceDto == null) {
        throw new NotFoundException(String.format("Project '%s' does not exist", componentKey));
      }
      return resourceDto.getId();
    }
  }

  private static List<GroupWithPermissionDto> toGroupQueryResult(List<GroupWithPermissionDto> dtos, OldPermissionQuery query) {
    addAnyoneGroup(dtos, query);
    List<GroupWithPermissionDto> filteredDtos = filterMembership(dtos, query);

    Paging paging = forPageIndex(query.pageIndex())
      .withPageSize(query.pageSize())
      .andTotal(filteredDtos.size());

    return pagedGroups(filteredDtos, paging);
  }

  private static int offset(OldPermissionQuery query) {
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();
    return (pageIndex - 1) * pageSize;
  }

  private static List<GroupWithPermissionDto> filterMembership(List<GroupWithPermissionDto> dtos, OldPermissionQuery query) {
    return newArrayList(Iterables.filter(dtos, new GroupWithPermissionMatchQuery(query)));
  }

  /**
   * As the anyone group does not exists in db, it's not returned when it has not the permission.
   * We have to manually add it at the begin of the list, if it matched the search text
   */
  private static void addAnyoneGroup(List<GroupWithPermissionDto> groups, OldPermissionQuery query) {
    boolean hasAnyoneGroup = Iterables.any(groups, IsAnyoneGroup.INSTANCE);
    if (!hasAnyoneGroup
      && !GlobalPermissions.SYSTEM_ADMIN.equals(query.permission())
      && (query.search() == null || containsIgnoreCase(DefaultGroups.ANYONE, query.search()))) {
      groups.add(0, new GroupWithPermissionDto().setName(DefaultGroups.ANYONE));
    }
  }

  private static List<GroupWithPermissionDto> pagedGroups(Collection<GroupWithPermissionDto> dtos, Paging paging) {
    List<GroupWithPermissionDto> groups = newArrayList();
    int index = 0;
    for (GroupWithPermissionDto dto : dtos) {
      if (index >= paging.offset() && groups.size() < paging.pageSize()) {
        groups.add(dto);
      } else if (groups.size() >= paging.pageSize()) {
        break;
      }
      index++;
    }
    return groups;
  }

  private static class GroupWithPermissionMatchQuery implements Predicate<GroupWithPermissionDto> {
    private final OldPermissionQuery query;

    public GroupWithPermissionMatchQuery(OldPermissionQuery query) {
      this.query = query;
    }

    @Override
    public boolean apply(@Nonnull GroupWithPermissionDto dto) {
      if (OldPermissionQuery.IN.equals(query.membership())) {
        return dto.getPermission() != null;
      } else if (OldPermissionQuery.OUT.equals(query.membership())) {
        return dto.getPermission() == null;
      }
      return true;
    }
  }

  private enum IsAnyoneGroup implements Predicate<GroupWithPermissionDto> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull GroupWithPermissionDto group) {
      return group.getName().equals(DefaultGroups.ANYONE);
    }
  }
}
