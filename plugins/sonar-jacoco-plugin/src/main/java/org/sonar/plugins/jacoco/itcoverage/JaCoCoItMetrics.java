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
package org.sonar.plugins.jacoco.itcoverage;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.SumChildValuesFormula;

import java.util.Arrays;
import java.util.List;

/**
 * Should be in {@link org.sonar.api.measures.CoreMetrics}
 *
 * @author Evgeny Mandrikov
 */
public final class JaCoCoItMetrics implements Metrics {

  public static final String DOMAIN_IT_TESTS = "Integration Tests";

  public static final String IT_COVERAGE_KEY = "it_coverage";
  public static final Metric IT_COVERAGE = new Metric.Builder(IT_COVERAGE_KEY, "IT Coverage", Metric.ValueType.PERCENT)
      .setDescription("Coverage by integration tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_IT_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  public static final String IT_LINES_TO_COVER_KEY = "it_lines_to_cover";
  public static final Metric IT_LINES_TO_COVER = new Metric.Builder(IT_LINES_TO_COVER_KEY, "IT lines to cover", Metric.ValueType.INT)
      .setDescription("IT lines to cover")
      .setDirection(Metric.DIRECTION_BETTER)
      .setDomain(DOMAIN_IT_TESTS)
      .setQualitative(false)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .create();

  public static final String IT_UNCOVERED_LINES_KEY = "it_uncovered_lines";
  public static final Metric IT_UNCOVERED_LINES = new Metric.Builder(IT_UNCOVERED_LINES_KEY, "IT uncovered lines", Metric.ValueType.INT)
      .setDescription("IT uncovered lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_IT_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String IT_LINE_COVERAGE_KEY = "it_line_coverage";
  public static final Metric IT_LINE_COVERAGE = new Metric.Builder(IT_LINE_COVERAGE_KEY, "IT line coverage", Metric.ValueType.PERCENT)
      .setDescription("IT line coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_IT_TESTS)
      .create();

  public static final String IT_COVERAGE_LINE_HITS_DATA_KEY = "it_coverage_line_hits_data";
  public static final Metric IT_COVERAGE_LINE_HITS_DATA = new Metric.Builder(IT_COVERAGE_LINE_HITS_DATA_KEY, "IT Coverage hits data", Metric.ValueType.DATA)
      .setDescription("IT Code coverage line hits data")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_IT_TESTS)
      .create();

  public static final String IT_CONDITIONS_TO_COVER_KEY = "it_conditions_to_cover";
  public static final Metric IT_CONDITIONS_TO_COVER = new Metric.Builder(IT_CONDITIONS_TO_COVER_KEY, "IT Conditions to cover", Metric.ValueType.INT)
      .setDescription("IT Conditions to cover")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_IT_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .create();

  public static final String IT_UNCOVERED_CONDITIONS_KEY = "it_uncovered_conditions";
  public static final Metric IT_UNCOVERED_CONDITIONS = new Metric.Builder(IT_UNCOVERED_CONDITIONS_KEY, "IT Uncovered conditions", Metric.ValueType.INT)
      .setDescription("IT Uncovered conditions")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_IT_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String IT_BRANCH_COVERAGE_KEY = "it_branch_coverage";
  public static final Metric IT_BRANCH_COVERAGE = new Metric.Builder(IT_BRANCH_COVERAGE_KEY, "IT Branch coverage", Metric.ValueType.PERCENT)
      .setDescription("IT Branch coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_IT_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  public static final String IT_CONDITIONS_BY_LINE_KEY = "it_conditions_by_line";

  public static final Metric IT_CONDITIONS_BY_LINE = new Metric.Builder(IT_CONDITIONS_BY_LINE_KEY, "IT Conditions by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_IT_TESTS)
      .create();

  public static final String IT_COVERED_CONDITIONS_BY_LINE_KEY = "it_covered_conditions_by_line";

  public static final Metric IT_COVERED_CONDITIONS_BY_LINE = new Metric.Builder(IT_COVERED_CONDITIONS_BY_LINE_KEY, "IT Covered conditions by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_IT_TESTS)
      .create();


  public List<Metric> getMetrics() {
    return Arrays.asList(IT_COVERAGE, IT_LINES_TO_COVER, IT_UNCOVERED_LINES, IT_LINE_COVERAGE, IT_COVERAGE_LINE_HITS_DATA,
        IT_CONDITIONS_TO_COVER, IT_UNCOVERED_CONDITIONS, IT_BRANCH_COVERAGE, 
        IT_CONDITIONS_BY_LINE, IT_COVERED_CONDITIONS_BY_LINE);
  }

}
