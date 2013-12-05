/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.permission;

import org.sonar.api.ServerComponent;
import org.sonar.core.permission.*;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class PermissionFinder implements ServerComponent {

  private final PermissionDao dao;
  private final ResourceDao resourceDao;

  public PermissionFinder(PermissionDao dao, ResourceDao resourceDao) {
    this.resourceDao = resourceDao;
    this.dao = dao;
  }

  public UserWithPermissionQueryResult findUsersWithPermission(WithPermissionQuery query) {
    Long componentId = getComponentId(query.component());
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();

    int offset = (pageIndex - 1) * pageSize;
    // Add one to page size in order to be able to know if there's more results or not
    int limit = pageSize + 1;
    List<UserWithPermissionDto> dtos = dao.selectUsers(query, componentId, offset, limit);
    boolean hasMoreResults = false;
    if (dtos.size() == limit) {
      hasMoreResults = true;
      // Removed last entry as it's only need to know if there more results or not
      dtos.remove(dtos.size() - 1);
    }
    return new UserWithPermissionQueryResult(toUserWithPermissionList(dtos), hasMoreResults);
  }

  public GroupWithPermissionQueryResult findGroupsWithPermission(WithPermissionQuery query) {
    Long componentId = getComponentId(query.component());
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();

    int offset = (pageIndex - 1) * pageSize;
    // Add one to page size in order to be able to know if there's more results or not
    int limit = pageSize + 1;
    List<GroupWithPermissionDto> dtos = dao.selectGroups(query, componentId, offset, limit);
    boolean hasMoreResults = false;
    if (dtos.size() == limit) {
      hasMoreResults = true;
      // Removed last entry as it's only need to know if there more results or not
      dtos.remove(dtos.size() - 1);
    }
    return new GroupWithPermissionQueryResult(toGroupWithPermissionList(dtos), hasMoreResults);
  }

  private List<UserWithPermission> toUserWithPermissionList(List<UserWithPermissionDto> dtos) {
    List<UserWithPermission> users = newArrayList();
    for (UserWithPermissionDto dto : dtos) {
      users.add(dto.toUserWithPermission());
    }
    return users;
  }

  private List<GroupWithPermission> toGroupWithPermissionList(List<GroupWithPermissionDto> dtos) {
    List<GroupWithPermission> users = newArrayList();
    for (GroupWithPermissionDto dto : dtos) {
      users.add(dto.toGroupWithPermission());
    }
    return users;
  }

  @Nullable
  private Long getComponentId(String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(componentKey));
      if (resourceDto == null) {
        throw new NotFoundException(String.format("%s does not exist", componentKey));
      }
      return resourceDto.getId();
    }
  }
}
