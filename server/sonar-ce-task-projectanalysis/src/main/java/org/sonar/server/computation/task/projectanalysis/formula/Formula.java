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
package org.sonar.server.computation.task.projectanalysis.formula;

import com.google.common.base.Optional;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

/**
 * A formula is used to aggregated data on all nodes of a component tree
 */
public interface Formula<T extends Counter> {

  /**
   * Method responsible for creating an new instance of a the counter used by this formula.
   */
  T createNewCounter();

  /**
   * This method is used to create a measure on each node, using the value of the counter
   * If {@link Optional#absent()} is returned, no measure will be created
   *
   * @param context the context for which the measure must be created
   */
  Optional<Measure> createMeasure(T counter, CreateMeasureContext context);

  /**
   * The metric associated to the measure
   */
  String[] getOutputMetricKeys();

}
