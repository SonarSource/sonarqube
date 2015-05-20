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
package org.sonar.jpa.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Metric;

public class MeasuresDao {

  private final DatabaseSession session;
  private final Map<String, Metric> metricsByName = new HashMap<>();

  public MeasuresDao(DatabaseSession session) {
    this.session = session;
  }

  public Metric getMetric(String metricName) {
    return getMetricsByName().get(metricName);
  }

  public Collection<Metric> getMetrics() {
    return getMetricsByName().values();
  }

  public Collection<Metric> getEnabledMetrics() {
    return CollectionUtils.select(getMetricsByName().values(), new Predicate() {
      @Override
      public boolean evaluate(Object o) {
        return ((Metric) o).getEnabled();
      }
    });
  }

  public void disableAutomaticMetrics() {
    session.createQuery("update " + Metric.class.getSimpleName() + " m set m.enabled=false where m.userManaged=false").executeUpdate();
    session.commit();
    metricsByName.clear();
  }

  public void registerMetrics(Collection<Metric> metrics) {
    if (metrics != null) {
      for (Metric metric : metrics) {
        metric.setEnabled(Boolean.TRUE);
        persistMetricWithoutClear(metric);
      }
      session.commit();
    }
    metricsByName.clear();
  }

  private void persistMetricWithoutClear(Metric metric) {
    Metric dbMetric = getMetric(metric.getKey());
    if (dbMetric != null) {
      dbMetric.merge(metric);
      session.getEntityManager().merge(dbMetric);

    } else {
      session.getEntityManager().persist(new Metric().merge(metric));
    }
  }

  private Map<String, Metric> getMetricsByName() {
    if (metricsByName.isEmpty()) {
      List<Metric> metrics = session.getResults(Metric.class);
      for (Metric metric : metrics) {
        metricsByName.put(metric.getKey(), metric);
      }
    }
    return metricsByName;
  }

}
