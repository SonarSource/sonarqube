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
package org.sonar.batch.deprecated.components;

import org.sonar.batch.deprecated.components.PastSnapshotFinderByDays;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PastSnapshotFinderByDaysTest extends AbstractDbUnitTestCase {

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  public void shouldGetNextSnapshot() {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009); // 2008-11-16
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(getSession());

    assertThat(finder.findFromDays(projectSnapshot, 50).getProjectSnapshotId(), is(1000));
  }

  @Test
  public void shouldIgnoreUnprocessedSnapshots() {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009); // 2008-11-16
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(getSession());

    assertThat(finder.findFromDays(projectSnapshot, 7).getProjectSnapshotId(), is(1006));
  }

  @Test
  public void shouldNotFindSelf() {
    setupData("shouldNotFindSelf");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1009); // 2008-11-16
    PastSnapshotFinderByDays finder = new PastSnapshotFinderByDays(getSession());

    assertThat(finder.findFromDays(projectSnapshot, 1).getProjectSnapshot(), nullValue());
  }

  @Test
  public void shouldLocateNearestSnapshotBefore() throws ParseException {
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
  public void shouldLocateNearestSnapshotAfter() throws ParseException {
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
    assertThat(PastSnapshotFinderByDays.getNearestToTarget(snapshots, current, 15), IsNull.nullValue());
  }

  private Snapshot newSnapshot(int id, String date) throws ParseException {
    Snapshot snapshot = new Snapshot();
    snapshot.setId(id);
    snapshot.setCreatedAtMs(dateFormat.parse(date).getTime());
    return snapshot;
  }
}
