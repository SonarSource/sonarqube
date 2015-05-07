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
package org.sonar.batch.deprecated.components;

import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.core.event.db.EventMapper;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;

import static org.sonar.api.utils.DateUtils.longToDate;

@BatchSide
public class PastSnapshotFinderByPreviousVersion {

  private final DatabaseSession session;
  private final MyBatis mybatis;

  public PastSnapshotFinderByPreviousVersion(DatabaseSession session, MyBatis mybatis) {
    this.session = session;
    this.mybatis = mybatis;
  }

  public PastSnapshot findByPreviousVersion(Snapshot projectSnapshot) {
    String currentVersion = projectSnapshot.getVersion();
    Integer resourceId = projectSnapshot.getResourceId();
    Long snapshotId;
    // Commit Hibernate transaction to avoid lock of project table
    session.commit();
    try (DbSession dbSession = mybatis.openSession(false)) {
      snapshotId = dbSession.getMapper(EventMapper.class).findSnapshotIdOfPreviousVersion(resourceId, currentVersion);
    }

    if (snapshotId == null) {
      return new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    }

    Snapshot snapshot = session.getSingleResult(Snapshot.class, "id", snapshotId.intValue());

    return new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, longToDate(snapshot.getCreatedAtMs()), snapshot).setModeParameter(snapshot.getVersion());
  }

}
