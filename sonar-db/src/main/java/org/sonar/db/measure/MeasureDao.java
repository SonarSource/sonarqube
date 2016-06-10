/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.measure;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class MeasureDao implements Dao {

  public boolean existsByKey(DbSession session, String componentKey, String metricKey) {
    return mapper(session).countByComponentAndMetric(componentKey, metricKey) > 0;
  }

  @CheckForNull
  public MeasureDto selectByComponentKeyAndMetricKey(DbSession session, String componentKey, String metricKey) {
    return mapper(session).selectByComponentAndMetric(componentKey, metricKey);
  }

  public List<MeasureDto> selectByComponentKeyAndMetricKeys(DbSession session, String componentKey, List<String> metricKeys) {
    return executeLargeInputs(
      metricKeys,
      keys -> mapper(session).selectByComponentAndMetrics(componentKey, keys));
  }

  /**
   * Selects all measures of a specific snapshot for the specified metric keys.
   * <p/>
   * Used by Views.
   */
  public List<MeasureDto> selectBySnapshotIdAndMetricKeys(long snapshotId, Set<String> metricKeys, DbSession dbSession) {
    return executeLargeInputs(from(metricKeys).toSortedList(String.CASE_INSENSITIVE_ORDER),
      keys -> mapper(dbSession).selectBySnapshotAndMetricKeys(snapshotId, keys));
  }

  public List<PastMeasureDto> selectByComponentUuidAndProjectSnapshotIdAndMetricIds(DbSession session, String componentUuid, long projectSnapshotId,
    Set<Integer> metricIds) {
    return executeLargeInputs(
      metricIds,
      ids -> mapper(session).selectByComponentUuidAndProjectSnapshotIdAndStatusAndMetricIds(componentUuid, projectSnapshotId, ids,
        SnapshotDto.STATUS_PROCESSED));
  }

  /**
   * Used by plugin Developer Cockpit
   */
  public List<MeasureDto> selectByDeveloperForSnapshotAndMetrics(DbSession dbSession, @Nullable Long developerId, long snapshotId,
    Collection<Integer> metricIds) {
    return executeLargeInputs(
      metricIds,
      input -> mapper(dbSession).selectByDeveloperForSnapshotAndMetrics(developerId, snapshotId, input));
  }

  /**
   * Used by plugin Developer Cockpit
   */
  public List<MeasureDto> selectBySnapshotAndMetrics(DbSession dbSession, long snapshotId, Collection<Integer> metricIds) {
    return executeLargeInputs(
      metricIds,
      input -> mapper(dbSession).selectBySnapshotAndMetrics(snapshotId, input));
  }

  /**
   * Used by plugin Developer Cockpit
   */
  public List<MeasureDto> selectBySnapshotIdsAndMetricIds(DbSession dbSession, List<Long> snapshotIds, List<Integer> metricIds) {
    return selectByDeveloperAndSnapshotIdsAndMetricIds(dbSession, null, snapshotIds, metricIds);
  }

  public List<String> selectMetricKeysForSnapshot(DbSession session, long snapshotId) {
    return mapper(session).selectMetricKeysForSnapshot(snapshotId);
  }

  public List<MeasureDto> selectByDeveloperAndSnapshotIdsAndMetricIds(DbSession dbSession, @Nullable Long developerId, List<Long> snapshotIds, List<Integer> metricIds) {
    if (snapshotIds.isEmpty() || metricIds.isEmpty()) {
      return emptyList();
    }

    return executeLargeInputs(
      snapshotIds,
      snapshotIdsPartition -> mapper(dbSession).selectByDeveloperAndSnapshotIdsAndMetricIds(developerId, snapshotIdsPartition, metricIds));
  }

  /**
   * Retrieves all measures associated to a specific developer and to the last snapshot of any project.
   * <strong>property {@link MeasureDto#componentId} of the returned objects is populated</strong>
   *
   * Used by Developer Cockpit
   */
  public List<MeasureDto> selectProjectMeasuresByDeveloperForMetrics(DbSession dbSession, long developerId, Collection<Integer> metricIds) {
    return mapper(dbSession).selectProjectMeasuresByDeveloperForMetrics(developerId, metricIds);
  }

  public void insert(DbSession session, MeasureDto measureDto) {
    mapper(session).insert(measureDto);
  }

  public void insert(DbSession session, Collection<MeasureDto> items) {
    for (MeasureDto item : items) {
      insert(session, item);
    }
  }

  public void insert(DbSession session, MeasureDto item, MeasureDto... others) {
    insert(session, Lists.asList(item, others));
  }

  private static MeasureMapper mapper(DbSession session) {
    return session.getMapper(MeasureMapper.class);
  }
}
