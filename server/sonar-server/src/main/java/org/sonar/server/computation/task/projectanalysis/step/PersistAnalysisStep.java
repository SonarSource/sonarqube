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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;
import org.sonar.server.computation.task.step.ComputationStep;

/**
 * Persist analysis
 */
public class PersistAnalysisStep implements ComputationStep {

  private final System2 system2;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PeriodsHolder periodsHolder;

  public PersistAnalysisStep(System2 system2, DbClient dbClient, TreeRootHolder treeRootHolder,
                             AnalysisMetadataHolder analysisMetadataHolder, PeriodsHolder periodsHolder) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      new PathAwareCrawler<>(
          new PersistSnapshotsPathAwareVisitor(session, analysisMetadataHolder.getAnalysisDate()))
          .visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      dbClient.closeSession(session);
    }
  }

  private class PersistSnapshotsPathAwareVisitor extends PathAwareVisitorAdapter<SnapshotDtoHolder> {

    private final DbSession dbSession;
    private final long analysisDate;

    public PersistSnapshotsPathAwareVisitor(DbSession dbSession, long analysisDate) {
      super(CrawlerDepthLimit.ROOTS, Order.PRE_ORDER, SnapshotDtoHolderFactory.INSTANCE);
      this.dbSession = dbSession;
      this.analysisDate = analysisDate;
    }

    @Override
    public void visitProject(Component project, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createAnalysis(analysisMetadataHolder.getUuid(), project, true);
      updateSnapshotPeriods(snapshot);
      persist(snapshot, dbSession);
    }

    @Override
    public void visitView(Component view, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createAnalysis(analysisMetadataHolder.getUuid(), view, false);
      updateSnapshotPeriods(snapshot);
      persist(snapshot, dbSession);
    }

    private void updateSnapshotPeriods(SnapshotDto snapshotDto) {
      for (Period period : periodsHolder.getPeriods()) {
        int index = period.getIndex();
        snapshotDto.setPeriodMode(index, period.getMode());
        snapshotDto.setPeriodParam(index, period.getModeParameter());
        snapshotDto.setPeriodDate(index, period.getSnapshotDate());
      }
    }

    private SnapshotDto createAnalysis(String snapshotUuid, Component component, boolean setVersion) {
      String componentUuid = component.getUuid();
      return new SnapshotDto()
          .setUuid(snapshotUuid)
          .setVersion(setVersion ? component.getReportAttributes().getVersion() : null)
          .setComponentUuid(componentUuid)
          .setLast(false)
          .setStatus(SnapshotDto.STATUS_UNPROCESSED)
          .setCreatedAt(analysisDate)
          .setBuildDate(system2.now());
    }

    private void persist(SnapshotDto snapshotDto, DbSession dbSession) {
      dbClient.snapshotDao().insert(dbSession, snapshotDto);
    }
  }

  private static final class SnapshotDtoHolder {
    @CheckForNull
    private SnapshotDto snapshotDto;

    @CheckForNull
    public SnapshotDto getSnapshotDto() {
      return snapshotDto;
    }

    public void setSnapshotDto(@Nullable SnapshotDto snapshotDto) {
      this.snapshotDto = snapshotDto;
    }
  }

  /**
   * Factory of SnapshotDtoHolder.
   *
   * No need to create an instance for components of type FILE and PROJECT_VIEW, since they are the leaves of their
   * respective trees, no one will consume the value of the holder, so we save on creating useless objects.
   */
  private static class SnapshotDtoHolderFactory extends PathAwareVisitorAdapter.SimpleStackElementFactory<SnapshotDtoHolder> {
    public static final SnapshotDtoHolderFactory INSTANCE = new SnapshotDtoHolderFactory();

    @Override
    public SnapshotDtoHolder createForAny(Component component) {
      return new SnapshotDtoHolder();
    }

    @Override
    public SnapshotDtoHolder createForFile(Component file) {
      return null;
    }

    @Override
    public SnapshotDtoHolder createForProjectView(Component projectView) {
      return null;
    }
  }

  @Override
  public String getDescription() {
    return "Persist analysis";
  }
}
