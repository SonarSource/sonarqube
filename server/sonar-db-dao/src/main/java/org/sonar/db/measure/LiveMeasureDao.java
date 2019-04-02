/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.KeyType;
import org.sonar.db.dialect.Dialect;

import static java.util.Collections.singletonList;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class LiveMeasureDao implements Dao {

  private final System2 system2;

  public LiveMeasureDao(System2 system2) {
    this.system2 = system2;
  }

  public List<LiveMeasureDto> selectByComponentUuidsAndMetricIds(DbSession dbSession, Collection<String> largeComponentUuids, Collection<Integer> metricIds) {
    if (largeComponentUuids.isEmpty() || metricIds.isEmpty()) {
      return Collections.emptyList();
    }

    return executeLargeInputs(
      largeComponentUuids,
      componentUuids -> mapper(dbSession).selectByComponentUuidsAndMetricIds(componentUuids, metricIds));
  }

  public void scrollSelectByComponentUuidAndMetricKeys(DbSession dbSession, String componentUuid, Collection<String> metricIds,
    ResultHandler<LiveMeasureDto> handler) {
    if (metricIds.isEmpty()) {
      return;
    }

    mapper(dbSession).scrollSelectByComponentUuidAndMetricKeys(componentUuid, metricIds, handler);
  }

  public List<LiveMeasureDto> selectByComponentUuidsAndMetricKeys(DbSession dbSession, Collection<String> largeComponentUuids, Collection<String> metricKeys) {
    if (largeComponentUuids.isEmpty() || metricKeys.isEmpty()) {
      return Collections.emptyList();
    }

    return executeLargeInputs(
      largeComponentUuids,
      componentUuids -> mapper(dbSession).selectByComponentUuidsAndMetricKeys(componentUuids, metricKeys));
  }

  public Optional<LiveMeasureDto> selectByComponentUuidAndMetricKey(DbSession dbSession, String componentUuid, String metricKey) {
    LiveMeasureDto liveMeasureDto = mapper(dbSession).selectByComponentUuidAndMetricKey(componentUuid, metricKey);
    return Optional.ofNullable(liveMeasureDto);
  }

  public Optional<LiveMeasureDto> selectMeasure(DbSession dbSession, String componentUuid, String metricKey) {
    List<LiveMeasureDto> measures = selectByComponentUuidsAndMetricKeys(dbSession, singletonList(componentUuid), singletonList(metricKey));
    // couple of columns [component_uuid, metric_id] is unique. List can't have more than 1 item.
    if (measures.size() == 1) {
      return Optional.of(measures.get(0));
    }
    return Optional.empty();
  }

  public void selectTreeByQuery(DbSession dbSession, ComponentDto baseComponent, MeasureTreeQuery query, ResultHandler<LiveMeasureDto> resultHandler) {
    if (query.returnsEmpty()) {
      return;
    }
    mapper(dbSession).selectTreeByQuery(query, baseComponent.uuid(), query.getUuidPath(baseComponent), resultHandler);
  }

  /**
   * Example:
   * If Main Branch = 0 LOCs (provisioned but never analyzed) and the "largest long-lived branch" is 120 LOCs, I'm expecting to consider the value 120.
   * If Main Branch = 100 LOCs and the "largest long-lived branch" is 120 LOCs, I'm expecting to consider the value 120.
   * If Main Branch = 100 LOCs and the "largest long-lived branch" is 80 LOCs, I'm expecting to consider the value 100.
   */
  public long sumNclocOfBiggestLongLivingBranch(DbSession dbSession, SumNclocDbQuery dbQuery) {
    Long ncloc = mapper(dbSession).sumNclocOfBiggestLongLivingBranch(
      NCLOC_KEY, KeyType.BRANCH, BranchType.LONG, dbQuery.getOrganizationUuid(), dbQuery.getOnlyPrivateProjects(), dbQuery.getProjectUuidToExclude());
    return ncloc == null ? 0L : ncloc;
  }

  public void insert(DbSession dbSession, LiveMeasureDto dto) {
    mapper(dbSession).insert(dto, Uuids.create(), system2.now());
  }

  public void insertOrUpdate(DbSession dbSession, LiveMeasureDto dto) {
    LiveMeasureMapper mapper = mapper(dbSession);
    long now = system2.now();
    if (mapper.update(dto, now) == 0) {
      mapper.insert(dto, Uuids.create(), now);
    }
  }

  /**
   * Similar to {@link #insertOrUpdate(DbSession, LiveMeasureDto)}, except that:
   * <ul>
   * <li>it is batch session friendly (single same statement for both updates and inserts)</li>
   * <li>it triggers a single SQL request</li>
   * </ul>
   * <p>
   * <strong>This method should not be called unless {@link Dialect#supportsUpsert()} is true</strong>
   */
  public int upsert(DbSession dbSession, Iterable<LiveMeasureDto> dtos) {
    for (LiveMeasureDto dto : dtos) {
      dto.setUuidForUpsert(Uuids.create());
    }
    int updated = 0;
    for (List<LiveMeasureDto> chunk : Iterables.partition(dtos, 100)) {
      updated += mapper(dbSession).upsert(chunk, system2.now());
    }
    return updated;
  }

  public int deleteByComponentUuidExcludingMetricIds(DbSession dbSession, String componentUuid, List<Integer> excludedMetricIds) {
    return mapper(dbSession).deleteByComponentUuidExcludingMetricIds(componentUuid, excludedMetricIds);
  }

  private static LiveMeasureMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(LiveMeasureMapper.class);
  }
}
