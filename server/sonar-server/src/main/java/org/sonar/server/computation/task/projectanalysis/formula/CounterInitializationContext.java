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
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.period.Period;

/**
 * The context passing information to {@link Counter#initialize(CounterInitializationContext)}.
 */
public interface CounterInitializationContext {

  /**
   * The Component representing the currently processed leaf.
   */
  Component getLeaf();

  /**
   * Retrieve the measure for the current component for the specified metric key if it exists.
   */
  Optional<Measure> getMeasure(String metricKey);

  /**
   * Return Period defined for the current project. It can be used to retrieve variation Measure.
   */
  Period getPeriod();

  /**
   * Finds out whether the a period is definfed or not
   */
  boolean hasPeriod();

}
