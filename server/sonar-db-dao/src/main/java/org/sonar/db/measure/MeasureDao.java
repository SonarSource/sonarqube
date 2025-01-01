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
import java.util.Set;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.singletonList;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class MeasureDao implements Dao {

  private final System2 system2;

  public MeasureDao(System2 system2) {
    this.system2 = system2;
  }

  public int insert(DbSession dbSession, MeasureDto dto) {
    dto.computeJsonValueHash();
    return mapper(dbSession).insert(dto, system2.now());
  }

  /**
   * Update a measure. The measure json value will be overwritten.
   */
  public int update(DbSession dbSession, MeasureDto dto) {
    return mapper(dbSession).update(dto, system2.now());
  }

  /**
   * Unlike {@link #update(DbSession, MeasureDto)}, this method will not overwrite the entire json value,
   * but will update the measures inside the json.
   */
  public int insertOrUpdate(DbSession dbSession, MeasureDto dto) {
    long now = system2.now();
    Optional<MeasureDto> existingMeasureOpt = selectByComponentUuid(dbSession, dto.getComponentUuid());
    if (existingMeasureOpt.isPresent()) {
      MeasureDto existingDto = existingMeasureOpt.get();
      existingDto.getMetricValues().putAll(dto.getMetricValues());
      dto.getMetricValues().putAll(existingDto.getMetricValues());
      dto.computeJsonValueHash();
      return mapper(dbSession).update(dto, now);
    } else {
      dto.computeJsonValueHash();
      return mapper(dbSession).insert(dto, now);
    }
  }

  public Optional<MeasureDto> selectByComponentUuid(DbSession dbSession, String componentUuid) {
    List<MeasureDto> measures = mapper(dbSession).selectByComponentUuids(singletonList(componentUuid));
    if (!measures.isEmpty()) {
      // component_uuid column is unique. List can't have more than 1 item.
      return Optional.of(measures.get(0));
    }
    return Optional.empty();
  }

  public Optional<MeasureDto> selectByComponentUuidAndMetricKeys(DbSession dbSession, String componentUuid, Collection<String> metricKeys) {
    List<MeasureDto> measures = selectByComponentUuidsAndMetricKeys(dbSession, singletonList(componentUuid), metricKeys);
    // component_uuid column is unique. List can't have more than 1 item.
    if (measures.size() == 1) {
      return Optional.of(measures.get(0));
    }
    return Optional.empty();
  }

  public List<MeasureDto> selectByComponentUuidsAndMetricKeys(DbSession dbSession, Collection<String> largeComponentUuids,
    Collection<String> metricKeys) {
    if (largeComponentUuids.isEmpty() || metricKeys.isEmpty()) {
      return Collections.emptyList();
    }

    return executeLargeInputs(
      largeComponentUuids,
      componentUuids -> mapper(dbSession).selectByComponentUuids(componentUuids)).stream()
      .map(measureDto -> {
        measureDto.getMetricValues().entrySet().removeIf(entry -> !metricKeys.contains(entry.getKey()));
        return measureDto;
      })
      .filter(measureDto -> !measureDto.getMetricValues().isEmpty())
      .toList();
  }

  public void scrollSelectByComponentUuid(DbSession dbSession, String componentUuid, ResultHandler<MeasureDto> handler) {
    mapper(dbSession).scrollSelectByComponentUuid(componentUuid, handler);
  }

  public Set<MeasureHash> selectMeasureHashesForBranch(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectMeasureHashesForBranch(branchUuid);
  }

  public List<MeasureDto> selectBranchMeasuresForProject(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectBranchMeasuresForProject(projectUuid);
  }

  public void selectTreeByQuery(DbSession dbSession, ComponentDto baseComponent, MeasureTreeQuery query,
    ResultHandler<MeasureDto> resultHandler) {
    if (query.returnsEmpty()) {
      return;
    }
    mapper(dbSession).selectTreeByQuery(query, baseComponent.uuid(), query.getUuidPath(baseComponent), resultHandler);
  }

  public List<ProjectMainBranchMeasureDto> selectAllForProjectMainBranches(DbSession dbSession) {
    return mapper(dbSession).selectAllForProjectMainBranches();
  }

  public List<ProjectMainBranchMeasureDto> selectAllForProjectMainBranchesAssociatedToDefaultQualityProfile(DbSession dbSession) {
    return mapper(dbSession).selectAllForProjectMainBranchesAssociatedToDefaultQualityProfile();
  }

  public List<MeasureDto> selectAllForMainBranches(DbSession dbSession) {
    return mapper(dbSession).selectAllForMainBranches();
  }

  public long findNclocOfBiggestBranchForProject(DbSession dbSession, String projectUuid) {
    List<MeasureDto> branchMeasures = mapper(dbSession).selectBranchMeasuresForProject(projectUuid);

    long maxncloc = 0;
    for (MeasureDto measure : branchMeasures) {
      Long branchNcloc = measure.getLong(NCLOC_KEY);
      if (branchNcloc != null && branchNcloc > maxncloc) {
        maxncloc = branchNcloc;
      }
    }

    return maxncloc;
  }

  private static MeasureMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(MeasureMapper.class);
  }

}
