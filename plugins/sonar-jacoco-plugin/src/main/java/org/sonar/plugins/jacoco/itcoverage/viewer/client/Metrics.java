/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.jacoco.itcoverage.viewer.client;

/**
 * Should be in {@link org.sonar.gwt.Metrics}
 */
public interface Metrics {
  String IT_COVERAGE = "it_coverage";
  String IT_LINES_TO_COVER = "it_lines_to_cover";
  String IT_UNCOVERED_LINES = "it_uncovered_lines";
  String IT_LINE_COVERAGE = "it_line_coverage";
  String IT_COVERAGE_LINE_HITS_DATA = "it_coverage_line_hits_data";
  String IT_CONDITIONS_TO_COVER = "it_conditions_to_cover";
  String IT_UNCOVERED_CONDITIONS = "it_uncovered_conditions";
  String IT_BRANCH_COVERAGE = "it_branch_coverage";
  String IT_CONDITIONS_BY_LINE = "it_conditions_by_line";
  String IT_COVERED_CONDITIONS_BY_LINE = "it_covered_conditions_by_line";

  /**
   * @deprecated use IT_CONDITIONS_BY_LINE and IT_COVERED_CONDITIONS_BY_LINE
   */
  @Deprecated
  String IT_BRANCH_COVERAGE_HITS_DATA = "it_branch_coverage_hits_data";
}
