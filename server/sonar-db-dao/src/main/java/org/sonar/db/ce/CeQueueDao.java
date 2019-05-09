/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
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
    if (query.isShortCircuitedByMainComponentUuids()
      || query.isOnlyCurrents()
      || query.getMaxExecutedAt() != null) {
      return emptyList();
    }

    return mapper(dbSession).selectByQueryInDescOrder(query, new RowBounds(0, pageSize));
  }

  public int countByQuery(DbSession dbSession, CeTaskQuery query) {
    if (query.isShortCircuitedByMainComponentUuids()
      || query.isOnlyCurrents()
      || query.getMaxExecutedAt() != null) {
      return 0;
    }

    return mapper(dbSession).countByQuery(query);
  }

  /**
   * Ordered by ascending id: oldest to newest
   */
  public List<CeQueueDto> selectByMainComponentUuid(DbSession session, String projectUuid) {
    return mapper(session).selectByMainComponentUuid(projectUuid);
  }

  public Optional<CeQueueDto> selectByUuid(DbSession session, String uuid) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<CeQueueDto> selectPending(DbSession dbSession) {
    return mapper(dbSession).selectPending();
  }

  public List<CeQueueDto> selectWornout(DbSession dbSession) {
    return mapper(dbSession).selectWornout();
  }

  public List<CeQueueDto> selectInProgressStartedBefore(DbSession dbSession, long date) {
    return mapper(dbSession).selectInProgressStartedBefore(date);
  }

  public void resetTasksWithUnknownWorkerUUIDs(DbSession dbSession, Set<String> knownWorkerUUIDs) {
    if (knownWorkerUUIDs.isEmpty()) {
      mapper(dbSession).resetAllInProgressTasks(system2.now());
    } else {
      // executeLargeUpdates won't call the SQL command if knownWorkerUUIDs is empty
      executeLargeUpdates(knownWorkerUUIDs,
        uuids -> mapper(dbSession).resetTasksWithUnknownWorkerUUIDs(uuids, system2.now()));
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

  public int deleteByUuid(DbSession session, String uuid) {
    return deleteByUuid(session, uuid, null);
  }

  public int deleteByUuid(DbSession session, String uuid, @Nullable DeleteIf deleteIf) {
    return mapper(session).deleteByUuid(uuid, deleteIf);
  }

  /**
   * Update all tasks for the specified worker uuid which are not PENDING to:
   * STATUS='PENDING', STARTED_AT=NULL, UPDATED_AT={now}.
   */
  public int resetToPendingForWorker(DbSession session, String workerUuid) {
    return mapper(session).resetToPendingForWorker(workerUuid, system2.now());
  }

  public int countByStatus(DbSession dbSession, CeQueueDto.Status status) {
    return mapper(dbSession).countByStatusAndMainComponentUuid(status, null);
  }

  public int countByStatusAndMainComponentUuid(DbSession dbSession, CeQueueDto.Status status, @Nullable String mainComponentUuid) {
    return mapper(dbSession).countByStatusAndMainComponentUuid(status, mainComponentUuid);
  }

  public Optional<Long> selectCreationDateOfOldestPendingByMainComponentUuid(DbSession dbSession, @Nullable String mainComponentUuid) {
    return Optional.ofNullable(mapper(dbSession).selectCreationDateOfOldestPendingByMainComponentUuid(mainComponentUuid));
  }

  /**
   * Counts entries in the queue with the specified status for each specified main component uuid.
   * The returned map doesn't contain any entry for main component uuids for which there is no entry in the queue (ie.
   * all entries have a value >= 0).
   */
  public Map<String, Integer> countByStatusAndMainComponentUuids(DbSession dbSession, CeQueueDto.Status status, Set<String> projectUuids) {
    if (projectUuids.isEmpty()) {
      return emptyMap();
    }

    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    executeLargeUpdates(
      projectUuids,
      partitionOfProjectUuids -> {
        List<QueueCount> i = mapper(dbSession).countByStatusAndMainComponentUuids(status, partitionOfProjectUuids);
        i.forEach(o -> builder.put(o.getMainComponentUuid(), o.getTotal()));
      });
    return builder.build();
  }

  public Optional<CeQueueDto> peek(DbSession session, String workerUuid) {
    List<String> eligibles = mapper(session).selectEligibleForPeek(ONE_RESULT_PAGINATION);
    if (eligibles.isEmpty()) {
      return Optional.empty();
    }

    String eligible = eligibles.get(0);
    return tryToPeek(session, eligible, workerUuid);
  }

  private Optional<CeQueueDto> tryToPeek(DbSession session, String eligibleTaskUuid, String workerUuid) {
    long now = system2.now();
    int touchedRows = mapper(session).updateIf(eligibleTaskUuid,
      new UpdateIf.NewProperties(IN_PROGRESS, workerUuid, now, now),
      new UpdateIf.OldProperties(PENDING));
    if (touchedRows != 1) {
      return Optional.empty();
    }

    CeQueueDto result = mapper(session).selectByUuid(eligibleTaskUuid);
    session.commit();
    return Optional.ofNullable(result);
  }

  private static CeQueueMapper mapper(DbSession session) {
    return session.getMapper(CeQueueMapper.class);
  }
}
