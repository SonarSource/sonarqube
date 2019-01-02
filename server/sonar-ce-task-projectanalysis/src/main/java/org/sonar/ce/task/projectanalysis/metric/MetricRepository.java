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
package org.sonar.ce.task.projectanalysis.metric;

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
   * Gets the {@link Metric} with the specific id.
   *
   * @throws IllegalStateException if no Metric with the specified id is found
   */
  Metric getById(long id);

  /**
   * Gets the {@link Metric} with the specific id if it exists in the repository.
   */
  Optional<Metric> getOptionalById(long id);

  /**
   * Get iterable of all {@link Metric}.
   */
  Iterable<Metric> getAll();

}
