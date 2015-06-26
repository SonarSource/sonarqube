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
package org.sonar.server.computation.measure.newcoverage;

import org.sonar.api.measures.CoreMetrics;

public class FileCoverageMetricKeys implements NewCoverageMetricKeys {
  @Override
  public String coverageLineHitsData() {
    return CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY;
  }

  @Override
  public String conditionsByLine() {
    return CoreMetrics.CONDITIONS_BY_LINE_KEY;
  }

  @Override
  public String coveredConditionsByLine() {
    return CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY;
  }

  @Override
  public String newLinesToCover() {
    return CoreMetrics.NEW_LINES_TO_COVER_KEY;
  }

  @Override
  public String newUncoveredLines() {
    return CoreMetrics.NEW_UNCOVERED_LINES_KEY;
  }

  @Override
  public String newConditionsToCover() {
    return CoreMetrics.NEW_CONDITIONS_TO_COVER_KEY;
  }

  @Override
  public String newUncoveredConditions() {
    return CoreMetrics.NEW_UNCOVERED_CONDITIONS_KEY;
  }
}
