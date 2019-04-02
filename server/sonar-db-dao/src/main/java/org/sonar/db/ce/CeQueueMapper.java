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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;
import org.sonar.db.Pagination;

public interface CeQueueMapper {

  List<CeQueueDto> selectByMainComponentUuid(@Param("mainComponentUuid") String mainComponentUuid);

  List<CeQueueDto> selectAllInAscOrder();

  List<CeQueueDto> selectByQueryInDescOrder(@Param("query") CeTaskQuery query, RowBounds rowBounds);

  int countByQuery(@Param("query") CeTaskQuery query);

  List<String> selectEligibleForPeek(@Param("pagination") Pagination pagination);

  @CheckForNull
  CeQueueDto selectByUuid(@Param("uuid") String uuid);

  /**
   * Select all pending tasks
   */
  List<CeQueueDto> selectPending();

  /**
   * Select all pending tasks which have already been started.
   */
  List<CeQueueDto> selectWornout();

  /**
   * The tasks that are in the in-progress status for too long
   */
  List<CeQueueDto> selectInProgressStartedBefore(@Param("date") long date);

  /**
   * Select all tasks whose worker UUID is not present in {@code knownWorkerUUIDs}
   */
  void resetTasksWithUnknownWorkerUUIDs(@Param("knownWorkerUUIDs") List<String> knownWorkerUUIDs, @Param("updatedAt") long updatedAt);

  /**
   * Reset all IN_PROGRESS TASKS
   */
  void resetAllInProgressTasks(@Param("updatedAt") long updatedAt);

  int countByStatusAndMainComponentUuid(@Param("status") CeQueueDto.Status status, @Nullable @Param("mainComponentUuid") String mainComponentUuid);

  List<QueueCount> countByStatusAndMainComponentUuids(@Param("status") CeQueueDto.Status status, @Param("mainComponentUuids") List<String> mainComponentUuids);

  void insert(CeQueueDto dto);

  int resetToPendingForWorker(@Param("workerUuid") String workerUuid, @Param("updatedAt") long updatedAt);

  int updateIf(@Param("uuid") String uuid,
    @Param("new") UpdateIf.NewProperties newProperties,
    @Param("old") UpdateIf.OldProperties oldProperties);

  int deleteByUuid(@Param("uuid") String uuid, @Nullable @Param("deleteIf") DeleteIf deleteIf);
}
