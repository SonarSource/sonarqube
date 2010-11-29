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
package org.sonar.core.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CacheMetricFinder implements MetricFinder {

  private DatabaseSessionFactory sessionFactory;
  private Map<String, Metric> metricsByKey = Maps.newLinkedHashMap();
  private Map<Integer, Metric> metricsById = Maps.newLinkedHashMap();

  public CacheMetricFinder(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public void start() {
    List<Metric> list = sessionFactory.getSession().getResults(Metric.class, "enabled", true);
    for (Metric metric : list) {
      metricsByKey.put(metric.getKey(), metric);
      metricsById.put(metric.getId(), metric);
    }
  }

  public Metric findById(int metricId) {
    return metricsById.get(metricId);
  }

  public Metric findByKey(String key) {
    return metricsByKey.get(key);
  }

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

  public Collection<Metric> findAll() {
    return metricsByKey.values();
  }
}
