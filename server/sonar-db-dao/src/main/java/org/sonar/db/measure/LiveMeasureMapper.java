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
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

public interface LiveMeasureMapper {

  List<LiveMeasureDto> selectByComponentUuidsAndMetricUuids(
    @Param("componentUuids") Collection<String> componentUuids,
    @Param("metricUuids") Collection<String> metricUuids);

  LiveMeasureDto selectByComponentUuidAndMetricKey(
    @Param("componentUuid") String componentUuid,
    @Param("metricKey") String metricKey);

  void insert(
    @Param("dto") LiveMeasureDto dto,
    @Param("uuid") String uuid,
    @Param("now") long now);

  int update(
    @Param("dto") LiveMeasureDto dto,
    @Param("now") long now);

  int upsert(
    @Param("dto") LiveMeasureDto dto,
    @Param("now") long now);

  void deleteByComponentUuidExcludingMetricUuids(
    @Param("componentUuid") String componentUuid,
    @Param("excludedMetricUuids") List<String> excludedMetricUuids);

}
