/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentKeyGenerator;
import org.sonar.ce.task.projectanalysis.component.ComponentTreeBuilder;
import org.sonar.ce.task.projectanalysis.component.ComponentUuidFactory;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.scanner.protocol.output.ScannerReport;

/**
 * Populates the {@link MutableTreeRootHolder} and {@link MutableAnalysisMetadataHolder} from the {@link BatchReportReader}
 */
public class BuildComponentTreeStep implements ComputationStep {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MutableTreeRootHolder treeRootHolder;
  private final MutableAnalysisMetadataHolder analysisMetadataHolder;

  public BuildComponentTreeStep(DbClient dbClient, BatchReportReader reportReader,
    MutableTreeRootHolder treeRootHolder, MutableAnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public String getDescription() {
    return "Build tree of components";
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ScannerReport.Component reportProject = reportReader.readComponent(analysisMetadataHolder.getRootComponentRef());
      ComponentKeyGenerator keyGenerator = loadKeyGenerator();
      ComponentKeyGenerator publicKeyGenerator = loadPublicKeyGenerator();

      // root key of branch, not necessarily of project
      String rootKey = keyGenerator.generateKey(reportProject, null);

      // loads the UUIDs from database. If they don't exist, then generate new ones
      ComponentUuidFactory componentUuidFactory = new ComponentUuidFactory(dbClient, dbSession, rootKey);

      String rootUuid = componentUuidFactory.getOrCreateForKey(rootKey);
      SnapshotDto baseAnalysis = loadBaseAnalysis(dbSession, rootUuid);

      ComponentTreeBuilder builder = new ComponentTreeBuilder(keyGenerator, publicKeyGenerator,
        componentUuidFactory::getOrCreateForKey,
        reportReader::readComponent,
        analysisMetadataHolder.getProject(),
        analysisMetadataHolder.getBranch(),
        baseAnalysis);
      String relativePathFromScmRoot = reportReader.readMetadata().getRelativePathFromScmRoot();

      Component reportTreeRoot = builder.buildProject(reportProject, relativePathFromScmRoot);

      if (analysisMetadataHolder.isShortLivingBranch() || analysisMetadataHolder.isPullRequest()) {
        Component changedComponentTreeRoot = builder.buildChangedComponentTreeRoot(reportTreeRoot);
        treeRootHolder.setRoots(changedComponentTreeRoot, reportTreeRoot);
      } else {
        treeRootHolder.setRoots(reportTreeRoot, reportTreeRoot);
      }

      analysisMetadataHolder.setBaseAnalysis(toAnalysis(baseAnalysis));

      context.getStatistics().add("components", treeRootHolder.getSize());
    }
  }

  private ComponentKeyGenerator loadKeyGenerator() {
    return analysisMetadataHolder.getBranch();
  }

  private ComponentKeyGenerator loadPublicKeyGenerator() {
    Branch branch = analysisMetadataHolder.getBranch();

    // for non-legacy branches, the public key is different from the DB key.
    if (!branch.isLegacyFeature() && !branch.isMain()) {
      return new DefaultBranchImpl();
    }
    return branch;
  }

  @CheckForNull
  private SnapshotDto loadBaseAnalysis(DbSession dbSession, String rootUuid) {
    return dbClient.snapshotDao().selectAnalysisByQuery(
      dbSession,
      new SnapshotQuery()
        .setComponentUuid(rootUuid)
        .setIsLast(true));
  }

  @CheckForNull
  private static Analysis toAnalysis(@Nullable SnapshotDto dto) {
    if (dto != null) {
      return new Analysis.Builder()
        .setId(dto.getId())
        .setUuid(dto.getUuid())
        .setCreatedAt(dto.getCreatedAt())
        .build();
    }
    return null;
  }

}
