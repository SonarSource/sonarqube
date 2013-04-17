/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import org.sonar.api.database.model.Snapshot;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import java.util.Map;

public class ComponentDataPersister implements ScanPersister {
  private final ComponentDataCache data;
  private final SnapshotCache snapshots;
  private final SnapshotDataDao dao;

  public ComponentDataPersister(ComponentDataCache data, SnapshotCache snapshots, SnapshotDataDao dao) {
    this.data = data;
    this.snapshots = snapshots;
    this.dao = dao;
  }

  @Override
  public void persist() {
    for (Map.Entry<String, Snapshot> componentEntry : snapshots.snapshots()) {
      String componentKey = componentEntry.getKey();
      Snapshot snapshot = componentEntry.getValue();
      for (Cache.Entry<Data> dataEntry : data.entries(componentKey)) {
        if (dataEntry.value() != null) {
          SnapshotDataDto dto = new SnapshotDataDto();
          dto.setSnapshotId(snapshot.getId());
          dto.setResourceId(snapshot.getResourceId());
          dto.setDataType(dataEntry.key());
          dto.setData(dataEntry.value().writeString());

          // TODO bulk insert
          dao.insert(dto);
        }
      }
    }
  }
}
