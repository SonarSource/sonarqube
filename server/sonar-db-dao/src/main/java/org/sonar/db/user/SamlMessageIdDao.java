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

public class SamlMessageIdDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public SamlMessageIdDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public Optional<SamlMessageIdDto> selectByMessageId(DbSession session, String messageId) {
    return Optional.ofNullable(mapper(session).selectByMessageId(messageId));
  }

  public SamlMessageIdDto insert(DbSession session, SamlMessageIdDto dto) {
    long now = system2.now();
    mapper(session).insert(dto
      .setUuid(uuidFactory.create())
      .setCreatedAt(now));
    return dto;
  }

  public int deleteExpired(DbSession dbSession) {
    return mapper(dbSession).deleteExpired(system2.now());
  }

  private static SamlMessageIdMapper mapper(DbSession session) {
    return session.getMapper(SamlMessageIdMapper.class);
  }
}
