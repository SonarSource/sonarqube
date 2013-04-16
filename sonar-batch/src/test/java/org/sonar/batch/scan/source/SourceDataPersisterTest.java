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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import java.util.Map;

import static org.mockito.Mockito.*;

public class SourceDataPersisterTest {

  private SnapshotCache snapshots;
  private SourceDataCache sourceDataCache;
  private SnapshotDataDao snapshotDataDao;

  @Before
  public void setUpInjectedObjects() {
    snapshots = mock(SnapshotCache.class);
    sourceDataCache = mock(SyntaxHighlightingCache.class);
    snapshotDataDao = mock(SnapshotDataDao.class);
  }

  @Test
  public void should_persist_source_cache_data() throws Exception {

    Snapshot snapshotComponent1 = mock(Snapshot.class);
    when(snapshotComponent1.getId()).thenReturn(1);
    when(snapshotComponent1.getResourceId()).thenReturn(1);

    Map<String, String> sourceData = Maps.newHashMap();
    sourceData.put("component1", "source data for component 1");

    when(sourceDataCache.getSourceDataByComponent()).thenReturn(sourceData);
    when(sourceDataCache.getDataType()).thenReturn("myDataType");
    when(snapshots.get("component1")).thenReturn(snapshotComponent1);

    SourceDataPersister persister = new SourceDataPersister(snapshotDataDao, new SourceDataCache[]{sourceDataCache}, snapshots);
    persister.persist();

    verify(snapshotDataDao).insert(argThat(new ArgumentMatcher<SnapshotDataDto>() {
      @Override
      public boolean matches(Object o) {
        SnapshotDataDto insertedData = (SnapshotDataDto) o;
        return insertedData.getSnapshotId() == 1 && insertedData.getDataType() == "myDataType";
      }
    }));
  }

  @Test
  public void should_ignore_components_without_snapshot() throws Exception {

    Map<String, String> sourceData = Maps.newHashMap();
    sourceData.put("component1", "source data for component 1");

    when(sourceDataCache.getSourceDataByComponent()).thenReturn(sourceData);
    when(snapshots.get("component1")).thenReturn(null);

    SourceDataPersister persister = new SourceDataPersister(snapshotDataDao, new SourceDataCache[]{sourceDataCache}, snapshots);
    persister.persist();

    verify(snapshotDataDao, times(0)).insert(any(SnapshotDataDto.class));
  }
}
