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
package org.sonar.core.purge;

import org.sonar.api.batch.Purge;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.*;
import org.sonar.api.design.DependencyDto;

import javax.persistence.Query;
import java.util.List;

public abstract class AbstractPurge implements Purge {

  private static final int MAX_IN_ELEMENTS = 950;

  private int sqlInPageSize = MAX_IN_ELEMENTS;
  private DatabaseSession session;

  public AbstractPurge(DatabaseSession session) {
    this.session = session;
  }

  protected DatabaseSession getSession() {
    return session;
  }

  /**
   * Delete SNAPSHOTS and all dependent tables (MEASURES, ...)
   */
  protected void deleteSnapshotData(List<Integer> snapshotIds) {
    deleteMeasuresBySnapshotId(snapshotIds);
    deleteSources(snapshotIds);
    deleteViolations(snapshotIds);
    deleteDependencies(snapshotIds);
    deleteSnapshots(snapshotIds);
  }

  protected void deleteDependencies(List<Integer> snapshotIds) {
    executeQuery(snapshotIds, "delete from " + DependencyDto.class.getSimpleName() + " d where d.fromSnapshotId in (:ids)");
    executeQuery(snapshotIds, "delete from " + DependencyDto.class.getSimpleName() + " d where d.toSnapshotId in (:ids)");
  }

  /**
   * Delete all measures, including MEASURE_DATA
   */
  protected void deleteMeasuresBySnapshotId(List<Integer> snapshotIds) {
    executeQuery(snapshotIds, "delete from " + MeasureData.class.getSimpleName() + " m where m.snapshotId in (:ids)");
    executeQuery(snapshotIds, "delete from " + MeasureModel.class.getSimpleName() + " m where m.snapshotId in (:ids)");
  }

  /**
   * Delete all measures, including MEASURE_DATA
   */
  protected void deleteMeasuresById(List<Integer> measureIds) {
    executeQuery(measureIds, "delete from " + MeasureData.class.getSimpleName() + " m where m.measure.id in (:ids)");
    executeQuery(measureIds, "delete from " + MeasureModel.class.getSimpleName() + " m where m.id in (:ids)");
  }

  /**
   * Delete SNAPSHOT_SOURCES table
   */
  protected void deleteSources(List<Integer> snapshotIds) {
    executeQuery(snapshotIds, "delete from " + SnapshotSource.class.getSimpleName() + " e where e.snapshotId in (:ids)");
  }

  /**
   * Delete violations (RULE_FAILURES table)
   */
  protected void deleteViolations(List<Integer> snapshotIds) {
    executeQuery(snapshotIds, "delete from " + RuleFailureModel.class.getSimpleName() + " e where e.snapshotId in (:ids)");
  }

  /**
   * Delete SNAPSHOTS table
   */
  protected void deleteSnapshots(List<Integer> snapshotIds) {
    executeQuery(snapshotIds, "delete from " + Snapshot.class.getSimpleName() + " s where s.id in (:ids)");
  }


  /**
   * Paginate execution of SQL requests to avoid exceeding size of rollback segment
   */
  protected void executeQuery(List<Integer> ids, String hql) {
    if (ids == null || ids.isEmpty()) {
      return;
    }

    int index = 0;
    while (index < ids.size()) {
      Query query = session.createQuery(hql);
      List<Integer> paginedSids = ids.subList(index, Math.min(ids.size(), index + sqlInPageSize));
      query.setParameter("ids", paginedSids);
      query.executeUpdate();
      index += sqlInPageSize;
      session.commit();
    }
  }
}
