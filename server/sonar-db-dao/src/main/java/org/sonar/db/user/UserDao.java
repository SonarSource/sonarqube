/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;

public class UserDao implements Dao {

  private final System2 system2;

  public UserDao(System2 system2) {
    this.system2 = system2;
  }

  @CheckForNull
  public UserDto selectUserById(DbSession session, int userId) {
    return mapper(session).selectUser(userId);
  }

  /**
   * Select users by ids, including disabled users. An empty list is returned
   * if list of ids is empty, without any db round trips.
   *
   * Used by the Governance plugin
   */
  public List<UserDto> selectByIds(DbSession session, Collection<Integer> ids) {
    return executeLargeInputs(ids, mapper(session)::selectByIds);
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
   * Gets a list users by their logins. The result does NOT contain {@code null} values for users not found, so
   * the size of result may be less than the number of keys.
   * A single user is returned if input keys contain multiple occurrences of a key.
   * <p>Contrary to {@link #selectByLogins(DbSession, Collection)}, results are in the same order as input keys.</p>
   */
  public List<UserDto> selectByOrderedLogins(DbSession session, Collection<String> logins) {
    List<UserDto> unordered = selectByLogins(session, logins);
    return logins.stream()
      .map(new LoginToUser(unordered))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public List<UserDto> selectUsers(DbSession dbSession, UserQuery query) {
    return mapper(dbSession).selectUsers(query);
  }

  public long countRootUsersButLogin(DbSession dbSession, String login) {
    return mapper(dbSession).countRootUsersButLogin(login);
  }

  public UserDto insert(DbSession session, UserDto dto) {
    long now = system2.now();
    mapper(session).insert(dto, now);
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);
    return dto;
  }

  public UserDto update(DbSession session, UserDto dto) {
    long now = system2.now();
    mapper(session).update(dto, now);
    dto.setUpdatedAt(now);
    return dto;
  }

  public void setRoot(DbSession session, String login, boolean root) {
    mapper(session).setRoot(login, root, system2.now());
  }

  public void deactivateUser(DbSession dbSession, UserDto user) {
    mapper(dbSession).deactivateUser(user.getLogin(), system2.now());
  }

  public void cleanHomepage(DbSession dbSession, OrganizationDto organization) {
    mapper(dbSession).clearHomepage("ORGANIZATION", organization.getUuid(), system2.now());
  }

  public void cleanHomepage(DbSession dbSession, ComponentDto project) {
    mapper(dbSession).clearHomepage("PROJECT", project.uuid(), system2.now());
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
   * Search for an active user with the given email exits in database
   *
   * Please note that email is case insensitive, result for searching 'mail@email.com' or 'Mail@Email.com' will be the same
   */
  @CheckForNull
  public UserDto selectByEmail(DbSession dbSession, String email) {
    return mapper(dbSession).selectByEmail(email.toLowerCase(Locale.ENGLISH));
  }

  public void scrollByLogins(DbSession dbSession, Collection<String> logins, Consumer<UserDto> consumer) {
    UserMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(logins,
      pageOfLogins -> mapper
        .selectByLogins(pageOfLogins)
        .forEach(consumer));
  }

  public void scrollAll(DbSession dbSession, Consumer<UserDto> consumer) {
    mapper(dbSession).scrollAll(context -> {
      UserDto user = context.getResultObject();
      consumer.accept(user);
    });
  }

  private static UserMapper mapper(DbSession session) {
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
