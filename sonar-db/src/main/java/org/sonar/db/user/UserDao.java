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
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
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
    try (DbSession session = mybatis.openSession(false)) {
      return selectUserById(session, userId);
    }
  }

  public UserDto selectUserById(DbSession session, long userId) {
    return mapper(session).selectUser(userId);
  }

  /**
   * Select users by ids, including disabled users. An empty list is returned
   * if list of ids is empty, without any db round trips.
   *
   * Used by the Governance plugin
   */
  public List<UserDto> selectByIds(DbSession session, Collection<Long> ids) {
    return executeLargeInputs(ids, mapper(session)::selectByIds);
  }

  /**
   * Search for user by login. Disabled users are ignored.
   *
   * @return the user, null if user not found
   */
  @CheckForNull
  public UserDto selectActiveUserByLogin(String login) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectActiveUserByLogin(session, login);
    }
  }

  @CheckForNull
  public UserDto selectActiveUserByLogin(DbSession session, String login) {
    UserMapper mapper = mapper(session);
    return mapper.selectUserByLogin(login);
  }

  /**
   * Select users by logins, including disabled users. An empty list is returned
   * if list of logins is empty, without any db round trips.
   */
  public List<UserDto> selectByLogins(DbSession session, Collection<String> logins) {
    return executeLargeInputs(logins, mapper(session)::selectByLogins);
  }

  /**
   * @deprecated since 6.0 please use {@link #selectByLogins(DbSession, Collection)} instead
   */
  @Deprecated
  public List<UserDto> selectByLogins(Collection<String> logins) {
    try (DbSession session = mybatis.openSession(false)) {
      return selectByLogins(session, logins);
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
    try (DbSession session = mybatis.openSession(false)) {
      return selectUsers(session, query);
    }
  }

  public List<UserDto> selectUsers(DbSession dbSession, UserQuery query) {
    return mapper(dbSession).selectUsers(query);
  }

  public long countRootUsersButLogin(DbSession dbSession, String login) {
    return mapper(dbSession).countRootUsersButLogin(login);
  }

  public UserDto insert(DbSession session, UserDto dto) {
    mapper(session).insert(dto);
    return dto;
  }

  public UserDto update(DbSession session, UserDto dto) {
    mapper(session).update(dto);
    return dto;
  }

  public void setRoot(DbSession session, String login, boolean root) {
    mapper(session).setRoot(login, root, system2.now());
  }

  /**
   * Deactivate a user and drops all his preferences.
   * @return false if the user does not exist, true if the existing user has been deactivated
   */
  public boolean deactivateUserByLogin(DbSession dbSession, String login) {
    UserMapper mapper = mapper(dbSession);
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
    return mapper(dbSession).countByEmail(email.toLowerCase(Locale.ENGLISH)) > 0;
  }

  /**
   * Ensures the specified user has its root flag set or unset depending on whether the user has the 'admin' permission
   * in the default organization or not.
   */
  public void updateRootFlagFromPermissions(DbSession dbSession, long userId, String defaultOrganizationUuid) {
    long now = system2.now();
    UserMapper mapper = mapper(dbSession);
    mapper.updateRootUser(userId, defaultOrganizationUuid, now);
    mapper.updateNonRootUser(userId, defaultOrganizationUuid, now);
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
