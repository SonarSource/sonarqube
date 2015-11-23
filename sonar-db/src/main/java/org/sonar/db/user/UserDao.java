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
package org.sonar.db.user;

import com.google.common.base.Function;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.RowNotFoundException;

public class UserDao implements Dao {

  private final MyBatis mybatis;
  private final System2 system2;

  public UserDao(MyBatis mybatis, System2 system2) {
    this.mybatis = mybatis;
    this.system2 = system2;
  }

  public UserDto selectUserById(long userId) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectUserById(session, userId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public UserDto selectUserById(SqlSession session, long userId) {
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

  /**
   * Select users by logins, including disabled users. An empty list is returned
   * if list of logins is empty, without any db round trips.
   */
  public List<UserDto> selectByLogins(DbSession session, Collection<String> logins) {
    return DatabaseUtils.executeLargeInputs(logins, new SelectByLogins(mapper(session)));
  }

  public List<UserDto> selectByLogins(Collection<String> logins) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByLogins(session, logins);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static class SelectByLogins implements Function<List<String>, List<UserDto>> {
    private final UserMapper mapper;

    private SelectByLogins(UserMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public List<UserDto> apply(@Nonnull List<String> partitionOfLogins) {
      return mapper.selectByLogins(partitionOfLogins);
    }
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
  public boolean deactivateUserByLogin(DbSession dbSession, String login) {
    UserMapper mapper = dbSession.getMapper(UserMapper.class);
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
    dbSession.commit();
    return true;
  }

  @CheckForNull
  public UserDto selectByLogin(DbSession session, String login) {
    return mapper(session).selectByLogin(login);
  }

  public UserDto selectOrFailByLogin(DbSession session, String login) {
    UserDto user = selectByLogin(session, login);
    if (user == null) {
      throw new RowNotFoundException(String.format("User with login '%s' has not been found", login));
    }
    return user;
  }

  public List<UserDto> selectByScmAccountOrLoginOrEmail(DbSession session, String scmAccountOrLoginOrEmail) {
    String like = new StringBuilder().append("%")
      .append(UserDto.SCM_ACCOUNTS_SEPARATOR).append(scmAccountOrLoginOrEmail)
      .append(UserDto.SCM_ACCOUNTS_SEPARATOR).append("%").toString();
    return mapper(session).selectNullableByScmAccountOrLoginOrEmail(scmAccountOrLoginOrEmail, like);
  }

  protected UserMapper mapper(DbSession session) {
    return session.getMapper(UserMapper.class);
  }
}
