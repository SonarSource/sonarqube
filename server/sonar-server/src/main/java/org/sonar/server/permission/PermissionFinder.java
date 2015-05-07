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
package org.sonar.server.permission;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.Paging;
import org.sonar.core.permission.GroupWithPermission;
import org.sonar.core.permission.GroupWithPermissionDto;
import org.sonar.core.permission.PermissionDao;
import org.sonar.core.permission.PermissionQuery;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.permission.UserWithPermission;
import org.sonar.core.permission.UserWithPermissionDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.server.exceptions.NotFoundException;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class PermissionFinder {

  private final PermissionDao permissionDao;
  private final ResourceDao resourceDao;

  private final PermissionTemplateDao permissionTemplateDao;

  public PermissionFinder(PermissionDao permissionDao, ResourceDao resourceDao, PermissionTemplateDao permissionTemplateDao) {
    this.resourceDao = resourceDao;
    this.permissionDao = permissionDao;
    this.permissionTemplateDao = permissionTemplateDao;
  }

  public UserWithPermissionQueryResult findUsersWithPermission(PermissionQuery query) {
    Long componentId = componentId(query.component());
    int limit = limit(query);
    return toUserQueryResult(permissionDao.selectUsers(query, componentId, offset(query), limit), limit);
  }

  public UserWithPermissionQueryResult findUsersWithPermissionTemplate(PermissionQuery query) {
    Long permissionTemplateId = templateId(query.template());
    int limit = limit(query);
    return toUserQueryResult(permissionTemplateDao.selectUsers(query, permissionTemplateId, offset(query), limit), limit);
  }

  /**
   * Paging for groups search is done in Java in order to correctly handle the 'Anyone' group
   */
  public GroupWithPermissionQueryResult findGroupsWithPermission(PermissionQuery query) {
    Long componentId = componentId(query.component());
    return toGroupQueryResult(permissionDao.selectGroups(query, componentId), query);
  }

  /**
   * Paging for groups search is done in Java in order to correctly handle the 'Anyone' group
   */
  public GroupWithPermissionQueryResult findGroupsWithPermissionTemplate(PermissionQuery query) {
    Long permissionTemplateId = templateId(query.template());
    return toGroupQueryResult(permissionTemplateDao.selectGroups(query, permissionTemplateId), query);
  }

  private UserWithPermissionQueryResult toUserQueryResult(List<UserWithPermissionDto> dtos, int limit) {
    boolean hasMoreResults = false;
    if (dtos.size() == limit) {
      hasMoreResults = true;
      // Removed last entry as it's only need to know if there more results or not
      dtos.remove(dtos.size() - 1);
    }
    return new UserWithPermissionQueryResult(toUserWithPermissionList(dtos), hasMoreResults);
  }

  private List<UserWithPermission> toUserWithPermissionList(List<UserWithPermissionDto> dtos) {
    List<UserWithPermission> users = newArrayList();
    for (UserWithPermissionDto dto : dtos) {
      users.add(dto.toUserWithPermission());
    }
    return users;
  }

  @Nullable
  private Long componentId(String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(componentKey));
      if (resourceDto == null) {
        throw new NotFoundException(String.format("Component '%s' does not exist", componentKey));
      }
      return resourceDto.getId();
    }
  }

  private GroupWithPermissionQueryResult toGroupQueryResult(List<GroupWithPermissionDto> dtos, PermissionQuery query) {
    addAnyoneGroup(dtos, query);
    List<GroupWithPermissionDto> filteredDtos = filterMembership(dtos, query);

    Paging paging = Paging.create(query.pageSize(), query.pageIndex(), filteredDtos.size());
    List<GroupWithPermission> pagedGroups = pagedGroups(filteredDtos, paging);
    return new GroupWithPermissionQueryResult(pagedGroups, paging.hasNextPage());
  }

  private Long templateId(String templateKey) {
    PermissionTemplateDto dto = permissionTemplateDao.selectTemplateByKey(templateKey);
    if (dto == null) {
      throw new NotFoundException(String.format("Template '%s' does not exist", templateKey));
    }
    return dto.getId();
  }

  private int offset(PermissionQuery query) {
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();
    return (pageIndex - 1) * pageSize;
  }

  private int limit(PermissionQuery query) {
    // Add one to page size in order to be able to know if there's more results or not
    return query.pageSize() + 1;
  }

  private List<GroupWithPermissionDto> filterMembership(List<GroupWithPermissionDto> dtos, final PermissionQuery query) {
    return newArrayList(Iterables.filter(dtos, new Predicate<GroupWithPermissionDto>() {
      @Override
      public boolean apply(GroupWithPermissionDto dto) {
        if (PermissionQuery.IN.equals(query.membership())) {
          return dto.getPermission() != null;
        } else if (PermissionQuery.OUT.equals(query.membership())) {
          return dto.getPermission() == null;
        }
        return true;
      }
    }));
  }

  /**
   * As the anyone group does not exists in db, it's not returned when it has not the permission.
   * We have to manually add it at the begin of the list, if it matched the search text
   */
  private void addAnyoneGroup(List<GroupWithPermissionDto> groups, PermissionQuery query) {
    boolean hasAnyoneGroup = Iterables.any(groups, new Predicate<GroupWithPermissionDto>() {
      @Override
      public boolean apply(GroupWithPermissionDto group) {
        return group.getName().equals(DefaultGroups.ANYONE);
      }
    });
    if (!hasAnyoneGroup && (query.search() == null || StringUtils.containsIgnoreCase(DefaultGroups.ANYONE, query.search()))) {
      groups.add(0, new GroupWithPermissionDto().setName(DefaultGroups.ANYONE));
    }
  }

  private List<GroupWithPermission> pagedGroups(Collection<GroupWithPermissionDto> dtos, Paging paging) {
    List<GroupWithPermission> groups = newArrayList();
    int index = 0;
    for (GroupWithPermissionDto dto : dtos) {
      if (index >= paging.offset() && groups.size() < paging.pageSize()) {
        groups.add(dto.toGroupWithPermission());
      } else if (groups.size() >= paging.pageSize()) {
        break;
      }
      index++;
    }
    return groups;
  }

}
