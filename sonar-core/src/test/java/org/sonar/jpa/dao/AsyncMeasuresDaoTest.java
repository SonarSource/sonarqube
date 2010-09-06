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
package org.sonar.jpa.dao;

import org.junit.Test;
import org.sonar.api.database.model.AsyncMeasureSnapshot;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncMeasuresDaoTest extends AbstractDbUnitTestCase {

  private static final int PROJECT_ID = 1;
  private static final int METRIC_ID = 1;

  @Test
  public void testGetNextAsyncMeasureSnapshot() {
    setupData("sharedFixture", "testGetNextAsyncMeasureSnapshot");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    AsyncMeasureSnapshot asyncMeasure = asyncMeasuresDao.getNextAsyncMeasureSnapshot(
        PROJECT_ID, METRIC_ID, stringToDate("2008-12-04 08:00:00.00"));

    assertThat(asyncMeasure.getId(), is(3));
  }

  @Test
  public void testGetNextSnapshotsUntilDate() {
    setupData("sharedFixture", "testGetNextSnapshotsUntilDate");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    MeasureModel asyncMeasure = getSession().getEntityManager().find(MeasureModel.class, 1l);
    List<Snapshot> snapshotIds = asyncMeasuresDao.getNextSnapshotsUntilDate(
        asyncMeasure, stringToDate("2008-12-06 12:00:00.00"));

    assertThat(snapshotIds.size(), is(2));
    assertThat(snapshotIds.get(0).getId(), is(2));
    assertThat(snapshotIds.get(1).getId(), is(4));
  }

  @Test
  public void testGetPreviousSnapshot() {
    setupData("sharedFixture", "testGetPreviousSnapshot");
    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    Snapshot s = new Snapshot();
    s.setCreatedAt(stringToDate("2008-12-04 08:00:00.00"));
    s.setScope(ResourceModel.SCOPE_PROJECT);
    ResourceModel resource1 = getSession().getEntity(ResourceModel.class, 1);
    ResourceModel resource2 = getSession().getEntity(ResourceModel.class, 2);

    s.setResource(resource1);
    assertThat(asyncMeasuresDao.getPreviousSnapshot(s).getId(), is(1));

    s.setResource(resource2);
    assertThat(asyncMeasuresDao.getPreviousSnapshot(s).getId(), is(5));
  }

  @Test
  public void testGetNextAsyncMeasureSnapshotsUntilDate() {
    setupData("sharedFixture", "testGetNextAsyncMeasureSnapshotsUntilDate");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    MeasureModel asyncMeasure = getSession().getEntityManager().find(MeasureModel.class, 3l);
    List<AsyncMeasureSnapshot> asyncMeasureSnapshots = asyncMeasuresDao.getNextAsyncMeasureSnapshotsUntilDate(
        asyncMeasure, stringToDate("2008-12-06 08:00:00.00"));

    assertThat(asyncMeasureSnapshots.size(), is(2));
    assertThat(asyncMeasureSnapshots.get(0).getId(), is(2));
    assertThat(asyncMeasureSnapshots.get(1).getId(), is(3));
  }

  @Test
  public void testDeleteAsyncMeasure() {
    setupData("sharedFixture", "testDeleteAsyncMeasure");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    MeasureModel asyncMeasure = getSession().getEntityManager().find(MeasureModel.class, 1l);
    asyncMeasuresDao.deleteAsyncMeasure(asyncMeasure);

    getSession().commit();
    checkTables("testDeleteAsyncMeasure", "project_measures", "async_measure_snapshots");
  }

  @Test
  public void testGetAsyncMeasureSnapshotsFromSnapshotId() {
    setupData("sharedFixture", "testGetAsyncMeasureSnapshotsFromSnapshotId");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    Integer snapshotId = 1;
    List<AsyncMeasureSnapshot> asyncMeasureSnapshots = asyncMeasuresDao.getAsyncMeasureSnapshotsFromSnapshotId(
        snapshotId, Arrays.asList(1));
    assertThat(asyncMeasureSnapshots.size(), is(1));
    assertThat(asyncMeasureSnapshots.get(0).getId(), is(2));
  }

  @Test
  public void testGetLastAsyncMeasureSnapshot() {
    setupData("sharedFixture", "testGetLastAsyncMeasureSnapshot");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    AsyncMeasureSnapshot asyncMeasureSnapshot = asyncMeasuresDao.getLastAsyncMeasureSnapshot(
        PROJECT_ID, METRIC_ID, stringToDate("2008-12-04 12:00:00.00"));
    assertThat(asyncMeasureSnapshot.getId(), is(2));
  }

  @Test
  public void testDeleteAsyncMeasureSnapshots() {
    setupData("sharedFixture", "testDeleteAsyncMeasureSnapshots");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    asyncMeasuresDao.deleteAsyncMeasureSnapshots(1l);

    checkTables("testDeleteAsyncMeasureSnapshots", "async_measure_snapshots");
  }

  @Test
  public void testGetPreviousAsyncMeasureSnapshots() {
    setupData("sharedFixture", "testGetPreviousAsyncMeasureSnapshots");

    AsyncMeasuresDao asyncMeasuresDao = new AsyncMeasuresDao(getSession());
    List<AsyncMeasureSnapshot> asyncMeasureSnapshots = asyncMeasuresDao.getPreviousAsyncMeasureSnapshots(
        PROJECT_ID, stringToDate("2008-12-04 08:00:00.00"), stringToDate("2008-12-08 08:00:00.00"));
    assertThat(asyncMeasureSnapshots.size(), is(2));
    assertThat(asyncMeasureSnapshots.get(0).getId(), is(5));
    assertThat(asyncMeasureSnapshots.get(1).getId(), is(6));
  }


  private static Date stringToDate(String sDate) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");
    try {
      return sdf.parse(sDate);
    } catch (ParseException e) {
      throw new RuntimeException("Bad date format.");
    }
  }

}
