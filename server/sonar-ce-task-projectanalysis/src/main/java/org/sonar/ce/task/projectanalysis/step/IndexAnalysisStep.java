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
package org.sonar.ce.task.projectanalysis.step;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.server.es.ProjectIndexer;

public class IndexAnalysisStep implements ComputationStep {

  private static final Logger LOGGER = Loggers.get(IndexAnalysisStep.class);

  private final TreeRootHolder treeRootHolder;
  private final ProjectIndexer[] indexers;

  public IndexAnalysisStep(TreeRootHolder treeRootHolder, ProjectIndexer... indexers) {
    this.treeRootHolder = treeRootHolder;
    this.indexers = indexers;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    String branchUuid = treeRootHolder.getRoot().getUuid();
    for (ProjectIndexer indexer : indexers) {
      LOGGER.debug("Call {}", indexer);
      indexer.indexOnAnalysis(branchUuid);
    }
  }

  @Override
  public String getDescription() {
    return "Index analysis";
  }
}
