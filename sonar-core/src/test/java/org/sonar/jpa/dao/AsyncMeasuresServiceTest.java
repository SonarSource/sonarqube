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
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class AsyncMeasuresServiceTest extends AbstractDbUnitTestCase {

  @Test
  public void assignLatestMeasuresToLastSnapshot() {
    setupData("sharedFixture", "assignLatestMeasuresToLastSnapshot");

    AsyncMeasuresService asyncMeasuresService = new AsyncMeasuresService(getSession());
    Snapshot snapshot = getSession().getEntityManager().find(Snapshot.class, 2);
    asyncMeasuresService.refresh(snapshot);

    checkTables("assignLatestMeasuresToLastSnapshot", "async_measure_snapshots");
  }

  @Test
  public void assignNewMeasuresToLastSnapshot() {
    setupData("sharedFixture", "assignNewMeasuresToLastSnapshot");

    AsyncMeasuresService asyncMeasuresService = new AsyncMeasuresService(getSession());
    Snapshot snapshot = getSession().getEntityManager().find(Snapshot.class, 2);
    asyncMeasuresService.refresh(snapshot);

    checkTables("assignNewMeasuresToLastSnapshot", "async_measure_snapshots");
  }

  @Test
  public void assignMeasuresWhenNoPreviousSnapshot() {
    setupData("sharedFixture", "assignMeasuresWhenNoPreviousSnapshot");

    AsyncMeasuresService asyncMeasuresService = new AsyncMeasuresService(getSession());
    Snapshot snapshot = getSession().getEntityManager().find(Snapshot.class, 1);
    asyncMeasuresService.refresh(snapshot);

    checkTables("assignMeasuresWhenNoPreviousSnapshot", "async_measure_snapshots");
  }

  @Test
  public void assignLatestMeasuresWhenNoPreviousSnapshot() {
    setupData("sharedFixture", "assignLatestMeasuresWhenNoPreviousSnapshot");

    AsyncMeasuresService asyncMeasuresService = new AsyncMeasuresService(getSession());
    Snapshot snapshot = getSession().getEntityManager().find(Snapshot.class, 1);
    asyncMeasuresService.refresh(snapshot);

    checkTables("assignLatestMeasuresWhenNoPreviousSnapshot", "async_measure_snapshots");
  }

  @Test
  public void assignPastMeasuresToPastSnapshot() {
    setupData("sharedFixture", "assignPastMeasuresToPastSnapshot");

    AsyncMeasuresService asyncMeasuresService = new AsyncMeasuresService(getSession());
    Snapshot snapshot = getSession().getEntityManager().find(Snapshot.class, 3);
    asyncMeasuresService.refresh(snapshot);

    checkTables("assignPastMeasuresToPastSnapshot", "async_measure_snapshots");
  }

  @Test
  public void assignNewMeasureToFutureSnapshots() {
    setupData("sharedFixture", "assignNewMeasureToFutureSnapshots");

    AsyncMeasuresService asyncMeasuresService = new AsyncMeasuresService(getSession());
    asyncMeasuresService.registerMeasure(2l);

    checkTables("assignNewMeasureToFutureSnapshots", "async_measure_snapshots");
  }

  @Test
  public void assignMeasureToFutureSnapshotsWithDifferentMetric() {
    setupData("sharedFixture", "assignMeasureToFutureSnapshotsWithDifferentMetric");

    AsyncMeasuresService asyncMeasureService = new AsyncMeasuresService(getSession());
    asyncMeasureService.registerMeasure(3l);

    checkTables("assignMeasureToFutureSnapshotsWithDifferentMetric", "async_measure_snapshots");
  }

  @Test
  public void assignAPastMeasureToNextSnapshotsWithDifferentMetric() {
    setupData("sharedFixture", "assignAPastMeasureToNextSnapshotsWithDifferentMetric");

    AsyncMeasuresService asyncMeasureService = new AsyncMeasuresService(getSession());
    asyncMeasureService.registerMeasure(2l);

    checkTables("assignAPastMeasureToNextSnapshotsWithDifferentMetric", "async_measure_snapshots");
  }

  @Test
  public void addFutureSnapshot() {
    setupData("sharedFixture", "addFutureSnapshot");

    AsyncMeasuresService asyncMeasureService = new AsyncMeasuresService(getSession());
    asyncMeasureService.registerMeasure(2l);

    checkTables("addFutureSnapshot", "async_measure_snapshots");
  }

  @Test
  public void addInvisibleMeasure() {
    setupData("sharedFixture", "addInvisibleMeasure");

    AsyncMeasuresService asyncMeasureService = new AsyncMeasuresService(getSession());
    asyncMeasureService.registerMeasure(2l);

    checkTables("addInvisibleMeasure", "async_measure_snapshots");
  }

  @Test
  public void deleteMeasure() {
    setupData("sharedFixture", "deleteMeasure");

    AsyncMeasuresService asyncMeasureService = new AsyncMeasuresService(getSession());
    asyncMeasureService.deleteMeasure(2l);

    checkTables("deleteMeasure", "async_measure_snapshots");
  }

  @Test
  public void deleteLastMeasure() {
    setupData("sharedFixture", "deleteLastMeasure");

    AsyncMeasuresService asyncMeasureService = new AsyncMeasuresService(getSession());
    asyncMeasureService.deleteMeasure(1l);

    checkTables("deleteLastMeasure", "async_measure_snapshots");
  }

}
