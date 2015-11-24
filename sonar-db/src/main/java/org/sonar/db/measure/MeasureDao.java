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

package org.sonar.db.measure;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;

import static com.google.common.collect.FluentIterable.from;

public class MeasureDao implements Dao {

  public boolean existsByKey(DbSession session, String componentKey, String metricKey) {
    return mapper(session).countByComponentAndMetric(componentKey, metricKey) > 0;
  }

  @CheckForNull
  public MeasureDto selectByComponentKeyAndMetricKey(DbSession session, String componentKey, String metricKey) {
    return mapper(session).selectByComponentAndMetric(componentKey, metricKey);
  }

  public List<MeasureDto> selectByComponentKeyAndMetricKeys(final DbSession session, final String componentKey, List<String> metricKeys) {
    return DatabaseUtils.executeLargeInputs(metricKeys, new Function<List<String>, List<MeasureDto>>() {
      @Override
      public List<MeasureDto> apply(List<String> keys) {
        return mapper(session).selectByComponentAndMetrics(componentKey, keys);
      }
    });
  }

  /**
   * Selects all measures of a specific snapshot for the specified metric keys.
   *
   * Uses by Views.
   */
  public List<MeasureDto> selectBySnapshotIdAndMetricKeys(final long snapshotId, Set<String> metricKeys, final DbSession dbSession) {
    return DatabaseUtils.executeLargeInputs(from(metricKeys).toSortedList(String.CASE_INSENSITIVE_ORDER),
      new Function<List<String>, List<MeasureDto>>() {
        @Override
        public List<MeasureDto> apply(List<String> keys) {
          return mapper(dbSession).selectBySnapshotAndMetricKeys(snapshotId, keys);
        }
      });
  }

  public List<PastMeasureDto> selectByComponentUuidAndProjectSnapshotIdAndMetricIds(final DbSession session, final String componentUuid, final long projectSnapshotId,
    Set<Integer> metricIds) {
    return DatabaseUtils.executeLargeInputs(metricIds, new Function<List<Integer>, List<PastMeasureDto>>() {
      @Override
      public List<PastMeasureDto> apply(List<Integer> ids) {
        return mapper(session).selectByComponentUuidAndProjectSnapshotIdAndStatusAndMetricIds(componentUuid, projectSnapshotId, ids,
          SnapshotDto.STATUS_PROCESSED);
      }
    });
  }

  /**
   * Used by plugin Developer Cockpit
   */
  public List<MeasureDto> selectByDeveloperForSnapshotAndMetrics(final DbSession dbSession, final long developerId, final long snapshotId, Collection<Integer> metricIds) {
    return DatabaseUtils.executeLargeInputs(metricIds, new Function<List<Integer>, List<MeasureDto>>() {
      @Override
      @Nonnull
      public List<MeasureDto> apply(@Nonnull List<Integer> input) {
        return mapper(dbSession).selectByDeveloperForSnapshotAndMetrics(developerId, snapshotId, input);
      }
    });
  }

  /**
   * Used by plugin Developer Cockpit
   */
  public List<MeasureDto> selectBySnapshotAndMetrics(final DbSession dbSession, final long snapshotId, Collection<Integer> metricIds) {
    return DatabaseUtils.executeLargeInputs(metricIds, new Function<List<Integer>, List<MeasureDto>>() {
      @Override
      @Nonnull
      public List<MeasureDto> apply(@Nonnull List<Integer> input) {
        return mapper(dbSession).selectBySnapshotAndMetrics(snapshotId, input);
      }
    });
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

  public List<String> selectMetricKeysForSnapshot(DbSession session, long snapshotId) {
    return mapper(session).selectMetricKeysForSnapshot(snapshotId);
  }

  private MeasureMapper mapper(DbSession session) {
    return session.getMapper(MeasureMapper.class);
  }
}
