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
package org.sonar.ce.task.projectanalysis.taskprocessor;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.issue.index.IssueIndexer;

public final class IndexIssuesStep implements ComputationStep {
  private static final Logger LOG = Loggers.get(IndexIssuesStep.class);
  private final CeTask ceTask;
  private final DbClient dbClient;
  private final IssueIndexer issueIndexer;

  public IndexIssuesStep(CeTask ceTask, DbClient dbClient, IssueIndexer issueIndexer) {
    this.ceTask = ceTask;
    this.dbClient = dbClient;
    this.issueIndexer = issueIndexer;
  }

  @Override
  public void execute(Context context) {
    String branchUuid = ceTask.getComponent().orElseThrow(() -> new UnsupportedOperationException("component not found in task")).getUuid();

    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.branchDao().selectByUuid(dbSession, branchUuid)
        .ifPresent(branchDto -> {

          if (branchDto.isNeedIssueSync()) {
            LOG.info("indexing issues of branch {}", branchUuid);
            issueIndexer.indexOnAnalysis(branchUuid);
            dbClient.branchDao().updateNeedIssueSync(dbSession, branchUuid, false);
            dbSession.commit();
          } else {
            // branch has been analyzed since task was created, do not index issues twice
            LOG.debug("issues of branch {} are already in sync", branchUuid);
          }
        });
    }
  }

  @Override
  public String getDescription() {
    return "index issues";
  }
}
