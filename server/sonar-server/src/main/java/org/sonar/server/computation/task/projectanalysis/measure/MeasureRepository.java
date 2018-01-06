/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;
import java.util.Set;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;

public interface MeasureRepository {

  /**
   * Retrieves the base measure (ie. the one currently existing in DB) for the specified {@link Component} for
   * the specified {@link MetricImpl} if it exists.
   * <p>
   * This method searches for Measure which are specific to the Component and not associated to a rule or a
   * characteristic.
   * </p>
   *
   * @throws NullPointerException if either argument is {@code null}
   */
  Optional<Measure> getBaseMeasure(Component component, Metric metric);

  /**
   * Retrieves the measure created during the current analysis for the specified {@link Component} for the specified
   * {@link Metric} if it exists (ie. one created by the Compute Engine or the Batch) and which is <strong>not</strong>
   * associated to a rule, a characteristic, or a developer.
   */
  Optional<Measure> getRawMeasure(Component component, Metric metric);

  /**
   * Returns the {@link Measure}s for the specified {@link Component} and the specified {@link Metric}.
   * <p>
   * Their will be one measure not associated to rules, characteristics or developers, the other ones will be associated to rules or to characteristics
   * (see {@link Measure#equals(Object)}.
   * </p>
   */
  Set<Measure> getRawMeasures(Component component, Metric metric);

  /**
   * Returns the {@link Measure}s for the specified {@link Component} mapped by their metric key.
   * <p>
   * Their can be multiple measures for the same Metric but only one which has no rule nor characteristic, one with a
   * specific ruleId and one with specific characteristicId (see {@link Measure#equals(Object)}.
   * </p>
   */
  SetMultimap<String, Measure> getRawMeasures(Component component);

  /**
   * Adds the specified measure for the specified Component and Metric. There can be no more than one measure for a
   * specific combination of Component, Metric and association to a specific rule or characteristic.
   *
   * @throws NullPointerException if any of the arguments is null
   * @throws UnsupportedOperationException when trying to add a measure when one already exists for the specified Component/Metric paar
   */
  void add(Component component, Metric metric, Measure measure);

  /**
   * Updates the specified measure for the specified Component and Metric. There can be no more than one measure for a
   * specific combination of Component, Metric and association to a specific rule or characteristic.
   *
   * @throws NullPointerException if any of the arguments is null
   * @throws UnsupportedOperationException when trying to update a non existing measure
   */
  void update(Component component, Metric metric, Measure measure);
}
