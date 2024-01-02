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
import java.util.Optional;

public interface MetricRepository {

  /**
   * Gets the {@link Metric} with the specific key.
   * <p>Since it does not make sense to encounter a reference (ie. a key) to a Metric during processing of
   * a new analysis and not finding it in DB (metrics are never deleted), this method will throw an
   * IllegalStateException if the metric with the specified key can not be found.</p>
   *
   * @throws IllegalStateException if no Metric with the specified key is found
   * @throws NullPointerException if the specified key is {@code null}
   */
  Metric getByKey(String key);

  /**
   * Gets the {@link Metric} with the specific uuid.
   *
   * @throws IllegalStateException if no Metric with the specified uuid is found
   */
  Metric getByUuid(String uuid);

  /**
   * Gets the {@link Metric} with the specific uuid if it exists in the repository.
   */
  Optional<Metric> getOptionalByUuid(String uuid);

  /**
   * Get iterable of all {@link Metric}.
   */
  Iterable<Metric> getAll();

  /**
   * Returns all the {@link Metric}s for the specific type.
   */
  List<Metric> getMetricsByType(Metric.MetricType type);

}
