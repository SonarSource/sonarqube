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
package org.sonar.core.metric;

import com.google.common.collect.Lists;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Collection;
import java.util.List;

public class DefaultMetricFinder implements MetricFinder {

  private static final String ENABLED = "enabled";
  private DatabaseSessionFactory sessionFactory;

  public DefaultMetricFinder(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Metric findById(int id) {
    return sessionFactory.getSession().getSingleResult(Metric.class, "id", id, ENABLED, true);
  }

  @Override
  public Metric findByKey(String key) {
    return sessionFactory.getSession().getSingleResult(Metric.class, "key", key, ENABLED, true);
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
    return doFindAll();
  }

  protected Collection<Metric> doFindAll() {
    return sessionFactory.getSession().getResults(Metric.class, ENABLED, true);
  }

}
