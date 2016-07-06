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
package org.sonar.db.user;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.RowNotFoundException;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

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
   * Select users by ids, including disabled users. An empty list is returned
   * if list of ids is empty, without any db round trips.
   *
   * Used by the Governance plugin
   */
  public List<UserDto> selectByIds(DbSession session, Collection<Long> ids) {
    return executeLargeInputs(ids, session.getMapper(UserMapper.class)::selectByIds);
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
    return executeLargeInputs(logins, session.getMapper(UserMapper.class)::selectByLogins);
  }

  /**
   * @deprecated since 6.0 please use {@link #selectByLogins(DbSession, Collection)} instead
   */
  @Deprecated
  public List<UserDto> selectByLogins(Collection<String> logins) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectByLogins(session, logins);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Gets a list users by their logins. The result does NOT contain {@code null} values for users not found, so
   * the size of result may be less than the number of keys.
   * A single user is returned if input keys contain multiple occurrences of a key.
   * <p>Contrary to {@link #selectByLogins(DbSession, Collection)}, results are in the same order as input keys.</p>
   */
  public List<UserDto> selectByOrderedLogins(DbSession session, Collection<String> logins) {
    List<UserDto> unordered = selectByLogins(session, logins);
    return from(logins).transform(new LoginToUser(unordered)).filter(Predicates.notNull()).toList();
  }

  public List<UserDto> selectUsers(UserQuery query) {
    DbSession session = mybatis.openSession(false);
    try {
      return selectUsers(session, query);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<UserDto> selectUsers(DbSession dbSession, UserQuery query) {
    UserMapper mapper = dbSession.getMapper(UserMapper.class);
    return mapper.selectUsers(query);
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
    mapper.deletePropertiesMatchingLogin(asList(DEFAULT_ISSUE_ASSIGNEE), dto.getLogin());
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

  /**
   * Check if an active user with the given email exits in database
   *
   * Please note that email is case insensitive, result for searching 'mail@email.com' or 'Mail@Email.com' will be the same
   */
  public boolean doesEmailExist(DbSession dbSession, String email) {
    return mapper(dbSession).countByEmail(email.toLowerCase()) > 0;
  }

  protected UserMapper mapper(DbSession session) {
    return session.getMapper(UserMapper.class);
  }

  private static class LoginToUser implements Function<String, UserDto> {
    private final Map<String, UserDto> map = new HashMap<>();

    private LoginToUser(Collection<UserDto> unordered) {
      for (UserDto dto : unordered) {
        map.put(dto.getLogin(), dto);
      }
    }

    @Override
    public UserDto apply(@Nonnull String login) {
      return map.get(login);
    }
  }

}
