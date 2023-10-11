/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.issue.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.SnapshotDto;

import static java.util.stream.Collectors.toCollection;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;
import static org.sonar.db.ce.CeTaskTypes.BRANCH_ISSUE_SYNC;

public class AsyncIssueIndexingImpl implements AsyncIssueIndexing {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncIssueIndexingImpl.class);

  private final CeQueue ceQueue;
  private final DbClient dbClient;

  public AsyncIssueIndexingImpl(CeQueue ceQueue, DbClient dbClient) {
    this.ceQueue = ceQueue;
    this.dbClient = dbClient;
  }

  @Override
  public void triggerOnIndexCreation() {

    try (DbSession dbSession = dbClient.openSession(false)) {

      // remove already existing indexation task, if any
      removeExistingIndexationTasks(dbSession);

      dbClient.branchDao().updateAllNeedIssueSync(dbSession);
      List<BranchDto> branchInNeedOfIssueSync = dbClient.branchDao().selectBranchNeedingIssueSync(dbSession);
      LOG.info("{} branch found in need of issue sync.", branchInNeedOfIssueSync.size());

      if (branchInNeedOfIssueSync.isEmpty()) {
        return;
      }

      List<String> projectUuids = branchInNeedOfIssueSync.stream().map(BranchDto::getProjectUuid).distinct().collect(toCollection(ArrayList<String>::new));
      LOG.info("{} projects found in need of issue sync.", projectUuids.size());

      sortProjectUuids(dbSession, projectUuids);

      Map<String, List<BranchDto>> branchesByProject = branchInNeedOfIssueSync.stream()
        .collect(Collectors.groupingBy(BranchDto::getProjectUuid));

      List<CeTaskSubmit> tasks = new ArrayList<>();
      for (String projectUuid : projectUuids) {
        List<BranchDto> branches = branchesByProject.get(projectUuid);
        for (BranchDto branch : branches) {
          tasks.add(buildTaskSubmit(branch));
        }
      }

      ceQueue.massSubmit(tasks);
      dbSession.commit();
    }
  }

  @Override
  public void triggerForProject(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      // remove already existing indexation task, if any
      removeExistingIndexationTasksForProject(dbSession, projectUuid);

      dbClient.branchDao().updateAllNeedIssueSyncForProject(dbSession, projectUuid);
      List<BranchDto> branchInNeedOfIssueSync = dbClient.branchDao().selectBranchNeedingIssueSyncForProject(dbSession, projectUuid);
      LOG.info("{} branch(es) found in need of issue sync for project.", branchInNeedOfIssueSync.size());

      List<CeTaskSubmit> tasks = new ArrayList<>();
      for (BranchDto branch : branchInNeedOfIssueSync) {
        tasks.add(buildTaskSubmit(branch));
      }

      ceQueue.massSubmit(tasks);
      dbSession.commit();
    }
  }

  private void sortProjectUuids(DbSession dbSession, List<String> projectUuids) {
    Map<String, SnapshotDto> snapshotByProjectUuid = dbClient.snapshotDao()
      .selectLastAnalysesByRootComponentUuids(dbSession, projectUuids).stream()
      .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, Function.identity()));

    projectUuids.sort(compareBySnapshot(snapshotByProjectUuid));
  }

  static Comparator<String> compareBySnapshot(Map<String, SnapshotDto> snapshotByProjectUuid) {
    return (uuid1, uuid2) -> {
      SnapshotDto snapshot1 = snapshotByProjectUuid.get(uuid1);
      SnapshotDto snapshot2 = snapshotByProjectUuid.get(uuid2);
      if (snapshot1 == null && snapshot2 == null) {
        return 0;
      }
      if (snapshot1 == null) {
        return 1;
      }
      if (snapshot2 == null) {
        return -1;
      }
      return snapshot2.getCreatedAt().compareTo(snapshot1.getCreatedAt());
    };
  }

  private void removeExistingIndexationTasks(DbSession dbSession) {
    Set<String> ceQueueUuids = dbClient.ceQueueDao().selectAllInAscOrder(dbSession)
      .stream().filter(p -> p.getTaskType().equals(BRANCH_ISSUE_SYNC))
      .map(CeQueueDto::getUuid).collect(Collectors.toSet());
    Set<String> ceActivityUuids = dbClient.ceActivityDao().selectByTaskType(dbSession, BRANCH_ISSUE_SYNC)
      .stream().map(CeActivityDto::getUuid).collect(Collectors.toSet());
    removeIndexationTasks(dbSession, ceQueueUuids, ceActivityUuids);
  }

  private void removeExistingIndexationTasksForProject(DbSession dbSession, String projectUuid) {
    Set<String> ceQueueUuidsForProject = dbClient.ceQueueDao().selectByEntityUuid(dbSession, projectUuid)
      .stream().filter(p -> p.getTaskType().equals(BRANCH_ISSUE_SYNC))
      .map(CeQueueDto::getUuid).collect(Collectors.toSet());
    Set<String> ceActivityUuidsForProject = dbClient.ceActivityDao().selectByTaskType(dbSession, BRANCH_ISSUE_SYNC)
      .stream()
      .filter(ceActivityDto -> projectUuid.equals(ceActivityDto.getEntityUuid()))
      .map(CeActivityDto::getUuid).collect(Collectors.toSet());
    removeIndexationTasks(dbSession, ceQueueUuidsForProject, ceActivityUuidsForProject);
  }

  private void removeIndexationTasks(DbSession dbSession, Set<String> ceQueueUuids, Set<String> ceActivityUuids) {
    LOG.info(String.format("%s pending indexation task found to be deleted...", ceQueueUuids.size()));
    for (String uuid : ceQueueUuids) {
      dbClient.ceQueueDao().deleteByUuid(dbSession, uuid);
    }

    LOG.info(String.format("%s completed indexation task found to be deleted...", ceQueueUuids.size()));
    dbClient.ceActivityDao().deleteByUuids(dbSession, ceActivityUuids);
    LOG.info("Indexation task deletion complete.");

    LOG.info("Deleting tasks characteristics...");
    Set<String> tasksUuid = Stream.concat(ceQueueUuids.stream(), ceActivityUuids.stream()).collect(Collectors.toSet());
    dbClient.ceTaskCharacteristicsDao().deleteByTaskUuids(dbSession, tasksUuid);
    LOG.info("Tasks characteristics deletion complete.");

    dbSession.commit();
  }

  private CeTaskSubmit buildTaskSubmit(BranchDto branch) {
    Map<String, String> characteristics = new HashMap<>();
    characteristics.put(branch.getBranchType() == BranchType.BRANCH ? CeTaskCharacteristics.BRANCH : PULL_REQUEST, branch.getKey());
    characteristics.put(BRANCH_TYPE, branch.getBranchType().name());

    return ceQueue.prepareSubmit()
      .setType(BRANCH_ISSUE_SYNC)
      .setComponent(new CeTaskSubmit.Component(branch.getUuid(), branch.getProjectUuid()))
      .setCharacteristics(characteristics).build();
  }
}
