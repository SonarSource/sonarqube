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

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface MeasureMapper {

  MeasureDto selectByKey(@Param("componentKey") String componentKey, @Param("metricKey") String metricKey);

  List<MeasureDto> selectByComponentAndMetrics(@Param("componentKey") String componentKey, @Param("metricKeys") List<String> metricKeys);

  List<MeasureDto> selectBySnapshotAndMetricKeys(@Param("snapshotId") long snapshotId, @Param("metricKeys") List<String> metricKeys);

  @CheckForNull
  MeasureDto selectByComponentAndMetric(@Param("componentKey") String componentKey, @Param("metricKey") String metricKey);

  long countByComponentAndMetric(@Param("componentKey") String componentKey, @Param("metricKey") String metricKey);

  List<PastMeasureDto> selectByComponentUuidAndProjectSnapshotIdAndStatusAndMetricIds(@Param("componentUuid") String componentuuid, @Param("rootSnapshotId") long rootSnapshotId,
    @Param("metricIds") List<Integer> metricIds, @Param("status") String status);

  void insert(MeasureDto measureDto);

  List<String> selectMetricKeysForSnapshot(@Param("snapshotId") long snapshotId);
}
