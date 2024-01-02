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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.es.AnalysisIndexer;

public class IndexAnalysisStep implements ComputationStep {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexAnalysisStep.class);

  private final TreeRootHolder treeRootHolder;
  private final FileStatuses fileStatuses;
  private final AnalysisIndexer[] indexers;
  private final DbClient dbClient;

  public IndexAnalysisStep(TreeRootHolder treeRootHolder, FileStatuses fileStatuses, DbClient dbClient, AnalysisIndexer... indexers) {
    this.treeRootHolder = treeRootHolder;
    this.fileStatuses = fileStatuses;
    this.indexers = indexers;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    String branchUuid = treeRootHolder.getRoot().getUuid();
    Consumer<AnalysisIndexer> analysisIndexerConsumer = getAnalysisIndexerConsumer(branchUuid);
    for (AnalysisIndexer indexer : indexers) {
      LOGGER.debug("Call {}", indexer);
      analysisIndexerConsumer.accept(indexer);
    }
  }

  private Consumer<AnalysisIndexer> getAnalysisIndexerConsumer(String branchUuid) {
    Set<String> fileUuidsMarkedAsUnchanged = fileStatuses.getFileUuidsMarkedAsUnchanged();
    return isBranchNeedIssueSync(branchUuid)
      ? (indexer -> indexer.indexOnAnalysis(branchUuid))
      : (indexer -> indexer.indexOnAnalysis(branchUuid, fileUuidsMarkedAsUnchanged));
  }

  private boolean isBranchNeedIssueSync(String branchUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.branchDao().isBranchNeedIssueSync(dbSession, branchUuid);
    }
  }

  @Override
  public String getDescription() {
    return "Index analysis";
  }
}
