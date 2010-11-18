/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

public class KeepLastSnapshotFilterTest {

  @Test
  public void testFilter() {
    List<Snapshot> snapshots = Lists.newLinkedList();
    snapshots.add(createSnapshot(1, "0.1"));
    Snapshot lastSnapshot = createSnapshot(2, "0.1");
    lastSnapshot.setLast(true);
    snapshots.add(lastSnapshot);

    assertThat(new KeepLastSnapshotFilter().filter(snapshots), is(1));
    assertThat(snapshots.size(), is(1));
    assertThat(snapshots.get(0).getId(), is(1));
  }
}
