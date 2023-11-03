/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Collection;
import java.util.function.Consumer;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.RemovedMetricConverter;
import org.sonarqube.ws.Measures;

import static org.sonar.db.metric.RemovedMetricConverter.REMOVED_METRIC;
import static org.sonar.db.metric.RemovedMetricConverter.DEPRECATED_METRIC_REPLACEMENT;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.metricDtoToWsMetric;
import static org.sonar.server.measure.ws.MetricDtoToWsMetric.wontFixToAcceptedWsMetric;

public class ComponentResponseCommon {

  private ComponentResponseCommon() {
    // static methods only
  }

  static void addMetricToResponseIncludingRenamedMetric(Consumer<org.sonarqube.ws.Common.Metric> responseBuilder, Collection<String> requestedMetrics,
    MetricDto metricDto) {
    if (metricDto.getKey().equals(DEPRECATED_METRIC_REPLACEMENT)) {
      if (requestedMetrics.contains(DEPRECATED_METRIC_REPLACEMENT)) {
        responseBuilder.accept(metricDtoToWsMetric(metricDto));
      }
      if (requestedMetrics.contains(REMOVED_METRIC)) {
        responseBuilder.accept(wontFixToAcceptedWsMetric(metricDto));
      }
    } else {
      responseBuilder.accept(metricDtoToWsMetric(metricDto));
    }
  }

  public static void addMetricToSearchHistoryResponseIncludingRenamedMetric(Measures.SearchHistoryResponse.Builder response,
    Collection<String> requestedMetrics, Measures.SearchHistoryResponse.HistoryMeasure.Builder measure) {
    if (measure.getMetric().equals(DEPRECATED_METRIC_REPLACEMENT)) {
      if (requestedMetrics.contains(DEPRECATED_METRIC_REPLACEMENT)) {
        response.addMeasures(measure.build());
      }
      if (requestedMetrics.contains(RemovedMetricConverter.REMOVED_METRIC)) {
        response.addMeasures(measure.setMetric(RemovedMetricConverter.REMOVED_METRIC).build());
      }
    } else {
      response.addMeasures(measure.build());
    }
  }

  static void addMeasureIncludingRenamedMetric(Collection<String> requestedMetrics, Measures.Component.Builder componentBuilder,
    Measures.Measure.Builder measureBuilder) {
    if (measureBuilder.getMetric().equals(DEPRECATED_METRIC_REPLACEMENT)) {
      if (requestedMetrics.contains(DEPRECATED_METRIC_REPLACEMENT)) {
        componentBuilder.addMeasures(measureBuilder.build());
      }
      if (requestedMetrics.contains(REMOVED_METRIC)) {
        componentBuilder.addMeasures(measureBuilder.setMetric(REMOVED_METRIC).build());
      }
    } else {
      componentBuilder.addMeasures(measureBuilder.build());
    }
  }
}
