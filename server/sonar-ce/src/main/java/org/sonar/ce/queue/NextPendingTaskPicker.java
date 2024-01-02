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
package org.sonar.ce.queue;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.ComputeEngineProperties;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDao;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskDtoLight;
import org.sonar.db.ce.PrOrBranchTask;

import static org.sonar.db.ce.CeTaskCharacteristicDto.BRANCH_KEY;
import static org.sonar.db.ce.CeTaskCharacteristicDto.PULL_REQUEST;

@ComputeEngineSide
public class NextPendingTaskPicker {
  private static final Logger LOG = Loggers.get(NextPendingTaskPicker.class);

  private final Configuration config;
  private final CeQueueDao ceQueueDao;

  public NextPendingTaskPicker(Configuration config, DbClient dbClient) {
    this.config = config;
    this.ceQueueDao = dbClient.ceQueueDao();
  }

  Optional<CeQueueDto> findPendingTask(String workerUuid, DbSession dbSession, boolean prioritizeAnalysisAndRefresh) {
    // try to find tasks including indexation job & excluding app/portfolio and if no match, try the opposite
    // when prioritizeAnalysisAndRefresh is false, search first excluding indexation jobs and including app/portfolio, then the opposite
    Optional<CeTaskDtoLight> eligibleForPeek = ceQueueDao.selectEligibleForPeek(dbSession, prioritizeAnalysisAndRefresh, !prioritizeAnalysisAndRefresh);
    Optional<CeTaskDtoLight> eligibleForPeekInParallel = eligibleForPeekInParallel(dbSession);

    if (eligibleForPeek.isPresent() || eligibleForPeekInParallel.isPresent()) {
      return submitOldest(dbSession, workerUuid, eligibleForPeek.orElse(null), eligibleForPeekInParallel.orElse(null));
    }

    eligibleForPeek = ceQueueDao.selectEligibleForPeek(dbSession, !prioritizeAnalysisAndRefresh, prioritizeAnalysisAndRefresh);
    if (eligibleForPeek.isPresent()) {
      return ceQueueDao.tryToPeek(dbSession, eligibleForPeek.get().getCeTaskUuid(), workerUuid);
    }
    return Optional.empty();
  }

  /**
   * priority is always given to the task that is waiting longer - to avoid starvation
   */
  private Optional<CeQueueDto> submitOldest(DbSession session, String workerUuid, @Nullable CeTaskDtoLight eligibleForPeek, @Nullable CeTaskDtoLight eligibleForPeekInParallel) {
    CeTaskDtoLight oldest = (CeTaskDtoLight) ObjectUtils.min(eligibleForPeek, eligibleForPeekInParallel);
    Optional<CeQueueDto> ceQueueDto = ceQueueDao.tryToPeek(session, oldest.getCeTaskUuid(), workerUuid);
    if (!Objects.equals(oldest, eligibleForPeek)) {
      ceQueueDto.ifPresent(t -> LOG.info("Task [uuid = " + t.getUuid() + "] will be run concurrently with other tasks for the same project"));
    }
    return ceQueueDto;
  }

  Optional<CeTaskDtoLight> eligibleForPeekInParallel(DbSession dbSession) {
    Optional<Boolean> parallelProjectTasksEnabled = config.getBoolean(ComputeEngineProperties.CE_PARALLEL_PROJECT_TASKS_ENABLED);
    if (parallelProjectTasksEnabled.isPresent() && Boolean.TRUE.equals(parallelProjectTasksEnabled.get())) {
      return findPendingConcurrentCandidateTasks(ceQueueDao, dbSession);
    }
    return Optional.empty();
  }

  /**
   * Some of the tasks of the same project (mostly PRs) can be assigned and executed on workers at the same time/concurrently.
   * We look for them in this method.
   */
  private static Optional<CeTaskDtoLight> findPendingConcurrentCandidateTasks(CeQueueDao ceQueueDao, DbSession session) {
    List<PrOrBranchTask> queuedPrOrBranches = filterOldestPerProject(ceQueueDao.selectOldestPendingPrOrBranch(session));
    List<PrOrBranchTask> inProgressTasks = ceQueueDao.selectInProgressWithCharacteristics(session);

    for (PrOrBranchTask task : queuedPrOrBranches) {
      if ((Objects.equals(task.getBranchType(), PULL_REQUEST) && canRunPr(task, inProgressTasks))
        || (Objects.equals(task.getBranchType(), BRANCH_KEY) && canRunBranch(task, inProgressTasks))) {
        return Optional.of(task);
      }
    }
    return Optional.empty();
  }

  private static List<PrOrBranchTask> filterOldestPerProject(List<PrOrBranchTask> queuedPrOrBranches) {
    Set<String> mainComponentUuidsSeen = new HashSet<>();
    return queuedPrOrBranches.stream().filter(t -> mainComponentUuidsSeen.add(t.getMainComponentUuid())).toList();
  }

  /**
   * Branches cannot be run concurrently at this moment with other branches. And branches can already be returned in
   * {@link CeQueueDao#selectEligibleForPeek(org.sonar.db.DbSession, boolean, boolean)}. But we need this method because branches can be
   * assigned to a worker in a situation where the only type of in-progress tasks for a given project are {@link #PULLREQUEST_TYPE}.
   * <p>
   * This method returns the longest waiting branch in the queue which can be scheduled concurrently with pull requests.
   */
  private static boolean canRunBranch(PrOrBranchTask task, List<PrOrBranchTask> inProgress) {
    String mainComponentUuid = task.getMainComponentUuid();
    List<PrOrBranchTask> sameComponentTasks = inProgress.stream()
      .filter(t -> t.getMainComponentUuid().equals(mainComponentUuid))
      .toList();
    //we can peek branch analysis task only if all the other in progress tasks for this component uuid are pull requests
    return sameComponentTasks.stream().map(PrOrBranchTask::getBranchType).allMatch(s -> Objects.equals(s, PULL_REQUEST));
  }

  /**
   * Queued pull requests can almost always be assigned to worker unless there is already PR running with the same ID (text_value column)
   * and for the same project. We look for the one that waits for the longest time.
   */
  private static boolean canRunPr(PrOrBranchTask task, List<PrOrBranchTask> inProgress) {
    // return true unless the same PR is already in progress
    return inProgress.stream()
      .noneMatch(pr -> pr.getMainComponentUuid().equals(task.getMainComponentUuid()) && Objects.equals(pr.getBranchType(), PULL_REQUEST) &&
        Objects.equals(pr.getComponentUuid(), (task.getComponentUuid())));
  }
}
