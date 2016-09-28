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
package org.sonar.db.permission;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class UserPermissionDao implements Dao {

  /**
   * @see UserPermissionMapper#selectByQuery(PermissionQuery, Collection, RowBounds)
   */
  public List<ExtendedUserPermissionDto> select(DbSession dbSession, PermissionQuery query, @Nullable Collection<String> userLogins) {
    if (userLogins != null) {
      if (userLogins.isEmpty()) {
        return emptyList();
      }
      checkArgument(userLogins.size() <= DatabaseUtils.PARTITION_SIZE_FOR_ORACLE, "Maximum 1'000 users are accepted");
    }

    RowBounds rowBounds = new RowBounds(query.getPageOffset(), query.getPageSize());
    return mapper(dbSession).selectByQuery(query, userLogins, rowBounds);
  }

  /**
   * Shortcut over {@link #select(DbSession, PermissionQuery, Collection)} to return only logins, in the same order.
   */
  public List<String> selectLogins(DbSession dbSession, PermissionQuery query) {
    return select(dbSession, query, null).stream()
      .map(ExtendedUserPermissionDto::getUserLogin)
      .distinct()
      .collect(Collectors.toList());
  }

  /**
   * Shortcut over {@link #select(DbSession, PermissionQuery, Collection)}
   * @param userLogin the non-null user login
   * @param projectUuid if null, then return global permissions, else return permissions of user on this project
   */
  public Set<String> selectPermissionsByLogin(DbSession dbSession, String userLogin, @Nullable String projectUuid) {
    PermissionQuery query = PermissionQuery.builder()
      .withAtLeastOnePermission()
      .setComponentUuid(projectUuid)
      .build();
    return select(dbSession, query, asList(userLogin)).stream()
      .map(ExtendedUserPermissionDto::getPermission)
      .collect(Collectors.toSet());
  }


  /**
   * @see UserPermissionMapper#countUsersByQuery(PermissionQuery, Collection)
   */
  public int countUsers(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countUsersByQuery(query, null);
  }

  /**
   * Count the number of users per permission for a given list of projects
   *
   * @param projectIds a non-null list of project ids to filter on. If empty then an empty list is returned.
   */
  public List<CountPerProjectPermission> countUsersByProjectPermission(DbSession dbSession, Collection<Long> projectIds) {
    return executeLargeInputs(projectIds, mapper(dbSession)::countUsersByProjectPermission);
  }

  public void insert(DbSession dbSession, UserPermissionDto dto) {
    mapper(dbSession).insert(dto);
  }

  /**
   * Delete permissions for a user, permissions for a project, or a mix of them. In all cases
   * scope can be restricted to a specified permission.
   *
   * Examples:
   * <ul>
   *   <li>{@code delete(dbSession, "marius", null, null)} deletes all permissions of Marius, including global and project permissions</li>
   *   <li>{@code delete(dbSession, null, "ABC", null)} deletes all permissions of project ABC</li>
   *   <li>{@code delete(dbSession, "marius", "ABC", null)} deletes the permissions of Marius on project "ABC"</li>
   * </ul>
   * 
   * @see UserPermissionMapper#delete(String, String, String)
   */
  public void delete(DbSession dbSession, @Nullable String login, @Nullable String projectUuid, @Nullable String permission) {
    checkArgument(isNotEmpty(login) || isNotEmpty(projectUuid), "At least one of login or project must be set");
    mapper(dbSession).delete(login, projectUuid, permission);
  }

  private static UserPermissionMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserPermissionMapper.class);
  }
}
