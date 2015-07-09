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

import java.util.List;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

public class DumbCreateMeasureContext implements CreateMeasureContext {
  private final Component component;
  private final Metric metric;
  private final PeriodsHolder periodsHolder;

  public DumbCreateMeasureContext(Component component, Metric metric, PeriodsHolder periodsHolder) {
    this.component = component;
    this.metric = metric;
    this.periodsHolder = periodsHolder;
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
  public List<Period> getPeriods() {
    return periodsHolder.getPeriods();
  }
}
