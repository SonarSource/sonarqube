/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.formula;

import com.google.common.base.Optional;
import org.sonar.server.computation.component.Component.Type;
import org.sonar.server.computation.measure.Measure;

/**
 * A formula is used to aggregated data on all nodes of a component tree
 */
public interface Formula<COUNTER extends Counter> {

  COUNTER createNewCounter();

  /**
   * This method is used to create a measure on each node, using the value of the counter
   * If {@link Optional#absent()} is returned, no measure will be created
   *
   * @param componentType can be used for instance to not create a measure on {@link Type#FILE}
   */
  Optional<Measure> createMeasure(COUNTER counter, Type componentType);

  /**
   * The metric associated to the measure
   */
  String getOutputMetricKey();

}
