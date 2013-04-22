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
package org.sonar.server.startup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.profiles.Alert;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.jpa.dao.MeasuresDao;

import javax.persistence.Query;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

public class RegisterMetrics {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterMetrics.class);

  private final MeasuresDao measuresDao;
  private final Metrics[] metricsRepositories;
  private final DatabaseSession session;

  public RegisterMetrics(DatabaseSession session, MeasuresDao measuresDao, Metrics[] metricsRepositories) {
    this.session = session;
    this.measuresDao = measuresDao;
    this.metricsRepositories = metricsRepositories;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load metrics");
    measuresDao.disableAutomaticMetrics();

    List<Metric> metricsToRegister = newArrayList();
    metricsToRegister.addAll(CoreMetrics.getMetrics());
    metricsToRegister.addAll(getMetricsRepositories());
    register(metricsToRegister);
    cleanAlerts();
    profiler.stop();
  }

  @VisibleForTesting
  List<Metric> getMetricsRepositories() {
    List<Metric> metricsToRegister = newArrayList();
    Map<String, Metrics> metricsByRepository = Maps.newHashMap();
    if (metricsRepositories != null) {
      for (Metrics metrics : metricsRepositories) {
        checkMetrics(metricsByRepository, metrics);
        metricsToRegister.addAll(removeExistingUserManagedMetrics(metrics.getMetrics()));
      }
    }
    return metricsToRegister;
  }

  private List<Metric> removeExistingUserManagedMetrics(List<Metric> metrics) {
    return newArrayList(Iterables.filter(metrics, new Predicate<Metric>() {
      @Override
      public boolean apply(Metric metric) {
        // It should be better to use the template mechanism (as it's done in #RegisterNewDashboards to register provided user manager metrics
        return !metric.getUserManaged() || measuresDao.getMetric(metric) == null;
      }
    }));
  }

  private void checkMetrics(Map<String, Metrics> metricsByRepository, Metrics metrics) {
    for (Metric metric : metrics.getMetrics()) {
      String metricKey = metric.getKey();
      if (CoreMetrics.getMetrics().contains(metric)) {
        throw new IllegalStateException("The following metric is already defined in sonar: " + metricKey);
      }
      Metrics anotherRepository = metricsByRepository.get(metricKey);
      if (anotherRepository != null) {
        throw new IllegalStateException("The metric '" + metricKey + "' is already defined in the extension: " + anotherRepository);
      }
      metricsByRepository.put(metricKey, metrics);
    }
  }

  protected void cleanAlerts() {
    LOG.info("cleaning alert thresholds...");
    Query query = session.createQuery("delete from " + Alert.class.getSimpleName() + " a where NOT EXISTS(FROM Metric m WHERE m=a.metric))");
    query.executeUpdate();

    query = session.createQuery("delete from " + Alert.class.getSimpleName() + " a where NOT EXISTS(FROM Metric m WHERE m=a.metric and m.enabled=:enabled))");
    query.setParameter("enabled", Boolean.TRUE);
    query.executeUpdate();
    session.commit();
  }

  protected void register(List<Metric> metrics) {
    measuresDao.registerMetrics(metrics);
  }
}
