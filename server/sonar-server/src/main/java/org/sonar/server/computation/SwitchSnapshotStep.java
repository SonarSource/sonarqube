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

package org.sonar.server.computation;

import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.db.SnapshotDao;

import java.util.List;

public class SwitchSnapshotStep implements ComputationStep {

  private SnapshotDao dao;

  public SwitchSnapshotStep(SnapshotDao dao) {
    this.dao = dao;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto report, ComponentDto project) {
    disablePreviousSnapshot(session, report);
    enableCurrentSnapshot(session, report);
  }

  @Override
  public String description() {
    return "Switch last snapshot flag";
  }

  private void disablePreviousSnapshot(DbSession session, AnalysisReportDto report) {
    SnapshotDto referenceSnapshot;

    try {
      referenceSnapshot = dao.getByKey(session, report.getSnapshotId());
    } catch (Exception exception) {
      throw new IllegalStateException(String.format("Unexpected error while trying to retrieve snapshot of analysis %s", report), exception);
    }

    List<SnapshotDto> snapshots = dao.findSnapshotAndChildrenOfProjectScope(session, referenceSnapshot);
    for (SnapshotDto snapshot : snapshots) {
      SnapshotDto previousLastSnapshot = dao.getLastSnapshot(session, snapshot);
      if (previousLastSnapshot != null) {
        dao.updateSnapshotAndChildrenLastFlag(session, previousLastSnapshot, false);
        session.commit();
      }
    }
  }

  private void enableCurrentSnapshot(DbSession session, AnalysisReportDto report) {
    SnapshotDto snapshot = dao.getByKey(session, report.getSnapshotId());
    SnapshotDto previousLastSnapshot = dao.getLastSnapshot(session, snapshot);

    boolean isLast = previousLastSnapshot == null || previousLastSnapshot.getCreatedAt().before(snapshot.getCreatedAt());
    dao.updateSnapshotAndChildrenLastFlagAndStatus(session, snapshot, isLast, SnapshotDto.STATUS_PROCESSED);
    session.commit();
  }
}
