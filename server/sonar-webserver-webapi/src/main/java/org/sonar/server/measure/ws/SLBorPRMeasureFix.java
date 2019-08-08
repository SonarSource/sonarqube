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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;

import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;

/**
 * See SONAR-11736
 * This class should be removed in 8.0.
 */
class SLBorPRMeasureFix {
  static final BiMap<String, String> METRICS;

  static {
    METRICS = HashBiMap.create();

    METRICS.put(NEW_VIOLATIONS_KEY, VIOLATIONS_KEY);

    // issue severities
    METRICS.put(NEW_BLOCKER_VIOLATIONS_KEY, BLOCKER_VIOLATIONS_KEY);
    METRICS.put(NEW_CRITICAL_VIOLATIONS_KEY, CRITICAL_VIOLATIONS_KEY);
    METRICS.put(NEW_MAJOR_VIOLATIONS_KEY, MAJOR_VIOLATIONS_KEY);
    METRICS.put(NEW_MINOR_VIOLATIONS_KEY, MINOR_VIOLATIONS_KEY);
    METRICS.put(NEW_INFO_VIOLATIONS_KEY, INFO_VIOLATIONS_KEY);

    // issue types
    METRICS.put(NEW_BUGS_KEY, BUGS_KEY);
    METRICS.put(NEW_CODE_SMELLS_KEY, CODE_SMELLS_KEY);
    METRICS.put(NEW_VULNERABILITIES_KEY, VULNERABILITIES_KEY);

    // ratings
    METRICS.put(NEW_SECURITY_RATING_KEY, SECURITY_RATING_KEY);
    METRICS.put(NEW_RELIABILITY_RATING_KEY, RELIABILITY_RATING_KEY);

    // effort
    METRICS.put(NEW_TECHNICAL_DEBT_KEY, TECHNICAL_DEBT_KEY);
    METRICS.put(NEW_SECURITY_REMEDIATION_EFFORT_KEY, SECURITY_REMEDIATION_EFFORT_KEY);
    METRICS.put(NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, RELIABILITY_REMEDIATION_EFFORT_KEY);
  }

  private SLBorPRMeasureFix() {
    // static only
  }

  static void addReplacementMetricKeys(Collection<String> metricKeys) {
    Set<String> keysToAdd = metricKeys.stream()
      .filter(METRICS::containsKey)
      .map(METRICS::get)
      .collect(Collectors.toSet());
    metricKeys.addAll(keysToAdd);
  }

  static void removeMetricsNotRequested(List<MetricDto> metrics, Set<String> requestedMetricKeys) {
    metrics.removeIf(m -> !requestedMetricKeys.contains(m.getKey()));
  }

  static void createReplacementMeasures(List<MetricDto> metrics, Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric,
    Set<String> requestedMetricKeys) {
    Map<String, MetricDto> metricByKey = Maps.uniqueIndex(metrics, MetricDto::getKey);

    for (MetricDto metric : measuresByComponentUuidAndMetric.columnKeySet()) {
      Map<String, ComponentTreeData.Measure> newEntries = new HashMap<>();

      String originalKey = METRICS.inverse().get(metric.getKey());
      if (originalKey != null && requestedMetricKeys.contains(originalKey)) {
        for (Map.Entry<String, ComponentTreeData.Measure> e : measuresByComponentUuidAndMetric.column(metric).entrySet()) {
          newEntries.put(e.getKey(), copyMeasureToVariation(e.getValue()));
        }

        MetricDto originalMetric = metricByKey.get(originalKey);
        newEntries.forEach((k, v) -> measuresByComponentUuidAndMetric.put(k, originalMetric, v));
      }
    }

    List<MetricDto> toRemove = measuresByComponentUuidAndMetric.columnKeySet().stream().filter(m -> !requestedMetricKeys.contains(m.getKey())).collect(Collectors.toList());
    measuresByComponentUuidAndMetric.columnKeySet().removeAll(toRemove);
  }

  static void createReplacementMeasures(List<MetricDto> metrics, Map<MetricDto, LiveMeasureDto> measuresByMetric, Set<String> requestedMetricKeys) {
    Map<String, MetricDto> metricByKey = Maps.uniqueIndex(metrics, MetricDto::getKey);
    Map<MetricDto, LiveMeasureDto> newEntries = new HashMap<>();

    for (Map.Entry<MetricDto, LiveMeasureDto> e : measuresByMetric.entrySet()) {
      String originalKey = METRICS.inverse().get(e.getKey().getKey());

      if (originalKey != null && requestedMetricKeys.contains(originalKey)) {
        MetricDto metricDto = metricByKey.get(originalKey);
        newEntries.put(metricDto, copyMeasureToVariation(e.getValue(), metricDto.getId()));
      }
    }

    measuresByMetric.entrySet().removeIf(e -> !requestedMetricKeys.contains(e.getKey().getKey()));
    measuresByMetric.putAll(newEntries);
  }

  private static ComponentTreeData.Measure copyMeasureToVariation(ComponentTreeData.Measure measure) {
    return new ComponentTreeData.Measure(null, null, measure.getValue());
  }

  private static LiveMeasureDto copyMeasureToVariation(LiveMeasureDto dto, Integer metricId) {
    LiveMeasureDto copy = new LiveMeasureDto();
    copy.setVariation(dto.getValue());
    copy.setProjectUuid(dto.getProjectUuid());
    copy.setComponentUuid(dto.getComponentUuid());
    copy.setMetricId(metricId);
    return copy;
  }
}
