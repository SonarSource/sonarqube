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
package org.sonar.server.measure.ws;

import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Common.Metric;

import static java.util.Optional.ofNullable;
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
    ofNullable(metricDto.getDescription()).ifPresent(metric::setDescription);
    ofNullable(metricDto.getDomain()).ifPresent(metric::setDomain);
    if (metricDto.getDirection() != 0) {
      metric.setHigherValuesAreBetter(metricDto.getDirection() > 0);
    }
    metric.setQualitative(metricDto.isQualitative());
    metric.setHidden(metricDto.isHidden());
    metric.setCustom(metricDto.isUserManaged());
    ofNullable(metricDto.getDecimalScale()).ifPresent(metric::setDecimalScale);
    ofNullable(metricDto.getBestValue()).ifPresent(bv -> metric.setBestValue(formatNumericalValue(bv, metricDto)));
    ofNullable(metricDto.getWorstValue()).ifPresent(wv -> metric.setWorstValue(formatNumericalValue(wv, metricDto)));

    return metric.build();
  }
}
