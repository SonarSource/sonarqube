/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.jpa.dao;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Metric;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeasuresDao extends BaseDao {

  private final Map<String, Metric> metricsByName = new HashMap<String, Metric>();

  public MeasuresDao(DatabaseSession session) {
    super(session);
  }

  public Metric getMetric(Metric metric) {
    return getMetricsByName().get(metric.getKey());
  }

  public List<Metric> getMetrics(List<Metric> metrics) {
    List<Metric> result = new ArrayList<Metric>();
    for (Metric metric : metrics) {
      result.add(getMetric(metric));
    }
    return result;
  }

  public Metric getMetric(String metricName) {
    return getMetricsByName().get(metricName);
  }

  public Collection<Metric> getMetrics() {
    return getMetricsByName().values();
  }

  public Collection<Metric> getEnabledMetrics() {
    return Collections2.filter(getMetricsByName().values(), new Predicate<Metric>() {
      public boolean apply(@Nullable Metric o) {
        return o != null && o.getEnabled();
      }
    });
  }

  public Collection<Metric> getUserDefinedMetrics() {
    return Collections2.filter(getMetricsByName().values(), new Predicate<Metric>() {
      public boolean apply(@Nullable Metric metric) {
        return metric != null && metric.getEnabled() && metric.getOrigin() != Metric.Origin.JAV;
      }
    });
  }

  public void disableAutomaticMetrics() {
    getSession().createQuery("update " + Metric.class.getSimpleName() + " m set m.enabled=false where m.userManaged=false").executeUpdate();
    getSession().commit();
    metricsByName.clear();
  }

  public void registerMetrics(Collection<Metric> metrics) {
    if (metrics != null) {
      for (Metric metric : metrics) {
        metric.setEnabled(Boolean.TRUE);
        persistMetric(metric);
      }
      getSession().commit();
    }
  }

  public void persistMetric(Metric metric) {
    Metric dbMetric = getMetric(metric);
    if (dbMetric != null) {
      dbMetric.merge(metric);
      getSession().getEntityManager().merge(dbMetric);

    } else {
      getSession().getEntityManager().persist(metric);
    }

    metricsByName.clear();
  }

  public void disabledMetrics(Collection<Metric> metrics) {
    for (Metric metric : metrics) {
      metric.setEnabled(Boolean.FALSE);
      getSession().getEntityManager().persist(metric);
      metricsByName.put(metric.getName(), metric);
    }
  }

  private Map<String, Metric> getMetricsByName() {
    if (metricsByName.isEmpty()) {
      List<Metric> metrics = getSession().getResults(Metric.class);
      for (Metric metric : metrics) {
        metricsByName.put(metric.getKey(), metric);
      }
    }
    return metricsByName;
  }
}
