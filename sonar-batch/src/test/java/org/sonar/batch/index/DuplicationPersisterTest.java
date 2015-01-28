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

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.File;
import org.sonar.api.rules.RuleFinder;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DuplicationPersisterTest extends AbstractDaoTestCase {

  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  public static final int FILE_SNAPSHOT_ID = 3003;

  DuplicationPersister duplicationPersister;
  RuleFinder ruleFinder = mock(RuleFinder.class);
  File aFile = File.create("org/foo/Bar.java");
  Snapshot fileSnapshot = snapshot(FILE_SNAPSHOT_ID);

  private DuplicationCache duplicationCache;

  @Before
  public void mockResourcePersister() {
    duplicationCache = mock(DuplicationCache.class);

    ResourceCache resourceCache = mock(ResourceCache.class);
    BatchResource batchResource = mock(BatchResource.class);
    when(batchResource.resource()).thenReturn(aFile);
    when(batchResource.snapshotId()).thenReturn(FILE_SNAPSHOT_ID);
    when(resourceCache.get("foo:org/foo/Bar.java")).thenReturn(batchResource);

    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey(CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(CoreMetrics.DUPLICATIONS_DATA.setId(2));

    duplicationPersister = new DuplicationPersister(getMyBatis(), ruleFinder, resourceCache, duplicationCache, metricFinder);
  }

  @Test
  public void should_insert_duplications() {
    setupData("empty");

    DuplicationGroup.Block originBlock = new DuplicationGroup.Block("foo:org/foo/Bar.java", 1, 4);

    DuplicationGroup group = new DuplicationGroup(originBlock)
      .addDuplicate(new DuplicationGroup.Block("foo:org/foo/Foo.java", 5, 9));

    when(duplicationCache.entries()).thenReturn(
      Arrays.<Cache.Entry<List<DuplicationGroup>>>asList(new Cache.Entry(new String[] {"foo:org/foo/Bar.java"}, Arrays.asList(group))));

    duplicationPersister.persist();

    checkTables("shouldInsertDuplication", "project_measures");
  }

  private static Snapshot snapshot(int id) {
    Snapshot snapshot = mock(Snapshot.class);
    when(snapshot.getId()).thenReturn(id);
    return snapshot;
  }

}
