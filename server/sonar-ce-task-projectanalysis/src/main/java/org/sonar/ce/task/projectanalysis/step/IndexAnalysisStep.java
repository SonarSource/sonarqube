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

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.index.IndexDiffResolver;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.es.AnalysisIndexer;

public class IndexAnalysisStep implements ComputationStep {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexAnalysisStep.class);
  private final TreeRootHolder treeRootHolder;
  private final IndexDiffResolver indexDiffResolver;
  private final AnalysisIndexer[] indexers;
  private final DbClient dbClient;

  public IndexAnalysisStep(TreeRootHolder treeRootHolder, IndexDiffResolver indexDiffResolver, DbClient dbClient, AnalysisIndexer... indexers) {
    this.treeRootHolder = treeRootHolder;
    this.indexDiffResolver = indexDiffResolver;
    this.indexers = indexers;
    this.dbClient = dbClient;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    Component root = treeRootHolder.getRoot();
    String branchUuid = root.getUuid();

    for (AnalysisIndexer indexer : indexers) {
      LOGGER.debug("Call {}", indexer);
      if (isDiffIndexingSupported(root, indexer) && hasPreviousAnalysisSucceeded(branchUuid) && !isBranchNeedIssueSync(branchUuid)) {
        Collection<String> diffSet = indexDiffResolver.resolve(indexer.getClass());
        indexer.indexOnAnalysis(branchUuid, diffSet);
      } else {
        indexer.indexOnAnalysis(branchUuid);
      }
    }
  }

  private boolean hasPreviousAnalysisSucceeded(String branchUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.ceActivityDao().selectLastByComponentUuidAndTaskType(dbSession, branchUuid, CeTaskTypes.REPORT)
        .filter(activityDto -> CeActivityDto.Status.SUCCESS.equals(activityDto.getStatus()))
        .isPresent();
    }
  }

  private static boolean isDiffIndexingSupported(Component root, AnalysisIndexer indexer) {
    return Component.Type.PROJECT.equals(root.getType()) && indexer.supportDiffIndexing();
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
