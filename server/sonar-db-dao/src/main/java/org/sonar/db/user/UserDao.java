/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.api.user.UserQuery;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserNewValue;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Locale.ENGLISH;
import static java.util.concurrent.TimeUnit.DAYS;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.user.UserDto.SCM_ACCOUNTS_SEPARATOR;

public class UserDao implements Dao {
  private static final long WEEK_IN_MS = DAYS.toMillis(7L);
  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public UserDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  @CheckForNull
  public UserDto selectByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
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
   * Select users by uuids, including disabled users. An empty list is returned
   * if list of uuids is empty, without any db round trips.
   */
  public List<UserDto> selectByUuids(DbSession session, Collection<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectByUuids);
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
      .toList();
  }

  public List<UserDto> selectUsers(DbSession dbSession, UserQuery query) {
    return mapper(dbSession).selectUsers(query);
  }

  public List<UserTelemetryDto> selectUsersForTelemetry(DbSession dbSession) {
    return mapper(dbSession).selectUsersForTelemetry();
  }

  public UserDto insert(DbSession session, UserDto dto) {
    long now = system2.now();
    mapper(session).insert(dto.setUuid(uuidFactory.create()).setCreatedAt(now).setUpdatedAt(now));
    auditPersister.addUser(session, new UserNewValue(dto.getUuid(), dto.getLogin()));
    return dto;
  }

  public UserDto update(DbSession session, UserDto dto) {
    return update(session, dto, true);
  }

  public UserDto update(DbSession session, UserDto dto, boolean track) {
    mapper(session).update(dto.setUpdatedAt(system2.now()));
    if (track) {
      auditPersister.updateUser(session, new UserNewValue(dto));
    }
    return dto;
  }

  public void updateSonarlintLastConnectionDate(DbSession session, String login) {
    mapper(session).updateSonarlintLastConnectionDate(login, system2.now());
  }

  public void deactivateUser(DbSession dbSession, UserDto user) {
    mapper(dbSession).deactivateUser(user.getLogin(), system2.now());
    auditPersister.deactivateUser(dbSession, new UserNewValue(user.getUuid(), user.getLogin()));
  }

  public void cleanHomepage(DbSession dbSession, ProjectDto project) {
    mapper(dbSession).clearHomepages("PROJECT", project.getUuid(), system2.now());
  }

  public void cleanHomepage(DbSession dbSession, ComponentDto project) {
    mapper(dbSession).clearHomepages("PROJECT", project.uuid(), system2.now());
  }

  public void cleanHomepage(DbSession dbSession, UserDto user) {
    mapper(dbSession).clearHomepage(user.getLogin(), system2.now());
  }

  @CheckForNull
  public UserDto selectByLogin(DbSession session, String login) {
    return mapper(session).selectByLogin(login);
  }

  public List<UserDto> selectByScmAccountOrLoginOrEmail(DbSession session, String scmAccountOrLoginOrEmail) {
    String like = new StringBuilder().append("%")
      .append(SCM_ACCOUNTS_SEPARATOR).append(scmAccountOrLoginOrEmail)
      .append(SCM_ACCOUNTS_SEPARATOR).append("%").toString();
    return mapper(session).selectNullableByScmAccountOrLoginOrEmail(scmAccountOrLoginOrEmail, like);
  }

  /**
   * Search for an active user with the given emailCaseInsensitive exits in database
   *
   * Select is case insensitive. Result for searching 'mail@emailCaseInsensitive.com' or 'Mail@Email.com' is the same
   */
  public List<UserDto> selectByEmail(DbSession dbSession, String emailCaseInsensitive) {
    return mapper(dbSession).selectByEmail(emailCaseInsensitive.toLowerCase(ENGLISH));
  }

  @CheckForNull
  public UserDto selectByExternalIdAndIdentityProvider(DbSession dbSession, String externalId, String externalIdentityProvider) {
    return mapper(dbSession).selectByExternalIdAndIdentityProvider(externalId, externalIdentityProvider);
  }

  public List<String> selectExternalIdentityProviders(DbSession dbSession) {
    return mapper(dbSession).selectExternalIdentityProviders();
  }

  public List<UserDto> selectByExternalIdsAndIdentityProvider(DbSession dbSession, Collection<String> externalIds, String externalIdentityProvider) {
    return executeLargeInputs(externalIds, e -> mapper(dbSession).selectByExternalIdsAndIdentityProvider(e, externalIdentityProvider));
  }

  @CheckForNull
  public UserDto selectByExternalLoginAndIdentityProvider(DbSession dbSession, String externalLogin, String externalIdentityProvider) {
    return mapper(dbSession).selectByExternalLoginAndIdentityProvider(externalLogin, externalIdentityProvider);
  }

  public List<UserDto> selectByExternalLogin(DbSession dbSession, String externalLogin) {
    return mapper(dbSession).selectByExternalLogin(externalLogin);
  }


  public long countSonarlintWeeklyUsers(DbSession dbSession) {
    long threshold = system2.now() - WEEK_IN_MS;
    return mapper(dbSession).countActiveSonarlintUsers(threshold);
  }

  public void scrollByUuids(DbSession dbSession, Collection<String> uuids, Consumer<UserDto> consumer) {
    UserMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(uuids,
      pageOfUuids -> mapper
        .selectByUuids(pageOfUuids)
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
