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

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.user.UserQuery;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * @since 3.2
 */
public class UserDao {
  private final MyBatis mybatis;

  public UserDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * Search for user by login. Disabled users are ignored.
   *
   * @return the user, null if user not found
   */
  @CheckForNull
  public UserDto selectActiveUserByLogin(String login) {
    SqlSession session = mybatis.openSession();
    try {
      return selectActiveUserByLogin(login, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public UserDto selectActiveUserByLogin(String login, SqlSession session) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    return mapper.selectUserByLogin(login);
  }

  public List<UserDto> selectUsersByLogins(List<String> logins) {
    List<UserDto> users = Lists.newArrayList();
    if (!logins.isEmpty()) {
      SqlSession session = mybatis.openSession();
      try {
        UserMapper mapper = session.getMapper(UserMapper.class);
        List<List<String>> partitions = Lists.partition(logins, 1000);
        for (List<String> partition : partitions) {
          users.addAll(mapper.selectUsersByLogins(partition));
        }
      } finally {
        MyBatis.closeQuietly(session);
      }
    }
    return users;
  }

  public List<UserDto> selectUsers(UserQuery query) {
    SqlSession session = mybatis.openSession();
    try {
      UserMapper mapper = session.getMapper(UserMapper.class);
      return mapper.selectUsers(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Deactivate a user and drops all his preferences.
   * @return false if the user does not exist, true if the existing user has been deactivated
   */
  public boolean deactivateUserByLogin(String login) {
    SqlSession session = mybatis.openSession();
    try {
      UserMapper mapper = session.getMapper(UserMapper.class);
      UserDto dto = mapper.selectUserByLogin(login);
      if (dto == null) {
        return false;
      }

      mapper.removeUserFromGroups(dto.getId());
      mapper.deleteUserActiveDashboards(dto.getId());
      mapper.deleteUserDashboards(dto.getId());
      mapper.deleteUserIssueFilters(dto.getLogin());
      mapper.deleteUserIssueFilterFavourites(dto.getLogin());
      mapper.deleteUserMeasureFilters(dto.getId());
      mapper.deleteUserMeasureFilterFavourites(dto.getId());
      mapper.deleteUserProperties(dto.getId());
      mapper.deleteUserRoles(dto.getId());
      mapper.deactivateUser(dto.getId());
      session.commit();
      return true;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Search for group by name.
   *
   * @return the group, null if group not found
   */
  @CheckForNull
  public GroupDto selectGroupByName(String name, SqlSession session) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    return mapper.selectGroupByName(name);
  }

  @CheckForNull
  public GroupDto selectGroupByName(String name) {
    SqlSession session = mybatis.openSession();
    try {
      return selectGroupByName(name, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
