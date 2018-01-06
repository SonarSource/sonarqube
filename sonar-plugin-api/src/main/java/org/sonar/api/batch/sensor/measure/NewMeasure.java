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
package org.sonar.api.batch.sensor.measure;

import java.io.Serializable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.measure.MetricFinder;

/**
 * Builder to create new Measure.
 * Should not be implemented by client.
 * @since 5.2
 */
public interface NewMeasure<G extends Serializable> {

  /**
   * The {@link InputComponent} the measure belongs to. Mandatory.
   */
  NewMeasure<G> on(InputComponent component);

  /**
   * Set the metric this measure belong to. To find a metric based on its key you can use {@link MetricFinder}.
   */
  NewMeasure<G> forMetric(Metric<G> metric);

  /**
   * Value of the measure.
   */
  NewMeasure<G> withValue(G value);

  /**
   * Save the measure. It is not permitted so save several measures of the same metric on the same file/project.
   */
  void save();

}
