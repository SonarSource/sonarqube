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

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.AsyncMeasureSnapshot;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.Snapshot;

import java.util.*;

public class AsyncMeasuresService {
  private final DatabaseSession session;

  public AsyncMeasuresService(DatabaseSession session) {
    this.session = session;
  }

  public void refresh(Snapshot snapshot) {
    AsyncMeasuresDao dao = new AsyncMeasuresDao(session);
    Snapshot previousSnapshot = dao.getPreviousSnapshot(snapshot);
    Date datePreviousSnapshot = (previousSnapshot != null ? previousSnapshot.getCreatedAt() : null);

    List<AsyncMeasureSnapshot> previousAsyncMeasureSnapshots = dao.getPreviousAsyncMeasureSnapshots(
        snapshot.getResourceId(), datePreviousSnapshot, snapshot.getCreatedAt());
    if (previousSnapshot != null) {
      previousAsyncMeasureSnapshots.addAll(dao.getAsyncMeasureSnapshotsFromSnapshotId(
          previousSnapshot.getId(), getMetricIds(previousAsyncMeasureSnapshots)));
    }

    for (AsyncMeasureSnapshot asyncMeasureSnapshot : purge(previousAsyncMeasureSnapshots)) {
      if (asyncMeasureSnapshot.getSnapshotId() == null) {
        dao.updateAsyncMeasureSnapshot(asyncMeasureSnapshot, snapshot);
      } else {
        dao.createAsyncMeasureSnapshot(
            asyncMeasureSnapshot.getMeasureId(), snapshot.getId(), asyncMeasureSnapshot.getMeasureDate(),
            snapshot.getCreatedAt(), asyncMeasureSnapshot.getMetricId(), asyncMeasureSnapshot.getProjectId());
      }
    }
    session.commit();
  }

  public void registerMeasure(Long id) {
    AsyncMeasuresDao dao = new AsyncMeasuresDao(session);
    registerMeasure(dao.getAsyncMeasure(id), dao);
  }

  private List<Integer> getMetricIds(List<AsyncMeasureSnapshot> list) {
    List<Integer> ids = new ArrayList<Integer>();
    for (AsyncMeasureSnapshot ams : list) {
      ids.add(ams.getMetricId());
    }
    return ids;
  }

  private Collection<AsyncMeasureSnapshot> purge(List<AsyncMeasureSnapshot> list) {
    Map<Integer, AsyncMeasureSnapshot> measuresById = new LinkedHashMap<Integer, AsyncMeasureSnapshot>();
    for (AsyncMeasureSnapshot currentAsyncMeasureSnapshot : list) {
      AsyncMeasureSnapshot asyncMeasureSnapshotFromMap = measuresById.get(currentAsyncMeasureSnapshot.getMetricId());
      if (asyncMeasureSnapshotFromMap != null) {
        if (asyncMeasureSnapshotFromMap.getMeasureDate().before(currentAsyncMeasureSnapshot.getMeasureDate())) {
          measuresById.put(currentAsyncMeasureSnapshot.getMetricId(), currentAsyncMeasureSnapshot);
        }
      } else {
        measuresById.put(currentAsyncMeasureSnapshot.getMetricId(), currentAsyncMeasureSnapshot);
      }
    }
    return measuresById.values();
  }


  public void deleteMeasure(Long id) {
    AsyncMeasuresDao dao = new AsyncMeasuresDao(session);
    MeasureModel measure = dao.getAsyncMeasure(id);
    AsyncMeasureSnapshot pastAsyncMeasureSnapshot = dao.getLastAsyncMeasureSnapshot(measure.getProjectId(),
        measure.getMetric().getId(), measure.getMeasureDate());
    dao.deleteAsyncMeasure(measure);
    if (pastAsyncMeasureSnapshot != null) {
      MeasureModel pastAsyncMeasure = dao.getAsyncMeasure(pastAsyncMeasureSnapshot.getMeasureId());
      dao.deleteAsyncMeasureSnapshots(pastAsyncMeasureSnapshot.getMeasureId());
      registerMeasure(pastAsyncMeasure, dao);
    }
    session.commit();
  }

  private void registerMeasure(MeasureModel measure, AsyncMeasuresDao dao) {
    AsyncMeasureSnapshot nextAsyncMeasureSnapshot = dao.getNextAsyncMeasureSnapshot(
        measure.getProjectId(), measure.getMetric().getId(), measure.getMeasureDate());
    Date dateNextAsyncMeasure = (nextAsyncMeasureSnapshot != null) ? nextAsyncMeasureSnapshot.getMeasureDate() : null;

    List<AsyncMeasureSnapshot> nextAsyncMeasureSnapshots = dao.getNextAsyncMeasureSnapshotsUntilDate(
        measure, dateNextAsyncMeasure);
    if (!nextAsyncMeasureSnapshots.isEmpty()) {
      for (AsyncMeasureSnapshot asyncMeasureSnapshot : nextAsyncMeasureSnapshots) {
        dao.createAsyncMeasureSnapshot(measure.getId(), asyncMeasureSnapshot.getSnapshotId(), measure.getMeasureDate(),
            asyncMeasureSnapshot.getSnapshotDate(), measure.getMetric().getId(), measure.getProjectId());
        dao.removeSnapshotFromAsyncMeasureSnapshot(asyncMeasureSnapshot);
      }
    } else {
      List<Snapshot> nextSnapshotsUntilDate = dao.getNextSnapshotsUntilDate(measure, dateNextAsyncMeasure);
      if (!nextSnapshotsUntilDate.isEmpty()) {
        for (Snapshot nextSnapshot : nextSnapshotsUntilDate) {
          dao.createAsyncMeasureSnapshot(measure.getId(), nextSnapshot.getId(), measure.getMeasureDate(),
              nextSnapshot.getCreatedAt(), measure.getMetric().getId(), measure.getProjectId());
        }
      } else {
        dao.createAsyncMeasureSnapshot(measure.getId(), null, measure.getMeasureDate(),
            null, measure.getMetric().getId(), measure.getProjectId());
      }
    }
  }

}