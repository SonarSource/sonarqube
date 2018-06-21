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

import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.period.Period;

/**
 * Context passing information to implementation of {@link Formula#createMeasure(Counter, CreateMeasureContext)} method.
 */
public interface CreateMeasureContext {
  /**
   * The component for which the measure is to be created.
   */
  Component getComponent();

  /**
   * The Metric for which the measure is to be created.
   */
  Metric getMetric();

  /**
   * The period for which variation of the measure can be created.
   */
  Period getPeriod();

  /**
   * Finds out whether the a period is definfed or not
   */
  boolean hasPeriod();
}
