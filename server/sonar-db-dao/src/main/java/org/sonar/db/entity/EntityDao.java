/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.entity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class EntityDao implements Dao {
  public Optional<EntityDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public List<EntityDto> selectByUuids(DbSession dbSession, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }

    return executeLargeInputs(uuids, partition -> mapper(dbSession).selectByUuids(partition));
  }

  public Optional<EntityDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(mapper(dbSession).selectByKey(key));
  }

  public List<EntityDto> selectByKeys(DbSession dbSession, Collection<String> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(keys, partition -> mapper(dbSession).selectByKeys(partition));
  }

  public Optional<EntityDto> selectByComponentUuid(DbSession dbSession, String componentUuid) {
    return Optional.ofNullable(mapper(dbSession).selectByComponentUuid(componentUuid));
  }

  public void scrollForIndexing(DbSession session, ResultHandler<EntityDto> handler) {
    mapper(session).scrollForIndexing(handler);
  }

  private static EntityMapper mapper(DbSession session) {
    return session.getMapper(EntityMapper.class);
  }

}
