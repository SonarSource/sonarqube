/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.measure;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.batch.protocol.input.GlobalRepositories;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultMetricFinder implements MetricFinder {

  private Map<String, Metric> metricsByKey = Maps.newLinkedHashMap();

  public DefaultMetricFinder(GlobalRepositories globalReferentials) {
    for (org.sonar.batch.protocol.input.Metric metric : globalReferentials.metrics()) {
      metricsByKey.put(metric.key(), new org.sonar.api.measures.Metric.Builder(metric.key(), metric.key(), ValueType.valueOf(metric.valueType())).create());
    }
  }

  @Override
  public Metric findByKey(String key) {
    return metricsByKey.get(key);
  }

  @Override
  public Collection<Metric> findAll(List<String> metricKeys) {
    List<Metric> result = Lists.newLinkedList();
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
