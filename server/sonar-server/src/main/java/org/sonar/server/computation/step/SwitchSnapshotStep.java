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

import java.util.List;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.TreeRootHolder;

import static org.sonar.db.component.SnapshotDao.isLast;

public class SwitchSnapshotStep implements ComputationStep {

  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final DbIdsRepository dbIdsRepository;

  public SwitchSnapshotStep(DbClient dbClient, TreeRootHolder treeRootHolder, DbIdsRepository dbIdsRepository) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.dbIdsRepository = dbIdsRepository;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      Component project = treeRootHolder.getRoot();
      long snapshotId = dbIdsRepository.getSnapshotId(project);
      disablePreviousSnapshot(session, snapshotId);
      enableCurrentSnapshot(session, snapshotId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public String getDescription() {
    return "Enable snapshot";
  }

  private void disablePreviousSnapshot(DbSession session, long reportSnapshotId) {
    List<SnapshotDto> snapshots = dbClient.snapshotDao().selectSnapshotAndChildrenOfProjectScope(session, reportSnapshotId);
    for (SnapshotDto snapshot : snapshots) {
      SnapshotDto previousLastSnapshot = dbClient.snapshotDao().selectLastSnapshotByComponentId(session, snapshot.getComponentId());
      if (previousLastSnapshot != null) {
        dbClient.snapshotDao().updateSnapshotAndChildrenLastFlag(session, previousLastSnapshot, false);
        session.commit();
      }
    }
  }

  private void enableCurrentSnapshot(DbSession session, long reportSnapshotId) {
    SnapshotDao dao = dbClient.snapshotDao();
    SnapshotDto snapshot = dao.selectOrFailById(session, reportSnapshotId);
    SnapshotDto previousLastSnapshot = dao.selectLastSnapshotByComponentId(session, snapshot.getComponentId());

    boolean isLast = isLast(snapshot, previousLastSnapshot);
    dao.updateSnapshotAndChildrenLastFlagAndStatus(session, snapshot, isLast, SnapshotDto.STATUS_PROCESSED);
    session.commit();
  }
}
