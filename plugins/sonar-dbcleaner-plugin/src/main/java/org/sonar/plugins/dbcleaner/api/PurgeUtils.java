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
package org.sonar.plugins.dbcleaner.api;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.batch.Event;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.*;
import org.sonar.api.design.DependencyDto;
import org.sonar.api.utils.TimeProfiler;

import com.google.common.annotations.VisibleForTesting;

import javax.persistence.Query;
import java.util.List;

/**
 * @since 2.5
 */
public final class PurgeUtils {

  public static final int DEFAULT_MINIMUM_PERIOD_IN_HOURS = 12;
  public static final String PROP_KEY_MINIMUM_PERIOD_IN_HOURS = "sonar.purge.minimumPeriodInHours";

  /**
   * Maximum elements in the SQL statement "IN" due to an Oracle limitation (see error ORA-01795)
   */
  public static final int MAX_IN_ELEMENTS = 950;

  private PurgeUtils() {
    // only static methods
  }

  public static int getMinimumPeriodInHours(Configuration conf) {
    int hours = DEFAULT_MINIMUM_PERIOD_IN_HOURS;
    if (conf != null) {
      hours = conf.getInt(PROP_KEY_MINIMUM_PERIOD_IN_HOURS, DEFAULT_MINIMUM_PERIOD_IN_HOURS);
    }
    return hours;
  }

  public static void deleteSnapshotsData(DatabaseSession session, List<Integer> snapshotIds) {
    deleteMeasuresBySnapshotId(session, snapshotIds);
    deleteSources(session, snapshotIds);
    deleteViolations(session, snapshotIds);
    deleteDependencies(session, snapshotIds);
    deleteDuplicationBlocks(session, snapshotIds);
    deleteEvents(session, snapshotIds);
    deleteSnapshots(session, snapshotIds);
  }

  public static void deleteDependencies(DatabaseSession session, List<Integer> snapshotIds) {
    executeQuery(session, "delete dependencies", snapshotIds, "delete from " + DependencyDto.class.getSimpleName() + " d where d.fromSnapshotId in (:ids)");
    executeQuery(session, "delete dependencies", snapshotIds, "delete from " + DependencyDto.class.getSimpleName() + " d where d.toSnapshotId in (:ids)");
  }

  /**
   * Delete all measures, including MEASURE_DATA
   */
  public static void deleteMeasuresBySnapshotId(DatabaseSession session, List<Integer> snapshotIds) {
    executeQuery(session, "delete measures by snapshot id", snapshotIds, "delete from " + MeasureData.class.getSimpleName() + " m where m.snapshotId in (:ids)");
    executeQuery(session, "delete measures by snapshot id", snapshotIds, "delete from " + MeasureModel.class.getSimpleName() + " m where m.snapshotId in (:ids)");
  }

  /**
   * Delete all measures, including MEASURE_DATA
   */
  public static void deleteMeasuresById(DatabaseSession session, List<Integer> measureIds) {
    executeQuery(session, "delete measures by id", measureIds, "delete from " + MeasureData.class.getSimpleName() + " m where m.measure.id in (:ids)");
    executeQuery(session, "delete measures by id", measureIds, "delete from " + MeasureModel.class.getSimpleName() + " m where m.id in (:ids)");
  }

  /**
   * Delete SNAPSHOT_SOURCES table
   */
  public static void deleteSources(DatabaseSession session, List<Integer> snapshotIds) {
    executeQuery(session, "delete sources", snapshotIds, "delete from " + SnapshotSource.class.getSimpleName() + " e where e.snapshotId in (:ids)");
  }

  /**
   * Delete violations (RULE_FAILURES table)
   */
  public static void deleteViolations(DatabaseSession session, List<Integer> snapshotIds) {
    executeQuery(session, "delete violations", snapshotIds, "delete from " + RuleFailureModel.class.getSimpleName() + " e where e.snapshotId in (:ids)");
  }

  /**
   * Delete DUPLICATIONS_INDEX table
   *
   * @since 2.11
   */
  private static void deleteDuplicationBlocks(DatabaseSession session, List<Integer> snapshotIds) {
    executeNativeQuery(session, "delete duplication blocks", snapshotIds, "delete from duplications_index where snapshot_id in (:ids)");
  }

  /**
   * Delete EVENTS table
   */
  public static void deleteEvents(DatabaseSession session, List<Integer> snapshotIds) {
    executeQuery(session, "delete events", snapshotIds, "delete from " + Event.class.getSimpleName() + " e where e.snapshot.id in (:ids)");
  }

  /**
   * Delete SNAPSHOTS table
   */
  public static void deleteSnapshots(DatabaseSession session, List<Integer> snapshotIds) {
    executeQuery(session, "delete snapshots", snapshotIds, "delete from " + Snapshot.class.getSimpleName() + " s where s.id in (:ids)");
  }

  public static void deleteResources(DatabaseSession session, List<Integer> ids) {
    executeQuery(session, "", ids, "DELETE FROM " + ResourceModel.class.getSimpleName() + " WHERE id in (:ids)");
    deleteResourceIndex(session, ids);
  }

  /**
   * Delete RESOURCE_INDEX table
   */
  public static void deleteResourceIndex(DatabaseSession session, List<Integer> resourceIds) {
    executeNativeQuery(session, "delete resource_index", resourceIds, "delete from resource_index where resource_id in (:ids)");
  }

  /**
   * Paginate execution of SQL requests to avoid exceeding size of rollback segment
   */
  public static void executeQuery(DatabaseSession session, String description, List<Integer> ids, String hql) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    TimeProfiler profiler = new TimeProfiler().setLevelToDebug().start("Execute " + description);
    int index = 0;
    while (index < ids.size()) {
      List<Integer> paginedSids = ids.subList(index, Math.min(ids.size(), index + MAX_IN_ELEMENTS));
      Query query = session.createQuery(hql);
      query.setParameter("ids", paginedSids);
      query.executeUpdate();
      index += MAX_IN_ELEMENTS;
      session.commit();
    }
    profiler.stop();
  }

  /**
   * @since 2.13
   */
  @VisibleForTesting
  static void executeNativeQuery(DatabaseSession session, String description, List<Integer> ids, String sql) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    TimeProfiler profiler = new TimeProfiler().setLevelToDebug().start("Execute " + description);
    int index = 0;
    while (index < ids.size()) {
      List<Integer> paginedSids = ids.subList(index, Math.min(ids.size(), index + MAX_IN_ELEMENTS));
      Query query = session.createNativeQuery(sql);
      query.setParameter("ids", paginedSids);
      query.executeUpdate();
      index += MAX_IN_ELEMENTS;
      session.commit();
    }
    profiler.stop();
  }

}
