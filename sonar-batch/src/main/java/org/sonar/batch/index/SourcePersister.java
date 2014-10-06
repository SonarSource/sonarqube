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

import com.google.common.collect.Sets;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.DuplicatedSourceException;
import org.sonar.api.resources.Resource;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.core.source.db.SnapshotSourceDto;

import javax.annotation.CheckForNull;

import java.util.Set;

public final class SourcePersister {

  private Set<Integer> savedSnapshotIds = Sets.newHashSet();
  private ResourcePersister resourcePersister;
  private final SnapshotSourceDao sourceDao;

  public SourcePersister(ResourcePersister resourcePersister, SnapshotSourceDao sourceDao) {
    this.resourcePersister = resourcePersister;
    this.sourceDao = sourceDao;
  }

  public void saveSource(Resource resource, String source) {
    Snapshot snapshot = resourcePersister.getSnapshotOrFail(resource);
    if (isCached(snapshot)) {
      throw new DuplicatedSourceException(resource);
    }
    SnapshotSourceDto dto = new SnapshotSourceDto();
    dto.setSnapshotId(snapshot.getId().longValue());
    dto.setData(source);
    sourceDao.insert(dto);
    addToCache(snapshot);
  }

  @CheckForNull
  public String getSource(Resource resource) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    if (snapshot != null && snapshot.getId() != null) {
      return sourceDao.selectSnapshotSource(snapshot.getId());
    }
    return null;
  }

  private boolean isCached(Snapshot snapshot) {
    return savedSnapshotIds.contains(snapshot.getId());
  }

  private void addToCache(Snapshot snapshot) {
    savedSnapshotIds.add(snapshot.getId());
  }

  public void clear() {
    savedSnapshotIds.clear();
  }
}
