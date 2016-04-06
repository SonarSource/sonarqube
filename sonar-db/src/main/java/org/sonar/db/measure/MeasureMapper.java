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

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface MeasureMapper {

  MeasureDto selectByKey(@Param("componentKey") String componentKey, @Param("metricKey") String metricKey);

  List<MeasureDto> selectByComponentAndMetrics(@Param("componentKey") String componentKey, @Param("metricKeys") List<String> metricKeys);

  List<MeasureDto> selectBySnapshotAndMetricKeys(@Param("snapshotId") long snapshotId, @Param("metricKeys") List<String> metricKeys);

  List<MeasureDto> selectByDeveloperForSnapshotAndMetrics(@Nullable @Param("developerId") Long developerId, @Param("snapshotId") long snapshotId,
    @Param("metricIds") List<Integer> metricIds);

  List<MeasureDto> selectBySnapshotAndMetrics(@Param("snapshotId") long snapshotId, @Param("metricIds") List<Integer> input);

  List<MeasureDto> selectByDeveloperAndSnapshotIdsAndMetricIds(@Nullable @Param("developerId") Long developerId, @Param("snapshotIds") List<Long> snapshotIds,
    @Param("metricIds") List<Integer> metricIds);

  List<MeasureDto> selectProjectMeasuresByDeveloperForMetrics(@Param("developerId") long developerId, @Param("metricIds") Collection<Integer> metricIds);

  @CheckForNull
  MeasureDto selectByComponentAndMetric(@Param("componentKey") String componentKey, @Param("metricKey") String metricKey);

  long countByComponentAndMetric(@Param("componentKey") String componentKey, @Param("metricKey") String metricKey);

  List<PastMeasureDto> selectByComponentUuidAndProjectSnapshotIdAndStatusAndMetricIds(@Param("componentUuid") String componentuuid, @Param("rootSnapshotId") long rootSnapshotId,
    @Param("metricIds") List<Integer> metricIds, @Param("status") String status);

  void insert(MeasureDto measureDto);

  List<String> selectMetricKeysForSnapshot(@Param("snapshotId") long snapshotId);
}
