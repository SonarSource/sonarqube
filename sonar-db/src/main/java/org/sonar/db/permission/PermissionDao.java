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

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Only the operations involving both user and group permissions.
 *
 * @see GroupPermissionDao
 * @see UserPermissionDao
 */
public class PermissionDao implements Dao {

  private static final String USER_ID_PARAM = "userId";
  private final MyBatis mybatis;

  public PermissionDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public Collection<Long> keepAuthorizedProjectIds(DbSession session, Collection<Long> componentIds, @Nullable Integer userId, String role) {
    return executeLargeInputs(
      componentIds,
      partition -> {
        if (userId == null) {
          return session.getMapper(PermissionMapper.class).keepAuthorizedProjectIdsForAnonymous(role, componentIds);
        }
        return session.getMapper(PermissionMapper.class).keepAuthorizedProjectIdsForUser(userId, role, componentIds);
      });
  }

  public Collection<String> selectAuthorizedRootProjectsKeys(DbSession dbSession, @Nullable Integer userId, String role) {
    String sql;
    Map<String, Object> params = new HashMap<>(2);
    sql = "selectAuthorizedRootProjectsKeys";
    params.put(USER_ID_PARAM, userId);
    params.put("role", role);

    return dbSession.selectList(sql, params);
  }

  public Collection<String> selectAuthorizedRootProjectsUuids(DbSession dbSession, @Nullable Integer userId, String role) {
    String sql;
    Map<String, Object> params = new HashMap<>(2);
    sql = "selectAuthorizedRootProjectsUuids";
    params.put(USER_ID_PARAM, userId);
    params.put("role", role);

    return dbSession.selectList(sql, params);
  }

  public List<String> selectGlobalPermissions(@Nullable String userLogin) {
    DbSession session = mybatis.openSession(false);
    try {
      Map<String, Object> params = new HashMap<>(1);
      params.put("userLogin", userLogin);
      return session.selectList("selectGlobalPermissions", params);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Keep only authorized user that have the given permission on a given project.
   * Please Note that if the permission is 'Anyone' is NOT taking into account by thie method.
   */
  public Collection<Long> keepAuthorizedUsersForRoleAndProject(final DbSession session, Collection<Long> userIds, String role, final long projectId) {
    return executeLargeInputs(
      userIds,
      partitionOfIds -> session.getMapper(PermissionMapper.class).keepAuthorizedUsersForRoleAndProject(role, projectId, partitionOfIds));
  }

  public boolean isAuthorizedComponentKey(String componentKey, @Nullable Integer userId, String role) {
    DbSession session = mybatis.openSession(false);
    try {
      return keepAuthorizedComponentKeys(session, componentKey, userId, role).size() == 1;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static List<String> keepAuthorizedComponentKeys(final DbSession session, final String componentKey, @Nullable final Integer userId, final String role) {
    if (userId == null) {
      return session.getMapper(PermissionMapper.class).keepAuthorizedComponentKeysForAnonymous(role, Sets.newHashSet(componentKey));
    } else {
      return session.getMapper(PermissionMapper.class).keepAuthorizedComponentKeysForUser(userId, role, Sets.newHashSet(componentKey));
    }
  }

}
