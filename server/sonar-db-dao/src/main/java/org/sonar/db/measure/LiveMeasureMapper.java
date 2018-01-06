/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

public interface LiveMeasureMapper {

  List<LiveMeasureDto> selectByComponentUuidsAndMetricIds(
    @Param("componentUuids") List<String> componentUuids,
    @Param("metricIds") Collection<Integer> metricIds);

  List<LiveMeasureDto> selectByComponentUuidsAndMetricKeys(
    @Param("componentUuids") List<String> componentUuids,
    @Param("metricKeys") Collection<String> metricKeys);

  void selectTreeByQuery(
    @Param("query") MeasureTreeQuery measureQuery,
    @Param("baseUuid") String baseUuid,
    @Param("baseUuidPath") String baseUuidPath,
    ResultHandler<LiveMeasureDto> resultHandler);

  void insert(
    @Param("dto") LiveMeasureDto dto,
    @Param("uuid") String uuid,
    @Nullable @Param("marker") String marker,
    @Param("now") long now);

  int update(
    @Param("dto") LiveMeasureDto dto,
    @Nullable @Param("marker") String marker,
    @Param("now") long now);

  void deleteByProjectUuidExcludingMarker(
    @Param("projectUuid") String projectUuid,
    @Param("marker") String marker);
}
