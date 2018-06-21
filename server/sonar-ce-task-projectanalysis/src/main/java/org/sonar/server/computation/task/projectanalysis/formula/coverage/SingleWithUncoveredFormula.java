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
package org.sonar.server.computation.task.projectanalysis.formula.coverage;

import static java.util.Objects.requireNonNull;

/**
 * A Formula which implements the aggregation of a specific measure and its associated "uncovered" measure into a third
 * measure.
 */
public class SingleWithUncoveredFormula extends CoverageFormula<SingleWithUncoveredCounter> {
  private final SingleWithUncoveredMetricKeys inputKeys;
  private final String outputKeys;

  protected SingleWithUncoveredFormula(SingleWithUncoveredMetricKeys inputKeys, String outputKeys) {
    this.inputKeys = requireNonNull(inputKeys);
    this.outputKeys = requireNonNull(outputKeys);
  }

  @Override
  public SingleWithUncoveredCounter createNewCounter() {
    return new SingleWithUncoveredCounter(inputKeys);
  }

  @Override
  public String[] getOutputMetricKeys() {
    return new String[] {outputKeys};
  }
}
