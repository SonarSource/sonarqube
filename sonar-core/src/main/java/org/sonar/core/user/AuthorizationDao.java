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
package org.sonar.core.user;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

public class AuthorizationDao implements ServerComponent {

  private final MyBatis mybatis;

  public AuthorizationDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public Set<Long> keepAuthorizedComponentIds(Set<Long> componentIds, @Nullable Integer userId, String role) {
    SqlSession session = mybatis.openSession();
    try {
      return keepAuthorizedComponentIds(componentIds, userId, role, session);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Set<Long> keepAuthorizedComponentIds(Set<Long> componentIds, @Nullable Integer userId, String role, SqlSession session) {
    if (componentIds.isEmpty()) {
      return Collections.emptySet();
    }
    String sql;
    Map<String, Object> params;
    if (userId == null) {
      sql = "keepAuthorizedComponentIdsForAnonymous";
      params = ImmutableMap.of("role", role, "componentIds", componentIds);
    } else {
      sql = "keepAuthorizedComponentIdsForUser";
      params = ImmutableMap.of("userId", userId, "role", role, "componentIds", componentIds);
    }

    return Sets.newHashSet(session.<Long>selectList(sql, params));
  }

  public boolean isAuthorizedComponentId(long componentId, @Nullable Integer userId, String role) {
    return keepAuthorizedComponentIds(Sets.newHashSet(componentId), userId, role).size() == 1;
  }

  public Collection<Long> selectAuthorizedRootProjectsIds(@Nullable Integer userId, String role) {
    SqlSession session = mybatis.openSession();
    try {
      return selectAuthorizedRootProjectsIds(userId, role, session);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<Long> selectAuthorizedRootProjectsIds(@Nullable Integer userId, String role, SqlSession session) {
    String sql;
    Map<String, Object> params = newHashMap();
    sql = "selectAuthorizedRootProjectsIds";
    params.put("userId", userId);
    params.put("role", role);

    return session.selectList(sql, params);
  }
}
