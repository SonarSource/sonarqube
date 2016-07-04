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

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.version.Select;

public interface MeasureMapper {

  List<MeasureDto> selectByQuery(@Param("query") MeasureQuery query);

  List<MeasureDto> selectByQuery(@Param("query") MeasureQuery query, Select.RowHandler rowHandler);

  List<PastMeasureDto> selectPastMeasures(@Param("componentUuid") String componentUuid, @Param("analysisUuid") String analysisUuid, @Param("metricIds") List<Integer> metricIds);

  void insert(MeasureDto measureDto);

}
