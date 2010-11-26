/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class DefaultMetricFinder implements MetricFinder {

  private DatabaseSession session;
  private Map<String, Metric> metrics = Maps.newHashMap();

  public DefaultMetricFinder(DatabaseSession session) {
    this.session = session;
  }

  public void start() {
    List<Metric> list = session.getResults(Metric.class, "enabled", true);
    for (Metric metric : list) {
      metrics.put(metric.getKey(), metric);
    }
  }

  public Metric find(String key) {
    return metrics.get(key);
  }

  public Collection<Metric> findAll(List<String> metricKeys) {
    List<Metric> result = Lists.newLinkedList();
    for (String metricKey : metricKeys) {
      Metric metric = find(metricKey);
      if (metric != null) {
        result.add(metric);
      }
    }
    return result;
  }

  public Collection<Metric> findAll() {
    return metrics.values();
  }
}
