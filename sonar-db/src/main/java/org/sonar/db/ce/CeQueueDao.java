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
package org.sonar.db.ce;

import com.google.common.base.Optional;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Collections.emptyList;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

public class CeQueueDao implements Dao {

  private static final RowBounds ONE_ROW_LIMIT = new RowBounds(0, 1);

  private final System2 system2;

  public CeQueueDao(System2 system2) {
    this.system2 = system2;
  }

  /**
   * Ordered by ascending id: oldest to newest
   */
  public List<CeQueueDto> selectAllInAscOrder(DbSession session) {
    return mapper(session).selectAllInAscOrder();
  }

  public List<CeQueueDto> selectByQueryInDescOrder(DbSession dbSession, CeTaskQuery query, int pageSize) {
    if (query.isShortCircuitedByComponentUuids()
      || query.isOnlyCurrents()
      || query.getMaxExecutedAt() != null) {
      return emptyList();
    }

    return mapper(dbSession).selectByQueryInDescOrder(query, new RowBounds(0, pageSize));
  }

  public int countByQuery(DbSession dbSession, CeTaskQuery query) {
    if (query.isShortCircuitedByComponentUuids()
      || query.isOnlyCurrents()
      || query.getMaxExecutedAt() != null) {
      return 0;
    }

    return mapper(dbSession).countByQuery(query);
  }

  /**
   * Ordered by ascending id: oldest to newest
   */
  public List<CeQueueDto> selectByComponentUuid(DbSession session, String componentUuid) {
    return mapper(session).selectByComponentUuid(componentUuid);
  }

  public Optional<CeQueueDto> selectByUuid(DbSession session, String uuid) {
    return Optional.fromNullable(mapper(session).selectByUuid(uuid));
  }

  public CeQueueDto insert(DbSession session, CeQueueDto dto) {
    if (dto.getCreatedAt() == 0L || dto.getUpdatedAt() == 0L) {
      dto.setCreatedAt(system2.now());
      dto.setUpdatedAt(system2.now());
    }

    mapper(session).insert(dto);
    return dto;
  }

  public void deleteByUuid(DbSession session, String uuid) {
    mapper(session).deleteByUuid(uuid);
  }

  /**
   * Update all rows with: STATUS='PENDING', STARTED_AT=NULL, UPDATED_AT={now}
   */
  public void resetAllToPendingStatus(DbSession session) {
    mapper(session).resetAllToPendingStatus(system2.now());
  }

  public int countByStatus(DbSession dbSession, CeQueueDto.Status status) {
    return mapper(dbSession).countByStatusAndComponentUuid(status, null);
  }

  public int countByStatusAndComponentUuid(DbSession dbSession, CeQueueDto.Status status, @Nullable String componentUuid) {
    return mapper(dbSession).countByStatusAndComponentUuid(status, componentUuid);
  }

  public Optional<CeQueueDto> peek(DbSession session) {
    List<String> taskUuids = mapper(session).selectEligibleForPeek(ONE_ROW_LIMIT);
    if (taskUuids.isEmpty()) {
      return Optional.absent();
    }

    String taskUuid = taskUuids.get(0);
    return tryToPeek(session, taskUuid);
  }

  private Optional<CeQueueDto> tryToPeek(DbSession session, String taskUuid) {
    int touchedRows = mapper(session).updateIfStatus(taskUuid, IN_PROGRESS, system2.now(), system2.now(), PENDING);
    if (touchedRows != 1) {
      return Optional.absent();
    }

    CeQueueDto result = mapper(session).selectByUuid(taskUuid);
    session.commit();
    return Optional.of(result);
  }

  private static CeQueueMapper mapper(DbSession session) {
    return session.getMapper(CeQueueMapper.class);
  }
}
