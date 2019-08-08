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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Measures.SearchHistoryResponse;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryMeasure;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryValue;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatMeasureValue;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatNumericalValue;

class SearchHistoryResponseFactory {
  private final SearchHistoryResult result;
  private final HistoryMeasure.Builder measure;
  private final HistoryValue.Builder value;

  SearchHistoryResponseFactory(SearchHistoryResult result) {
    this.result = result;
    this.measure = HistoryMeasure.newBuilder();
    this.value = HistoryValue.newBuilder();
  }

  public SearchHistoryResponse apply() {
    return Stream.of(SearchHistoryResponse.newBuilder())
      .map(addPaging())
      .map(addMeasures())
      .map(SearchHistoryResponse.Builder::build)
      .collect(MoreCollectors.toOneElement());
  }

  private UnaryOperator<SearchHistoryResponse.Builder> addPaging() {
    return response -> response.setPaging(result.getPaging());
  }

  private UnaryOperator<SearchHistoryResponse.Builder> addMeasures() {
    Map<Integer, MetricDto> metricsById = result.getMetrics().stream().collect(MoreCollectors.uniqueIndex(MetricDto::getId));
    Map<String, SnapshotDto> analysesByUuid = result.getAnalyses().stream().collect(MoreCollectors.uniqueIndex(SnapshotDto::getUuid));
    Table<MetricDto, SnapshotDto, MeasureDto> measuresByMetricByAnalysis = HashBasedTable.create(result.getMetrics().size(), result.getAnalyses().size());
    result.getMeasures().forEach(m -> measuresByMetricByAnalysis.put(metricsById.get(m.getMetricId()), analysesByUuid.get(m.getAnalysisUuid()), m));

    return response -> {
      result.getMetrics().stream()
        .map(clearMetric())
        .map(addMetric())
        .map(metric -> addValues(measuresByMetricByAnalysis.row(metric)).apply(metric))
        .forEach(metric -> response.addMeasures(measure));

      return response;
    };
  }

  private UnaryOperator<MetricDto> addMetric() {
    return metric -> {
      measure.setMetric(metric.getKey());
      return metric;
    };
  }

  private UnaryOperator<MetricDto> addValues(Map<SnapshotDto, MeasureDto> measuresByAnalysis) {
    return metric -> {
      result.getAnalyses().stream()
        .map(clearValue())
        .map(addDate())
        .map(analysis -> addValue(analysis, metric, measuresByAnalysis.get(analysis)))
        .forEach(analysis -> measure.addHistory(value));

      return metric;
    };
  }

  private UnaryOperator<SnapshotDto> addDate() {
    return analysis -> {
      value.setDate(formatDateTime(analysis.getCreatedAt()));
      return analysis;
    };
  }

  private SnapshotDto addValue(SnapshotDto analysis, MetricDto dbMetric, @Nullable MeasureDto dbMeasure) {
    if (dbMeasure != null) {
      String measureValue = dbMetric.getKey().startsWith("new_")
        ? formatNumericalValue(dbMeasure.getVariation(), dbMetric)
        : formatMeasureValue(dbMeasure, dbMetric);
      if (measureValue != null) {
        value.setValue(measureValue);
      }
    }

    return analysis;
  }

  private UnaryOperator<MetricDto> clearMetric() {
    return metric -> {
      measure.clear();
      return metric;
    };
  }

  private UnaryOperator<SnapshotDto> clearValue() {
    return analysis -> {
      value.clear();
      return analysis;
    };
  }
}
