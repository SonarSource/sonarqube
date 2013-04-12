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

package org.sonar.batch.scan.source;

import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.index.ScanPersister;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import java.util.Map;

public class SyntaxHighlightingPersister implements ScanPersister {

  public static final String DATA_TYPE = "highlight_syntax";

  private final SnapshotDataDao snapshotDataDao;
  private final SyntaxHighlightingCache highlightingCache;
  private final SnapshotCache snapshots;

  public SyntaxHighlightingPersister(SnapshotDataDao snapshotDataDao, SyntaxHighlightingCache highlightingCache, SnapshotCache snapshots) {
    this.snapshotDataDao = snapshotDataDao;
    this.highlightingCache = highlightingCache;
    this.snapshots = snapshots;
  }

  @Override
  public void persist() {

    Map<String, String> highlightingRules = highlightingCache.getHighlightingRulesByComponent();

    for (Map.Entry<String, String> componentRules : highlightingRules.entrySet()) {

      Snapshot snapshotForComponent = snapshots.get(componentRules.getKey());

      SnapshotDataDto snapshotDataDto = new SnapshotDataDto();
      if(snapshotForComponent != null) {
        snapshotDataDto.setSnapshotId(snapshotForComponent.getId());
        snapshotDataDto.setResourceId(snapshotForComponent.getResourceId());
        snapshotDataDto.setDataType(DATA_TYPE);
        snapshotDataDto.setData(highlightingRules.get(componentRules.getValue()));
        snapshotDataDao.insert(snapshotDataDto);
      }
    }
  }
}
