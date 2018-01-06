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
package org.sonar.api.batch.sensor.coverage;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sonar.api.measures.Metric;

import static java.util.Arrays.asList;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.IT_COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.IT_UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.IT_UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.OVERALL_UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES;

/**
 * Different coverage categories.
 * @since 5.2
 *
 * @deprecated since 6.2 SonarQube merge all coverage reports and don't keep track of different test category
 */
@Deprecated
public enum CoverageType {

  UNIT(LINES_TO_COVER, UNCOVERED_LINES, COVERAGE_LINE_HITS_DATA, CONDITIONS_TO_COVER, UNCOVERED_CONDITIONS, COVERED_CONDITIONS_BY_LINE, CONDITIONS_BY_LINE),
  IT(IT_LINES_TO_COVER, IT_UNCOVERED_LINES, IT_COVERAGE_LINE_HITS_DATA, IT_CONDITIONS_TO_COVER, IT_UNCOVERED_CONDITIONS, IT_COVERED_CONDITIONS_BY_LINE, IT_CONDITIONS_BY_LINE),
  OVERALL(OVERALL_LINES_TO_COVER, OVERALL_UNCOVERED_LINES, OVERALL_COVERAGE_LINE_HITS_DATA, OVERALL_CONDITIONS_TO_COVER, OVERALL_UNCOVERED_CONDITIONS,
    OVERALL_COVERED_CONDITIONS_BY_LINE, OVERALL_CONDITIONS_BY_LINE);

  private final Metric linesToCover;
  private final Metric uncoveredLines;
  private final Metric lineHitsData;
  private final Metric conditionsToCover;
  private final Metric uncoveredConditions;
  private final Metric coveredConditionsByLine;
  private final Metric conditionsByLine;
  private final Set<Metric> all;

  CoverageType(Metric linesToCover, Metric uncoveredLines, Metric lineHitsData, Metric conditionsToCover, Metric uncoveredConditions, Metric coveredConditionsByLine,
    Metric conditionsByLine) {
    this.linesToCover = linesToCover;
    this.uncoveredLines = uncoveredLines;
    this.lineHitsData = lineHitsData;
    this.conditionsToCover = conditionsToCover;
    this.uncoveredConditions = uncoveredConditions;
    this.coveredConditionsByLine = coveredConditionsByLine;
    this.conditionsByLine = conditionsByLine;
    this.all = Collections.unmodifiableSet(new LinkedHashSet<>(
      asList(linesToCover, uncoveredLines, lineHitsData, conditionsToCover, uncoveredConditions, coveredConditionsByLine, conditionsByLine)));
  }

  public Set<Metric> allMetrics() {
    return all;
  }

  public Metric linesToCover() {
    return linesToCover;
  }

  public Metric uncoveredLines() {
    return uncoveredLines;
  }

  public Metric lineHitsData() {
    return lineHitsData;
  }

  public Metric conditionsToCover() {
    return conditionsToCover;
  }

  public Metric uncoveredConditions() {
    return uncoveredConditions;
  }

  public Metric coveredConditionsByLine() {
    return coveredConditionsByLine;
  }

  public Metric conditionsByLine() {
    return conditionsByLine;
  }

}
