/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.base.Optional;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.Analysis;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ComponentRootBuilder;
import org.sonar.server.computation.task.projectanalysis.component.MutableTreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.UuidFactory;
import org.sonar.server.computation.task.step.ComputationStep;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.component.ComponentKeys.createKey;

/**
 * Populates the {@link MutableTreeRootHolder} and {@link MutableAnalysisMetadataHolder} from the {@link BatchReportReader}
 */
public class BuildComponentTreeStep implements ComputationStep {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MutableTreeRootHolder treeRootHolder;
  private final MutableAnalysisMetadataHolder analysisMetadataHolder;

  public BuildComponentTreeStep(DbClient dbClient, BatchReportReader reportReader, MutableTreeRootHolder treeRootHolder, MutableAnalysisMetadataHolder analysisMetadataHolder) {
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
  public void execute() {
    String branch = analysisMetadataHolder.getBranch();
    ScannerReport.Component reportProject = reportReader.readComponent(analysisMetadataHolder.getRootComponentRef());
    String projectKey = createKey(reportProject.getKey(), branch);
    UuidFactory uuidFactory = new UuidFactory(dbClient, projectKey);

    try (DbSession dbSession = dbClient.openSession(false)) {
      BaseAnalysisSupplier baseAnalysisSupplier = new BaseAnalysisSupplier(dbClient, dbSession);
      ComponentRootBuilder rootBuilder = new ComponentRootBuilder(branch,
        uuidFactory::getOrCreateForKey,
        reportReader::readComponent,
        () -> dbClient.componentDao().selectByKey(dbSession, projectKey),
        baseAnalysisSupplier);
      Component project = rootBuilder.build(reportProject, projectKey);
      treeRootHolder.setRoot(project);
      analysisMetadataHolder.setBaseAnalysis(toAnalysis(baseAnalysisSupplier.apply(project.getUuid())));
    }
  }

  /**
   * A supplier of the base analysis of the project (if it exists) that will cache the retrieved SnapshotDto and
   * implement a sanity check to ensure it is always call with the same UUID value (since it's the project's UUID, it
   * is unique for a whole task).
   */
  private static final class BaseAnalysisSupplier implements Function<String, Optional<SnapshotDto>> {
    private final DbClient dbClient;
    private final DbSession dbSession;
    private String projectUuid = null;
    private Optional<SnapshotDto> cache = null;

    private BaseAnalysisSupplier(DbClient dbClient, DbSession dbSession) {
      this.dbClient = dbClient;
      this.dbSession = dbSession;
    }

    @Override
    public Optional<SnapshotDto> apply(String projectUuid) {
      if (this.cache == null) {
        this.cache = Optional.fromNullable(
          dbClient.snapshotDao().selectAnalysisByQuery(
            dbSession,
            new SnapshotQuery()
              .setComponentUuid(projectUuid)
              .setIsLast(true)));
        this.projectUuid = projectUuid;
      } else {
        checkState(
          Objects.equals(this.projectUuid, projectUuid),
          "BaseAnalysisSupplier called with different project uuid values. First one was %s but current one is %s",
          this.projectUuid, projectUuid);
      }
      return this.cache;
    }
  }

  @CheckForNull
  private static Analysis toAnalysis(Optional<SnapshotDto> snapshotDto) {
    if (snapshotDto.isPresent()) {
      SnapshotDto dto = snapshotDto.get();
      return new Analysis.Builder()
        .setId(dto.getId())
        .setUuid(dto.getUuid())
        .setCreatedAt(dto.getCreatedAt())
        .build();
    }
    return null;
  }

}
