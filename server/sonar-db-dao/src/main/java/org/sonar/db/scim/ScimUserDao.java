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

import java.util.List;
import java.util.Optional;
import org.sonar.core.util.UuidFactory;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

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

  public List<ScimUserDto> findScimUsers(DbSession dbSession, ScimUserQuery scimUserQuery, int offset, int limit) {
    return mapper(dbSession).findScimUsers(scimUserQuery, new RowBounds(offset, limit));
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
}
