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
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;

public class DumbCreateMeasureContext implements CreateMeasureContext {
  private final Component component;
  private final Metric metric;
  private final PeriodHolder periodHolder;

  public DumbCreateMeasureContext(Component component, Metric metric, PeriodHolder periodHolder) {
    this.component = component;
    this.metric = metric;
    this.periodHolder = periodHolder;
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public Metric getMetric() {
    return metric;
  }

  @Override
  public Period getPeriod() {
    return periodHolder.getPeriod();
  }

  @Override
  public boolean hasPeriod() {
    return periodHolder.hasPeriod();
  }
}
