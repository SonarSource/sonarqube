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
package org.sonar.server.metric;

import com.google.common.base.Function;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Metric;
import org.sonar.db.metric.MetricDto;

public enum MetricToDto implements Function<Metric, MetricDto> {
  INSTANCE;
  @Override
  @Nonnull
  public MetricDto apply(@Nonnull Metric metric) {
    MetricDto dto = new MetricDto();
    dto.setId(metric.getId());
    dto.setKey(metric.getKey());
    dto.setDescription(metric.getDescription());
    dto.setShortName(metric.getName());
    dto.setBestValue(metric.getBestValue());
    dto.setDomain(metric.getDomain());
    dto.setEnabled(metric.getEnabled());
    dto.setDirection(metric.getDirection());
    dto.setHidden(metric.isHidden());
    dto.setQualitative(metric.getQualitative());
    dto.setValueType(metric.getType().name());
    dto.setOptimizedBestValue(metric.isOptimizedBestValue());
    dto.setUserManaged(metric.getUserManaged());
    dto.setWorstValue(metric.getWorstValue());
    dto.setDeleteHistoricalData(metric.getDeleteHistoricalData());
    dto.setDecimalScale(metric.getDecimalScale());
    return dto;
  }
}
