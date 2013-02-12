/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.measures;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.SonarException;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @since 1.10
 */
public final class CoreMetrics {

  private CoreMetrics() {
    // only static stuff
  }

  public static final String DOMAIN_SIZE = "Size";
  public static final String DOMAIN_TESTS = "Tests";
  public static final String DOMAIN_INTEGRATION_TESTS = "Tests (Integration)";
  public static final String DOMAIN_SYSTEM_TESTS = "Tests (System)";
  public static final String DOMAIN_OVERALL_TESTS = "Tests (Overall)";
  public static final String DOMAIN_COMPLEXITY = "Complexity";
  public static final String DOMAIN_DOCUMENTATION = "Documentation";
  public static final String DOMAIN_RULES = "Rules";
  public static final String DOMAIN_SCM = "SCM";
  public static final String DOMAIN_REVIEWS = "Reviews";

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final String DOMAIN_RULE_CATEGORIES = "Rule categories";

  public static final String DOMAIN_GENERAL = "General";
  public static final String DOMAIN_DUPLICATION = "Duplication";
  public static final String DOMAIN_DESIGN = "Design";

  public static final String LINES_KEY = "lines";
  public static final Metric LINES = new Metric.Builder(LINES_KEY, "Lines", Metric.ValueType.INT)
      .setDescription("Lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String GENERATED_LINES_KEY = "generated_lines";
  public static final Metric GENERATED_LINES = new Metric.Builder(GENERATED_LINES_KEY, "Generated Lines", Metric.ValueType.INT)
      .setDescription("Number of generated lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String NCLOC_KEY = "ncloc";
  public static final Metric NCLOC = new Metric.Builder(NCLOC_KEY, "Lines of code", Metric.ValueType.INT)
      .setDescription("Non Commenting Lines of Code")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String GENERATED_NCLOC_KEY = "generated_ncloc";
  public static final Metric GENERATED_NCLOC = new Metric.Builder(GENERATED_NCLOC_KEY, "Generated lines of code", Metric.ValueType.INT)
      .setDescription("Generated non Commenting Lines of Code")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String CLASSES_KEY = "classes";
  public static final Metric CLASSES = new Metric.Builder(CLASSES_KEY, "Classes", Metric.ValueType.INT)
      .setDescription("Classes")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String FILES_KEY = "files";
  public static final Metric FILES = new Metric.Builder(FILES_KEY, "Files", Metric.ValueType.INT)
      .setDescription("Number of files")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .create();

  public static final String DIRECTORIES_KEY = "directories";
  public static final Metric DIRECTORIES = new Metric.Builder(DIRECTORIES_KEY, "Directories", Metric.ValueType.INT)
      .setDescription("Directories")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .create();

  public static final String PACKAGES_KEY = "packages";
  public static final Metric PACKAGES = new Metric.Builder(PACKAGES_KEY, "Packages", Metric.ValueType.INT)
      .setDescription("Packages")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String FUNCTIONS_KEY = "functions";
  public static final Metric FUNCTIONS = new Metric.Builder(FUNCTIONS_KEY, "Methods", Metric.ValueType.INT)
      .setDescription("Methods")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String ACCESSORS_KEY = "accessors";
  public static final Metric ACCESSORS = new Metric.Builder(ACCESSORS_KEY, "Accessors", Metric.ValueType.INT)
      .setDescription("Accessors")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String STATEMENTS_KEY = "statements";
  public static final Metric STATEMENTS = new Metric.Builder(STATEMENTS_KEY, "Statements", Metric.ValueType.INT)
      .setDescription("Number of statements")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String PUBLIC_API_KEY = "public_api";
  public static final Metric PUBLIC_API = new Metric.Builder(PUBLIC_API_KEY, "Public API", Metric.ValueType.INT)
      .setDescription("Public API")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 3.0
   */
  public static final String PROJECTS_KEY = "projects";

  /**
   * @since 3.0
   */
  public static final Metric PROJECTS = new Metric.Builder(PROJECTS_KEY, "Projects", Metric.ValueType.INT)
      .setDescription("Number of projects")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SIZE)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // DOCUMENTATION
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String COMMENT_LINES_KEY = "comment_lines";
  public static final Metric COMMENT_LINES = new Metric.Builder(COMMENT_LINES_KEY, "Comment lines", Metric.ValueType.INT)
      .setDescription("Number of comment lines")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_DOCUMENTATION)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String COMMENT_LINES_DENSITY_KEY = "comment_lines_density";
  public static final Metric COMMENT_LINES_DENSITY = new Metric.Builder(COMMENT_LINES_DENSITY_KEY, "Comments (%)", Metric.ValueType.PERCENT)
      .setDescription("Comments balanced by ncloc + comment lines")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_DOCUMENTATION)
      .create();

  /**
   * @deprecated since 3.3 - see SONAR-3768
   */
  @Deprecated
  public static final String COMMENT_BLANK_LINES_KEY = "comment_blank_lines";

  /**
   * @deprecated since 3.3 - see SONAR-3768
   */
  @Deprecated
  public static final Metric COMMENT_BLANK_LINES = new Metric.Builder(COMMENT_BLANK_LINES_KEY, "Blank comments", Metric.ValueType.INT)
      .setDescription("Comments that do not contain comments")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DOCUMENTATION)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String PUBLIC_DOCUMENTED_API_DENSITY_KEY = "public_documented_api_density";
  public static final Metric PUBLIC_DOCUMENTED_API_DENSITY = new Metric.Builder(PUBLIC_DOCUMENTED_API_DENSITY_KEY, "Public documented API (%)", Metric.ValueType.PERCENT)
      .setDescription("Public documented classes and methods balanced by ncloc")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_DOCUMENTATION)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String PUBLIC_UNDOCUMENTED_API_KEY = "public_undocumented_api";
  public static final Metric PUBLIC_UNDOCUMENTED_API = new Metric.Builder(PUBLIC_UNDOCUMENTED_API_KEY, "Public undocumented API", Metric.ValueType.INT)
      .setDescription("Public undocumented classes, methods and variables")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DOCUMENTATION)
      .setBestValue(0.0)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String COMMENTED_OUT_CODE_LINES_KEY = "commented_out_code_lines";
  public static final Metric COMMENTED_OUT_CODE_LINES = new Metric.Builder(COMMENTED_OUT_CODE_LINES_KEY, "Commented-out LOC", Metric.ValueType.INT)
      .setDescription("Commented lines of code")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DOCUMENTATION)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // COMPLEXITY
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String COMPLEXITY_KEY = "complexity";
  public static final Metric COMPLEXITY = new Metric.Builder(COMPLEXITY_KEY, "Complexity", Metric.ValueType.INT)
      .setDescription("Cyclomatic complexity")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String CLASS_COMPLEXITY_KEY = "class_complexity";
  public static final Metric CLASS_COMPLEXITY = new Metric.Builder(CLASS_COMPLEXITY_KEY, "Complexity /class", Metric.ValueType.FLOAT)
      .setDescription("Complexity average by class")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.CLASSES))
      .create();

  public static final String FUNCTION_COMPLEXITY_KEY = "function_complexity";
  public static final Metric FUNCTION_COMPLEXITY = new Metric.Builder(FUNCTION_COMPLEXITY_KEY, "Complexity /method", Metric.ValueType.FLOAT)
      .setDescription("Complexity average by method")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FUNCTIONS))
      .create();

  public static final String FILE_COMPLEXITY_KEY = "file_complexity";
  public static final Metric FILE_COMPLEXITY = new Metric.Builder(FILE_COMPLEXITY_KEY, "Complexity /file", Metric.ValueType.FLOAT)
      .setDescription("Complexity average by file")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(AverageFormula.create(CoreMetrics.COMPLEXITY, CoreMetrics.FILES))
      .create();

  /**
   * @deprecated in 3.0 - see SONAR-3289
   */
  @Deprecated
  public static final String CLASS_COMPLEXITY_DISTRIBUTION_KEY = "class_complexity_distribution";

  /**
   * @deprecated in 3.0 - see SONAR-3289
   */
  @Deprecated
  public static final Metric CLASS_COMPLEXITY_DISTRIBUTION = new Metric.Builder(CLASS_COMPLEXITY_DISTRIBUTION_KEY, "Classes distribution /complexity", Metric.ValueType.DISTRIB)
      .setDescription("Classes distribution /complexity")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY))
      .create();

  public static final String FUNCTION_COMPLEXITY_DISTRIBUTION_KEY = "function_complexity_distribution";
  public static final Metric FUNCTION_COMPLEXITY_DISTRIBUTION = new Metric.Builder(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "Functions distribution /complexity",
      Metric.ValueType.DISTRIB)
      .setDescription("Functions distribution /complexity")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY))
      .create();

  public static final String FILE_COMPLEXITY_DISTRIBUTION_KEY = "file_complexity_distribution";
  public static final Metric FILE_COMPLEXITY_DISTRIBUTION = new Metric.Builder(FILE_COMPLEXITY_DISTRIBUTION_KEY, "Files distribution /complexity", Metric.ValueType.DISTRIB)
      .setDescription("Files distribution /complexity")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY))
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // UNIT TESTS
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String TESTS_KEY = "tests";

  /**
   * Value of measure for this metric can be saved from Sensor, taking into account following rules:
   * <ul>
   * <li>If tool (like Maven Surefire Plugin) has not been activated to run unit tests, then Sensor should not save anything. For example there is no such tool for COBOL.</li>
   * <li>If tool has been activated, but there was no unit tests to run, then zero value should be saved for project.</li>
   * <li>Non-zero value should be saved for resources representing tests. And Sonar provides default Decorator, which will decorate parent resources.</li>
   * <li>Should include {@link #TEST_FAILURES} and {@link #TEST_ERRORS}, but should not include {@link #SKIPPED_TESTS}.</li>
   * </ul>
   */
  public static final Metric TESTS = new Metric.Builder(TESTS_KEY, "Unit tests", Metric.ValueType.INT)
      .setDescription("Number of unit tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_TESTS)
      .create();

  public static final String TEST_EXECUTION_TIME_KEY = "test_execution_time";
  public static final Metric TEST_EXECUTION_TIME = new Metric.Builder(TEST_EXECUTION_TIME_KEY, "Unit tests duration", Metric.ValueType.MILLISEC)
      .setDescription("Execution duration of unit tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_TESTS)
      .create();

  public static final String TEST_ERRORS_KEY = "test_errors";
  public static final Metric TEST_ERRORS = new Metric.Builder(TEST_ERRORS_KEY, "Unit test errors", Metric.ValueType.INT)
      .setDescription("Number of unit test errors")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String SKIPPED_TESTS_KEY = "skipped_tests";
  public static final Metric SKIPPED_TESTS = new Metric.Builder(SKIPPED_TESTS_KEY, "Skipped unit tests", Metric.ValueType.INT)
      .setDescription("Number of skipped unit tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String TEST_FAILURES_KEY = "test_failures";
  public static final Metric TEST_FAILURES = new Metric.Builder(TEST_FAILURES_KEY, "Unit test failures", Metric.ValueType.INT)
      .setDescription("Number of unit test failures")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String TEST_SUCCESS_DENSITY_KEY = "test_success_density";
  public static final Metric TEST_SUCCESS_DENSITY = new Metric.Builder(TEST_SUCCESS_DENSITY_KEY, "Unit test success (%)", Metric.ValueType.PERCENT)
      .setDescription("Density of successful unit tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String TEST_DATA_KEY = "test_data";
  public static final Metric TEST_DATA = new Metric.Builder(TEST_DATA_KEY, "Unit tests details", Metric.ValueType.DATA)
      .setDescription("Unit tests details")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_TESTS)
      .create();

  public static final String COVERAGE_KEY = "coverage";
  public static final Metric COVERAGE = new Metric.Builder(COVERAGE_KEY, "Coverage", Metric.ValueType.PERCENT)
      .setDescription("Coverage by unit tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  public static final String NEW_COVERAGE_KEY = "new_coverage";
  public static final Metric NEW_COVERAGE = new Metric.Builder(NEW_COVERAGE_KEY, "New coverage", Metric.ValueType.PERCENT)
      .setDescription("Coverage of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  public static final String LINES_TO_COVER_KEY = "lines_to_cover";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric LINES_TO_COVER = new Metric.Builder(LINES_TO_COVER_KEY, "Lines to cover", Metric.ValueType.INT)
      .setDescription("Lines to cover")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String NEW_LINES_TO_COVER_KEY = "new_lines_to_cover";
  public static final Metric NEW_LINES_TO_COVER = new Metric.Builder(NEW_LINES_TO_COVER_KEY, "New lines to cover", Metric.ValueType.INT)
      .setDescription("New lines to cover")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  public static final String UNCOVERED_LINES_KEY = "uncovered_lines";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric UNCOVERED_LINES = new Metric.Builder(UNCOVERED_LINES_KEY, "Uncovered lines", Metric.ValueType.INT)
      .setDescription("Uncovered lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .create();

  public static final String NEW_UNCOVERED_LINES_KEY = "new_uncovered_lines";
  public static final Metric NEW_UNCOVERED_LINES = new Metric.Builder(NEW_UNCOVERED_LINES_KEY, "New uncovered lines", Metric.ValueType.INT)
      .setDescription("New uncovered lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  public static final String LINE_COVERAGE_KEY = "line_coverage";
  public static final Metric LINE_COVERAGE = new Metric.Builder(LINE_COVERAGE_KEY, "Line coverage", Metric.ValueType.PERCENT)
      .setDescription("Line coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  public static final String NEW_LINE_COVERAGE_KEY = "new_line_coverage";
  public static final Metric NEW_LINE_COVERAGE = new Metric.Builder(NEW_LINE_COVERAGE_KEY, "New line coverage", Metric.ValueType.PERCENT)
      .setDescription("Line coverage of added/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDomain(DOMAIN_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  public static final String COVERAGE_LINE_HITS_DATA_KEY = "coverage_line_hits_data";

  /**
   * Key-value pairs, where key - is a number of line, and value - is a number of hits for this line.
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric COVERAGE_LINE_HITS_DATA = new Metric.Builder(COVERAGE_LINE_HITS_DATA_KEY, "Coverage hits by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  public static final String CONDITIONS_TO_COVER_KEY = "conditions_to_cover";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric CONDITIONS_TO_COVER = new Metric.Builder(CONDITIONS_TO_COVER_KEY, "Conditions to cover", Metric.ValueType.INT)
      .setDescription("Conditions to cover")
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .create();

  public static final String NEW_CONDITIONS_TO_COVER_KEY = "new_conditions_to_cover";
  public static final Metric NEW_CONDITIONS_TO_COVER = new Metric.Builder(NEW_CONDITIONS_TO_COVER_KEY, "New conditions to cover", Metric.ValueType.INT)
      .setDescription("New conditions to cover")
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  public static final String UNCOVERED_CONDITIONS_KEY = "uncovered_conditions";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric UNCOVERED_CONDITIONS = new Metric.Builder(UNCOVERED_CONDITIONS_KEY, "Uncovered conditions", Metric.ValueType.INT)
      .setDescription("Uncovered conditions")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .create();

  public static final String NEW_UNCOVERED_CONDITIONS_KEY = "new_uncovered_conditions";
  public static final Metric NEW_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_UNCOVERED_CONDITIONS_KEY, "New uncovered conditions", Metric.ValueType.INT)
      .setDescription("New uncovered conditions")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  public static final String BRANCH_COVERAGE_KEY = "branch_coverage";
  public static final Metric BRANCH_COVERAGE = new Metric.Builder(BRANCH_COVERAGE_KEY, "Branch coverage", Metric.ValueType.PERCENT)
      .setDescription("Branch coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  public static final String NEW_BRANCH_COVERAGE_KEY = "new_branch_coverage";
  public static final Metric NEW_BRANCH_COVERAGE = new Metric.Builder(NEW_BRANCH_COVERAGE_KEY, "New branch coverage", Metric.ValueType.PERCENT)
      .setDescription("Branch coverage of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @deprecated in 2.7. Replaced by {@link #CONDITIONS_BY_LINE_KEY} and {@link #COVERED_CONDITIONS_BY_LINE_KEY}
   */
  @Deprecated
  public static final String BRANCH_COVERAGE_HITS_DATA_KEY = "branch_coverage_hits_data";

  /**
   * @deprecated in 2.7. Replaced by metrics {@link #CONDITIONS_BY_LINE} and {@link #COVERED_CONDITIONS_BY_LINE}
   */
  @Deprecated
  public static final Metric BRANCH_COVERAGE_HITS_DATA = new Metric.Builder(BRANCH_COVERAGE_HITS_DATA_KEY, "Branch coverage hits", Metric.ValueType.DATA)
      .setDomain(DOMAIN_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  public static final String CONDITIONS_BY_LINE_KEY = "conditions_by_line";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   *
   * @since 2.7
   */
  public static final Metric CONDITIONS_BY_LINE = new Metric.Builder(CONDITIONS_BY_LINE_KEY, "Conditions by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  public static final String COVERED_CONDITIONS_BY_LINE_KEY = "covered_conditions_by_line";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   *
   * @since 2.7
   */
  public static final Metric COVERED_CONDITIONS_BY_LINE = new Metric.Builder(COVERED_CONDITIONS_BY_LINE_KEY, "Covered conditions by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // INTEGRATION TESTS
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.12
   */
  public static final String IT_COVERAGE_KEY = "it_coverage";

  /**
   * @since 2.12
   */
  public static final Metric IT_COVERAGE = new Metric.Builder(IT_COVERAGE_KEY, "IT coverage", Metric.ValueType.PERCENT)
      .setDescription("Coverage by integration tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_COVERAGE_KEY = "new_it_coverage";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_COVERAGE = new Metric.Builder(NEW_IT_COVERAGE_KEY, "New coverage by IT", Metric.ValueType.PERCENT)
      .setDescription("Integration Tests Coverage of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_LINES_TO_COVER_KEY = "it_lines_to_cover";

  /**
   * @since 2.12
   */
  public static final Metric IT_LINES_TO_COVER = new Metric.Builder(IT_LINES_TO_COVER_KEY, "IT lines to cover", Metric.ValueType.INT)
      .setDescription("Lines to cover by Integration Tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setQualitative(false)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_LINES_TO_COVER_KEY = "new_it_lines_to_cover";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_LINES_TO_COVER = new Metric.Builder(NEW_IT_LINES_TO_COVER_KEY, "New lines to cover by IT", Metric.ValueType.INT)
      .setDescription("New lines to cover by Integration Tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_UNCOVERED_LINES_KEY = "it_uncovered_lines";

  /**
   * @since 2.12
   */
  public static final Metric IT_UNCOVERED_LINES = new Metric.Builder(IT_UNCOVERED_LINES_KEY, "IT uncovered lines", Metric.ValueType.INT)
      .setDescription("IT uncovered lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_UNCOVERED_LINES_KEY = "new_it_uncovered_lines";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_UNCOVERED_LINES = new Metric.Builder(NEW_IT_UNCOVERED_LINES_KEY, "New uncovered lines by IT", Metric.ValueType.INT)
      .setDescription("New uncovered lines by Integration Tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_LINE_COVERAGE_KEY = "it_line_coverage";

  /**
   * @since 2.12
   */
  public static final Metric IT_LINE_COVERAGE = new Metric.Builder(IT_LINE_COVERAGE_KEY, "IT line coverage", Metric.ValueType.PERCENT)
      .setDescription("IT line coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_LINE_COVERAGE_KEY = "new_it_line_coverage";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_LINE_COVERAGE = new Metric.Builder(NEW_IT_LINE_COVERAGE_KEY, "New line coverage by IT", Metric.ValueType.PERCENT)
      .setDescription("Line Coverage by Integration Tests of added/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_COVERAGE_LINE_HITS_DATA_KEY = "it_coverage_line_hits_data";

  /**
   * @since 2.12
   */
  public static final Metric IT_COVERAGE_LINE_HITS_DATA = new Metric.Builder(IT_COVERAGE_LINE_HITS_DATA_KEY, "IT coverage hits data", Metric.ValueType.DATA)
      .setDescription("IT Code coverage line hits data")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_CONDITIONS_TO_COVER_KEY = "it_conditions_to_cover";

  /**
   * @since 2.12
   */
  public static final Metric IT_CONDITIONS_TO_COVER = new Metric.Builder(IT_CONDITIONS_TO_COVER_KEY, "IT branches to cover", Metric.ValueType.INT)
      .setDescription("IT Conditions to cover")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_CONDITIONS_TO_COVER_KEY = "new_it_conditions_to_cover";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_CONDITIONS_TO_COVER = new Metric.Builder(NEW_IT_CONDITIONS_TO_COVER_KEY, "New conditions to cover by IT", Metric.ValueType.INT)
      .setDescription("New conditions to cover by Integration Tests")
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_UNCOVERED_CONDITIONS_KEY = "it_uncovered_conditions";

  /**
   * @since 2.12
   */
  public static final Metric IT_UNCOVERED_CONDITIONS = new Metric.Builder(IT_UNCOVERED_CONDITIONS_KEY, "IT uncovered branches", Metric.ValueType.INT)
      .setDescription("IT Uncovered conditions")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_UNCOVERED_CONDITIONS_KEY = "new_it_uncovered_conditions";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_IT_UNCOVERED_CONDITIONS_KEY, "New uncovered conditions by IT", Metric.ValueType.INT)
      .setDescription("New uncovered conditions by Integration Tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_BRANCH_COVERAGE_KEY = "it_branch_coverage";

  /**
   * @since 2.12
   */
  public static final Metric IT_BRANCH_COVERAGE = new Metric.Builder(IT_BRANCH_COVERAGE_KEY, "IT branch coverage", Metric.ValueType.PERCENT)
      .setDescription("IT Branch coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  /**
   * @since 2.12
   */
  public static final String NEW_IT_BRANCH_COVERAGE_KEY = "new_it_branch_coverage";

  /**
   * @since 2.12
   */
  public static final Metric NEW_IT_BRANCH_COVERAGE = new Metric.Builder(NEW_IT_BRANCH_COVERAGE_KEY, "New branch coverage by IT", Metric.ValueType.PERCENT)
      .setDescription("Branch coverage by Integration Tests of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_CONDITIONS_BY_LINE_KEY = "it_conditions_by_line";

  /**
   * @since 2.12
   */
  public static final Metric IT_CONDITIONS_BY_LINE = new Metric.Builder(IT_CONDITIONS_BY_LINE_KEY, "IT branches by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.12
   */
  public static final String IT_COVERED_CONDITIONS_BY_LINE_KEY = "it_covered_conditions_by_line";

  /**
   * @since 2.12
   */
  public static final Metric IT_COVERED_CONDITIONS_BY_LINE = new Metric.Builder(IT_COVERED_CONDITIONS_BY_LINE_KEY, "IT covered branches by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_INTEGRATION_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // SYSTEM TESTS
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 3.6
   */
  public static final String SYSTEM_COVERAGE_KEY = "system_coverage";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_COVERAGE = new Metric.Builder(SYSTEM_COVERAGE_KEY, "System coverage", Metric.ValueType.PERCENT)
      .setDescription("Coverage by system tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_COVERAGE_KEY = "new_system_coverage";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_COVERAGE = new Metric.Builder(NEW_SYSTEM_COVERAGE_KEY, "New coverage by System", Metric.ValueType.PERCENT)
      .setDescription("System Tests Coverage of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_LINES_TO_COVER_KEY = "system_lines_to_cover";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_LINES_TO_COVER = new Metric.Builder(SYSTEM_LINES_TO_COVER_KEY, "System lines to cover", Metric.ValueType.INT)
      .setDescription("Lines to cover by System Tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setQualitative(false)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_LINES_TO_COVER_KEY = "new_system_lines_to_cover";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_LINES_TO_COVER = new Metric.Builder(NEW_SYSTEM_LINES_TO_COVER_KEY, "New lines to cover by System", Metric.ValueType.INT)
      .setDescription("New lines to cover by System Tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_UNCOVERED_LINES_KEY = "system_uncovered_lines";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_UNCOVERED_LINES = new Metric.Builder(SYSTEM_UNCOVERED_LINES_KEY, "System uncovered lines", Metric.ValueType.INT)
      .setDescription("System uncovered lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_UNCOVERED_LINES_KEY = "new_system_uncovered_lines";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_UNCOVERED_LINES = new Metric.Builder(NEW_SYSTEM_UNCOVERED_LINES_KEY, "New uncovered lines by System", Metric.ValueType.INT)
      .setDescription("New uncovered lines by System Tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_LINE_COVERAGE_KEY = "system_line_coverage";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_LINE_COVERAGE = new Metric.Builder(SYSTEM_LINE_COVERAGE_KEY, "System line coverage", Metric.ValueType.PERCENT)
      .setDescription("System line coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_LINE_COVERAGE_KEY = "new_system_line_coverage";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_LINE_COVERAGE = new Metric.Builder(NEW_SYSTEM_LINE_COVERAGE_KEY, "New line coverage by System", Metric.ValueType.PERCENT)
      .setDescription("Line Coverage by System Tests of added/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_COVERAGE_LINE_HITS_DATA_KEY = "system_coverage_line_hits_data";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_COVERAGE_LINE_HITS_DATA = new Metric.Builder(SYSTEM_COVERAGE_LINE_HITS_DATA_KEY, "System coverage hits data", Metric.ValueType.DATA)
      .setDescription("System Code coverage line hits data")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_CONDITIONS_TO_COVER_KEY = "system_conditions_to_cover";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_CONDITIONS_TO_COVER = new Metric.Builder(SYSTEM_CONDITIONS_TO_COVER_KEY, "System branches to cover", Metric.ValueType.INT)
      .setDescription("System Conditions to cover")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_CONDITIONS_TO_COVER_KEY = "new_system_conditions_to_cover";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_CONDITIONS_TO_COVER = new Metric.Builder(NEW_SYSTEM_CONDITIONS_TO_COVER_KEY, "New conditions to cover by System", Metric.ValueType.INT)
      .setDescription("New conditions to cover by System Tests")
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_UNCOVERED_CONDITIONS_KEY = "system_uncovered_conditions";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_UNCOVERED_CONDITIONS = new Metric.Builder(SYSTEM_UNCOVERED_CONDITIONS_KEY, "System uncovered branches", Metric.ValueType.INT)
      .setDescription("System Uncovered conditions")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_UNCOVERED_CONDITIONS_KEY = "new_system_uncovered_conditions";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_SYSTEM_UNCOVERED_CONDITIONS_KEY, "New uncovered conditions by System", Metric.ValueType.INT)
      .setDescription("New uncovered conditions by System Tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_BRANCH_COVERAGE_KEY = "system_branch_coverage";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_BRANCH_COVERAGE = new Metric.Builder(SYSTEM_BRANCH_COVERAGE_KEY, "System branch coverage", Metric.ValueType.PERCENT)
      .setDescription("System Branch coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  /**
   * @since 3.6
   */
  public static final String NEW_SYSTEM_BRANCH_COVERAGE_KEY = "new_system_branch_coverage";

  /**
   * @since 3.6
   */
  public static final Metric NEW_SYSTEM_BRANCH_COVERAGE = new Metric.Builder(NEW_SYSTEM_BRANCH_COVERAGE_KEY, "New branch coverage by System", Metric.ValueType.PERCENT)
      .setDescription("Branch coverage by System Tests of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_CONDITIONS_BY_LINE_KEY = "system_conditions_by_line";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_CONDITIONS_BY_LINE = new Metric.Builder(SYSTEM_CONDITIONS_BY_LINE_KEY, "System branches by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.6
   */
  public static final String SYSTEM_COVERED_CONDITIONS_BY_LINE_KEY = "system_covered_conditions_by_line";

  /**
   * @since 3.6
   */
  public static final Metric SYSTEM_COVERED_CONDITIONS_BY_LINE = new Metric.Builder(SYSTEM_COVERED_CONDITIONS_BY_LINE_KEY, "System covered branches by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_SYSTEM_TESTS)
      .setDeleteHistoricalData(true)
      .create();
  
  // --------------------------------------------------------------------------------------------------------------------
  //
  // OVERALL TESTS
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 3.3
   */
  public static final String OVERALL_COVERAGE_KEY = "overall_coverage";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_COVERAGE = new Metric.Builder(OVERALL_COVERAGE_KEY, "Overall coverage", Metric.ValueType.PERCENT)
      .setDescription("Overall test coverage")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_COVERAGE_KEY = "new_overall_coverage";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_COVERAGE = new Metric.Builder(NEW_OVERALL_COVERAGE_KEY, "Overall new coverage", Metric.ValueType.PERCENT)
      .setDescription("Overall coverage of new/changed code")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_LINES_TO_COVER_KEY = "overall_lines_to_cover";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_LINES_TO_COVER = new Metric.Builder(OVERALL_LINES_TO_COVER_KEY, "Overall lines to cover", Metric.ValueType.INT)
      .setDescription("Overall lines to cover by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setQualitative(false)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_LINES_TO_COVER_KEY = "new_overall_lines_to_cover";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_LINES_TO_COVER = new Metric.Builder(NEW_OVERALL_LINES_TO_COVER_KEY, "Overall new lines to cover", Metric.ValueType.INT)
      .setDescription("New lines to cover by all tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_UNCOVERED_LINES_KEY = "overall_uncovered_lines";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_UNCOVERED_LINES = new Metric.Builder(OVERALL_UNCOVERED_LINES_KEY, "Overall uncovered lines", Metric.ValueType.INT)
      .setDescription("Uncovered lines by all tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_UNCOVERED_LINES_KEY = "new_overall_uncovered_lines";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_UNCOVERED_LINES = new Metric.Builder(NEW_OVERALL_UNCOVERED_LINES_KEY, "Overall new lines uncovered", Metric.ValueType.INT)
      .setDescription("New lines that are not covered by any tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_LINE_COVERAGE_KEY = "overall_line_coverage";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_LINE_COVERAGE = new Metric.Builder(OVERALL_LINE_COVERAGE_KEY, "Overall line coverage", Metric.ValueType.PERCENT)
      .setDescription("Line coverage by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_LINE_COVERAGE_KEY = "new_overall_line_coverage";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_LINE_COVERAGE = new Metric.Builder(NEW_OVERALL_LINE_COVERAGE_KEY, "Overall new line coverage", Metric.ValueType.PERCENT)
      .setDescription("Line coverage of added/changed code by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_COVERAGE_LINE_HITS_DATA_KEY = "overall_coverage_line_hits_data";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_COVERAGE_LINE_HITS_DATA = new Metric.Builder(OVERALL_COVERAGE_LINE_HITS_DATA_KEY, "Overall coverage hits by line", Metric.ValueType.DATA)
      .setDescription("Coverage hits by all tests and by line")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_CONDITIONS_TO_COVER_KEY = "overall_conditions_to_cover";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_CONDITIONS_TO_COVER = new Metric.Builder(OVERALL_CONDITIONS_TO_COVER_KEY, "Overall branches to cover", Metric.ValueType.INT)
      .setDescription("Branches to cover by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_CONDITIONS_TO_COVER_KEY = "new_overall_conditions_to_cover";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_CONDITIONS_TO_COVER = new Metric.Builder(NEW_OVERALL_CONDITIONS_TO_COVER_KEY, "Overall new branches to cover", Metric.ValueType.INT)
      .setDescription("New branches to cover by all tests")
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_UNCOVERED_CONDITIONS_KEY = "overall_uncovered_conditions";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_UNCOVERED_CONDITIONS = new Metric.Builder(OVERALL_UNCOVERED_CONDITIONS_KEY, "Overall uncovered branches", Metric.ValueType.INT)
      .setDescription("Uncovered branches by all tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_UNCOVERED_CONDITIONS_KEY = "new_overall_uncovered_conditions";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_OVERALL_UNCOVERED_CONDITIONS_KEY, "Overall new branches uncovered", Metric.ValueType.INT)
      .setDescription("New branches that are not covered by any test")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_BRANCH_COVERAGE_KEY = "overall_branch_coverage";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_BRANCH_COVERAGE = new Metric.Builder(OVERALL_BRANCH_COVERAGE_KEY, "Overall branch coverage", Metric.ValueType.PERCENT)
      .setDescription("Branch coverage by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .create();

  /**
   * @since 3.3
   */
  public static final String NEW_OVERALL_BRANCH_COVERAGE_KEY = "new_overall_branch_coverage";

  /**
   * @since 3.3
   */
  public static final Metric NEW_OVERALL_BRANCH_COVERAGE = new Metric.Builder(NEW_OVERALL_BRANCH_COVERAGE_KEY, "Overall new branch coverage", Metric.ValueType.PERCENT)
      .setDescription("Branch coverage of new/changed code by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_CONDITIONS_BY_LINE_KEY = "overall_conditions_by_line";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_CONDITIONS_BY_LINE = new Metric.Builder(OVERALL_CONDITIONS_BY_LINE_KEY, "Overall branches by line", Metric.ValueType.DATA)
      .setDescription("Overall branches by all tests and by line")
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   */
  public static final String OVERALL_COVERED_CONDITIONS_BY_LINE_KEY = "overall_covered_conditions_by_line";

  /**
   * @since 3.3
   */
  public static final Metric OVERALL_COVERED_CONDITIONS_BY_LINE = new Metric.Builder(OVERALL_COVERED_CONDITIONS_BY_LINE_KEY, "Overall covered branches by line",
      Metric.ValueType.DATA)
      .setDescription("Overall covered branches by all tests and by line")
      .setDomain(DOMAIN_OVERALL_TESTS)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // DUPLICATIONS
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String DUPLICATED_LINES_KEY = "duplicated_lines";
  public static final Metric DUPLICATED_LINES = new Metric.Builder(DUPLICATED_LINES_KEY, "Duplicated lines", Metric.ValueType.INT)
      .setDescription("Duplicated lines")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DUPLICATION)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String DUPLICATED_BLOCKS_KEY = "duplicated_blocks";
  public static final Metric DUPLICATED_BLOCKS = new Metric.Builder(DUPLICATED_BLOCKS_KEY, "Duplicated blocks", Metric.ValueType.INT)
      .setDescription("Duplicated blocks")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DUPLICATION)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String DUPLICATED_FILES_KEY = "duplicated_files";

  /**
   * For files: if it contains duplicates, then 1, otherwise 0.
   * For other resources: amount of files under this resource with duplicates.
   */
  public static final Metric DUPLICATED_FILES = new Metric.Builder(DUPLICATED_FILES_KEY, "Duplicated files", Metric.ValueType.INT)
      .setDescription("Duplicated files")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DUPLICATION)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String DUPLICATED_LINES_DENSITY_KEY = "duplicated_lines_density";
  public static final Metric DUPLICATED_LINES_DENSITY = new Metric.Builder(DUPLICATED_LINES_DENSITY_KEY, "Duplicated lines (%)", Metric.ValueType.PERCENT)
      .setDescription("Duplicated lines balanced by statements")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DUPLICATION)
      .setWorstValue(50.0)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String DUPLICATIONS_DATA_KEY = "duplications_data";

  /**
   * Information about duplications, which is represented as an XML string.
   * <p>
   * Here is the format (since Sonar 2.12):
   * <pre>
   *   {@code<duplications>
   *     <!-- Multiple groups: -->
   *     <g>
   *       <!-- Multiple blocks: -->
   *       <b r="[resource key]" s="[first line]" l="[number of lines]" />
   *       ...
   *     </g>
   *     ...
   *   </duplications>}
   * </pre>
   * </p>
   */
  public static final Metric DUPLICATIONS_DATA = new Metric.Builder(DUPLICATIONS_DATA_KEY, "Duplications details", Metric.ValueType.DATA)
      .setDescription("Duplications details")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DUPLICATION)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // CODING RULES
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final String USABILITY_KEY = "usability";

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final Metric USABILITY = new Metric(USABILITY_KEY, "Usability", "Usability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final String RELIABILITY_KEY = "reliability";

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final Metric RELIABILITY = new Metric(RELIABILITY_KEY, "Reliability", "Reliability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final String EFFICIENCY_KEY = "efficiency";

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final Metric EFFICIENCY = new Metric(EFFICIENCY_KEY, "Efficiency", "Efficiency", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final String PORTABILITY_KEY = "portability";

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final Metric PORTABILITY = new Metric(PORTABILITY_KEY, "Portability", "Portability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final String MAINTAINABILITY_KEY = "maintainability";

  /**
   * @deprecated since 2.5 See SONAR-2007
   */
  @Deprecated
  public static final Metric MAINTAINABILITY = new Metric.Builder(MAINTAINABILITY_KEY, "Maintainability", Metric.ValueType.PERCENT)
      .setDescription("Maintainability")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_RULE_CATEGORIES)
      .setBestValue(100.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String WEIGHTED_VIOLATIONS_KEY = "weighted_violations";
  public static final Metric WEIGHTED_VIOLATIONS = new Metric.Builder(WEIGHTED_VIOLATIONS_KEY, "Weighted violations", Metric.ValueType.INT)
      .setDescription("Weighted Violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String VIOLATIONS_DENSITY_KEY = "violations_density";
  public static final Metric VIOLATIONS_DENSITY = new Metric.Builder(VIOLATIONS_DENSITY_KEY, "Rules compliance", Metric.ValueType.PERCENT)
      .setDescription("Rules compliance")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .create();

  public static final String VIOLATIONS_KEY = "violations";
  public static final Metric VIOLATIONS = new Metric.Builder(VIOLATIONS_KEY, "Violations", Metric.ValueType.INT)
      .setDescription("Violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String BLOCKER_VIOLATIONS_KEY = "blocker_violations";
  public static final Metric BLOCKER_VIOLATIONS = new Metric.Builder(BLOCKER_VIOLATIONS_KEY, "Blocker violations", Metric.ValueType.INT)
      .setDescription("Blocker violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String CRITICAL_VIOLATIONS_KEY = "critical_violations";
  public static final Metric CRITICAL_VIOLATIONS = new Metric.Builder(CRITICAL_VIOLATIONS_KEY, "Critical violations", Metric.ValueType.INT)
      .setDescription("Critical violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String MAJOR_VIOLATIONS_KEY = "major_violations";
  public static final Metric MAJOR_VIOLATIONS = new Metric.Builder(MAJOR_VIOLATIONS_KEY, "Major violations", Metric.ValueType.INT)
      .setDescription("Major violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String MINOR_VIOLATIONS_KEY = "minor_violations";
  public static final Metric MINOR_VIOLATIONS = new Metric.Builder(MINOR_VIOLATIONS_KEY, "Minor violations", Metric.ValueType.INT)
      .setDescription("Minor violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String INFO_VIOLATIONS_KEY = "info_violations";
  public static final Metric INFO_VIOLATIONS = new Metric.Builder(INFO_VIOLATIONS_KEY, "Info violations", Metric.ValueType.INT)
      .setDescription("Info violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  public static final String NEW_VIOLATIONS_KEY = "new_violations";
  public static final Metric NEW_VIOLATIONS = new Metric.Builder(NEW_VIOLATIONS_KEY, "New Violations", Metric.ValueType.INT)
      .setDescription("New Violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String NEW_BLOCKER_VIOLATIONS_KEY = "new_blocker_violations";
  public static final Metric NEW_BLOCKER_VIOLATIONS = new Metric.Builder(NEW_BLOCKER_VIOLATIONS_KEY, "New Blocker violations", Metric.ValueType.INT)
      .setDescription("New Blocker violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String NEW_CRITICAL_VIOLATIONS_KEY = "new_critical_violations";
  public static final Metric NEW_CRITICAL_VIOLATIONS = new Metric.Builder(NEW_CRITICAL_VIOLATIONS_KEY, "New Critical violations", Metric.ValueType.INT)
      .setDescription("New Critical violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String NEW_MAJOR_VIOLATIONS_KEY = "new_major_violations";
  public static final Metric NEW_MAJOR_VIOLATIONS = new Metric.Builder(NEW_MAJOR_VIOLATIONS_KEY, "New Major violations", Metric.ValueType.INT)
      .setDescription("New Major violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String NEW_MINOR_VIOLATIONS_KEY = "new_minor_violations";
  public static final Metric NEW_MINOR_VIOLATIONS = new Metric.Builder(NEW_MINOR_VIOLATIONS_KEY, "New Minor violations", Metric.ValueType.INT)
      .setDescription("New Minor violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String NEW_INFO_VIOLATIONS_KEY = "new_info_violations";
  public static final Metric NEW_INFO_VIOLATIONS = new Metric.Builder(NEW_INFO_VIOLATIONS_KEY, "New Info violations", Metric.ValueType.INT)
      .setDescription("New Info violations")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_RULES)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // DESIGN
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String ABSTRACTNESS_KEY = "abstractness";
  public static final Metric ABSTRACTNESS = new Metric.Builder(ABSTRACTNESS_KEY, "Abstractness", Metric.ValueType.PERCENT)
      .setDescription("Abstractness")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String INSTABILITY_KEY = "instability";
  public static final Metric INSTABILITY = new Metric.Builder(INSTABILITY_KEY, "Instability", Metric.ValueType.PERCENT)
      .setDescription("Instability")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String DISTANCE_KEY = "distance";
  public static final Metric DISTANCE = new Metric.Builder(DISTANCE_KEY, "Distance", Metric.ValueType.FLOAT)
      .setDescription("Distance")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String DEPTH_IN_TREE_KEY = "dit";
  public static final Metric DEPTH_IN_TREE = new Metric.Builder(DEPTH_IN_TREE_KEY, "Depth in Tree", Metric.ValueType.INT)
      .setDescription("Depth in Inheritance Tree")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String NUMBER_OF_CHILDREN_KEY = "noc";
  public static final Metric NUMBER_OF_CHILDREN = new Metric.Builder(NUMBER_OF_CHILDREN_KEY, "Number of Children", Metric.ValueType.INT)
      .setDescription("Number of Children")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String RFC_KEY = "rfc";
  public static final Metric RFC = new Metric.Builder(RFC_KEY, "RFC", Metric.ValueType.INT)
      .setDescription("Response for Class")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setFormula(new WeightedMeanAggregationFormula(CoreMetrics.FILES, false))
      .create();

  public static final String RFC_DISTRIBUTION_KEY = "rfc_distribution";
  public static final Metric RFC_DISTRIBUTION = new Metric.Builder(RFC_DISTRIBUTION_KEY, "Class distribution /RFC", Metric.ValueType.DISTRIB)
      .setDescription("Class distribution /RFC")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .setFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY))
      .create();

  public static final String LCOM4_KEY = "lcom4";
  public static final Metric LCOM4 = new Metric.Builder(LCOM4_KEY, "LCOM4", Metric.ValueType.FLOAT)
      .setDescription("Lack of Cohesion of Methods")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .setBestValue(1.0)
      .setFormula(new WeightedMeanAggregationFormula(CoreMetrics.FILES, false))
      .create();

  public static final String LCOM4_BLOCKS_KEY = "lcom4_blocks";
  public static final Metric LCOM4_BLOCKS = new Metric.Builder(LCOM4_BLOCKS_KEY, "LCOM4 blocks", Metric.ValueType.DATA)
      .setDescription("LCOM4 blocks")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String LCOM4_DISTRIBUTION_KEY = "lcom4_distribution";
  public static final Metric LCOM4_DISTRIBUTION = new Metric.Builder(LCOM4_DISTRIBUTION_KEY, "Class distribution /LCOM4", Metric.ValueType.DISTRIB)
      .setDescription("Class distribution /LCOM4")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .setFormula(new SumChildDistributionFormula().setMinimumScopeToPersist(Scopes.DIRECTORY))
      .create();

  public static final String SUSPECT_LCOM4_DENSITY_KEY = "suspect_lcom4_density";
  public static final Metric SUSPECT_LCOM4_DENSITY = new Metric.Builder(SUSPECT_LCOM4_DENSITY_KEY, "Suspect LCOM4 density", Metric.ValueType.PERCENT)
      .setDescription("Density of classes having LCOM4>1")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String AFFERENT_COUPLINGS_KEY = "ca";
  public static final Metric AFFERENT_COUPLINGS = new Metric.Builder(AFFERENT_COUPLINGS_KEY, "Afferent couplings", Metric.ValueType.INT)
      .setDescription("Afferent couplings")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String EFFERENT_COUPLINGS_KEY = "ce";
  public static final Metric EFFERENT_COUPLINGS = new Metric.Builder(EFFERENT_COUPLINGS_KEY, "Efferent couplings", Metric.ValueType.INT)
      .setDescription("Efferent couplings")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String DEPENDENCY_MATRIX_KEY = "dsm";
  public static final Metric DEPENDENCY_MATRIX = new Metric.Builder(DEPENDENCY_MATRIX_KEY, "Dependency Matrix", Metric.ValueType.DATA)
      .setDescription("Dependency Matrix")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setDeleteHistoricalData(true)
      .create();

  public static final String PACKAGE_CYCLES_KEY = "package_cycles";
  public static final Metric PACKAGE_CYCLES = new Metric.Builder(PACKAGE_CYCLES_KEY, "Package cycles", Metric.ValueType.INT)
      .setDescription("Package cycles")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .setBestValue(0.0)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String PACKAGE_TANGLE_INDEX_KEY = "package_tangle_index";
  public static final Metric PACKAGE_TANGLE_INDEX = new Metric.Builder(PACKAGE_TANGLE_INDEX_KEY, "Package tangle index", Metric.ValueType.PERCENT)
      .setDescription("Package tangle index")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(0.0)
      .setDomain(DOMAIN_DESIGN)
      .create();

  public static final String PACKAGE_TANGLES_KEY = "package_tangles";
  public static final Metric PACKAGE_TANGLES = new Metric.Builder(PACKAGE_TANGLES_KEY, "File dependencies to cut", Metric.ValueType.INT)
      .setDescription("File dependencies to cut")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setFormula(new SumChildValuesFormula(false))
      .create();

  public static final String PACKAGE_FEEDBACK_EDGES_KEY = "package_feedback_edges";
  public static final Metric PACKAGE_FEEDBACK_EDGES = new Metric.Builder(PACKAGE_FEEDBACK_EDGES_KEY, "Package dependencies to cut", Metric.ValueType.INT)
      .setDescription("Package dependencies to cut")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setFormula(new SumChildValuesFormula(false))
      .setBestValue(0.0)
      .create();

  public static final String PACKAGE_EDGES_WEIGHT_KEY = "package_edges_weight";
  public static final Metric PACKAGE_EDGES_WEIGHT = new Metric.Builder(PACKAGE_EDGES_WEIGHT_KEY, "Package edges weight", Metric.ValueType.INT)
      .setDescription("Package edges weight")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setFormula(new SumChildValuesFormula(false))
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String FILE_CYCLES_KEY = "file_cycles";
  public static final Metric FILE_CYCLES = new Metric.Builder(FILE_CYCLES_KEY, "File cycles", Metric.ValueType.INT)
      .setDescription("File cycles")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setBestValue(0.0)
      .create();

  public static final String FILE_TANGLE_INDEX_KEY = "file_tangle_index";
  public static final Metric FILE_TANGLE_INDEX = new Metric.Builder(FILE_TANGLE_INDEX_KEY, "File tangle index", Metric.ValueType.PERCENT)
      .setDescription("File tangle index")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DESIGN)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setBestValue(0.0)
      .create();

  public static final String FILE_TANGLES_KEY = "file_tangles";
  public static final Metric FILE_TANGLES = new Metric.Builder(FILE_TANGLES_KEY, "File tangles", Metric.ValueType.INT)
      .setDescription("Files tangles")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  public static final String FILE_FEEDBACK_EDGES_KEY = "file_feedback_edges";
  public static final Metric FILE_FEEDBACK_EDGES = new Metric.Builder(FILE_FEEDBACK_EDGES_KEY, "Suspect file dependencies", Metric.ValueType.INT)
      .setDescription("Suspect file dependencies")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .setBestValue(0.0)
      .create();

  public static final String FILE_EDGES_WEIGHT_KEY = "file_edges_weight";
  public static final Metric FILE_EDGES_WEIGHT = new Metric.Builder(FILE_EDGES_WEIGHT_KEY, "File edges weight", Metric.ValueType.INT)
      .setDescription("File edges weight")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(DOMAIN_DESIGN)
      .setHidden(true)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // SCM
  // These metrics are computed by the SCM Activity plugin, since version 1.2 and introduced here since version 2.7.
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.7
   */
  public static final String SCM_AUTHORS_BY_LINE_KEY = "authors_by_line";

  /**
   * Key-value pairs, where key - is a number of line, and value - is an author for this line.
   *
   * @see org.sonar.api.utils.KeyValueFormat#formatIntString(java.util.Map)
   * @see org.sonar.api.utils.KeyValueFormat#parseIntString(String)
   * @since 2.7
   */
  public static final Metric SCM_AUTHORS_BY_LINE = new Metric.Builder(SCM_AUTHORS_BY_LINE_KEY, "Authors by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_SCM)
      .create();

  /**
   * @since 2.7
   */
  public static final String SCM_REVISIONS_BY_LINE_KEY = "revisions_by_line";

  /**
   * Key-value pairs, where key - is a number of line, and value - is a revision for this line.
   *
   * @see org.sonar.api.utils.KeyValueFormat#formatIntString(java.util.Map)
   * @see org.sonar.api.utils.KeyValueFormat#parseIntString(String)
   * @since 2.7
   */
  public static final Metric SCM_REVISIONS_BY_LINE = new Metric.Builder(SCM_REVISIONS_BY_LINE_KEY, "Revisions by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_SCM)
      .create();

  /**
   * @since 2.7
   */
  public static final String SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY = "last_commit_datetimes_by_line";

  /**
   * Key-value pairs, where key - is a number of line, and value - is a date of last commit for this line.
   *
   * @see org.sonar.api.utils.KeyValueFormat#formatIntDateTime(java.util.Map)
   * @see org.sonar.api.utils.KeyValueFormat#parseIntDateTime(String)
   * @since 2.7
   */
  public static final Metric SCM_LAST_COMMIT_DATETIMES_BY_LINE = new Metric.Builder(SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY, "Last commit dates by line", Metric.ValueType.DATA)
      .setDomain(DOMAIN_SCM)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // REVIEWS (since 2.14)
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.14
   */
  public static final String UNREVIEWED_VIOLATIONS_KEY = "unreviewed_violations";

  /**
   * @since 2.14
   */
  public static final Metric UNREVIEWED_VIOLATIONS = new Metric.Builder(UNREVIEWED_VIOLATIONS_KEY, "Unreviewed violations", Metric.ValueType.INT)
      .setDescription("Violations that have not been reviewed yet")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_REVIEWS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  /**
   * @since 2.14
   */
  public static final String NEW_UNREVIEWED_VIOLATIONS_KEY = "new_unreviewed_violations";

  /**
   * @since 2.14
   */
  public static final Metric NEW_UNREVIEWED_VIOLATIONS = new Metric.Builder(NEW_UNREVIEWED_VIOLATIONS_KEY, "New unreviewed violations", Metric.ValueType.INT)
      .setDescription("New violations that have not been reviewed yet")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_REVIEWS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 2.14
   */
  public static final String FALSE_POSITIVE_REVIEWS_KEY = "false_positive_reviews";

  /**
   * @since 2.14
   */
  public static final Metric FALSE_POSITIVE_REVIEWS = new Metric.Builder(FALSE_POSITIVE_REVIEWS_KEY, "False-positive reviews", Metric.ValueType.INT)
      .setDescription("Active false-positive reviews")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_REVIEWS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  /**
   * @since 2.14
   */
  public static final String ACTIVE_REVIEWS_KEY = "active_reviews";

  /**
   * @since 2.14
   */
  public static final Metric ACTIVE_REVIEWS = new Metric.Builder(ACTIVE_REVIEWS_KEY, "Active reviews", Metric.ValueType.INT)
      .setDescription("Active open and reopened reviews")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_REVIEWS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  /**
   * @since 2.14
   */
  public static final String UNASSIGNED_REVIEWS_KEY = "unassigned_reviews";

  /**
   * @since 2.14
   */
  public static final Metric UNASSIGNED_REVIEWS = new Metric.Builder(UNASSIGNED_REVIEWS_KEY, "Unassigned reviews", Metric.ValueType.INT)
      .setDescription("Active unassigned reviews")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_REVIEWS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  /**
   * @since 2.14
   */
  public static final String UNPLANNED_REVIEWS_KEY = "unplanned_reviews";

  /**
   * @since 2.14
   */
  public static final Metric UNPLANNED_REVIEWS = new Metric.Builder(UNPLANNED_REVIEWS_KEY, "Unplanned reviews", Metric.ValueType.INT)
      .setDescription("Active unplanned reviews")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_REVIEWS)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // FILE DATA
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.14
   */
  @Beta
  public static final String NCLOC_DATA_KEY = "ncloc_data";

  /**
   * Information about lines of code in file.
   * Key-value pairs, where key - is a number of line, and value - is an indicator of whether line contains code (1) or not (0).
   *
   * @see org.sonar.api.measures.FileLinesContext
   * @since 2.14
   */
  @Beta
  public static final Metric NCLOC_DATA = new Metric.Builder(NCLOC_DATA_KEY, "ncloc_data", Metric.ValueType.DATA)
      .setHidden(true)
      .setDomain(DOMAIN_SIZE)
      .create();

  /**
   * @since 2.14
   */
  @Beta
  public static final String COMMENT_LINES_DATA_KEY = "comment_lines_data";

  /**
   * Information about comments in file.
   * Key-value pairs, where key - is a number of line, and value - is an indicator of whether line contains comment (1) or not (0).
   *
   * @see org.sonar.api.measures.FileLinesContext
   * @since 2.14
   */
  @Beta
  public static final Metric COMMENT_LINES_DATA = new Metric.Builder(COMMENT_LINES_DATA_KEY, "comment_lines_data", Metric.ValueType.DATA)
      .setHidden(true)
      .setDomain(DOMAIN_DOCUMENTATION)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // OTHERS
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String ALERT_STATUS_KEY = "alert_status";
  public static final Metric ALERT_STATUS = new Metric.Builder(ALERT_STATUS_KEY, "Alert", Metric.ValueType.LEVEL)
      .setDescription("Alert")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_GENERAL)
      .create();

  public static final String PROFILE_KEY = "profile";
  public static final Metric PROFILE = new Metric.Builder(PROFILE_KEY, "Profile", Metric.ValueType.DATA)
      .setDescription("Selected quality profile")
      .setDomain(DOMAIN_GENERAL)
      .create();

  /**
   * @since 2.9
   */
  public static final String PROFILE_VERSION_KEY = "profile_version";

  /**
   * @since 2.9
   */
  public static final Metric PROFILE_VERSION = new Metric.Builder(PROFILE_VERSION_KEY, "Profile version", Metric.ValueType.INT)
      .setDescription("Selected quality profile version")
      .setQualitative(false)
      .setDomain(DOMAIN_GENERAL)
      .setHidden(true)
      .create();

  private static final List<Metric> METRICS;

  static {
    METRICS = Lists.newLinkedList();
    for (Field field : CoreMetrics.class.getFields()) {
      if (Metric.class.isAssignableFrom(field.getType())) {
        try {
          Metric metric = (Metric) field.get(null);
          if (!StringUtils.equals(metric.getDomain(), DOMAIN_RULE_CATEGORIES)) {
            METRICS.add(metric);
          }
        } catch (IllegalAccessException e) {
          throw new SonarException("can not introspect " + CoreMetrics.class + " to get metrics", e);
        }
      }
    }
  }

  public static List<Metric> getMetrics() {
    return METRICS;
  }
}
