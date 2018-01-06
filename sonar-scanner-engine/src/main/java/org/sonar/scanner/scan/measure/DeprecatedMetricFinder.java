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
package org.sonar.scanner.scan.measure;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.scanner.repository.MetricsRepository;

public final class DeprecatedMetricFinder implements MetricFinder {

  private Map<String, Metric> metricsByKey = new LinkedHashMap<>();
  private Map<Integer, Metric> metricsById = new LinkedHashMap<>();

  public DeprecatedMetricFinder(MetricsRepository metricsRepository) {
    for (Metric metric : metricsRepository.metrics()) {
      metricsByKey.put(metric.key(), metric);
      metricsById.put(metric.getId(), metric);
    }
  }

  @Override
  public Metric findById(int metricId) {
    return metricsById.get(metricId);
  }

  @Override
  public Metric findByKey(String key) {
    return metricsByKey.get(key);
  }

  @Override
  public Collection<Metric> findAll(List<String> metricKeys) {
    List<Metric> result = new LinkedList<>();
    for (String metricKey : metricKeys) {
      Metric metric = findByKey(metricKey);
      if (metric != null) {
        result.add(metric);
      }
    }
    return result;
  }

  @Override
  public Collection<Metric> findAll() {
    return metricsByKey.values();
  }
}
