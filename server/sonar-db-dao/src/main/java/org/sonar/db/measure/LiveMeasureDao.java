/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.dialect.Dialect;

import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class LiveMeasureDao implements Dao {

  private final System2 system2;

  public LiveMeasureDao(System2 system2) {
    this.system2 = system2;
  }

  public List<LiveMeasureDto> selectByComponentUuidsAndMetricUuids(DbSession dbSession, Collection<String> largeComponentUuids, Collection<String> metricUuids) {
    if (largeComponentUuids.isEmpty() || metricUuids.isEmpty()) {
      return Collections.emptyList();
    }

    return executeLargeInputs(
      largeComponentUuids,
      componentUuids -> mapper(dbSession).selectByComponentUuidsAndMetricUuids(componentUuids, metricUuids));
  }

  public List<LiveMeasureDto> selectForProjectsByMetricUuids(DbSession dbSession, Collection<String> metricUuids) {
    return mapper(dbSession).selectForProjectsByMetricUuids(metricUuids);
  }

  public List<ProjectMainBranchLiveMeasureDto> selectForProjectMainBranchesByMetricUuids(DbSession dbSession, Collection<String> metricUuids) {
    return mapper(dbSession).selectForProjectMainBranchesByMetricUuids(metricUuids);
  }

  public void scrollSelectByComponentUuidAndMetricKeys(DbSession dbSession, String componentUuid, Collection<String> metricKeys, ResultHandler<LiveMeasureDto> handler) {
    if (metricKeys.isEmpty()) {
      return;
    }

    mapper(dbSession).scrollSelectByComponentUuidAndMetricKeys(componentUuid, metricKeys, handler);
  }

  public List<LiveMeasureDto> selectByComponentUuidsAndMetricKeys(DbSession dbSession, Collection<String> largeComponentUuids, Collection<String> metricKeys) {
    if (largeComponentUuids.isEmpty() || metricKeys.isEmpty()) {
      return Collections.emptyList();
    }

    return executeLargeInputs(
      largeComponentUuids,
      componentUuids -> mapper(dbSession).selectByComponentUuidsAndMetricKeys(componentUuids, metricKeys));
  }

  public List<LiveMeasureDto> selectByComponentUuidAndMetricKeys(DbSession dbSession, String componentUuid, Collection<String> metricKeys) {
    if (metricKeys.isEmpty()) {
      return Collections.emptyList();
    }

    return mapper(dbSession).selectByComponentUuidAndMetricKeys(componentUuid, metricKeys);
  }

  public Optional<LiveMeasureDto> selectMeasure(DbSession dbSession, String componentUuid, String metricKey) {
    LiveMeasureDto liveMeasureDto = mapper(dbSession).selectByComponentUuidAndMetricKey(componentUuid, metricKey);
    return Optional.ofNullable(liveMeasureDto);
  }

  public void selectTreeByQuery(DbSession dbSession, ComponentDto baseComponent, MeasureTreeQuery query, ResultHandler<LiveMeasureDto> resultHandler) {
    if (query.returnsEmpty()) {
      return;
    }
    mapper(dbSession).selectTreeByQuery(query, baseComponent.uuid(), query.getUuidPath(baseComponent), resultHandler);
  }

  public long findNclocOfBiggestBranchForProject(DbSession dbSession, String projectUuid){
    Long ncloc = mapper(dbSession).findNclocOfBiggestBranchForProject(projectUuid, NCLOC_KEY);
    return ncloc == null ? 0L : ncloc;
  }

  public List<LargestBranchNclocDto> getLargestBranchNclocPerProject(DbSession dbSession, String nclocMetricUuid) {
    return mapper(dbSession).getLargestBranchNclocPerProject(nclocMetricUuid);
  }

  public List<ProjectLocDistributionDto> selectLargestBranchesLocDistribution(DbSession session, String nclocUuid, String nclocDistributionUuid) {
    return mapper(session).selectLargestBranchesLocDistribution(nclocUuid, nclocDistributionUuid);
  }

  public long countProjectsHavingMeasure(DbSession dbSession, String metric) {
    return mapper(dbSession).countProjectsHavingMeasure(metric);
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

  public void deleteByComponent(DbSession dbSession, String componentUuid) {
    mapper(dbSession).deleteByComponent(componentUuid);
  }

  /**
   * Similar to {@link #insertOrUpdate(DbSession, LiveMeasureDto)}, except that it triggers a single SQL request
   * <strong>This method should not be called unless {@link Dialect#supportsUpsert()} is true</strong>
   */
  public int upsert(DbSession dbSession, LiveMeasureDto dto) {
    dto.setUuidForUpsert(Uuids.create());
    return mapper(dbSession).upsert(dto, system2.now());
  }

  public void deleteByComponentUuidExcludingMetricUuids(DbSession dbSession, String componentUuid, List<String> excludedMetricUuids) {
    mapper(dbSession).deleteByComponentUuidExcludingMetricUuids(componentUuid, excludedMetricUuids);
  }

  private static LiveMeasureMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(LiveMeasureMapper.class);
  }
}
