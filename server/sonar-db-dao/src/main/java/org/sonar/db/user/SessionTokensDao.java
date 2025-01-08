/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class SessionTokensDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public SessionTokensDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public Optional<SessionTokenDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public SessionTokenDto insert(DbSession session, SessionTokenDto dto) {
    long now = system2.now();
    mapper(session).insert(dto
      .setUuid(uuidFactory.create())
      .setCreatedAt(now)
      .setUpdatedAt(now));
    return dto;
  }

  public SessionTokenDto update(DbSession session, SessionTokenDto dto) {
    long now = system2.now();
    mapper(session).update(dto.setUpdatedAt(now));
    return dto;
  }

  public void deleteByUuid(DbSession dbSession, String uuid) {
    mapper(dbSession).deleteByUuid(uuid);
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    mapper(dbSession).deleteByUserUuid(user.getUuid());
  }

  public int deleteExpired(DbSession dbSession) {
    return mapper(dbSession).deleteExpired(system2.now());
  }

  private static SessionTokenMapper mapper(DbSession session) {
    return session.getMapper(SessionTokenMapper.class);
  }
}
