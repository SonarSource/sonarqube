/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

public class PastSnapshotFinderByDaysTest extends AbstractDbUnitTestCase {

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  public void shouldLoadSnapshotsFromDatabase() {
    setupData("shouldLoadSnapshotsFromDatabase");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009);
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(projectSnapshot, getSession());
    List<Snapshot> snapshots = finder.loadSnapshotsFromDatabase();
    List<Integer> snapshotIds = Lists.newLinkedList();
    for (Snapshot snapshot : snapshots) {
      snapshotIds.add(snapshot.getId());
    }
    assertThat(snapshotIds.size(), is(2));
    assertThat(snapshotIds, hasItems(1000, 1003)); // project snapshots
  }

  @Test
  public void shouldGetNearestSnapshotBefore() throws ParseException {
    Date current = dateFormat.parse("2010-10-20");
    // distance: 15 => target is 2010-10-05

    List<Snapshot> snapshots = Arrays.asList(
        newSnapshot(1, "2010-09-30"),
        newSnapshot(2, "2010-10-03"),// -2 days
        newSnapshot(3, "2010-10-08"),// +3 days
        newSnapshot(4, "2010-10-12") // + 7 days
    );
    assertThat(PastSnapshotFinderByDays.getNearestToTarget(snapshots, current, 15).getId(), is(2));
  }

  @Test
  public void shouldgetNearestSnapshotAfter() throws ParseException {
    Date current = dateFormat.parse("2010-10-20");
    // distance: 15 => target is 2010-10-05

    List<Snapshot> snapshots = Arrays.asList(
        newSnapshot(1, "2010-09-30"),
        newSnapshot(2, "2010-10-01"),// -4 days
        newSnapshot(3, "2010-10-08"),// +3 days
        newSnapshot(4, "2010-10-12") // + 7 days
    );
    assertThat(PastSnapshotFinderByDays.getNearestToTarget(snapshots, current, 15).getId(), is(3));
  }

  @Test
  public void shouldReturnNullIfNoSnapshots() throws ParseException {
    Date current = dateFormat.parse("2010-10-20");
    List<Snapshot> snapshots = Collections.emptyList();
    assertThat(PastSnapshotFinderByDays.getNearestToTarget(snapshots, current, 15), nullValue());
  }

  private Snapshot newSnapshot(int id, String date) throws ParseException {
    Snapshot snapshot = new Snapshot();
    snapshot.setId(id);
    snapshot.setCreatedAt(dateFormat.parse(date));
    return snapshot;
  }
}
