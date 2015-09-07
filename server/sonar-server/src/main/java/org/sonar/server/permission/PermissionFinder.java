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
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Paging;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.GroupWithPermission;
import org.sonar.core.permission.UserWithPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceQuery;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.sonar.api.utils.Paging.forPageIndex;

@ServerSide
public class PermissionFinder {

  private final PermissionDao permissionDao;
  private final ResourceDao resourceDao;
  private final PermissionTemplateDao permissionTemplateDao;

  public PermissionFinder(DbClient dbClient) {
    this.resourceDao = dbClient.resourceDao();
    this.permissionDao = dbClient.permissionDao();
    this.permissionTemplateDao = dbClient.permissionTemplateDao();
  }

  public UserWithPermissionQueryResult findUsersWithPermission(DbSession dbSession, PermissionQuery query) {
    Long componentId = componentId(query.component());
    int limit = query.pageSize();
    int total = permissionDao.countUsers(dbSession, query, componentId);
    return toUserQueryResult(permissionDao.selectUsers(dbSession, query, componentId, offset(query), limit), total);
  }

  public UserWithPermissionQueryResult findUsersWithPermissionTemplate(DbSession dbSession, PermissionQuery query) {
    Long permissionTemplateId = templateId(query.template());
    int limit = query.pageSize();
    int total = permissionTemplateDao.countUsers(dbSession, query, permissionTemplateId);
    return toUserQueryResult(permissionTemplateDao.selectUsers(dbSession, query, permissionTemplateId, offset(query), limit), total);
  }

  /**
   * Paging for groups search is done in Java in order to correctly handle the 'Anyone' group
   */
  public GroupWithPermissionQueryResult findGroupsWithPermission(DbSession dbSession, PermissionQuery query) {
    Long componentId = componentId(query.component());
    return toGroupQueryResult(permissionDao.selectGroups(dbSession, query, componentId), query);
  }

  /**
   * Paging for groups search is done in Java in order to correctly handle the 'Anyone' group
   */
  public GroupWithPermissionQueryResult findGroupsWithPermissionTemplate(DbSession dbSession, PermissionQuery query) {
    Long permissionTemplateId = templateId(query.template());
    return toGroupQueryResult(permissionTemplateDao.selectGroups(dbSession, query, permissionTemplateId), query);
  }

  private static UserWithPermissionQueryResult toUserQueryResult(List<UserWithPermissionDto> dtos, int total) {
    return new UserWithPermissionQueryResult(toUserWithPermissionList(dtos), total);
  }

  private static List<UserWithPermission> toUserWithPermissionList(List<UserWithPermissionDto> dtos) {
    List<UserWithPermission> users = newArrayList();
    for (UserWithPermissionDto dto : dtos) {
      users.add(dto.toUserWithPermission());
    }
    return users;
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

  private GroupWithPermissionQueryResult toGroupQueryResult(List<GroupWithPermissionDto> dtos, PermissionQuery query) {
    addAnyoneGroup(dtos, query);
    List<GroupWithPermissionDto> filteredDtos = filterMembership(dtos, query);

    Paging paging = forPageIndex(query.pageIndex())
      .withPageSize(query.pageSize())
      .andTotal(filteredDtos.size());

    List<GroupWithPermission> pagedGroups = pagedGroups(filteredDtos, paging);
    return new GroupWithPermissionQueryResult(pagedGroups, filteredDtos.size());
  }

  private Long templateId(String templateKey) {
    PermissionTemplateDto dto = permissionTemplateDao.selectByUuid(templateKey);
    if (dto == null) {
      throw new NotFoundException(String.format("Template '%s' does not exist", templateKey));
    }
    return dto.getId();
  }

  private static int offset(PermissionQuery query) {
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();
    return (pageIndex - 1) * pageSize;
  }

  private List<GroupWithPermissionDto> filterMembership(List<GroupWithPermissionDto> dtos, PermissionQuery query) {
    return newArrayList(Iterables.filter(dtos, new GroupWithPermissionMatchQuery(query)));
  }

  /**
   * As the anyone group does not exists in db, it's not returned when it has not the permission.
   * We have to manually add it at the begin of the list, if it matched the search text
   */
  private void addAnyoneGroup(List<GroupWithPermissionDto> groups, PermissionQuery query) {
    boolean hasAnyoneGroup = Iterables.any(groups, IsAnyoneGroup.INSTANCE);
    if (!hasAnyoneGroup
      && !GlobalPermissions.SYSTEM_ADMIN.equals(query.permission())
      && (query.search() == null || containsIgnoreCase(DefaultGroups.ANYONE, query.search()))) {
      groups.add(0, new GroupWithPermissionDto().setName(DefaultGroups.ANYONE));
    }
  }

  private static List<GroupWithPermission> pagedGroups(Collection<GroupWithPermissionDto> dtos, Paging paging) {
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

  private static class GroupWithPermissionMatchQuery implements Predicate<GroupWithPermissionDto> {
    private final PermissionQuery query;

    public GroupWithPermissionMatchQuery(PermissionQuery query) {
      this.query = query;
    }

    @Override
    public boolean apply(@Nonnull GroupWithPermissionDto dto) {
      if (PermissionQuery.IN.equals(query.membership())) {
        return dto.getPermission() != null;
      } else if (PermissionQuery.OUT.equals(query.membership())) {
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
