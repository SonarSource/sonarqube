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
package org.sonar.core.user;

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@ServerSide
public class AuthorizationDao implements DaoComponent {

  private static final String USER_ID_PARAM = "userId";
  private final MyBatis mybatis;

  public AuthorizationDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public Collection<Long> keepAuthorizedProjectIds(final DbSession session, final Collection<Long> componentIds, @Nullable final Integer userId, final String role) {
    if (componentIds.isEmpty()) {
      return Collections.emptySet();
    }
    return DaoUtils.executeLargeInputs(componentIds, new Function<List<Long>, List<Long>>() {
      @Override
      public List<Long> apply(List<Long> partition) {
        if (userId == null) {
          return session.getMapper(AuthorizationMapper.class).keepAuthorizedProjectIdsForAnonymous(role, componentIds);
        } else {
          return session.getMapper(AuthorizationMapper.class).keepAuthorizedProjectIdsForUser(userId, role, componentIds);
        }
      }
    });
  }

  /**
   * Used by the Views Plugin
   */
  public boolean isAuthorizedComponentKey(String componentKey, @Nullable Integer userId, String role) {
    DbSession session = mybatis.openSession(false);
    try {
      return keepAuthorizedComponentKeys(session, componentKey, userId, role).size() == 1;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private List<String> keepAuthorizedComponentKeys(final DbSession session, final String componentKey, @Nullable final Integer userId, final String role) {
    if (userId == null) {
      return session.getMapper(AuthorizationMapper.class).keepAuthorizedComponentKeysForAnonymous(role, Sets.newHashSet(componentKey));
    } else {
      return session.getMapper(AuthorizationMapper.class).keepAuthorizedComponentKeysForUser(userId, role, Sets.newHashSet(componentKey));
    }
  }

  public Collection<String> selectAuthorizedRootProjectsKeys(@Nullable Integer userId, String role) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectAuthorizedRootProjectsKeys(userId, role, session);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<String> selectAuthorizedRootProjectsUuids(@Nullable Integer userId, String role) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectAuthorizedRootProjectsUuids(userId, role, session);

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public Collection<String> selectAuthorizedRootProjectsKeys(@Nullable Integer userId, String role, SqlSession session) {
    String sql;
    Map<String, Object> params = newHashMap();
    sql = "selectAuthorizedRootProjectsKeys";
    params.put(USER_ID_PARAM, userId);
    params.put("role", role);

    return session.selectList(sql, params);
  }

  public Collection<String> selectAuthorizedRootProjectsUuids(@Nullable Integer userId, String role, SqlSession session) {
    String sql;
    Map<String, Object> params = newHashMap();
    sql = "selectAuthorizedRootProjectsUuids";
    params.put(USER_ID_PARAM, userId);
    params.put("role", role);

    return session.selectList(sql, params);
  }

  public List<String> selectGlobalPermissions(@Nullable String userLogin) {
    SqlSession session = mybatis.openSession(false);
    try {
      Map<String, Object> params = newHashMap();
      params.put("userLogin", userLogin);
      return session.selectList("selectGlobalPermissions", params);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
