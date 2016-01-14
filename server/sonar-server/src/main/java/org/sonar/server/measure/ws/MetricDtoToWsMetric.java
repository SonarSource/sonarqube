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
package org.sonar.server.measure.ws;

import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Common.Metric;

import static org.sonar.server.measure.ws.MeasureValueFormatter.formatNumericalValue;

class MetricDtoToWsMetric {
  private MetricDtoToWsMetric() {
    // static methods only
  }

  static Metric metricDtoToWsMetric(MetricDto metricDto) {
    Metric.Builder metric = Metric.newBuilder();
    metric.setKey(metricDto.getKey());
    metric.setType(metricDto.getValueType());
    metric.setName(metricDto.getShortName());
    if (metricDto.getDescription() != null) {
      metric.setDescription(metricDto.getDescription());
    }
    metric.setDomain(metricDto.getDomain());
    if (metricDto.getDirection() != 0) {
      metric.setHigherValuesAreBetter(metricDto.getDirection() > 0);
    }
    metric.setQualitative(metricDto.isQualitative());
    metric.setHidden(metricDto.isHidden());
    metric.setCustom(metricDto.isUserManaged());
    if (metricDto.getDecimalScale() != null) {
      metric.setDecimalScale(metricDto.getDecimalScale());
    }
    if (metricDto.getBestValue() != null) {
      metric.setBestValue(formatNumericalValue(metricDto.getBestValue(), metricDto));
    }
    if (metricDto.getWorstValue() != null) {
      metric.setWorstValue(formatNumericalValue(metricDto.getWorstValue(), metricDto));
    }

    return metric.build();
  }
}
