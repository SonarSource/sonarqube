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

import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * @since 3.2
 */
@BatchSide
@ServerSide
public class UserDao implements DaoComponent {

  private final MyBatis mybatis;
  private final System2 system2;

  public UserDao(MyBatis mybatis, System2 system2) {
    this.mybatis = mybatis;
    this.system2 = system2;
  }

  public UserDto getUser(long userId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return getUser(userId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public UserDto getUser(long userId, SqlSession session) {
    return session.getMapper(UserMapper.class).selectUser(userId);
  }

  /**
   * Search for user by login. Disabled users are ignored.
   *
   * @return the user, null if user not found
   */
  @CheckForNull
  public UserDto selectActiveUserByLogin(String login) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectActiveUserByLogin(session, login);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public UserDto selectActiveUserByLogin(DbSession session, String login) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    return mapper.selectUserByLogin(login);
  }

  public List<UserDto> selectUsersByLogins(List<String> logins) {
    List<UserDto> users = Lists.newArrayList();
    DbSession session = mybatis.openSession(false);
    try {
      users.addAll(selectUsersByLogins(session, logins));
    } finally {
      MyBatis.closeQuietly(session);
    }
    return users;
  }

  public List<UserDto> selectUsersByLogins(DbSession session, List<String> logins) {
    List<UserDto> users = Lists.newArrayList();
    if (!logins.isEmpty()) {
      UserMapper mapper = session.getMapper(UserMapper.class);
      List<List<String>> partitions = Lists.partition(logins, 1000);
      for (List<String> partition : partitions) {
        users.addAll(mapper.selectUsersByLogins(partition));
      }
    }
    return users;
  }

  public List<UserDto> selectUsers(UserQuery query) {
    SqlSession session = mybatis.openSession(false);
    try {
      UserMapper mapper = session.getMapper(UserMapper.class);
      return mapper.selectUsers(query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public UserDto insert(SqlSession session, UserDto dto) {
    session.getMapper(UserMapper.class).insert(dto);
    return dto;
  }

  public UserDto update(SqlSession session, UserDto dto) {
    session.getMapper(UserMapper.class).update(dto);
    return dto;
  }

  /**
   * Deactivate a user and drops all his preferences.
   * @return false if the user does not exist, true if the existing user has been deactivated
   */
  public boolean deactivateUserByLogin(String login) {
    SqlSession session = mybatis.openSession(false);
    try {
      UserMapper mapper = session.getMapper(UserMapper.class);
      UserDto dto = mapper.selectUserByLogin(login);
      if (dto == null) {
        return false;
      }

      mapper.removeUserFromGroups(dto.getId());
      mapper.deleteUserActiveDashboards(dto.getId());
      mapper.deleteUnsharedUserDashboards(dto.getId());
      mapper.deleteUnsharedUserIssueFilters(dto.getLogin());
      mapper.deleteUserIssueFilterFavourites(dto.getLogin());
      mapper.deleteUnsharedUserMeasureFilters(dto.getId());
      mapper.deleteUserMeasureFilterFavourites(dto.getId());
      mapper.deleteUserProperties(dto.getId());
      mapper.deleteUserRoles(dto.getId());
      mapper.deactivateUser(dto.getId(), system2.now());
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
   *
   * TODO should be moved to GroupDao
   */
  @CheckForNull
  public GroupDto selectGroupByName(String name, DbSession session) {
    UserMapper mapper = session.getMapper(UserMapper.class);
    return mapper.selectGroupByName(name);
  }

  /**
   * TODO should be moved to GroupDao
   */
  @CheckForNull
  public GroupDto selectGroupByName(String name) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectGroupByName(name, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
