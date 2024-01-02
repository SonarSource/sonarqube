/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.Startable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.metric.MetricToDto;

import static com.google.common.collect.FluentIterable.concat;
import static com.google.common.collect.Lists.newArrayList;
import org.springframework.beans.factory.annotation.Autowired;

public class RegisterMetrics implements Startable {

  private static final Logger LOG = Loggers.get(RegisterMetrics.class);

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final Metrics[] metricsRepositories;

  @Autowired(required = false)
  public RegisterMetrics(DbClient dbClient, UuidFactory uuidFactory, Metrics[] metricsRepositories) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.metricsRepositories = metricsRepositories;
  }

  /**
   * Used when no plugin is defining Metrics
   */
  @Autowired(required = false)
  public RegisterMetrics(DbClient dbClient, UuidFactory uuidFactory) {
    this(dbClient, uuidFactory, new Metrics[] {});
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
    try (DbSession session = dbClient.openSession(true)) {
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
    var allMetrics = dbClient.metricDao().selectAll(session);
    for (MetricDto base : allMetrics) {
      basesByKey.put(base.getKey(), base);
    }

    for (Metric metric : metrics) {
      MetricDto dto = MetricToDto.INSTANCE.apply(metric);
      MetricDto base = basesByKey.get(metric.getKey());
      if (base == null) {
        // new metric, never installed
        dto.setUuid(uuidFactory.create());
        dbClient.metricDao().insert(session, dto);
      } else {
        dto.setUuid(base.getUuid());
        dbClient.metricDao().update(session, dto);
      }
      basesByKey.remove(metric.getKey());
    }

    for (MetricDto nonUpdatedBase : basesByKey.values()) {
      if (dbClient.metricDao().disableByKey(session, nonUpdatedBase.getKey())) {
        LOG.info("Disable metric {} [{}]", nonUpdatedBase.getShortName(), nonUpdatedBase.getKey());
      }
    }
  }

  @VisibleForTesting
  List<Metric> getPluginMetrics() {
    List<Metric> metricsToRegister = newArrayList();
    Map<String, Metrics> metricsByRepository = new HashMap<>();
    for (Metrics metrics : metricsRepositories) {
      checkMetrics(metricsByRepository, metrics);
      metricsToRegister.addAll(metrics.getMetrics());
    }

    return metricsToRegister;
  }

  private static void checkMetrics(Map<String, Metrics> metricsByRepository, Metrics metrics) {
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
