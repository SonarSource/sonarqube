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
package org.sonar.db.ce;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class CeActivityDao implements Dao {

  private final System2 system2;

  public CeActivityDao(System2 system2) {
    this.system2 = system2;
  }

  public Optional<CeActivityDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.fromNullable(mapper(dbSession).selectByUuid(uuid));
  }

  public void insert(DbSession dbSession, CeActivityDto dto) {
    dto.setCreatedAt(system2.now());
    dto.setUpdatedAt(system2.now());
    dto.setIsLast(false);
    mapper(dbSession).insert(dto);

    List<String> uuids = mapper(dbSession).selectUuidsOfRecentlyCreatedByIsLastKey(dto.getIsLastKey(), new RowBounds(0, 1));
    // should never be empty, as a row was just inserted!
    if (!uuids.isEmpty()) {
      mapper(dbSession).updateIsLastToFalseForLastKey(dto.getIsLastKey(), dto.getUpdatedAt());
      mapper(dbSession).updateIsLastToTrueForUuid(uuids.get(0), dto.getUpdatedAt());
    }
  }

  public List<CeActivityDto> selectOlderThan(DbSession dbSession, long beforeDate) {
    return mapper(dbSession).selectOlderThan(beforeDate);
  }

  public void deleteByUuid(DbSession dbSession, String uuid) {
    mapper(dbSession).deleteByUuid(uuid);
  }

  /**
   * Ordered by id desc -> newest to oldest
   */
  public List<CeActivityDto> selectByQuery(DbSession dbSession, CeActivityQuery query, RowBounds rowBounds) {
    if (query.isShortCircuitedByComponentUuids()) {
      return Collections.emptyList();
    }
    return mapper(dbSession).selectByQuery(query, rowBounds);
  }

  public int countByQuery(DbSession dbSession, CeActivityQuery query) {
    if (query.isShortCircuitedByComponentUuids()) {
      return 0;
    }
    return mapper(dbSession).countByQuery(query);
  }

  private CeActivityMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(CeActivityMapper.class);
  }
}
