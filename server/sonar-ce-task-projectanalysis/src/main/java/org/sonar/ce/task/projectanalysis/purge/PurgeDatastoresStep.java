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
package org.sonar.ce.task.projectanalysis.purge;

import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.DisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;
import static org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit.reportMaxDepth;

public class PurgeDatastoresStep implements ComputationStep {

  private final ProjectCleaner projectCleaner;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final ConfigurationRepository configRepository;
  private final DisabledComponentsHolder disabledComponentsHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public PurgeDatastoresStep(DbClient dbClient, ProjectCleaner projectCleaner, TreeRootHolder treeRootHolder,
    ConfigurationRepository configRepository, DisabledComponentsHolder disabledComponentsHolder, AnalysisMetadataHolder analysisMetadataHolder) {
    this.projectCleaner = projectCleaner;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.configRepository = configRepository;
    this.disabledComponentsHolder = disabledComponentsHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(reportMaxDepth(PROJECT).withViewsMaxDepth(VIEW), PRE_ORDER) {
        @Override
        public void visitProject(Component project) {
          execute(project);
        }

        @Override
        public void visitView(Component view) {
          execute(view);
        }
      }).visit(treeRootHolder.getRoot());
  }

  private void execute(Component root) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      String projectUuid = analysisMetadataHolder.getProject().getUuid();
      projectCleaner.purge(dbSession, root.getUuid(), projectUuid, configRepository.getConfiguration(), disabledComponentsHolder.getUuids());
      dbSession.commit();
    }
  }

  @Override
  public String getDescription() {
    return "Purge db";
  }
}
