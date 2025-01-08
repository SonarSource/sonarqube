/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Measures.SearchHistoryResponse;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryMeasure;
import org.sonarqube.ws.Measures.SearchHistoryResponse.HistoryValue;

import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.measure.ws.ComponentResponseCommon.addMetricToSearchHistoryResponseIncludingRenamedMetric;
import static org.sonar.server.measure.ws.MeasureValueFormatter.formatMeasureValue;

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
    return Optional.of(SearchHistoryResponse.newBuilder())
      .map(addPaging())
      .map(addMeasures())
      .map(SearchHistoryResponse.Builder::build)
      .orElseThrow();
  }

  private UnaryOperator<SearchHistoryResponse.Builder> addPaging() {
    return response -> response.setPaging(result.getPaging());
  }

  private UnaryOperator<SearchHistoryResponse.Builder> addMeasures() {
    Map<String, MetricDto> metricsByUuid = result.getMetrics().stream().collect(Collectors.toMap(MetricDto::getUuid, Function.identity()));
    Map<String, SnapshotDto> analysesByUuid = result.getAnalyses().stream().collect(Collectors.toMap(SnapshotDto::getUuid, Function.identity()));
    Table<MetricDto, SnapshotDto, ProjectMeasureDto> measuresByMetricByAnalysis = HashBasedTable.create(result.getMetrics().size(), result.getAnalyses().size());
    result.getMeasures().forEach(m -> measuresByMetricByAnalysis.put(metricsByUuid.get(m.getMetricUuid()), analysesByUuid.get(m.getAnalysisUuid()), m));

    return response -> {
      for (MetricDto metric : result.getMetrics()) {
        measure.setMetric(metric.getKey());
        addValues(measuresByMetricByAnalysis.row(metric)).apply(metric);
        addMetricToSearchHistoryResponseIncludingRenamedMetric(response, result.getRequestedMetrics(), measure);
        measure.clear();
      }

      return response;
    };
  }

  private UnaryOperator<MetricDto> addValues(Map<SnapshotDto, ProjectMeasureDto> measuresByAnalysis) {
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

  private SnapshotDto addValue(SnapshotDto analysis, MetricDto dbMetric, @Nullable ProjectMeasureDto dbMeasure) {
    if (dbMeasure != null) {
      String measureValue = formatMeasureValue(dbMeasure, dbMetric);
      if (measureValue != null) {
        value.setValue(measureValue);
      }
    }

    return analysis;
  }

  private UnaryOperator<SnapshotDto> clearValue() {
    return analysis -> {
      value.clear();
      return analysis;
    };
  }
}
