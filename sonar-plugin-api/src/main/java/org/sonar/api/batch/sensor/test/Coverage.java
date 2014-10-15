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
package org.sonar.api.batch.sensor.test;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.measures.CoreMetrics;

/**
 * @since 5.0
 */
public interface Coverage {

  public enum CoverageType {
    UNIT(CoreMetrics.LINES_TO_COVER, CoreMetrics.UNCOVERED_LINES, CoreMetrics.COVERAGE_LINE_HITS_DATA, CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.UNCOVERED_CONDITIONS,
      CoreMetrics.CONDITIONS_BY_LINE, CoreMetrics.COVERED_CONDITIONS_BY_LINE),
    INTEGRATION(CoreMetrics.IT_LINES_TO_COVER, CoreMetrics.IT_UNCOVERED_LINES, CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, CoreMetrics.IT_CONDITIONS_TO_COVER,
      CoreMetrics.IT_UNCOVERED_CONDITIONS, CoreMetrics.IT_CONDITIONS_BY_LINE, CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE),
    OVERALL(CoreMetrics.OVERALL_LINES_TO_COVER, CoreMetrics.OVERALL_UNCOVERED_LINES, CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, CoreMetrics.OVERALL_CONDITIONS_TO_COVER,
      CoreMetrics.OVERALL_UNCOVERED_CONDITIONS, CoreMetrics.OVERALL_CONDITIONS_BY_LINE, CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE);

    private Metric<Integer> linesToCover;
    private Metric<Integer> uncoveredLines;
    private Metric<String> lineHitsData;
    private Metric<Integer> conditionsToCover;
    private Metric<Integer> uncoveredConditions;
    private Metric<String> conditionsByLine;
    private Metric<String> coveredConditionsByLine;

    private CoverageType(Metric<Integer> linesToCover, Metric<Integer> uncoveredLines, Metric<String> lineHitsData, Metric<Integer> conditionsToCover,
      Metric<Integer> uncoveredConditions, Metric<String> conditionsByLine, Metric<String> coveredConditionsByLine) {
      this.linesToCover = linesToCover;
      this.uncoveredLines = uncoveredLines;
      this.lineHitsData = lineHitsData;
      this.conditionsToCover = conditionsToCover;
      this.uncoveredConditions = uncoveredConditions;
      this.conditionsByLine = conditionsByLine;
      this.coveredConditionsByLine = coveredConditionsByLine;
    }

    public Metric<Integer> linesToCover() {
      return linesToCover;
    }

    public Metric<Integer> uncoveredLines() {
      return uncoveredLines;
    }

    public Metric<String> lineHitsData() {
      return lineHitsData;
    }

    public Metric<Integer> conditionsToCover() {
      return conditionsToCover;
    }

    public Metric<Integer> uncoveredConditions() {
      return uncoveredConditions;
    }

    public Metric<String> conditionsByLine() {
      return conditionsByLine;
    }

    public Metric<String> coveredConditionsByLine() {
      return coveredConditionsByLine;
    }
  }

  /**
   * The file you are storing coverage on.
   */
  Coverage onFile(InputFile inputFile);

  Coverage ofType(CoverageType type);

  Coverage lineHits(int line, int hits);

  Coverage conditions(int line, int conditions, int coveredConditions);

  /**
   * Call this method only once when your are done with defining the test case coverage.
   */
  void save();

}
