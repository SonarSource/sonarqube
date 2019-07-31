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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.Map;
import java.util.Optional;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;

public interface MeasureRepository {

  /**
   * Retrieves the base measure (ie. the one currently existing in DB) for the specified {@link Component} for
   * the specified {@link MetricImpl} if it exists.
   * <p>
   * This method searches for Measure which are specific to the Component.
   * </p>
   *
   * @throws NullPointerException if either argument is {@code null}
   */
  Optional<Measure> getBaseMeasure(Component component, Metric metric);

  /**
   * Retrieves the measure created during the current analysis for the specified {@link Component} for the specified
   * {@link Metric} if it exists (ie. one created by the Compute Engine or the Scanner).
   */
  Optional<Measure> getRawMeasure(Component component, Metric metric);

  /**
   * Returns the {@link Measure}s for the specified {@link Component} mapped by their metric key.
   */
  Map<String, Measure> getRawMeasures(Component component);

  /**
   * Adds the specified measure for the specified Component and Metric. There can be no more than one measure for a
   * specific combination of Component, Metric.
   *
   * @throws NullPointerException          if any of the arguments is null
   * @throws UnsupportedOperationException when trying to add a measure when one already exists for the specified Component/Metric paar
   */
  void add(Component component, Metric metric, Measure measure);

  /**
   * Updates the specified measure for the specified Component and Metric. There can be no more than one measure for a
   * specific combination of Component, Metric.
   *
   * @throws NullPointerException          if any of the arguments is null
   * @throws UnsupportedOperationException when trying to update a non existing measure
   */
  void update(Component component, Metric metric, Measure measure);
}
