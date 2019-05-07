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

import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;

import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;

/**
 * Persist analysis
 */
public class PersistAnalysisStep implements ComputationStep {

  private final System2 system2;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodHolder periodHolder;

  public PersistAnalysisStep(System2 system2, DbClient dbClient, TreeRootHolder treeRootHolder,
    AnalysisMetadataHolder analysisMetadataHolder, PeriodHolder periodHolder) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodHolder = periodHolder;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      new DepthTraversalTypeAwareCrawler(
        new PersistSnapshotsPathAwareVisitor(dbSession, analysisMetadataHolder.getAnalysisDate()))
          .visit(treeRootHolder.getRoot());
      dbSession.commit();
    }
  }

  private class PersistSnapshotsPathAwareVisitor extends TypeAwareVisitorAdapter {

    private final DbSession dbSession;
    private final long analysisDate;

    public PersistSnapshotsPathAwareVisitor(DbSession dbSession, long analysisDate) {
      super(CrawlerDepthLimit.ROOTS, Order.PRE_ORDER);
      this.dbSession = dbSession;
      this.analysisDate = analysisDate;
    }

    @Override
    public void visitProject(Component project) {
      SnapshotDto snapshot = createAnalysis(analysisMetadataHolder.getUuid(), project);
      updateSnapshotPeriods(snapshot);
      persist(snapshot, dbSession);
    }

    @Override
    public void visitView(Component view) {
      SnapshotDto snapshot = createAnalysis(analysisMetadataHolder.getUuid(), view);
      updateSnapshotPeriods(snapshot);
      persist(snapshot, dbSession);
    }

    private void updateSnapshotPeriods(SnapshotDto snapshotDto) {
      if (!periodHolder.hasPeriod()) {
        return;
      }
      Period period = periodHolder.getPeriod();
      snapshotDto.setPeriodMode(period.getMode());
      snapshotDto.setPeriodParam(period.getModeParameter());
      snapshotDto.setPeriodDate(period.getSnapshotDate());
    }

    private SnapshotDto createAnalysis(String snapshotUuid, Component component) {
      String componentUuid = component.getUuid();
      String projectVersion = component.getType() == PROJECT ? component.getProjectAttributes().getProjectVersion() : null;
      String buildString = component.getType() == PROJECT ? component.getProjectAttributes().getBuildString().orElse(null) : null;
      SnapshotDto dto = new SnapshotDto()
        .setUuid(snapshotUuid)
        .setProjectVersion(projectVersion)
        .setBuildString(buildString)
        .setComponentUuid(componentUuid)
        .setLast(false)
        .setStatus(SnapshotDto.STATUS_UNPROCESSED)
        .setCreatedAt(analysisDate)
        .setBuildDate(system2.now());

      if (component.getType() == PROJECT) {
        component.getProjectAttributes().getScmRevisionId().ifPresent(dto::setRevision);
      }

      return dto;
    }

    private void persist(SnapshotDto snapshotDto, DbSession dbSession) {
      dbClient.snapshotDao().insert(dbSession, snapshotDto);
    }
  }

  @Override
  public String getDescription() {
    return "Persist analysis";
  }
}
