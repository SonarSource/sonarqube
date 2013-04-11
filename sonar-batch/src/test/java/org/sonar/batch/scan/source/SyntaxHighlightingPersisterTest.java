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
import org.mockito.InOrder;
import org.sonar.api.database.model.Snapshot;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import java.util.Map;

import static org.mockito.Mockito.*;

public class SyntaxHighlightingPersisterTest {

  private SnapshotCache snapshots;
  private SyntaxHighlightingCache highlightingCache;
  private SnapshotDataDao snapshotDataDao;

  @Before
  public void setUpInjectedObjects() {
    snapshots = mock(SnapshotCache.class);
    highlightingCache = mock(SyntaxHighlightingCache.class);
    snapshotDataDao = mock(SnapshotDataDao.class);
  }

  @Test
  public void should_persist_components_highlighting() throws Exception {

    Snapshot snapshotComponent1 = mock(Snapshot.class);
    when(snapshotComponent1.getId()).thenReturn(1);
    when(snapshotComponent1.getResourceId()).thenReturn(1);

    Snapshot snapshotComponent2 = mock(Snapshot.class);
    when(snapshotComponent2.getId()).thenReturn(2);
    when(snapshotComponent2.getResourceId()).thenReturn(2);

    Map<String, String> highlightingRules = Maps.newHashMap();
    highlightingRules.put("component1", "0,10,k;2,4,k;15,25,cppd;");
    highlightingRules.put("component2", "0,5,cppd;15,25,k;");

    when(highlightingCache.getHighlightingRulesByComponent()).thenReturn(highlightingRules);

    when(snapshots.get("component1")).thenReturn(snapshotComponent1);
    when(snapshots.get("component2")).thenReturn(snapshotComponent2);

    SyntaxHighlightingPersister persister = new SyntaxHighlightingPersister(snapshotDataDao, highlightingCache, snapshots);
    persister.persist();

    InOrder orderedMock = inOrder(snapshotDataDao);

    orderedMock.verify(snapshotDataDao).insert(argThat(new ArgumentMatcher<SnapshotDataDto>() {
      @Override
      public boolean matches(Object o) {
        return ((SnapshotDataDto)o).getSnapshotId() == 1;
      }
    }));

    orderedMock.verify(snapshotDataDao).insert(argThat(new ArgumentMatcher<SnapshotDataDto>() {
      @Override
      public boolean matches(Object o) {
        return ((SnapshotDataDto)o).getSnapshotId() == 2;
      }
    }));
  }
}
