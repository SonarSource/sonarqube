/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.MutableDbIdsRepository;
import org.sonar.server.computation.component.PathAwareCrawler;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

/**
 * Persist snapshots
 * Also feed the components cache {@link DbIdsRepositoryImpl} with snapshot ids
 */
public class PersistSnapshotsStep implements ComputationStep {

  private final System2 system2;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MutableDbIdsRepository dbIdsRepository;
  private final PeriodsHolder periodsHolder;

  public PersistSnapshotsStep(System2 system2, DbClient dbClient, TreeRootHolder treeRootHolder, AnalysisMetadataHolder analysisMetadataHolder,
    MutableDbIdsRepository dbIdsRepository, PeriodsHolder periodsHolder) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbIdsRepository = dbIdsRepository;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      new PathAwareCrawler<>(
        new PersistSnapshotsPathAwareVisitor(session, analysisMetadataHolder.getAnalysisDate().getTime(), dbIdsRepository))
          .visit(treeRootHolder.getRoot());
      session.commit();
    } finally {
      dbClient.closeSession(session);
    }
  }

  private class PersistSnapshotsPathAwareVisitor extends PathAwareVisitorAdapter<SnapshotDtoHolder> {

    private final DbSession dbSession;
    private final long analysisDate;
    private final DbIdsRepository dbIdsRepository;

    private long rootId;

    public PersistSnapshotsPathAwareVisitor(DbSession dbSession, long analysisDate, DbIdsRepository dbIdsRepository) {
      super(CrawlerDepthLimit.LEAVES, Order.PRE_ORDER, SnapshotDtoHolderFactory.INSTANCE);
      this.dbSession = dbSession;
      this.analysisDate = analysisDate;
      this.dbIdsRepository = dbIdsRepository;
    }

    @Override
    public void visitProject(Component project, Path<SnapshotDtoHolder> path) {
      this.rootId = dbIdsRepository.getComponentId(project);
      SnapshotDto snapshot = createSnapshot(project, path, Qualifiers.PROJECT, Scopes.PROJECT, true);
      updateSnapshotPeriods(snapshot);
      commonForAnyVisit(project, path, snapshot);
    }

    @Override
    public void visitModule(Component module, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createSnapshot(module, path, Qualifiers.MODULE, Scopes.PROJECT, true);
      updateSnapshotPeriods(snapshot);
      commonForAnyVisit(module, path, snapshot);
    }

    @Override
    public void visitDirectory(Component directory, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createSnapshot(directory, path, Qualifiers.DIRECTORY, Scopes.DIRECTORY, false);
      commonForAnyVisit(directory, path, snapshot);
    }

    @Override
    public void visitFile(Component file, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createSnapshot(file, path, getFileQualifier(file), Scopes.FILE, false);
      commonForAnyVisit(file, path, snapshot);
    }

    @Override
    public void visitView(Component view, Path<SnapshotDtoHolder> path) {
      this.rootId = dbIdsRepository.getComponentId(view);
      SnapshotDto snapshot = createSnapshot(view, path, Qualifiers.VIEW, Scopes.PROJECT, false);
      updateSnapshotPeriods(snapshot);
      commonForAnyVisit(view, path, snapshot);
    }

    @Override
    public void visitSubView(Component subView, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createSnapshot(subView, path, Qualifiers.SUBVIEW, Scopes.PROJECT, false);
      updateSnapshotPeriods(snapshot);
      commonForAnyVisit(subView, path, snapshot);
    }

    @Override
    public void visitProjectView(Component projectView, Path<SnapshotDtoHolder> path) {
      SnapshotDto snapshot = createSnapshot(projectView, path, Qualifiers.PROJECT, Scopes.FILE, false);
      updateSnapshotPeriods(snapshot);
      commonForAnyVisit(projectView, path, snapshot);
    }

    private void commonForAnyVisit(Component project, Path<SnapshotDtoHolder> path, SnapshotDto snapshot) {
      persist(snapshot, dbSession);
      addToCache(project, snapshot);
      if (path.current() != null) {
        path.current().setSnapshotDto(snapshot);
      }
    }

    private SnapshotDto createSnapshot(Component component, Path<SnapshotDtoHolder> path,
      String qualifier, String scope, boolean setVersion) {
      long componentId = dbIdsRepository.getComponentId(component);
      SnapshotDto snapshotDto = new SnapshotDto()
        .setRootProjectId(rootId)
        .setVersion(setVersion ? component.getReportAttributes().getVersion() : null)
        .setComponentId(componentId)
        .setQualifier(qualifier)
        .setScope(scope)
        .setLast(false)
        .setStatus(SnapshotDto.STATUS_UNPROCESSED)
        .setCreatedAt(analysisDate)
        .setBuildDate(system2.now());

      SnapshotDto parentSnapshot = path.isRoot() ? null : path.parent().getSnapshotDto();
      if (parentSnapshot != null) {
        snapshotDto
          .setParentId(parentSnapshot.getId())
          .setRootId(parentSnapshot.getRootId() == null ? parentSnapshot.getId() : parentSnapshot.getRootId())
          .setDepth(parentSnapshot.getDepth() + 1)
          .setPath(parentSnapshot.getPath() + parentSnapshot.getId() + ".");
      } else {
        snapshotDto
          // On Oracle, the path will be null
          .setPath("")
          .setDepth(0);
      }
      return snapshotDto;
    }
  }

  private void persist(SnapshotDto snapshotDto, DbSession dbSession) {
    dbClient.snapshotDao().insert(dbSession, snapshotDto);
  }

  private void addToCache(Component component, SnapshotDto snapshotDto) {
    dbIdsRepository.setSnapshotId(component, snapshotDto.getId());
  }

  private void updateSnapshotPeriods(SnapshotDto snapshotDto) {
    for (Period period : periodsHolder.getPeriods()) {
      int index = period.getIndex();
      snapshotDto.setPeriodMode(index, period.getMode());
      snapshotDto.setPeriodParam(index, period.getModeParameter());
      snapshotDto.setPeriodDate(index, period.getSnapshotDate());
    }
  }

  private static String getFileQualifier(Component component) {
    return component.getFileAttributes().isUnitTest() ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
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
    return "Persist snapshots";
  }
}
