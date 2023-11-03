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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Common;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Paging.offset;
import static org.sonar.db.metric.MetricDtoFunctions.isOptimizedForBestValue;
import static org.sonar.server.measure.ws.MetricDtoWithBestValue.isEligibleForBestValue;

public class SearchHistoryResult {
  private final int page;
  private final int pageSize;
  private List<SnapshotDto> analyses;
  private List<MetricDto> metrics;
  private List<MeasureDto> measures;
  private Common.Paging paging;
  private ComponentDto component;
  private List<String> requestedMetrics;

  public SearchHistoryResult(int page, int pageSize) {
    this.page = page;
    this.pageSize = pageSize;
  }

  public ComponentDto getComponent() {
    return requireNonNull(component);
  }

  public SearchHistoryResult setComponent(ComponentDto component) {
    this.component = component;

    return this;
  }

  public List<SnapshotDto> getAnalyses() {
    return requireNonNull(analyses);
  }

  public SearchHistoryResult setAnalyses(List<SnapshotDto> analyses) {
    this.paging = Common.Paging.newBuilder().setPageIndex(page).setPageSize(pageSize).setTotal(analyses.size()).build();
    this.analyses = analyses.stream().skip(offset(page, pageSize)).limit(pageSize).toList();

    return this;
  }

  public List<MetricDto> getMetrics() {
    return requireNonNull(metrics);
  }

  public SearchHistoryResult setMetrics(List<MetricDto> metrics) {
    this.metrics = metrics;
    return this;
  }

  public List<MeasureDto> getMeasures() {
    return requireNonNull(measures);
  }

  public SearchHistoryResult setMeasures(List<MeasureDto> measures) {
    Set<String> analysisUuids = analyses.stream().map(SnapshotDto::getUuid).collect(Collectors.toSet());
    ImmutableList.Builder<MeasureDto> measuresBuilder = ImmutableList.builder();
    List<MeasureDto> filteredMeasures = measures.stream()
      .filter(measure -> analysisUuids.contains(measure.getAnalysisUuid()))
      .toList();
    measuresBuilder.addAll(filteredMeasures);
    measuresBuilder.addAll(computeBestValues(filteredMeasures));

    this.measures = measuresBuilder.build();

    return this;
  }

  /**
   * Conditions for best value measure:
   * <ul>
   * <li>component is a production file or test file</li>
   * <li>metric is optimized for best value</li>
   * </ul>
   */
  private List<MeasureDto> computeBestValues(List<MeasureDto> measures) {
    if (!isEligibleForBestValue().test(component)) {
      return emptyList();
    }

    requireNonNull(metrics);
    requireNonNull(analyses);

    Table<String, String, MeasureDto> measuresByMetricUuidAndAnalysisUuid = HashBasedTable.create(metrics.size(), analyses.size());
    measures.forEach(measure -> measuresByMetricUuidAndAnalysisUuid.put(measure.getMetricUuid(), measure.getAnalysisUuid(), measure));
    List<MeasureDto> bestValues = new ArrayList<>();
    metrics.stream()
      .filter(isOptimizedForBestValue())
      .forEach(metric -> analyses.stream()
        .filter(analysis -> !measuresByMetricUuidAndAnalysisUuid.contains(metric.getUuid(), analysis.getUuid()))
        .map(analysis -> toBestValue(metric, analysis))
        .forEach(bestValues::add));

    return bestValues;
  }

  private static MeasureDto toBestValue(MetricDto metric, SnapshotDto analysis) {
    return new MeasureDto()
      .setMetricUuid(metric.getUuid())
      .setAnalysisUuid(analysis.getUuid())
      .setValue(metric.getBestValue());
  }

  Common.Paging getPaging() {
    return requireNonNull(paging);
  }

  public SearchHistoryResult setRequestedMetrics(List<String> requestedMetrics) {
    this.requestedMetrics = requestedMetrics;
    return this;
  }

  public List<String> getRequestedMetrics() {
    return requestedMetrics;
  }
}
