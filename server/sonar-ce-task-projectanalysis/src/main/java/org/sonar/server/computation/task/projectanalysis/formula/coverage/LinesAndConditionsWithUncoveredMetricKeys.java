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

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public final class LinesAndConditionsWithUncoveredMetricKeys {
  private final String lines;
  private final String conditions;
  private final String uncoveredLines;
  private final String uncoveredConditions;

  public LinesAndConditionsWithUncoveredMetricKeys(String lines, String conditions, String uncoveredLines, String uncoveredConditions) {
    this.lines = requireNonNull(lines);
    this.conditions = requireNonNull(conditions);
    this.uncoveredLines = requireNonNull(uncoveredLines);
    this.uncoveredConditions = requireNonNull(uncoveredConditions);
  }

  public String getLines() {
    return lines;
  }

  public String getConditions() {
    return conditions;
  }

  public String getUncoveredLines() {
    return uncoveredLines;
  }

  public String getUncoveredConditions() {
    return uncoveredConditions;
  }
}
