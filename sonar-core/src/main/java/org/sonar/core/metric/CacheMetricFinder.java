/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.metric;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.measures.Metric;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CacheMetricFinder extends DefaultMetricFinder {

  private Map<String, Metric> metricsByKey = Maps.newLinkedHashMap();
  private Map<Integer, Metric> metricsById = Maps.newLinkedHashMap();

  public CacheMetricFinder(DatabaseSessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public void start() {
    Collection<Metric> metrics = doFindAll();
    for (Metric metric : metrics) {
      metricsByKey.put(metric.getKey(), metric);
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
