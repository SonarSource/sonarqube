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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentKeyGenerator;
import org.sonar.ce.task.projectanalysis.component.ComponentTreeBuilder;
import org.sonar.ce.task.projectanalysis.component.ComponentUuidFactory;
import org.sonar.ce.task.projectanalysis.component.ComponentUuidFactoryImpl;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.ProjectAttributes;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Populates the {@link MutableTreeRootHolder} and {@link MutableAnalysisMetadataHolder} from the {@link BatchReportReader}
 */
public class BuildComponentTreeStep implements ComputationStep {

  private static final String DEFAULT_PROJECT_VERSION = "not provided";

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MutableTreeRootHolder treeRootHolder;
  private final MutableAnalysisMetadataHolder analysisMetadataHolder;

  public BuildComponentTreeStep(DbClient dbClient, BatchReportReader reportReader, MutableTreeRootHolder treeRootHolder,
    MutableAnalysisMetadataHolder analysisMetadataHolder) {
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
      ScannerReport.Metadata metadata = reportReader.readMetadata();

      // root key of branch, not necessarily of project
      String rootKey = keyGenerator.generateKey(reportProject.getKey(), null);
      // loads the UUIDs from database. If they don't exist, then generate new ones
      ComponentUuidFactory componentUuidFactory = new ComponentUuidFactoryImpl(dbClient, dbSession, rootKey, analysisMetadataHolder.getBranch());

      String rootUuid = componentUuidFactory.getOrCreateForKey(rootKey);
      Optional<SnapshotDto> baseAnalysis = dbClient.snapshotDao().selectLastAnalysisByRootComponentUuid(dbSession, rootUuid);

      ComponentTreeBuilder builder = new ComponentTreeBuilder(keyGenerator,
        componentUuidFactory::getOrCreateForKey,
        reportReader::readComponent,
        analysisMetadataHolder.getProject(),
        analysisMetadataHolder.getBranch(),
        createProjectAttributes(metadata, baseAnalysis.orElse(null)));
      String relativePathFromScmRoot = metadata.getRelativePathFromScmRoot();

      Component reportTreeRoot = builder.buildProject(reportProject, relativePathFromScmRoot);

      if (analysisMetadataHolder.isPullRequest()) {
        Component changedComponentTreeRoot = builder.buildChangedComponentTreeRoot(reportTreeRoot);
        treeRootHolder.setRoots(changedComponentTreeRoot, reportTreeRoot);
      } else {
        treeRootHolder.setRoots(reportTreeRoot, reportTreeRoot);
      }

      analysisMetadataHolder.setBaseAnalysis(baseAnalysis.map(BuildComponentTreeStep::toAnalysis).orElse(null));

      context.getStatistics().add("components", treeRootHolder.getSize());
    }
  }

  private static ProjectAttributes createProjectAttributes(ScannerReport.Metadata metadata, @Nullable SnapshotDto baseAnalysis) {
    String projectVersion = computeProjectVersion(trimToNull(metadata.getProjectVersion()), baseAnalysis);
    String buildString = trimToNull(metadata.getBuildString());
    String scmRevisionId = trimToNull(metadata.getScmRevisionId());
    return new ProjectAttributes(projectVersion, buildString, scmRevisionId);
  }

  private static String computeProjectVersion(@Nullable String projectVersion, @Nullable SnapshotDto baseAnalysis) {
    if (projectVersion != null) {
      return projectVersion;
    }
    if (baseAnalysis != null) {
      return firstNonNull(baseAnalysis.getProjectVersion(), DEFAULT_PROJECT_VERSION);
    }
    return DEFAULT_PROJECT_VERSION;
  }

  private ComponentKeyGenerator loadKeyGenerator() {
    return analysisMetadataHolder.getBranch();
  }

  private static Analysis toAnalysis(SnapshotDto dto) {
    return new Analysis.Builder()
      .setUuid(dto.getUuid())
      .setCreatedAt(dto.getCreatedAt())
      .build();
  }

}
