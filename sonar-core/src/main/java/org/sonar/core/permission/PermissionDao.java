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

package org.sonar.core.permission;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerSide;
import org.sonar.api.security.DefaultGroups;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@ServerSide
public class PermissionDao {

  private static final String QUERY_PARAMETER = "query";
  private static final String COMPONENT_ID_PARAMETER = "componentId";

  private final MyBatis myBatis;

  public PermissionDao(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  /**
   * @return a paginated list of users.
   */
  public List<UserWithPermissionDto> selectUsers(PermissionQuery query, @Nullable Long componentId, int offset, int limit) {
    SqlSession session = myBatis.openSession(false);
    try {
      Map<String, Object> params = newHashMap();
      params.put(QUERY_PARAMETER, query);
      params.put(COMPONENT_ID_PARAMETER, componentId);
      return session.selectList("org.sonar.core.permission.PermissionMapper.selectUsers", params, new RowBounds(offset, limit));
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  List<UserWithPermissionDto> selectUsers(PermissionQuery query, @Nullable Long componentId) {
    return selectUsers(query, componentId, 0, Integer.MAX_VALUE);
  }

  /**
   * 'Anyone' group is not returned when it has not the asked permission.
   * Membership parameter from query is not taking into account in order to deal more easily with the 'Anyone' group
   * @return a non paginated list of groups.
   */
  public List<GroupWithPermissionDto> selectGroups(PermissionQuery query, @Nullable Long componentId) {
    SqlSession session = myBatis.openSession(false);
    try {
      Map<String, Object> params = newHashMap();
      params.put(QUERY_PARAMETER, query);
      params.put(COMPONENT_ID_PARAMETER, componentId);
      params.put("anyoneGroup", DefaultGroups.ANYONE);
      return session.selectList("org.sonar.core.permission.PermissionMapper.selectGroups", params);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
