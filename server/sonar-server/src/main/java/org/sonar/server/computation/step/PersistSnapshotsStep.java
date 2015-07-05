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

import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.DbSession;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;
import org.sonar.db.DbClient;

/**
 * Persist snapshots
 * Also feed the components cache {@link DbIdsRepository} with snapshot ids
 */
public class PersistSnapshotsStep implements ComputationStep {

  private final System2 system2;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final DbIdsRepository dbIdsRepository;
  private final PeriodsHolder periodsHolder;

  public PersistSnapshotsStep(System2 system2, DbClient dbClient, TreeRootHolder treeRootHolder, BatchReportReader reportReader, DbIdsRepository dbIdsRepository,
    PeriodsHolder periodsHolder) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.dbIdsRepository = dbIdsRepository;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(false);
    try {
      Component root = treeRootHolder.getRoot();
      ProcessPersistSnapshot processPersistSnapshot = new ProcessPersistSnapshot(session, reportReader.readMetadata().getAnalysisDate());
      processPersistSnapshot.process(root, null);
      session.commit();
    } finally {
      session.close();
    }
  }

  private class ProcessPersistSnapshot {

    private final DbSession dbSession;
    private final long analysisDate;

    private long projectId;

    public ProcessPersistSnapshot(DbSession dbSession, long analysisDate) {
      this.dbSession = dbSession;
      this.analysisDate = analysisDate;
    }

    private void process(Component component, @Nullable SnapshotDto parentSnapshot) {
      long componentId = dbIdsRepository.getComponentId(component);

      switch (component.getType()) {
        case PROJECT:
          this.projectId = componentId;
          SnapshotDto projectSnapshot = persistSnapshot(componentId, Qualifiers.PROJECT, Scopes.PROJECT, component.getVersion(), parentSnapshot, true);
          addToCache(component, projectSnapshot);
          processChildren(component, projectSnapshot);
          break;
        case MODULE:
          SnapshotDto moduleSnapshot = persistSnapshot(componentId, Qualifiers.MODULE, Scopes.PROJECT, component.getVersion(), parentSnapshot, true);
          addToCache(component, moduleSnapshot);
          processChildren(component, moduleSnapshot);
          break;
        case DIRECTORY:
          SnapshotDto directorySnapshot = persistSnapshot(componentId, Qualifiers.DIRECTORY, Scopes.DIRECTORY, null, parentSnapshot, false);
          addToCache(component, directorySnapshot);
          processChildren(component, directorySnapshot);
          break;
        case FILE:
          SnapshotDto fileSnapshot = persistSnapshot(componentId, getFileQualifier(component), Scopes.FILE, null, parentSnapshot, false);
          addToCache(component, fileSnapshot);
          break;
        default:
          throw new IllegalStateException(String.format("Unsupported component type '%s'", component.getType()));
      }
    }

    private void processChildren(Component component, SnapshotDto parentSnapshot) {
      for (Component child : component.getChildren()) {
        process(child, parentSnapshot);
      }
    }

    private SnapshotDto persistSnapshot(long componentId, String qualifier, String scope, @Nullable String version, @Nullable SnapshotDto parentSnapshot, boolean setPeriods) {
      SnapshotDto snapshotDto = new SnapshotDto()
        .setRootProjectId(projectId)
        .setVersion(version)
        .setComponentId(componentId)
        .setQualifier(qualifier)
        .setScope(scope)
        .setLast(false)
        .setStatus(SnapshotDto.STATUS_UNPROCESSED)
        .setCreatedAt(analysisDate)
        .setBuildDate(system2.now());
      if (setPeriods) {
        updateSnapshotPeriods(snapshotDto);
      }

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
      dbClient.snapshotDao().insert(dbSession, snapshotDto);
      return snapshotDto;
    }

    private void updateSnapshotPeriods(SnapshotDto snapshotDto) {
      for (Period period : periodsHolder.getPeriods()) {
        int index = period.getIndex();
        snapshotDto.setPeriodMode(index, period.getMode());
        snapshotDto.setPeriodParam(index, period.getModeParameter());
        snapshotDto.setPeriodDate(index, period.getSnapshotDate());
      }
    }

    private void addToCache(Component component, SnapshotDto snapshotDto) {
      dbIdsRepository.setSnapshotId(component, snapshotDto.getId());
    }
  }

  private static String getFileQualifier(Component component) {
    return component.getFileAttributes().isUnitTest() ? Qualifiers.UNIT_TEST_FILE : Qualifiers.FILE;
  }

  @Override
  public String getDescription() {
    return "Persist snapshots";
  }
}
