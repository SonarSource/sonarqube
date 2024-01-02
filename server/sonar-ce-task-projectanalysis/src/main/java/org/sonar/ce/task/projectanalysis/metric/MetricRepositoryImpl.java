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
package org.sonar.ce.task.projectanalysis.metric;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.Startable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;

import static java.util.Objects.requireNonNull;

public class MetricRepositoryImpl implements MetricRepository, Startable {

  private final DbClient dbClient;
  @CheckForNull
  private Map<String, Metric> metricsByKey;
  @CheckForNull
  private Map<String, Metric> metricsByUuid;

  public MetricRepositoryImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<MetricDto> metricList = dbClient.metricDao().selectEnabled(dbSession);
      this.metricsByKey = metricList.stream().map(MetricDtoToMetric.INSTANCE).collect(Collectors.toMap(Metric::getKey, x -> x));
      this.metricsByUuid = metricList.stream().map(MetricDtoToMetric.INSTANCE).collect(Collectors.toMap(Metric::getUuid, x -> x));
    }
  }

  @Override
  public void stop() {
    // nothing to do when stopping
  }

  @Override
  public Metric getByKey(String key) {
    requireNonNull(key);
    verifyMetricsInitialized();

    Metric res = this.metricsByKey.get(key);
    if (res == null) {
      throw new IllegalStateException(String.format("Metric with key '%s' does not exist", key));
    }
    return res;
  }

  @Override
  public Metric getByUuid(String uuid) {
    return getOptionalByUuid(uuid)
      .orElseThrow(() -> new IllegalStateException(String.format("Metric with uuid '%s' does not exist", uuid)));
  }

  @Override
  public Optional<Metric> getOptionalByUuid(String uuid) {
    verifyMetricsInitialized();

    return Optional.ofNullable(this.metricsByUuid.get(uuid));
  }

  @Override
  public Iterable<Metric> getAll() {
    return metricsByKey.values();
  }

  @Override
  public List<Metric> getMetricsByType(Metric.MetricType type) {
    verifyMetricsInitialized();
    
    return metricsByKey.values().stream().filter(m -> m.getType() == type).toList();
  }

  private void verifyMetricsInitialized() {
    if (this.metricsByKey == null) {
      throw new IllegalStateException("Metric cache has not been initialized");
    }
  }
}
