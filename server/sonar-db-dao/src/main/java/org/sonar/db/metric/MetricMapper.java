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
package org.sonar.db.metric;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface MetricMapper {

  MetricDto selectById(long id);

  List<MetricDto> selectByIds(@Param("ids") List<Integer> ids);

  MetricDto selectByKey(@Param("key") String key);

  List<MetricDto> selectByKeys(@Param("keys") List<String> keys);

  List<MetricDto> selectAll();

  List<MetricDto> selectAllEnabled();

  List<MetricDto> selectAllEnabled(Map<String, Object> properties, RowBounds rowBounds);

  void insert(MetricDto dto);

  List<String> selectDomains();

  void disableByIds(@Param("ids") List<Integer> ids);

  int disableByKey(@Param("key") String key);

  int countEnabled(@Param("isCustom") @Nullable Boolean isCustom);

  void update(MetricDto metric);

  List<MetricDto> selectAvailableCustomMetricsByComponentUuid(String projectUuid);
}
