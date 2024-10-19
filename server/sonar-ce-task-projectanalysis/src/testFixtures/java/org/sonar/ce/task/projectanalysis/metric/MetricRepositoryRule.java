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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class MetricRepositoryRule extends ExternalResource implements MetricRepository, AfterEachCallback {
  private final Map<String, Metric> metricsByKey = new HashMap<>();
  private final Map<String, Metric> metricsByUuid = new HashMap<>();

  /**
   * Convenience method to add a {@link Metric} to the repository created from a {@link org.sonar.api.measures.Metric},
   * most of the time it will be a constant of the {@link org.sonar.api.measures.CoreMetrics} class.
   * <p>
   * For the id of the created metric, this method uses the hashCode of the metric's key. If you want to specify
   * the id of the create {@link Metric}, use {@link #add(String, org.sonar.api.measures.Metric)}
   * </p>
   */
  public MetricRepositoryRule add(org.sonar.api.measures.Metric<?> coreMetric) {
    add(from(coreMetric));
    return this;
  }

  /**
   * Convenience method to add a {@link Metric} to the repository created from a {@link org.sonar.api.measures.Metric}
   * and with the specified id, most of the time it will be a constant of the {@link org.sonar.api.measures.CoreMetrics}
   * class.
   */
  public MetricRepositoryRule add(String uuid, org.sonar.api.measures.Metric<?> coreMetric) {
    add(from(uuid, coreMetric));
    return this;
  }

  private static Metric from(org.sonar.api.measures.Metric<?> coreMetric) {
    return from(Long.toString(coreMetric.getKey().hashCode()), coreMetric);
  }

  private static Metric from(String uuid, org.sonar.api.measures.Metric<?> coreMetric) {
    return new MetricImpl(
      uuid, coreMetric.getKey(), coreMetric.getName(),
      convert(coreMetric.getType()),
      coreMetric.getDecimalScale(),
      coreMetric.getBestValue(), coreMetric.isOptimizedBestValue(), coreMetric.getDeleteHistoricalData());
  }

  private static Metric.MetricType convert(org.sonar.api.measures.Metric.ValueType coreMetricType) {
    return Metric.MetricType.valueOf(coreMetricType.name());
  }

  public MetricRepositoryRule add(Metric metric) {
    requireNonNull(metric.getKey(), "key can not be null");

    checkState(!metricsByKey.containsKey(metric.getKey()), format("Repository already contains a metric for key %s", metric.getKey()));
    checkState(!metricsByUuid.containsKey(metric.getUuid()), format("Repository already contains a metric for id %s", metric.getUuid()));

    metricsByKey.put(metric.getKey(), metric);
    metricsByUuid.put(metric.getUuid(), metric);

    return this;
  }

  @Override
  protected void after() {
    this.metricsByUuid.clear();
    this.metricsByUuid.clear();
  }

  @Override
  public Metric getByKey(String key) {
    Metric res = metricsByKey.get(key);
    checkState(res != null, format("No Metric can be found for key %s", key));
    return res;
  }

  @Override
  public Metric getByUuid(String uuid) {
    Metric res = metricsByUuid.get(uuid);
    checkState(res != null, format("No Metric can be found for uuid %s", uuid));
    return res;
  }

  @Override
  public Optional<Metric> getOptionalByUuid(String uuid) {
    return Optional.of(metricsByUuid.get(uuid));
  }

  @Override
  public Iterable<Metric> getAll() {
    return metricsByKey.values();
  }

  @Override
  public List<Metric> getMetricsByType(Metric.MetricType type) {
    return metricsByKey.values().stream().filter(m -> m.getType() == type).toList();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    after();
  }
}
