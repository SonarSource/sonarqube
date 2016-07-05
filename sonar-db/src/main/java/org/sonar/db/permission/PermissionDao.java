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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.user.UserPermissionDto;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class PermissionDao implements Dao {

  private static final String QUERY_PARAMETER = "query";
  private static final String COMPONENT_ID_PARAMETER = "componentId";
  private static final String ANYONE_GROUP_PARAMETER = "anyoneGroup";

  private final MyBatis myBatis;

  public PermissionDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  /**
   * @return a paginated list of users.
   */
  public List<UserWithPermissionDto> selectUsers(DbSession session, OldPermissionQuery query, @Nullable Long componentId, int offset, int limit) {
    Map<String, Object> params = usersParameters(query, componentId);

    return mapper(session).selectUsers(params, new RowBounds(offset, limit));
  }

  /**
   * Each row returns <code>{@link UserRef}</code>, ordered by user names
   */
  public void selectUsersByQuery(DbSession dbSession, PermissionQuery query, ResultHandler handler) {
    mapper(dbSession).selectUsersByQuery(query, new RowBounds(query.getPageOffset(), query.getPageSize()), handler);
  }

  public int countUsersByQuery(DbSession dbSession, PermissionQuery query) {
    return mapper(dbSession).countUsersByQuery(query);
  }

  public List<UserPermissionDto> selectUserPermissionsByQuery(DbSession dbSession, PermissionQuery query) {
    if (query.getLogins() != null && query.getLogins().isEmpty()) {
      return emptyList();
    }

    return mapper(dbSession).selectUserPermissionsByQuery(query);
  }

  public int countUsers(DbSession session, OldPermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = usersParameters(query, componentId);

    return mapper(session).countUsers(params);
  }

  private static Map<String, Object> usersParameters(OldPermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = newHashMap();
    params.put(QUERY_PARAMETER, query);
    params.put(COMPONENT_ID_PARAMETER, componentId);
    return params;
  }

  /**
   * 'Anyone' group is not returned when it has not the asked permission.
   * Membership parameter from query is not taking into account in order to deal more easily with the 'Anyone' group
   *
   * @return a non paginated list of groups.
   */
  public List<GroupWithPermissionDto> selectGroups(DbSession session, OldPermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = groupsParameters(query, componentId);
    return mapper(session).selectGroups(params);
  }

  public List<GroupWithPermissionDto> selectGroups(OldPermissionQuery query, @Nullable Long componentId) {
    DbSession session = myBatis.openSession(false);
    try {
      return selectGroups(session, query, componentId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countGroups(DbSession session, String permission, @Nullable Long componentId) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("permission", permission);
    parameters.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);
    parameters.put(COMPONENT_ID_PARAMETER, componentId);

    return mapper(session).countGroups(parameters);
  }

  /**
   * Each row returns a CountByProjectAndPermissionDto
   */
  public void usersCountByComponentIdAndPermission(DbSession dbSession, List<Long> componentIds, ResultHandler resultHandler) {
    Map<String, Object> parameters = new HashMap<>(1);

    executeLargeInputsWithoutOutput(
      componentIds,
      partitionedComponentIds -> {
        parameters.put("componentIds", partitionedComponentIds);
        mapper(dbSession).usersCountByProjectIdAndPermission(parameters, resultHandler);
        return null;
      });
  }

  /**
   * Each row returns a CountByProjectAndPermissionDto
   */
  public void groupsCountByComponentIdAndPermission(DbSession dbSession, List<Long> componentIds, ResultHandler resultHandler) {
    Map<String, Object> parameters = new HashMap<>(2);
    parameters.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);

    executeLargeInputsWithoutOutput(
      componentIds,
      partitionedComponentIds -> {
        parameters.put("componentIds", partitionedComponentIds);
        mapper(dbSession).groupsCountByProjectIdAndPermission(parameters, resultHandler);
        return null;
      });
  }

  private static Map<String, Object> groupsParameters(OldPermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = newHashMap();
    params.put(QUERY_PARAMETER, query);
    params.put(COMPONENT_ID_PARAMETER, componentId);
    params.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);
    return params;
  }

  private static PermissionMapper mapper(SqlSession session) {
    return session.getMapper(PermissionMapper.class);
  }

}
