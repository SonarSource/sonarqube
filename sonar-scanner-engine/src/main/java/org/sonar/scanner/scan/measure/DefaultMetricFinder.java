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
package org.sonar.scanner.scan.measure;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.scanner.repository.MetricsRepository;

@ThreadSafe
public class DefaultMetricFinder implements MetricFinder {

  private Map<String, Metric<Serializable>> metricsByKey;

  public DefaultMetricFinder(MetricsRepository metricsRepository) {
    Map<String, Metric<Serializable>> metrics = new LinkedHashMap<>();
    for (org.sonar.api.measures.Metric metric : metricsRepository.metrics()) {
      metrics.put(metric.key(), new org.sonar.api.measures.Metric.Builder(metric.key(), metric.key(), metric.getType()).create());
    }
    metricsByKey = Collections.unmodifiableMap(metrics);
  }

  @Override
  public Metric<Serializable> findByKey(String key) {
    return metricsByKey.get(key);
  }

  @Override
  public Collection<Metric<Serializable>> findAll(List<String> metricKeys) {
    List<Metric<Serializable>> result = Lists.newLinkedList();
    for (String metricKey : metricKeys) {
      Metric<Serializable> metric = findByKey(metricKey);
      if (metric != null) {
        result.add(metric);
      }
    }
    return result;
  }

  @Override
  public Collection<Metric<Serializable>> findAll() {
    return metricsByKey.values();
  }

}
