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
package org.sonar.db.ce;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

public class CeQueueDao implements Dao {

  private static final Pagination ONE_RESULT_PAGINATION = Pagination.forPage(1).andSize(1);

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
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<CeQueueDto> selectPendingByMinimumExecutionCount(DbSession dbSession, int minExecutionCount) {
    return mapper(dbSession).selectPendingByMinimumExecutionCount(minExecutionCount);
  }

  public void resetTasksWithUnknownWorkerUUIDs(DbSession dbSession, Set<String> knownWorkerUUIDs) {
    if (knownWorkerUUIDs.isEmpty()) {
      mapper(dbSession).resetAllInProgressTasks(system2.now());
    } else {
      // executeLargeUpdates won't call the SQL command if knownWorkerUUIDs is empty
      executeLargeUpdates(knownWorkerUUIDs,
        uuids -> mapper(dbSession).resetTasksWithUnknownWorkerUUIDs(uuids, system2.now())
      );
    }
  }

  public CeQueueDto insert(DbSession session, CeQueueDto dto) {
    if (dto.getCreatedAt() == 0L || dto.getUpdatedAt() == 0L) {
      long now = system2.now();
      dto.setCreatedAt(now);
      dto.setUpdatedAt(now);
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

  /**
   * Update all tasks for the specified worker uuid which are not PENDING to:
   * STATUS='PENDING', STARTED_AT=NULL, UPDATED_AT={now}.
   */
  public int resetToPendingForWorker(DbSession session, String workerUuid) {
    return mapper(session).resetToPendingForWorker(workerUuid, system2.now());
  }

  public int countByStatus(DbSession dbSession, CeQueueDto.Status status) {
    return mapper(dbSession).countByStatusAndComponentUuid(status, null);
  }

  public int countByStatusAndComponentUuid(DbSession dbSession, CeQueueDto.Status status, @Nullable String componentUuid) {
    return mapper(dbSession).countByStatusAndComponentUuid(status, componentUuid);
  }

  public Optional<CeQueueDto> peek(DbSession session, String workerUuid, int maxExecutionCount) {
    List<EligibleTaskDto> eligibles = mapper(session).selectEligibleForPeek(maxExecutionCount, ONE_RESULT_PAGINATION);
    if (eligibles.isEmpty()) {
      return Optional.empty();
    }

    EligibleTaskDto eligible = eligibles.get(0);
    return tryToPeek(session, eligible, workerUuid);
  }

  private Optional<CeQueueDto> tryToPeek(DbSession session, EligibleTaskDto eligible, String workerUuid) {
    long now = system2.now();
    int touchedRows = mapper(session).updateIf(eligible.getUuid(),
      new UpdateIf.NewProperties(IN_PROGRESS, workerUuid, eligible.getExecutionCount() + 1, now, now),
      new UpdateIf.OldProperties(PENDING, eligible.getExecutionCount()));
    if (touchedRows != 1) {
      return Optional.empty();
    }

    CeQueueDto result = mapper(session).selectByUuid(eligible.getUuid());
    session.commit();
    return Optional.of(result);
  }

  private static CeQueueMapper mapper(DbSession session) {
    return session.getMapper(CeQueueMapper.class);
  }
}
