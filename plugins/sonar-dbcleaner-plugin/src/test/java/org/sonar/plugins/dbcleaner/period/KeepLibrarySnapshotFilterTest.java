/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.dbcleaner.period;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonar.plugins.dbcleaner.Utils.createSnapshot;

public class KeepLibrarySnapshotFilterTest {

  @Test
  public void testFilter() {
    List<Snapshot> snapshots = Lists.newLinkedList();
    Snapshot snapshot = createSnapshot(2, "0.1");
    snapshots.add(snapshot);
    snapshot.setQualifier("TRK");
    snapshot = createSnapshot(2, "0.1");
    snapshot.setQualifier("LIB");
    snapshots.add(snapshot);

    assertThat(new KeepLibrarySnapshotFilter().filter(snapshots), is(1));
    assertThat(snapshots.size(), is(1));
    assertThat(snapshots.get(0).getId(), is(2));
  }
}
