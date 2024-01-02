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
package org.sonar.db.ce;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
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
    return selectByQueryInDescOrder(dbSession, query, 1, pageSize);
  }

  public List<CeQueueDto> selectByQueryInDescOrder(DbSession dbSession, CeTaskQuery query, int page, int pageSize) {
    if (query.isShortCircuitedByEntityUuids()
      || query.isOnlyCurrents()
      || query.getMaxExecutedAt() != null) {
      return emptyList();
    }

    return mapper(dbSession).selectByQueryInDescOrder(query, Pagination.forPage(page).andSize(pageSize));
  }

  public int countByQuery(DbSession dbSession, CeTaskQuery query) {
    if (query.isShortCircuitedByEntityUuids()
      || query.isOnlyCurrents()
      || query.getMaxExecutedAt() != null) {
      return 0;
    }

    return mapper(dbSession).countByQuery(query);
  }

  /**
   * Ordered by ascending id: oldest to newest
   */
  public List<CeQueueDto> selectByEntityUuid(DbSession session, String projectUuid) {
    return mapper(session).selectByEntityUuid(projectUuid);
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

  public void resetToPendingByUuid(DbSession session, String uuid) {
    mapper(session).resetToPendingByUuid(uuid, system2.now());
  }

  public List<CeQueueDto> selectNotPendingForWorker(DbSession session, String uuid) {
    return mapper(session).selectNotPendingForWorker(uuid);
  }

  public int countByStatus(DbSession dbSession, CeQueueDto.Status status) {
    return mapper(dbSession).countByStatusAndEntityUuid(status, null);
  }

  public int countByStatusAndEntityUuid(DbSession dbSession, CeQueueDto.Status status, @Nullable String entityUuid) {
    return mapper(dbSession).countByStatusAndEntityUuid(status, entityUuid);
  }

  public Optional<Long> selectCreationDateOfOldestPendingByEntityUuid(DbSession dbSession, @Nullable String entityUuid) {
    return Optional.ofNullable(mapper(dbSession).selectCreationDateOfOldestPendingByEntityUuid(entityUuid));
  }

  /**
   * Counts entries in the queue with the specified status for each specified entity uuid.
   * The returned map doesn't contain any entry for entity uuids for which there is no entry in the queue (ie.
   * all entries have a value >= 0).
   */
  public Map<String, Integer> countByStatusAndEntityUuids(DbSession dbSession, CeQueueDto.Status status, Set<String> projectUuids) {
    if (projectUuids.isEmpty()) {
      return emptyMap();
    }

    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    executeLargeUpdates(
      projectUuids,
      partitionOfProjectUuids -> {
        List<QueueCount> i = mapper(dbSession).countByStatusAndEntityUuids(status, partitionOfProjectUuids);
        i.forEach(o -> builder.put(o.getEntityUuid(), o.getTotal()));
      });
    return builder.build();
  }

  public Optional<CeQueueDto> tryToPeek(DbSession session, String eligibleTaskUuid, String workerUuid) {
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

  public boolean hasAnyIssueSyncTaskPendingOrInProgress(DbSession dbSession) {
    return mapper(dbSession).hasAnyIssueSyncTaskPendingOrInProgress();
  }

  private static CeQueueMapper mapper(DbSession session) {
    return session.getMapper(CeQueueMapper.class);
  }

  /**
   * Only returns tasks for projects that currently have no other tasks running
   */
  public Optional<CeTaskDtoLight> selectEligibleForPeek(DbSession session, boolean excludeIndexationJob, boolean excludeView) {
    return mapper(session).selectEligibleForPeek(ONE_RESULT_PAGINATION, excludeIndexationJob, excludeView);
  }

  public List<PrOrBranchTask> selectOldestPendingPrOrBranch(DbSession session) {
    return mapper(session).selectOldestPendingPrOrBranch();
  }

  public List<PrOrBranchTask> selectInProgressWithCharacteristics(DbSession session) {
    return mapper(session).selectInProgressWithCharacteristics();
  }
}
