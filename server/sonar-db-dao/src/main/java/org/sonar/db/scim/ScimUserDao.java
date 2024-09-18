/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.scim;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagineable;

import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class ScimUserDao implements Dao {
  private final UuidFactory uuidFactory;

  public ScimUserDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public List<ScimUserDto> findAll(DbSession dbSession) {
    return mapper(dbSession).findAll();
  }

  public Optional<ScimUserDto> findByScimUuid(DbSession dbSession, String scimUserUuid) {
    return Optional.ofNullable(mapper(dbSession).findByScimUuid(scimUserUuid));
  }

  public Optional<ScimUserDto> findByUserUuid(DbSession dbSession, String userUuid) {
    return Optional.ofNullable(mapper(dbSession).findByUserUuid(userUuid));
  }

  public ScimUserDto enableScimForUser(DbSession dbSession, String userUuid) {
    ScimUserDto scimUserDto = new ScimUserDto(uuidFactory.create(), userUuid);
    mapper(dbSession).insert(scimUserDto);
    return scimUserDto;
  }

  public List<ScimUserWithUsernameDto> findScimUsers(DbSession dbSession, ScimUserQuery scimUserQuery, Pagineable pagination) {
    checkState(scimUserQuery.getUserUuids() == null || scimUserQuery.getScimUserUuids() == null,
      "Only one of userUuids & scimUserUuids request parameter is supported.");
    if (scimUserQuery.getScimUserUuids() != null) {
      return executeLargeInputs(
        scimUserQuery.getScimUserUuids(),
        partialSetOfUsers -> createPartialQuery(scimUserQuery, partialSetOfUsers,
          (builder, scimUserUuids) -> builder.scimUserUuids(new HashSet<>(scimUserUuids)),
          dbSession, pagination)
      );
    }
    if (scimUserQuery.getUserUuids() != null) {
      return executeLargeInputs(
        scimUserQuery.getUserUuids(),
        partialSetOfUsers -> createPartialQuery(scimUserQuery, partialSetOfUsers,
          (builder, userUuids) -> builder.userUuids(new HashSet<>(userUuids)),
          dbSession, pagination)
      );
    }

    return mapper(dbSession).findScimUsers(scimUserQuery, pagination);
  }

  private static List<ScimUserWithUsernameDto> createPartialQuery(ScimUserQuery completeQuery, List<String> strings,
    BiFunction<ScimUserQuery.ScimUserQueryBuilder, List<String>, ScimUserQuery.ScimUserQueryBuilder> queryModifier,
    DbSession dbSession, Pagineable pagination) {

    ScimUserQuery.ScimUserQueryBuilder partialScimUserQuery = ScimUserQuery.builder()
      .userName(completeQuery.getUserName());
    partialScimUserQuery = queryModifier.apply(partialScimUserQuery, strings);
    return mapper(dbSession).findScimUsers(partialScimUserQuery.build(), pagination);
  }

  public int countScimUsers(DbSession dbSession, ScimUserQuery scimUserQuery) {
    return mapper(dbSession).countScimUsers(scimUserQuery);
  }

  private static ScimUserMapper mapper(DbSession session) {
    return session.getMapper(ScimUserMapper.class);
  }

  public void deleteByUserUuid(DbSession dbSession, String userUuid) {
    mapper(dbSession).deleteByUserUuid(userUuid);
  }

  public void deleteByScimUuid(DbSession dbSession, String scimUuid) {
    mapper(dbSession).deleteByScimUuid(scimUuid);
  }

  public String getManagedUserSqlFilter(boolean filterByManaged) {
    return String.format("%s exists (select user_uuid from scim_users su where su.user_uuid = uuid)", filterByManaged ? "" : "not");
  }

  public void deleteAll(DbSession dbSession) {
    mapper(dbSession).deleteAll();
  }
}
