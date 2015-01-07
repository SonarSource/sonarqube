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
package org.sonar.batch.index;

import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotDataDto;

/**
 * Store file hashes in snapshot_data for reuse in next analysis to know if a file is modified
 */
public class FileHashesPersister implements ScanPersister {
  private final ComponentDataCache data;
  private final ResourceCache resourceCache;
  private final SnapshotDataDao dao;
  private final MyBatis mybatis;

  public FileHashesPersister(ComponentDataCache data, ResourceCache resourceCache,
    SnapshotDataDao dao, MyBatis mybatis) {
    this.data = data;
    this.resourceCache = resourceCache;
    this.dao = dao;
    this.mybatis = mybatis;
  }

  @Override
  public void persist() {
    try (DbSession session = mybatis.openSession(true)) {
      for (BatchResource batchResource : resourceCache.all()) {
        String componentKey = batchResource.resource().getEffectiveKey();
        String fileHashesdata = data.getStringData(componentKey, SnapshotDataTypes.FILE_HASHES);
        if (fileHashesdata != null) {
          SnapshotDataDto dto = new SnapshotDataDto();
          dto.setSnapshotId(batchResource.snapshotId());
          dto.setResourceId(batchResource.resource().getId());
          dto.setDataType(SnapshotDataTypes.FILE_HASHES);
          dto.setData(fileHashesdata);
          dao.insert(session, dto);
        }
      }
      session.commit();
    }
  }
}
