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

package org.sonar.db.permission;

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

import static com.google.common.collect.Maps.newHashMap;

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
  public List<UserWithPermissionDto> selectUsers(DbSession session, PermissionQuery query, @Nullable Long componentId, int offset, int limit) {
    Map<String, Object> params = usersParameters(query, componentId);

    return mapper(session).selectUsers(params, new RowBounds(offset, limit));
  }

  public int countUsers(DbSession session, PermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = usersParameters(query, componentId);

    return mapper(session).countUsers(params);
  }

  private static Map<String, Object> usersParameters(PermissionQuery query, @Nullable Long componentId) {
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
  public List<GroupWithPermissionDto> selectGroups(DbSession session, PermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = groupsParameters(query, componentId);
    return mapper(session).selectGroups(params);
  }

  public List<GroupWithPermissionDto> selectGroups(PermissionQuery query, @Nullable Long componentId) {
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
  public void usersCountByComponentIdAndPermission(final DbSession dbSession, List<Long> componentIds, final ResultHandler resultHandler) {
    final Map<String, Object> parameters = new HashMap<>();

    DatabaseUtils.executeLargeInputsWithoutOutput(componentIds, new Function<List<Long>, Void>() {
      @Override
      public Void apply(@Nonnull List<Long> partitionedComponentIds) {
        parameters.put("componentIds", partitionedComponentIds);
        mapper(dbSession).usersCountByProjectIdAndPermission(parameters, resultHandler);
        return null;
      }
    });
  }

  /**
   * Each row returns a CountByProjectAndPermissionDto
   */
  public void groupsCountByComponentIdAndPermission(final DbSession dbSession, final List<Long> componentIds, final ResultHandler resultHandler) {
    final Map<String, Object> parameters = new HashMap<>();
    parameters.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);

    DatabaseUtils.executeLargeInputsWithoutOutput(componentIds, new Function<List<Long>, Void>() {
      @Override
      public Void apply(@Nonnull List<Long> partitionedComponentIds) {
        parameters.put("componentIds", partitionedComponentIds);
        mapper(dbSession).groupsCountByProjectIdAndPermission(parameters, resultHandler);
        return null;
      }
    });
  }

  private static Map<String, Object> groupsParameters(PermissionQuery query, @Nullable Long componentId) {
    Map<String, Object> params = newHashMap();
    params.put(QUERY_PARAMETER, query);
    params.put(COMPONENT_ID_PARAMETER, componentId);
    params.put(ANYONE_GROUP_PARAMETER, DefaultGroups.ANYONE);
    return params;
  }

  private PermissionMapper mapper(SqlSession session) {
    return session.getMapper(PermissionMapper.class);
  }
}
