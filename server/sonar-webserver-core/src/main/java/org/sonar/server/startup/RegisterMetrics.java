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
package org.sonar.server.startup;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.picocontainer.Startable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.metric.MetricToDto;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;

public class RegisterMetrics implements Startable {

  private static final Logger LOG = Loggers.get(RegisterMetrics.class);

  private final DbClient dbClient;
  private final Metrics[] metricsRepositories;

  public RegisterMetrics(DbClient dbClient, Metrics[] metricsRepositories) {
    this.dbClient = dbClient;
    this.metricsRepositories = metricsRepositories;
  }

  /**
   * Used when no plugin is defining Metrics
   */
  public RegisterMetrics(DbClient dbClient) {
    this(dbClient, new Metrics[] {});
  }

  @Override
  public void start() {
    register(concat(CoreMetrics.getMetrics(), getPluginMetrics()));
  }

  @Override
  public void stop() {
    // nothing to do
  }

  void register(Iterable<Metric> metrics) {
    Profiler profiler = Profiler.create(LOG).startInfo("Register metrics");
    try (DbSession session = dbClient.openSession(false)) {
      save(session, metrics);
      sanitizeQualityGates(session);
      session.commit();
    }
    profiler.stopDebug();
  }

  private void sanitizeQualityGates(DbSession session) {
    dbClient.gateConditionDao().deleteConditionsWithInvalidMetrics(session);
  }

  private void save(DbSession session, Iterable<Metric> metrics) {
    Map<String, MetricDto> basesByKey = new HashMap<>();
    for (MetricDto base : from(dbClient.metricDao().selectAll(session)).toList()) {
      basesByKey.put(base.getKey(), base);
    }

    for (Metric metric : metrics) {
      MetricDto dto = MetricToDto.INSTANCE.apply(metric);
      MetricDto base = basesByKey.get(metric.getKey());
      if (base == null) {
        // new metric, never installed
        dbClient.metricDao().insert(session, dto);
      } else if (!base.isUserManaged()) {
        // existing metric, update changes. Existing custom metrics are kept without applying changes.
        dto.setId(base.getId());
        dbClient.metricDao().update(session, dto);
      }
      basesByKey.remove(metric.getKey());
    }

    for (MetricDto nonUpdatedBase : basesByKey.values()) {
      if (!nonUpdatedBase.isUserManaged() && dbClient.metricDao().disableCustomByKey(session, nonUpdatedBase.getKey())) {
        LOG.info("Disable metric {} [{}]", nonUpdatedBase.getShortName(), nonUpdatedBase.getKey());
      }
    }
  }

  @VisibleForTesting
  List<Metric> getPluginMetrics() {
    List<Metric> metricsToRegister = newArrayList();
    Map<String, Metrics> metricsByRepository = Maps.newHashMap();
    for (Metrics metrics : metricsRepositories) {
      checkMetrics(metricsByRepository, metrics);
      metricsToRegister.addAll(metrics.getMetrics());
    }

    return metricsToRegister;
  }

  private void checkMetrics(Map<String, Metrics> metricsByRepository, Metrics metrics) {
    for (Metric metric : metrics.getMetrics()) {
      String metricKey = metric.getKey();
      if (CoreMetrics.getMetrics().contains(metric)) {
        throw new IllegalStateException(String.format("Metric [%s] is already defined by SonarQube", metricKey));
      }
      Metrics anotherRepository = metricsByRepository.get(metricKey);
      if (anotherRepository != null) {
        throw new IllegalStateException(String.format("Metric [%s] is already defined by the repository [%s]", metricKey, anotherRepository));
      }
      metricsByRepository.put(metricKey, metrics);
    }
  }

}
