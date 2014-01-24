/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.index;

import org.sonar.api.database.model.Snapshot;
import org.sonar.core.persistence.BatchSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotDataDto;

import java.util.Map;

public class ComponentDataPersister implements ScanPersister {
  private final ComponentDataCache data;
  private final SnapshotCache snapshots;
  private final SnapshotDataDao dao;
  private final MyBatis mybatis;

  public ComponentDataPersister(ComponentDataCache data, SnapshotCache snapshots,
                                SnapshotDataDao dao, MyBatis mybatis) {
    this.data = data;
    this.snapshots = snapshots;
    this.dao = dao;
    this.mybatis = mybatis;
  }

  @Override
  public void persist() {
    BatchSession session = mybatis.openBatchSession();
    for (Map.Entry<String, Snapshot> componentEntry : snapshots.snapshots()) {
      String componentKey = componentEntry.getKey();
      Snapshot snapshot = componentEntry.getValue();
      for (Cache.Entry<Data> dataEntry : data.entries(componentKey)) {
        Data value = dataEntry.value();
        if (value != null) {
          SnapshotDataDto dto = new SnapshotDataDto();
          dto.setSnapshotId(snapshot.getId());
          dto.setResourceId(snapshot.getResourceId());
          dto.setDataType(dataEntry.key());
          dto.setData(value.writeString());
          dao.insert(session, dto);
        }
      }
    }
    session.commit();
  }
}
