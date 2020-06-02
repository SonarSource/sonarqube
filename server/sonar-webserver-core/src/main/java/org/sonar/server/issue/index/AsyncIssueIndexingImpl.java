/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;

import static java.util.Collections.emptyMap;
import static org.sonar.db.ce.CeTaskTypes.BRANCH_ISSUE_SYNC;

@ServerSide
public class AsyncIssueIndexingImpl implements AsyncIssueIndexing {

  private static final Logger LOG = Loggers.get(AsyncIssueIndexingImpl.class);

  private final CeQueue ceQueue;
  private final DbClient dbClient;

  public AsyncIssueIndexingImpl(CeQueue ceQueue, DbClient dbClient) {
    this.ceQueue = ceQueue;
    this.dbClient = dbClient;
  }

  @Override
  public void triggerOnIndexCreation() {

    try (DbSession dbSession = dbClient.openSession(false)) {

      dbClient.branchDao().updateAllNeedIssueSync(dbSession);

      // TODO check the queue for any BRANCH_ISSUE_SYNC existing task pending

      List<BranchDto> branchInNeedOfIssueSync = dbClient.branchDao().selectBranchNeedingIssueSync(dbSession);

      if (branchInNeedOfIssueSync.isEmpty()) {
        LOG.info("No branch found in need of issue sync");
        return;
      }

      String branchListForDisplay = branchInNeedOfIssueSync.stream().map(BranchDto::toString).collect(Collectors.joining(", "));
      LOG.info("{} branch found in need of issue sync : {}", branchInNeedOfIssueSync.size(), branchListForDisplay);

      List<CeTaskSubmit> tasks = branchInNeedOfIssueSync.stream()
        .map(this::buildTaskSubmit)
        .collect(Collectors.toList());
      ceQueue.massSubmit(tasks);

      dbSession.commit();

    }
  }

  private CeTaskSubmit buildTaskSubmit(BranchDto branch) {
    return ceQueue.prepareSubmit()
      .setType(BRANCH_ISSUE_SYNC)
      .setComponent(new CeTaskSubmit.Component(branch.getUuid(), branch.getProjectUuid()))
      .setCharacteristics(emptyMap()).build();
  }
}
