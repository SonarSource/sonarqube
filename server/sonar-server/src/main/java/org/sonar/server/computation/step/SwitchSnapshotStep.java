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

import org.sonar.api.resources.Qualifiers;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;

import java.util.List;

public class SwitchSnapshotStep implements ComputationStep {

  private final DbClient dbClient;

  public SwitchSnapshotStep(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT, Qualifiers.VIEW};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    try {
      long snapshotId = context.getReportMetadata().getSnapshotId();
      disablePreviousSnapshot(session, snapshotId);
      enableCurrentSnapshot(session, snapshotId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public String getDescription() {
    return "Switch last snapshot flag";
  }

  private void disablePreviousSnapshot(DbSession session, long reportSnapshotId) {
    List<SnapshotDto> snapshots = dbClient.snapshotDao().findSnapshotAndChildrenOfProjectScope(session, dbClient.snapshotDao().getByKey(session, reportSnapshotId));
    for (SnapshotDto snapshot : snapshots) {
      SnapshotDto previousLastSnapshot = dbClient.snapshotDao().getLastSnapshot(session, snapshot);
      if (previousLastSnapshot != null) {
        dbClient.snapshotDao().updateSnapshotAndChildrenLastFlag(session, previousLastSnapshot, false);
        session.commit();
      }
    }
  }

  private void enableCurrentSnapshot(DbSession session, long reportSnapshotId) {
    SnapshotDao dao = dbClient.snapshotDao();
    SnapshotDto snapshot = dao.getByKey(session, reportSnapshotId);
    SnapshotDto previousLastSnapshot = dao.getLastSnapshot(session, snapshot);

    boolean isLast = dao.isLast(snapshot, previousLastSnapshot);
    dao.updateSnapshotAndChildrenLastFlagAndStatus(session, snapshot, isLast, SnapshotDto.STATUS_PROCESSED);
    session.commit();
  }
}
