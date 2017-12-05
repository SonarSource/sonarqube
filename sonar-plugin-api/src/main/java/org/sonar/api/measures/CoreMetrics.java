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
package org.sonar.api.measures;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.utils.SonarException;

/**
 * @since 1.10
 */
public final class CoreMetrics {

  // the following fields are not final to avoid compile-time constants used by plugins
  public static String DOMAIN_SIZE = "Size";
  public static String DOMAIN_COVERAGE = "Coverage";

  /**
   * @deprecated in 5.5. Merged into {@link #DOMAIN_COVERAGE}
   */
  @Deprecated
  public static String DOMAIN_TESTS = "Tests";

  /**
   * @deprecated in 5.5. Merged into {@link #DOMAIN_COVERAGE}
   */
  @Deprecated
  public static String DOMAIN_INTEGRATION_TESTS = "Tests (Integration)";

  /**
   * @deprecated in 5.5. Merged into {@link #DOMAIN_COVERAGE}
   */
  @Deprecated
  public static String DOMAIN_OVERALL_TESTS = "Tests (Overall)";
  public static String DOMAIN_COMPLEXITY = "Complexity";
  /**
   * @deprecated since 6.2. Merged into {@link #DOMAIN_SIZE}
   */
  @Deprecated
  public static String DOMAIN_DOCUMENTATION = "Documentation";
  public static String DOMAIN_SCM = "SCM";
  public static String DOMAIN_ISSUES = "Issues";
  public static String DOMAIN_GENERAL = "General";
  public static String DOMAIN_DUPLICATIONS = "Duplications";

  /**
   * @deprecated in 5.5. Renamed to {@link #DOMAIN_DUPLICATIONS}
   */
  @Deprecated
  public static String DOMAIN_DUPLICATION = "Duplication";
  public static String DOMAIN_DESIGN = "Design";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static String DOMAIN_MAINTAINABILITY = "Maintainability";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static String DOMAIN_RELIABILITY = "Reliability";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static String DOMAIN_SECURITY = "Security";

  /**
   * @since 4.0
   *
   * @deprecated in 5.5. Replaced by {@link #DOMAIN_MAINTAINABILITY}
   */
  @Deprecated
  public static String DOMAIN_TECHNICAL_DEBT = "Technical Debt";

  /**
   * @since 5.5
   */
  public static String DOMAIN_RELEASABILITY = "Releasability";

  /**
   * Computed by the platform since SQ 5.1
   */
  public static final String LINES_KEY = "lines";
  public static final Metric<Integer> LINES = new Metric.Builder(LINES_KEY, "Lines", Metric.ValueType.INT)
    .setDescription("Lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  public static final String GENERATED_LINES_KEY = "generated_lines";
  public static final Metric<Integer> GENERATED_LINES = new Metric.Builder(GENERATED_LINES_KEY, "Generated Lines", Metric.ValueType.INT)
    .setDescription("Number of generated lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NCLOC_KEY = "ncloc";
  public static final Metric<Integer> NCLOC = new Metric.Builder(NCLOC_KEY, "Lines of Code", Metric.ValueType.INT)
    .setDescription("Non commenting lines of code")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * @since 6.1
   */
  public static final String NEW_LINES_KEY = "new_lines";
  /**
   * @since 6.1
   */
  public static final Metric<Integer> NEW_LINES = new Metric.Builder(NEW_LINES_KEY, "New Lines", Metric.ValueType.INT)
    .setDescription("New lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @since 4.4
   */
  public static final String NCLOC_LANGUAGE_DISTRIBUTION_KEY = "ncloc_language_distribution";

  /**
   * @since 4.4
   */
  public static final Metric<String> NCLOC_LANGUAGE_DISTRIBUTION = new Metric.Builder(NCLOC_LANGUAGE_DISTRIBUTION_KEY, "Lines of Code Per Language", Metric.ValueType.DATA)
    .setDescription("Non Commenting Lines of Code Distributed By Language")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  public static final String GENERATED_NCLOC_KEY = "generated_ncloc";
  public static final Metric<Integer> GENERATED_NCLOC = new Metric.Builder(GENERATED_NCLOC_KEY, "Generated Lines of Code", Metric.ValueType.INT)
    .setDescription("Generated non Commenting Lines of Code")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String CLASSES_KEY = "classes";
  public static final Metric<Integer> CLASSES = new Metric.Builder(CLASSES_KEY, "Classes", Metric.ValueType.INT)
    .setDescription("Classes")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  public static final String FILES_KEY = "files";
  /**
   * Computed by the platform.
   */
  public static final Metric<Integer> FILES = new Metric.Builder(FILES_KEY, "Files", Metric.ValueType.INT)
    .setDescription("Number of files")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  public static final String DIRECTORIES_KEY = "directories";
  /**
   * Computed by the platform.
   */
  public static final Metric<Integer> DIRECTORIES = new Metric.Builder(DIRECTORIES_KEY, "Directories", Metric.ValueType.INT)
    .setDescription("Directories")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  public static final String FUNCTIONS_KEY = "functions";
  public static final Metric<Integer> FUNCTIONS = new Metric.Builder(FUNCTIONS_KEY, "Functions", Metric.ValueType.INT)
    .setDescription("Functions")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * @deprecated since 5.0.
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-5224">SONAR-5224</a>
   */
  @Deprecated
  public static final String ACCESSORS_KEY = "accessors";

  /**
   * @deprecated since 5.0.
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-5224">SONAR-5224</a>
   */
  @Deprecated
  public static final Metric<Integer> ACCESSORS = new Metric.Builder(ACCESSORS_KEY, "Accessors", Metric.ValueType.INT)
    .setDescription("Accessors")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .setHidden(true)
    .create();

  public static final String STATEMENTS_KEY = "statements";
  public static final Metric<Integer> STATEMENTS = new Metric.Builder(STATEMENTS_KEY, "Statements", Metric.ValueType.INT)
    .setDescription("Number of statements")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * @deprecated since 6.2
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-8328">SONAR-8328</a>
   */
  @Deprecated
  public static final String PUBLIC_API_KEY = "public_api";
  public static final Metric<Integer> PUBLIC_API = new Metric.Builder(PUBLIC_API_KEY, "Public API", Metric.ValueType.INT)
    .setDescription("Public API")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_DOCUMENTATION)
    .setHidden(true)
    .create();

  /**
   * @since 3.0
   */
  public static final String PROJECTS_KEY = "projects";

  /**
   * @since 3.0
   */
  public static final Metric<Integer> PROJECTS = new Metric.Builder(PROJECTS_KEY, "Projects", Metric.ValueType.INT)
    .setDescription("Number of projects")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * Moved to Size domain since 6.2
   */
  public static final String COMMENT_LINES_KEY = "comment_lines";
  public static final Metric<Integer> COMMENT_LINES = new Metric.Builder(COMMENT_LINES_KEY, "Comment Lines", Metric.ValueType.INT)
    .setDescription("Number of comment lines")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(false)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * Moved to Size domain since 6.2
   */
  public static final String COMMENT_LINES_DENSITY_KEY = "comment_lines_density";
  public static final Metric<Double> COMMENT_LINES_DENSITY = new Metric.Builder(COMMENT_LINES_DENSITY_KEY, "Comments (%)", Metric.ValueType.PERCENT)
    .setDescription("Comments balanced by ncloc + comment lines")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_SIZE)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // DOCUMENTATION
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @deprecated since 6.2
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-8328">SONAR-8328</a>
   */
  @Deprecated
  public static final String PUBLIC_DOCUMENTED_API_DENSITY_KEY = "public_documented_api_density";
  public static final Metric<Double> PUBLIC_DOCUMENTED_API_DENSITY = new Metric.Builder(PUBLIC_DOCUMENTED_API_DENSITY_KEY, "Public Documented API (%)", Metric.ValueType.PERCENT)
    .setDescription("Public documented classes and functions balanced by ncloc")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_DOCUMENTATION)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setOptimizedBestValue(true)
    .setHidden(true)
    .create();

  /**
   * @deprecated since 6.2
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-8328">SONAR-8328</a>
   */
  @Deprecated
  public static final String PUBLIC_UNDOCUMENTED_API_KEY = "public_undocumented_api";
  public static final Metric<Integer> PUBLIC_UNDOCUMENTED_API = new Metric.Builder(PUBLIC_UNDOCUMENTED_API_KEY, "Public Undocumented API", Metric.ValueType.INT)
    .setDescription("Public undocumented classes, functions and variables")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DOCUMENTATION)
    .setBestValue(0.0)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setHidden(true)
    .create();

  /**
   * @deprecated since 4.2 - see SONAR-4990
   */
  @Deprecated
  public static final String COMMENTED_OUT_CODE_LINES_KEY = "commented_out_code_lines";

  /**
   * @deprecated since 4.2 - see SONAR-4990
   */
  @Deprecated
  public static final Metric<Integer> COMMENTED_OUT_CODE_LINES = new Metric.Builder(COMMENTED_OUT_CODE_LINES_KEY, "Commented-Out LOC", Metric.ValueType.INT)
    .setDescription("Commented lines of code")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DOCUMENTATION)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setHidden(true)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // COMPLEXITY
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String COMPLEXITY_KEY = "complexity";
  public static final Metric<Integer> COMPLEXITY = new Metric.Builder(COMPLEXITY_KEY, "Cyclomatic Complexity", Metric.ValueType.INT)
    .setDescription("Cyclomatic complexity")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COMPLEXITY)
    .create();

  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String FILE_COMPLEXITY_KEY = "file_complexity";
  /**
   * Information about the cyclomatic complexity per file, calculated by divided the {@link #COMPLEXITY} by the number of {@link #FILES}.
   *
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<Double> FILE_COMPLEXITY = new Metric.Builder(FILE_COMPLEXITY_KEY, "Complexity / File", Metric.ValueType.FLOAT)
    .setDescription("Complexity average by file")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_COMPLEXITY)
    .setHidden(true)
    .create();

  /**
   * @since 3.6
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String COMPLEXITY_IN_CLASSES_KEY = "complexity_in_classes";

  /**
   * @since 3.6
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<Integer> COMPLEXITY_IN_CLASSES = new Metric.Builder(COMPLEXITY_IN_CLASSES_KEY, "Complexity in Classes", Metric.ValueType.INT)
    .setDescription("Cyclomatic complexity in classes")
    .setHidden(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COMPLEXITY)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String CLASS_COMPLEXITY_KEY = "class_complexity";
  /**
   * Information about the cyclomatic complexity per class, calculated by divided the {@link #COMPLEXITY_IN_CLASSES} by the number of {@link #CLASSES}.
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<Double> CLASS_COMPLEXITY = new Metric.Builder(CLASS_COMPLEXITY_KEY, "Complexity / Class", Metric.ValueType.FLOAT)
    .setDescription("Complexity average by class")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_COMPLEXITY)
    .setHidden(true)
    .create();

  /**
   * @since 3.6
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String COMPLEXITY_IN_FUNCTIONS_KEY = "complexity_in_functions";
  /**
   * @since 3.6
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<Integer> COMPLEXITY_IN_FUNCTIONS = new Metric.Builder(COMPLEXITY_IN_FUNCTIONS_KEY, "Complexity in Functions", Metric.ValueType.INT)
    .setDescription("Cyclomatic complexity in functions")
    .setHidden(true)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COMPLEXITY)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String FUNCTION_COMPLEXITY_KEY = "function_complexity";
  /**
   * Information about the cyclomatic complexity per function, calculated by divided the {@link #COMPLEXITY_IN_FUNCTIONS} by the number of {@link #FUNCTIONS}.
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<Double> FUNCTION_COMPLEXITY = new Metric.Builder(FUNCTION_COMPLEXITY_KEY, "Complexity / Function", Metric.ValueType.FLOAT)
    .setDescription("Complexity average by function")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_COMPLEXITY)
    .setHidden(true)
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
  public static final Metric<String> CLASS_COMPLEXITY_DISTRIBUTION = new Metric.Builder(CLASS_COMPLEXITY_DISTRIBUTION_KEY, "Class Distribution / Complexity",
    Metric.ValueType.DISTRIB)
      .setDescription("Classes distribution /complexity")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setHidden(true)
      .create();

  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String FUNCTION_COMPLEXITY_DISTRIBUTION_KEY = "function_complexity_distribution";
  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<String> FUNCTION_COMPLEXITY_DISTRIBUTION = new Metric.Builder(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "Function Distribution / Complexity",
    Metric.ValueType.DISTRIB)
      .setDescription("Functions distribution /complexity")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setHidden(true)
      .create();

  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final String FILE_COMPLEXITY_DISTRIBUTION_KEY = "file_complexity_distribution";
  /**
   * @deprecated since 6.7
   */
  @Deprecated
  public static final Metric<String> FILE_COMPLEXITY_DISTRIBUTION = new Metric.Builder(FILE_COMPLEXITY_DISTRIBUTION_KEY, "File Distribution / Complexity",
    Metric.ValueType.DISTRIB)
      .setDescription("Files distribution /complexity")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(true)
      .setDomain(DOMAIN_COMPLEXITY)
      .setHidden(true)
      .create();

  public static final String COGNITIVE_COMPLEXITY_KEY = "cognitive_complexity";
  public static final Metric<Integer> COGNITIVE_COMPLEXITY = new Metric.Builder(COGNITIVE_COMPLEXITY_KEY, "Cognitive Complexity", Metric.ValueType.INT)
    .setDescription("Cognitive complexity")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COMPLEXITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
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
   * <li>Non-zero value should be saved for resources representing tests. And Sonar provides default Decorator, which will decorate parent resources.</li>
   * <li>Should include {@link #TEST_FAILURES} and {@link #TEST_ERRORS}, but should not include {@link #SKIPPED_TESTS}.</li>
   * </ul>
   */
  public static final Metric<Integer> TESTS = new Metric.Builder(TESTS_KEY, "Unit Tests", Metric.ValueType.INT)
    .setDescription("Number of unit tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .create();

  public static final String TEST_EXECUTION_TIME_KEY = "test_execution_time";
  public static final Metric<Long> TEST_EXECUTION_TIME = new Metric.Builder(TEST_EXECUTION_TIME_KEY, "Unit Test Duration", Metric.ValueType.MILLISEC)
    .setDescription("Execution duration of unit tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .create();

  public static final String TEST_ERRORS_KEY = "test_errors";
  public static final Metric<Integer> TEST_ERRORS = new Metric.Builder(TEST_ERRORS_KEY, "Unit Test Errors", Metric.ValueType.INT)
    .setDescription("Number of unit test errors")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String SKIPPED_TESTS_KEY = "skipped_tests";
  public static final Metric<Integer> SKIPPED_TESTS = new Metric.Builder(SKIPPED_TESTS_KEY, "Skipped Unit Tests", Metric.ValueType.INT)
    .setDescription("Number of skipped unit tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String TEST_FAILURES_KEY = "test_failures";
  public static final Metric<Integer> TEST_FAILURES = new Metric.Builder(TEST_FAILURES_KEY, "Unit Test Failures", Metric.ValueType.INT)
    .setDescription("Number of unit test failures")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String TEST_SUCCESS_DENSITY_KEY = "test_success_density";
  public static final Metric<Double> TEST_SUCCESS_DENSITY = new Metric.Builder(TEST_SUCCESS_DENSITY_KEY, "Unit Test Success (%)", Metric.ValueType.PERCENT)
    .setDescription("Density of successful unit tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @deprecated since 5.2 use {@link MutableTestPlan}
   */
  @Deprecated
  public static final String TEST_DATA_KEY = "test_data";
  /**
   * @deprecated since 5.2 use {@link MutableTestPlan}
   */
  @Deprecated
  public static final Metric<String> TEST_DATA = new Metric.Builder(TEST_DATA_KEY, "Unit Test Details", Metric.ValueType.DATA)
    .setDescription("Unit tests details")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .create();

  public static final String COVERAGE_KEY = "coverage";
  public static final Metric<Double> COVERAGE = new Metric.Builder(COVERAGE_KEY, "Coverage", Metric.ValueType.PERCENT)
    .setDescription("Coverage by tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .create();

  public static final String NEW_COVERAGE_KEY = "new_coverage";
  public static final Metric<Double> NEW_COVERAGE = new Metric.Builder(NEW_COVERAGE_KEY, "Coverage on New Code", Metric.ValueType.PERCENT)
    .setDescription("Coverage of new/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDeleteHistoricalData(true)
    .create();

  public static final String LINES_TO_COVER_KEY = "lines_to_cover";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric<Integer> LINES_TO_COVER = new Metric.Builder(LINES_TO_COVER_KEY, "Lines to Cover", Metric.ValueType.INT)
    .setDescription("Lines to cover")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .create();

  public static final String NEW_LINES_TO_COVER_KEY = "new_lines_to_cover";
  public static final Metric<Integer> NEW_LINES_TO_COVER = new Metric.Builder(NEW_LINES_TO_COVER_KEY, "Lines to Cover on New Code", Metric.ValueType.INT)
    .setDescription("Lines to cover on new code")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  public static final String UNCOVERED_LINES_KEY = "uncovered_lines";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric<Integer> UNCOVERED_LINES = new Metric.Builder(UNCOVERED_LINES_KEY, "Uncovered Lines", Metric.ValueType.INT)
    .setDescription("Uncovered lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .create();

  public static final String NEW_UNCOVERED_LINES_KEY = "new_uncovered_lines";
  public static final Metric<Integer> NEW_UNCOVERED_LINES = new Metric.Builder(NEW_UNCOVERED_LINES_KEY, "Uncovered Lines on New Code", Metric.ValueType.INT)
    .setDescription("Uncovered lines on new code")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setDeleteHistoricalData(true)
    .create();

  public static final String LINE_COVERAGE_KEY = "line_coverage";
  public static final Metric<Double> LINE_COVERAGE = new Metric.Builder(LINE_COVERAGE_KEY, "Line Coverage", Metric.ValueType.PERCENT)
    .setDescription("Line coverage")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .create();

  public static final String NEW_LINE_COVERAGE_KEY = "new_line_coverage";
  public static final Metric<Double> NEW_LINE_COVERAGE = new Metric.Builder(NEW_LINE_COVERAGE_KEY, "Line Coverage on New Code", Metric.ValueType.PERCENT)
    .setDescription("Line coverage of added/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  /**
   *
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String COVERAGE_LINE_HITS_DATA_KEY = "coverage_line_hits_data";

  /**
   * Key-value pairs, where key - is a number of line, and value - is a number of hits for this line.
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> COVERAGE_LINE_HITS_DATA = new Metric.Builder(COVERAGE_LINE_HITS_DATA_KEY, "Coverage Hits by Line", Metric.ValueType.DATA)
    .setDescription("Coverage hits by line")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  public static final String CONDITIONS_TO_COVER_KEY = "conditions_to_cover";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric<Integer> CONDITIONS_TO_COVER = new Metric.Builder(CONDITIONS_TO_COVER_KEY, "Conditions to Cover", Metric.ValueType.INT)
    .setDescription("Conditions to cover")
    .setDomain(DOMAIN_COVERAGE)
    .create();

  public static final String NEW_CONDITIONS_TO_COVER_KEY = "new_conditions_to_cover";
  public static final Metric<Integer> NEW_CONDITIONS_TO_COVER = new Metric.Builder(NEW_CONDITIONS_TO_COVER_KEY, "Conditions to Cover on New Code", Metric.ValueType.INT)
    .setDescription("Conditions to cover on new code")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  public static final String UNCOVERED_CONDITIONS_KEY = "uncovered_conditions";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   */
  public static final Metric<Integer> UNCOVERED_CONDITIONS = new Metric.Builder(UNCOVERED_CONDITIONS_KEY, "Uncovered Conditions", Metric.ValueType.INT)
    .setDescription("Uncovered conditions")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .create();

  public static final String NEW_UNCOVERED_CONDITIONS_KEY = "new_uncovered_conditions";
  public static final Metric<Integer> NEW_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_UNCOVERED_CONDITIONS_KEY, "Uncovered Conditions on New Code", Metric.ValueType.INT)
    .setDescription("Uncovered conditions on new code")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setDeleteHistoricalData(true)
    .create();

  public static final String BRANCH_COVERAGE_KEY = "branch_coverage";
  public static final Metric<Double> BRANCH_COVERAGE = new Metric.Builder(BRANCH_COVERAGE_KEY, "Condition Coverage", Metric.ValueType.PERCENT)
    .setDescription("Condition coverage")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .create();

  public static final String NEW_BRANCH_COVERAGE_KEY = "new_branch_coverage";
  public static final Metric<Double> NEW_BRANCH_COVERAGE = new Metric.Builder(NEW_BRANCH_COVERAGE_KEY, "Condition Coverage on New Code", Metric.ValueType.PERCENT)
    .setDescription("Condition coverage of new/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String CONDITIONS_BY_LINE_KEY = "conditions_by_line";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   *
   * @since 2.7
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> CONDITIONS_BY_LINE = new Metric.Builder(CONDITIONS_BY_LINE_KEY, "Conditions by Line", Metric.ValueType.DATA)
    .setDescription("Conditions by line")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String COVERED_CONDITIONS_BY_LINE_KEY = "covered_conditions_by_line";

  /**
   * Use {@link CoverageMeasuresBuilder} to build measure for this metric.
   *
   * @since 2.7
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> COVERED_CONDITIONS_BY_LINE = new Metric.Builder(COVERED_CONDITIONS_BY_LINE_KEY, "Covered Conditions by Line", Metric.ValueType.DATA)
    .setDescription("Covered conditions by line")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // INTEGRATION TESTS
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_COVERAGE_KEY = "it_coverage";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> IT_COVERAGE = new Metric.Builder(IT_COVERAGE_KEY, "IT Coverage", Metric.ValueType.PERCENT)
    .setDescription("Integration tests coverage")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_COVERAGE_KEY = "new_it_coverage";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> NEW_IT_COVERAGE = new Metric.Builder(NEW_IT_COVERAGE_KEY, "Coverage by IT on New Code", Metric.ValueType.PERCENT)
    .setDescription("Integration tests coverage of new/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_LINES_TO_COVER_KEY = "it_lines_to_cover";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> IT_LINES_TO_COVER = new Metric.Builder(IT_LINES_TO_COVER_KEY, "IT Lines to Cover", Metric.ValueType.INT)
    .setDescription("Lines to cover by Integration Tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setDomain(DOMAIN_COVERAGE)
    .setQualitative(false)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_LINES_TO_COVER_KEY = "new_it_lines_to_cover";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_IT_LINES_TO_COVER = new Metric.Builder(NEW_IT_LINES_TO_COVER_KEY, "Lines to Cover by IT on New Code", Metric.ValueType.INT)
    .setDescription("Lines to cover on new code by integration tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_UNCOVERED_LINES_KEY = "it_uncovered_lines";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> IT_UNCOVERED_LINES = new Metric.Builder(IT_UNCOVERED_LINES_KEY, "IT Uncovered Lines", Metric.ValueType.INT)
    .setDescription("Uncovered lines by integration tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_UNCOVERED_LINES_KEY = "new_it_uncovered_lines";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_IT_UNCOVERED_LINES = new Metric.Builder(NEW_IT_UNCOVERED_LINES_KEY, "Uncovered Lines by IT on New Code", Metric.ValueType.INT)
    .setDescription("New lines that are not covered by integration tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_LINE_COVERAGE_KEY = "it_line_coverage";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> IT_LINE_COVERAGE = new Metric.Builder(IT_LINE_COVERAGE_KEY, "IT Line Coverage", Metric.ValueType.PERCENT)
    .setDescription("Line coverage by integration tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_LINE_COVERAGE_KEY = "new_it_line_coverage";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> NEW_IT_LINE_COVERAGE = new Metric.Builder(NEW_IT_LINE_COVERAGE_KEY, "Line Coverage by IT on New Code", Metric.ValueType.PERCENT)
    .setDescription("Integration tests line coverage of added/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String IT_COVERAGE_LINE_HITS_DATA_KEY = "it_coverage_line_hits_data";

  /**
   * @since 2.12
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> IT_COVERAGE_LINE_HITS_DATA = new Metric.Builder(IT_COVERAGE_LINE_HITS_DATA_KEY, "IT Coverage Hits by Line", Metric.ValueType.DATA)
    .setDescription("Coverage hits by line by integration tests")
    .setDirection(Metric.DIRECTION_NONE)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_CONDITIONS_TO_COVER_KEY = "it_conditions_to_cover";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> IT_CONDITIONS_TO_COVER = new Metric.Builder(IT_CONDITIONS_TO_COVER_KEY, "IT Branches to Cover", Metric.ValueType.INT)
    .setDescription("Integration Tests conditions to cover")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_CONDITIONS_TO_COVER_KEY = "new_it_conditions_to_cover";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_IT_CONDITIONS_TO_COVER = new Metric.Builder(NEW_IT_CONDITIONS_TO_COVER_KEY, "Branches to Cover by IT on New Code", Metric.ValueType.INT)
    .setDescription("Branches to cover by Integration Tests on New Code")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_UNCOVERED_CONDITIONS_KEY = "it_uncovered_conditions";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> IT_UNCOVERED_CONDITIONS = new Metric.Builder(IT_UNCOVERED_CONDITIONS_KEY, "IT Uncovered Conditions", Metric.ValueType.INT)
    .setDescription("Uncovered conditions by integration tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_UNCOVERED_CONDITIONS_KEY = "new_it_uncovered_conditions";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_IT_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_IT_UNCOVERED_CONDITIONS_KEY, "Uncovered Conditions by IT on New Code",
    Metric.ValueType.INT)
      .setDescription("New conditions that are not covered by integration tests")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_COVERAGE)
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .setHidden(true)
      .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String IT_BRANCH_COVERAGE_KEY = "it_branch_coverage";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> IT_BRANCH_COVERAGE = new Metric.Builder(IT_BRANCH_COVERAGE_KEY, "IT Condition Coverage", Metric.ValueType.PERCENT)
    .setDescription("Condition coverage by integration tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_IT_BRANCH_COVERAGE_KEY = "new_it_branch_coverage";

  /**
   * @since 2.12
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> NEW_IT_BRANCH_COVERAGE = new Metric.Builder(NEW_IT_BRANCH_COVERAGE_KEY, "Condition Coverage by IT on New Code", Metric.ValueType.PERCENT)
    .setDescription("Integration tests condition coverage of new/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String IT_CONDITIONS_BY_LINE_KEY = "it_conditions_by_line";

  /**
   * @since 2.12
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> IT_CONDITIONS_BY_LINE = new Metric.Builder(IT_CONDITIONS_BY_LINE_KEY, "IT Conditions by Line", Metric.ValueType.DATA)
    .setDescription("IT conditions by line")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @since 2.12
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String IT_COVERED_CONDITIONS_BY_LINE_KEY = "it_covered_conditions_by_line";

  /**
   * @since 2.12
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> IT_COVERED_CONDITIONS_BY_LINE = new Metric.Builder(IT_COVERED_CONDITIONS_BY_LINE_KEY, "IT Covered Conditions by Line", Metric.ValueType.DATA)
    .setDescription("IT covered conditions by line")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // OVERALL TESTS
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_COVERAGE_KEY = "overall_coverage";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> OVERALL_COVERAGE = new Metric.Builder(OVERALL_COVERAGE_KEY, "Overall Coverage", Metric.ValueType.PERCENT)
    .setDescription("Overall test coverage")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_COVERAGE_KEY = "new_overall_coverage";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> NEW_OVERALL_COVERAGE = new Metric.Builder(NEW_OVERALL_COVERAGE_KEY, "Overall Coverage on New Code", Metric.ValueType.PERCENT)
    .setDescription("Overall coverage of new/changed code")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_LINES_TO_COVER_KEY = "overall_lines_to_cover";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> OVERALL_LINES_TO_COVER = new Metric.Builder(OVERALL_LINES_TO_COVER_KEY, "Overall Lines to Cover", Metric.ValueType.INT)
    .setDescription("Overall lines to cover by all tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setDomain(DOMAIN_COVERAGE)
    .setQualitative(false)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_LINES_TO_COVER_KEY = "new_overall_lines_to_cover";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_OVERALL_LINES_TO_COVER = new Metric.Builder(NEW_OVERALL_LINES_TO_COVER_KEY, "Overall Lines to Cover on New Code", Metric.ValueType.INT)
    .setDescription("New lines to cover by all tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_UNCOVERED_LINES_KEY = "overall_uncovered_lines";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> OVERALL_UNCOVERED_LINES = new Metric.Builder(OVERALL_UNCOVERED_LINES_KEY, "Overall Uncovered Lines", Metric.ValueType.INT)
    .setDescription("Uncovered lines by all tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_UNCOVERED_LINES_KEY = "new_overall_uncovered_lines";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_OVERALL_UNCOVERED_LINES = new Metric.Builder(NEW_OVERALL_UNCOVERED_LINES_KEY, "Overall Uncovered Lines on New Code", Metric.ValueType.INT)
    .setDescription("New lines that are not covered by any tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setBestValue(0.0)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_LINE_COVERAGE_KEY = "overall_line_coverage";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> OVERALL_LINE_COVERAGE = new Metric.Builder(OVERALL_LINE_COVERAGE_KEY, "Overall Line Coverage", Metric.ValueType.PERCENT)
    .setDescription("Line coverage by all tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_LINE_COVERAGE_KEY = "new_overall_line_coverage";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> NEW_OVERALL_LINE_COVERAGE = new Metric.Builder(NEW_OVERALL_LINE_COVERAGE_KEY, "Overall Line Coverage on New Code", Metric.ValueType.PERCENT)
    .setDescription("Line coverage of added/changed code by all tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String OVERALL_COVERAGE_LINE_HITS_DATA_KEY = "overall_coverage_line_hits_data";

  /**
   * @since 3.3
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> OVERALL_COVERAGE_LINE_HITS_DATA = new Metric.Builder(OVERALL_COVERAGE_LINE_HITS_DATA_KEY, "Overall Coverage Hits by Line",
    Metric.ValueType.DATA)
      .setDescription("Coverage hits by all tests and by line")
      .setDirection(Metric.DIRECTION_NONE)
      .setQualitative(false)
      .setDomain(DOMAIN_COVERAGE)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_CONDITIONS_TO_COVER_KEY = "overall_conditions_to_cover";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> OVERALL_CONDITIONS_TO_COVER = new Metric.Builder(OVERALL_CONDITIONS_TO_COVER_KEY, "Overall Branches to Cover", Metric.ValueType.INT)
    .setDescription("Branches to cover by all tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(false)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_CONDITIONS_TO_COVER_KEY = "new_overall_conditions_to_cover";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_OVERALL_CONDITIONS_TO_COVER = new Metric.Builder(NEW_OVERALL_CONDITIONS_TO_COVER_KEY, "Overall Branches to Cover on New Code",
    Metric.ValueType.INT)
      .setDescription("New branches to cover by all tests")
      .setDomain(DOMAIN_COVERAGE)
      .setDeleteHistoricalData(true)
      .setHidden(true)
      .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_UNCOVERED_CONDITIONS_KEY = "overall_uncovered_conditions";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> OVERALL_UNCOVERED_CONDITIONS = new Metric.Builder(OVERALL_UNCOVERED_CONDITIONS_KEY, "Overall Uncovered Conditions", Metric.ValueType.INT)
    .setDescription("Uncovered conditions by all tests")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_COVERAGE)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_UNCOVERED_CONDITIONS_KEY = "new_overall_uncovered_conditions";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Integer> NEW_OVERALL_UNCOVERED_CONDITIONS = new Metric.Builder(NEW_OVERALL_UNCOVERED_CONDITIONS_KEY, "Overall Uncovered Conditions on New Code",
    Metric.ValueType.INT)
      .setDescription("New conditions that are not covered by any test")
      .setDirection(Metric.DIRECTION_WORST)
      .setDomain(DOMAIN_COVERAGE)
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .setHidden(true)
      .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String OVERALL_BRANCH_COVERAGE_KEY = "overall_branch_coverage";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> OVERALL_BRANCH_COVERAGE = new Metric.Builder(OVERALL_BRANCH_COVERAGE_KEY, "Overall Condition Coverage", Metric.ValueType.PERCENT)
    .setDescription("Condition coverage by all tests")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_COVERAGE)
    .setWorstValue(0.0)
    .setBestValue(100.0)
    .setHidden(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final String NEW_OVERALL_BRANCH_COVERAGE_KEY = "new_overall_branch_coverage";

  /**
   * @since 3.3
   * @deprecated since 6.2 all coverage reports are merged in the same measures 
   */
  @Deprecated
  public static final Metric<Double> NEW_OVERALL_BRANCH_COVERAGE = new Metric.Builder(NEW_OVERALL_BRANCH_COVERAGE_KEY, "Overall Condition Coverage on New Code",
    Metric.ValueType.PERCENT)
      .setDescription("Condition coverage of new/changed code by all tests")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(true)
      .setDomain(DOMAIN_COVERAGE)
      .setWorstValue(0.0)
      .setBestValue(100.0)
      .setDeleteHistoricalData(true)
      .setHidden(true)
      .create();

  /**
   * @since 3.3
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String OVERALL_CONDITIONS_BY_LINE_KEY = "overall_conditions_by_line";

  /**
   * @since 3.3
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> OVERALL_CONDITIONS_BY_LINE = new Metric.Builder(OVERALL_CONDITIONS_BY_LINE_KEY, "Overall Conditions by Line", Metric.ValueType.DATA)
    .setDescription("Overall conditions by all tests and by line")
    .setDomain(DOMAIN_COVERAGE)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @since 3.3
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final String OVERALL_COVERED_CONDITIONS_BY_LINE_KEY = "overall_covered_conditions_by_line";

  /**
   * @since 3.3
   * @deprecated since 5.2 soon to be removed
   */
  @Deprecated
  public static final Metric<String> OVERALL_COVERED_CONDITIONS_BY_LINE = new Metric.Builder(OVERALL_COVERED_CONDITIONS_BY_LINE_KEY, "Overall Covered Conditions by Line",
    Metric.ValueType.DATA)
      .setDescription("Overall covered conditions by all tests and by line")
      .setDomain(DOMAIN_COVERAGE)
      .setDeleteHistoricalData(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // DUPLICATIONS
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String DUPLICATED_LINES_KEY = "duplicated_lines";
  public static final Metric<Integer> DUPLICATED_LINES = new Metric.Builder(DUPLICATED_LINES_KEY, "Duplicated Lines", Metric.ValueType.INT)
    .setDescription("Duplicated lines")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 6.1
   */
  public static final String NEW_DUPLICATED_LINES_KEY = "new_duplicated_lines";

  /**
   * @since 6.1
   */
  public static final Metric<Integer> NEW_DUPLICATED_LINES = new Metric.Builder(NEW_DUPLICATED_LINES_KEY, "Duplicated Lines on New Code", Metric.ValueType.INT)
    .setDescription("Duplicated Lines on New Code")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setBestValue(0.0)
    .setDeleteHistoricalData(true)
    .create();

  public static final String DUPLICATED_BLOCKS_KEY = "duplicated_blocks";
  public static final Metric<Integer> DUPLICATED_BLOCKS = new Metric.Builder(DUPLICATED_BLOCKS_KEY, "Duplicated Blocks", Metric.ValueType.INT)
    .setDescription("Duplicated blocks")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 6.1
    */
  public static final String NEW_BLOCKS_DUPLICATED_KEY = "new_duplicated_blocks";
  /**
   * @since 6.1
   */
  public static final Metric<Integer> NEW_BLOCKS_DUPLICATED = new Metric.Builder(NEW_BLOCKS_DUPLICATED_KEY, "Duplicated Blocks on New Code", Metric.ValueType.INT)
    .setDescription("Duplicated blocks on new code")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setBestValue(0.0)
    .setDeleteHistoricalData(true)
    .create();

  public static final String DUPLICATED_FILES_KEY = "duplicated_files";

  /**
   * For files: if it contains duplicates, then 1, otherwise 0.
   * For other resources: amount of files under this resource with duplicates.
   */
  public static final Metric<Integer> DUPLICATED_FILES = new Metric.Builder(DUPLICATED_FILES_KEY, "Duplicated Files", Metric.ValueType.INT)
    .setDescription("Duplicated files")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String DUPLICATED_LINES_DENSITY_KEY = "duplicated_lines_density";

  public static final Metric<Double> DUPLICATED_LINES_DENSITY = new Metric.Builder(DUPLICATED_LINES_DENSITY_KEY, "Duplicated Lines (%)", Metric.ValueType.PERCENT)
    .setDescription("Duplicated lines balanced by statements")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setWorstValue(50.0)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 6.1
   */
  public static final String NEW_DUPLICATED_LINES_DENSITY_KEY = "new_duplicated_lines_density";

  /**
   * @since 6.1
   */
  public static final Metric<Integer> NEW_DUPLICATED_LINES_DENSITY = new Metric.Builder(NEW_DUPLICATED_LINES_DENSITY_KEY, "Duplicated Lines on New Code (%)",
    Metric.ValueType.PERCENT)
      .setDescription("Duplicated lines on new code balanced by statements")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setDomain(DOMAIN_DUPLICATIONS)
      .setBestValue(0.0)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @deprecated since 4.5. Internal storage of duplication is not an API.
   */
  @Deprecated
  public static final String DUPLICATIONS_DATA_KEY = "duplications_data";

  /**
   * Information about duplications, which is represented as an XML string.
   * <p>
   * Here is the format (since Sonar 2.12):
   * <pre>
   * {@literal
   * <duplications>
   *   <!-- Multiple groups: -->
   *   <g>
   *     <!-- Multiple blocks: -->
   *     <b r="[resource key]" s="[first line]" l="[number of lines]" />
   *     ...
   *   </g>
   *   ...
   * </duplications>
   * }
   * </pre>
   *
   * @deprecated since 4.5. Internal storage of duplication is not an API.
   */
  @Deprecated
  public static final Metric<String> DUPLICATIONS_DATA = new Metric.Builder(DUPLICATIONS_DATA_KEY, "Duplication Details", Metric.ValueType.DATA)
    .setDescription("Duplications details")
    .setDirection(Metric.DIRECTION_NONE)
    .setQualitative(false)
    .setDomain(DOMAIN_DUPLICATIONS)
    .setDeleteHistoricalData(true)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // CODING RULES
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String VIOLATIONS_KEY = "violations";
  public static final Metric<Integer> VIOLATIONS = new Metric.Builder(VIOLATIONS_KEY, "Issues", Metric.ValueType.INT)
    .setDescription("Issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String BLOCKER_VIOLATIONS_KEY = "blocker_violations";
  public static final Metric<Integer> BLOCKER_VIOLATIONS = new Metric.Builder(BLOCKER_VIOLATIONS_KEY, "Blocker Issues", Metric.ValueType.INT)
    .setDescription("Blocker issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String CRITICAL_VIOLATIONS_KEY = "critical_violations";
  public static final Metric<Integer> CRITICAL_VIOLATIONS = new Metric.Builder(CRITICAL_VIOLATIONS_KEY, "Critical Issues", Metric.ValueType.INT)
    .setDescription("Critical issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String MAJOR_VIOLATIONS_KEY = "major_violations";
  public static final Metric<Integer> MAJOR_VIOLATIONS = new Metric.Builder(MAJOR_VIOLATIONS_KEY, "Major Issues", Metric.ValueType.INT)
    .setDescription("Major issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String MINOR_VIOLATIONS_KEY = "minor_violations";
  public static final Metric<Integer> MINOR_VIOLATIONS = new Metric.Builder(MINOR_VIOLATIONS_KEY, "Minor Issues", Metric.ValueType.INT)
    .setDescription("Minor issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String INFO_VIOLATIONS_KEY = "info_violations";
  public static final Metric<Integer> INFO_VIOLATIONS = new Metric.Builder(INFO_VIOLATIONS_KEY, "Info Issues", Metric.ValueType.INT)
    .setDescription("Info issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  public static final String NEW_VIOLATIONS_KEY = "new_violations";
  public static final Metric<Integer> NEW_VIOLATIONS = new Metric.Builder(NEW_VIOLATIONS_KEY, "New Issues", Metric.ValueType.INT)
    .setDescription("New issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String NEW_BLOCKER_VIOLATIONS_KEY = "new_blocker_violations";
  public static final Metric<Integer> NEW_BLOCKER_VIOLATIONS = new Metric.Builder(NEW_BLOCKER_VIOLATIONS_KEY, "New Blocker Issues", Metric.ValueType.INT)
    .setDescription("New Blocker issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String NEW_CRITICAL_VIOLATIONS_KEY = "new_critical_violations";
  public static final Metric<Integer> NEW_CRITICAL_VIOLATIONS = new Metric.Builder(NEW_CRITICAL_VIOLATIONS_KEY, "New Critical Issues", Metric.ValueType.INT)
    .setDescription("New Critical issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String NEW_MAJOR_VIOLATIONS_KEY = "new_major_violations";
  public static final Metric<Integer> NEW_MAJOR_VIOLATIONS = new Metric.Builder(NEW_MAJOR_VIOLATIONS_KEY, "New Major Issues", Metric.ValueType.INT)
    .setDescription("New Major issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String NEW_MINOR_VIOLATIONS_KEY = "new_minor_violations";
  public static final Metric<Integer> NEW_MINOR_VIOLATIONS = new Metric.Builder(NEW_MINOR_VIOLATIONS_KEY, "New Minor Issues", Metric.ValueType.INT)
    .setDescription("New Minor issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  public static final String NEW_INFO_VIOLATIONS_KEY = "new_info_violations";
  public static final Metric<Integer> NEW_INFO_VIOLATIONS = new Metric.Builder(NEW_INFO_VIOLATIONS_KEY, "New Info Issues", Metric.ValueType.INT)
    .setDescription("New Info issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @since 3.6
   */
  public static final String FALSE_POSITIVE_ISSUES_KEY = "false_positive_issues";

  /**
   * @since 3.6
   */
  public static final Metric<Integer> FALSE_POSITIVE_ISSUES = new Metric.Builder(FALSE_POSITIVE_ISSUES_KEY, "False Positive Issues", Metric.ValueType.INT)
    .setDescription("False positive issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 5.6
   */
  public static final String WONT_FIX_ISSUES_KEY = "wont_fix_issues";

  /**
   * @since 5.6
   */
  public static final Metric<Integer> WONT_FIX_ISSUES = new Metric.Builder(WONT_FIX_ISSUES_KEY, "Won't Fix Issues", Metric.ValueType.INT)
    .setDescription("Won't fix issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 3.6
   */
  public static final String OPEN_ISSUES_KEY = "open_issues";

  /**
   * @since 3.6
   */
  public static final Metric<Integer> OPEN_ISSUES = new Metric.Builder(OPEN_ISSUES_KEY, "Open Issues", Metric.ValueType.INT)
    .setDescription("Open issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 3.6
   */
  public static final String REOPENED_ISSUES_KEY = "reopened_issues";

  /**
   * @since 3.6
   */
  public static final Metric<Integer> REOPENED_ISSUES = new Metric.Builder(REOPENED_ISSUES_KEY, "Reopened Issues", Metric.ValueType.INT)
    .setDescription("Reopened issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * @since 3.6
   */
  public static final String CONFIRMED_ISSUES_KEY = "confirmed_issues";

  /**
   * @since 3.6
   */
  public static final Metric<Integer> CONFIRMED_ISSUES = new Metric.Builder(CONFIRMED_ISSUES_KEY, "Confirmed Issues", Metric.ValueType.INT)
    .setDescription("Confirmed issues")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_ISSUES)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final String CODE_SMELLS_KEY = "code_smells";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final Metric<Integer> CODE_SMELLS = new Metric.Builder(CODE_SMELLS_KEY, "Code Smells", Metric.ValueType.INT)
    .setDescription("Code Smells")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final String NEW_CODE_SMELLS_KEY = "new_code_smells";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final Metric<Integer> NEW_CODE_SMELLS = new Metric.Builder(NEW_CODE_SMELLS_KEY, "New Code Smells", Metric.ValueType.INT)
    .setDescription("New Code Smells")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final String BUGS_KEY = "bugs";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final Metric<Integer> BUGS = new Metric.Builder(BUGS_KEY, "Bugs", Metric.ValueType.INT)
    .setDescription("Bugs")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_RELIABILITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final String NEW_BUGS_KEY = "new_bugs";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final Metric<Integer> NEW_BUGS = new Metric.Builder(NEW_BUGS_KEY, "New Bugs", Metric.ValueType.INT)
    .setDescription("New Bugs")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_RELIABILITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final String VULNERABILITIES_KEY = "vulnerabilities";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final Metric<Integer> VULNERABILITIES = new Metric.Builder(VULNERABILITIES_KEY, "Vulnerabilities", Metric.ValueType.INT)
    .setDescription("Vulnerabilities")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_SECURITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final String NEW_VULNERABILITIES_KEY = "new_vulnerabilities";

  /**
   * SonarQube Quality Model
   * @since 5.5
   */
  public static final Metric<Integer> NEW_VULNERABILITIES = new Metric.Builder(NEW_VULNERABILITIES_KEY, "New Vulnerabilities", Metric.ValueType.INT)
    .setDescription("New Vulnerabilities")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_SECURITY)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .setDeleteHistoricalData(true)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // DESIGN
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @deprecated since 5.0 this is an internal metric that should not be accessed by plugins
   */
  @Deprecated
  public static final String DEPENDENCY_MATRIX_KEY = "dsm";
  /**
   * @deprecated since 5.0 this is an internal metric that should not be accessed by plugins
   */
  @Deprecated
  public static final transient Metric<String> DEPENDENCY_MATRIX = new Metric.Builder(DEPENDENCY_MATRIX_KEY, "Dependency Matrix", Metric.ValueType.DATA)
    .setDescription("Dependency Matrix")
    .setDirection(Metric.DIRECTION_NONE)
    .setQualitative(false)
    .setDomain(DOMAIN_DESIGN)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String DIRECTORY_CYCLES_KEY = "package_cycles";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> DIRECTORY_CYCLES = new Metric.Builder(DIRECTORY_CYCLES_KEY, "Directory Cycles", Metric.ValueType.INT)
    .setDescription("Directory cycles")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DESIGN)
    .setBestValue(0.0)
    .create();

  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_CYCLES_KEY}
   */
  @Deprecated
  public static final String PACKAGE_CYCLES_KEY = DIRECTORY_CYCLES_KEY;
  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_CYCLES}
   */
  @Deprecated
  public static final transient Metric<Integer> PACKAGE_CYCLES = DIRECTORY_CYCLES;

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String DIRECTORY_TANGLE_INDEX_KEY = "package_tangle_index";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Double> DIRECTORY_TANGLE_INDEX = new Metric.Builder(DIRECTORY_TANGLE_INDEX_KEY, "Directory Tangle Index", Metric.ValueType.PERCENT)
    .setDescription("Directory tangle index")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(0.0)
    .setDomain(DOMAIN_DESIGN)
    .create();

  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_TANGLE_INDEX_KEY}
   */
  @Deprecated
  public static final String PACKAGE_TANGLE_INDEX_KEY = DIRECTORY_TANGLE_INDEX_KEY;
  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_TANGLE_INDEX}
   */
  @Deprecated
  public static final transient Metric<Double> PACKAGE_TANGLE_INDEX = DIRECTORY_TANGLE_INDEX;

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String DIRECTORY_TANGLES_KEY = "package_tangles";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> DIRECTORY_TANGLES = new Metric.Builder(DIRECTORY_TANGLES_KEY, "File Dependencies to Cut", Metric.ValueType.INT)
    .setDescription("File dependencies to cut")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_DESIGN)
    .create();

  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_TANGLES_KEY}
   */
  @Deprecated
  public static final String PACKAGE_TANGLES_KEY = DIRECTORY_TANGLES_KEY;
  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_TANGLES}
   */
  @Deprecated
  public static final transient Metric<Integer> PACKAGE_TANGLES = DIRECTORY_TANGLES;

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String DIRECTORY_FEEDBACK_EDGES_KEY = "package_feedback_edges";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> DIRECTORY_FEEDBACK_EDGES = new Metric.Builder(DIRECTORY_FEEDBACK_EDGES_KEY, "Package Dependencies to Cut", Metric.ValueType.INT)
    .setDescription("Package dependencies to cut")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_DESIGN)
    .setBestValue(0.0)
    .create();

  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_FEEDBACK_EDGES_KEY}
   */
  @Deprecated
  public static final String PACKAGE_FEEDBACK_EDGES_KEY = DIRECTORY_FEEDBACK_EDGES_KEY;
  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_FEEDBACK_EDGES}
   */
  @Deprecated
  public static final transient Metric<Integer> PACKAGE_FEEDBACK_EDGES = DIRECTORY_FEEDBACK_EDGES;

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String DIRECTORY_EDGES_WEIGHT_KEY = "package_edges_weight";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> DIRECTORY_EDGES_WEIGHT = new Metric.Builder(DIRECTORY_EDGES_WEIGHT_KEY, "Directory Edges Weight", Metric.ValueType.INT)
    .setDescription("Directory edges weight")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(false)
    .setDomain(DOMAIN_DESIGN)
    .setHidden(true)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_EDGES_WEIGHT_KEY}
   */
  @Deprecated
  public static final String PACKAGE_EDGES_WEIGHT_KEY = DIRECTORY_EDGES_WEIGHT_KEY;
  /**
   * @deprecated since 5.0 use {@link #DIRECTORY_EDGES_WEIGHT}
   */
  @Deprecated
  public static final transient Metric<Integer> PACKAGE_EDGES_WEIGHT = DIRECTORY_EDGES_WEIGHT;

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String FILE_CYCLES_KEY = "file_cycles";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> FILE_CYCLES = new Metric.Builder(FILE_CYCLES_KEY, "File Cycles", Metric.ValueType.INT)
    .setDescription("File cycles")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DESIGN)
    .setHidden(true)
    .setDeleteHistoricalData(true)
    .setBestValue(0.0)
    .create();

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String FILE_TANGLE_INDEX_KEY = "file_tangle_index";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Double> FILE_TANGLE_INDEX = new Metric.Builder(FILE_TANGLE_INDEX_KEY, "File Tangle Index", Metric.ValueType.PERCENT)
    .setDescription("File tangle index")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setDomain(DOMAIN_DESIGN)
    .setHidden(true)
    .setDeleteHistoricalData(true)
    .setBestValue(0.0)
    .create();

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String FILE_TANGLES_KEY = "file_tangles";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> FILE_TANGLES = new Metric.Builder(FILE_TANGLES_KEY, "File Tangles", Metric.ValueType.INT)
    .setDescription("Files tangles")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_DESIGN)
    .setHidden(true)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String FILE_FEEDBACK_EDGES_KEY = "file_feedback_edges";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> FILE_FEEDBACK_EDGES = new Metric.Builder(FILE_FEEDBACK_EDGES_KEY, "Suspect File Dependencies", Metric.ValueType.INT)
    .setDescription("Suspect file dependencies")
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(false)
    .setDomain(DOMAIN_DESIGN)
    .setHidden(true)
    .setDeleteHistoricalData(true)
    .setBestValue(0.0)
    .create();

  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final String FILE_EDGES_WEIGHT_KEY = "file_edges_weight";
  /**
   * @deprecated since 5.2 No more design features
   */
  @Deprecated
  public static final transient Metric<Integer> FILE_EDGES_WEIGHT = new Metric.Builder(FILE_EDGES_WEIGHT_KEY, "File Edges Weight", Metric.ValueType.INT)
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
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.7
   * @deprecated since 5.0 SCM data will no more be stored as measures
   */
  @Deprecated
  public static final String SCM_AUTHORS_BY_LINE_KEY = "authors_by_line";

  /**
   * Key-value pairs, where key - is a number of line, and value - is an author for this line.
   *
   * @see org.sonar.api.utils.KeyValueFormat#formatIntString(java.util.Map)
   * @see org.sonar.api.utils.KeyValueFormat#parseIntString(String)
   * @since 2.7
   * @deprecated since 5.0 SCM data will no more be stored as measures
   */
  @Deprecated
  public static final transient Metric<String> SCM_AUTHORS_BY_LINE = new Metric.Builder(SCM_AUTHORS_BY_LINE_KEY, "Authors by Line", Metric.ValueType.DATA)
    .setDomain(DOMAIN_SCM)
    .create();

  /**
   * @since 2.7
   * @deprecated since 5.0 SCM data will no more be stored as measures
   */
  @Deprecated
  public static final String SCM_REVISIONS_BY_LINE_KEY = "revisions_by_line";

  /**
   * Key-value pairs, where key - is a number of line, and value - is a revision for this line.
   *
   * @see org.sonar.api.utils.KeyValueFormat#formatIntString(java.util.Map)
   * @see org.sonar.api.utils.KeyValueFormat#parseIntString(String)
   * @since 2.7
   * @deprecated since 5.0 SCM data will no more be stored as measures
   */
  @Deprecated
  public static final transient Metric<String> SCM_REVISIONS_BY_LINE = new Metric.Builder(SCM_REVISIONS_BY_LINE_KEY, "Revisions by Line", Metric.ValueType.DATA)
    .setDomain(DOMAIN_SCM)
    .create();

  /**
   * @since 2.7
   * @deprecated since 5.0 SCM data will no more be stored as measures
   */
  @Deprecated
  public static final String SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY = "last_commit_datetimes_by_line";

  /**
   * Key-value pairs, where key - is a number of line, and value - is a date of last commit for this line.
   *
   * @see org.sonar.api.utils.KeyValueFormat#formatIntDateTime(java.util.Map)
   * @see org.sonar.api.utils.KeyValueFormat#parseIntDateTime(String)
   * @since 2.7
   * @deprecated since 5.0 SCM data will no more be stored as measures
   */
  @Deprecated
  public static final transient Metric<String> SCM_LAST_COMMIT_DATETIMES_BY_LINE = new Metric.Builder(SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY, "Last Commit Dates by Line",
    Metric.ValueType.DATA)
      .setDomain(DOMAIN_SCM)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // MAINTAINABILITY CHARACTERISTIC
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 4.0
   */
  // TODO should be renamed to MAINTAINABILITY_REMEDIATION_EFFORT_KEY = "maintainability_remediation_effort"
  public static final String TECHNICAL_DEBT_KEY = "sqale_index";

  /**
   * @since 4.0
   */
  // TODO should be renamed to MAINTAINABILITY_REMEDIATION_EFFORT
  public static final Metric<Long> TECHNICAL_DEBT = new Metric.Builder(TECHNICAL_DEBT_KEY, "Technical Debt", Metric.ValueType.WORK_DUR)
    .setDescription("Total effort (in days) to fix all the issues on the component and therefore to comply to all the requirements.")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .create();

  /**
   * @since 4.1
   */
  // TODO should be renamed to NEW_MAINTAINABILITY_REMEDIATION_EFFORT_KEY = "new_maintainability_remediation_effort"
  public static final String NEW_TECHNICAL_DEBT_KEY = "new_technical_debt";

  /**
   * @since 4.1
   */
  // TODO should be renamed to NEW_MAINTAINABILITY_REMEDIATION_EFFORT
  public static final Metric<Long> NEW_TECHNICAL_DEBT = new Metric.Builder(NEW_TECHNICAL_DEBT_KEY, "Added Technical Debt", Metric.ValueType.WORK_DUR)
    .setDescription("Added technical debt")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .setDeleteHistoricalData(true)
    .create();

  /**
   * @since 4.5
   */
  // TODO should be renamed to MAINTAINABILITY_RATING_KEY = "maintainability_rating"
  public static final String SQALE_RATING_KEY = "sqale_rating";

  /**
   * @since 4.5
   */
  // TODO should be renamed to MAINTAINABILITY_RATING
  public static final Metric<Integer> SQALE_RATING = new Metric.Builder(SQALE_RATING_KEY, "Maintainability Rating", Metric.ValueType.RATING)
    .setDescription("A-to-E rating based on the technical debt ratio")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  /**
   * @since 6.2
   */
  public static final String NEW_MAINTAINABILITY_RATING_KEY = "new_maintainability_rating";

  /**
   * @since 6.2
   */
  public static final Metric<Integer> NEW_MAINTAINABILITY_RATING = new Metric.Builder(NEW_MAINTAINABILITY_RATING_KEY, "Maintainability Rating on New Code", Metric.ValueType.RATING)
    .setDescription("Maintainability rating on new code")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  /**
   * @since 4.5
   */
  public static final String DEVELOPMENT_COST_KEY = "development_cost";

  /**
   * @since 4.5
   */
  public static final Metric<String> DEVELOPMENT_COST = new Metric.Builder(DEVELOPMENT_COST_KEY, "Development Cost", Metric.ValueType.STRING)
    .setDescription("Development cost")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .setHidden(true)
    .create();

  /**
   * @since 7.0
   */
  public static final String NEW_DEVELOPMENT_COST_KEY = "new_development_cost";

  /**
   * @since 7.0
   */
  public static final Metric<String> NEW_DEVELOPMENT_COST = new Metric.Builder(NEW_DEVELOPMENT_COST_KEY, "Development Cost on New Code", Metric.ValueType.STRING)
    .setDescription("Development cost on new code")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .setHidden(true)
    .create();

  /**
   * @since 4.5
   */
  // TODO should be renamed to TECHNICALDEBT_RATIO_KEY = "technicaldebt_ratio"
  public static final String SQALE_DEBT_RATIO_KEY = "sqale_debt_ratio";

  /**
   * @since 4.5
   */
  // TODO should be renamed to TECHNICALDEBT_RATIO
  public static final Metric<Double> SQALE_DEBT_RATIO = new Metric.Builder(SQALE_DEBT_RATIO_KEY, "Technical Debt Ratio", Metric.ValueType.PERCENT)
    .setDescription("Ratio of the actual technical debt compared to the estimated cost to develop the whole source code from scratch")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .create();

  /**
   * @since 5.2
   */
  // TODO should be renamed to TECHNICALDEBT_RATIO_ON_NEW_CODE_KEY = "technicaldebt_ratio_on_new_code"
  public static final String NEW_SQALE_DEBT_RATIO_KEY = "new_sqale_debt_ratio";

  /**
   * @since 5.2
   */
  // TODO should be renamed to TECHNICALDEBT_RATIO_ON_NEW_CODE
  public static final Metric<Double> NEW_SQALE_DEBT_RATIO = new Metric.Builder(NEW_SQALE_DEBT_RATIO_KEY, "Technical Debt Ratio on New Code", Metric.ValueType.PERCENT)
    .setDescription("Technical Debt Ratio of new/changed code.")
    .setDomain(DOMAIN_MAINTAINABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .create();

  /**
   * @since 5.5
   */
  public static final String EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY = "effort_to_reach_maintainability_rating_a";

  /**
   * @since 5.5
   */
  public static final Metric<Long> EFFORT_TO_REACH_MAINTAINABILITY_RATING_A = new Metric.Builder(EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_KEY,
    "Effort to Reach Maintainability Rating A", Metric.ValueType.WORK_DUR)
      .setDescription("Effort to reach maintainability rating A")
      .setDomain(DOMAIN_MAINTAINABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(true)
      .setBestValue(0.0)
      .setOptimizedBestValue(true)
      .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // RELIABILITY CHARACTERISTIC
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 5.5
   */
  public static final String RELIABILITY_REMEDIATION_EFFORT_KEY = "reliability_remediation_effort";

  /**
   * @since 5.5
   */
  public static final Metric<Long> RELIABILITY_REMEDIATION_EFFORT = new Metric.Builder(RELIABILITY_REMEDIATION_EFFORT_KEY, "Reliability Remediation Effort",
    Metric.ValueType.WORK_DUR)
      .setDescription("Reliability Remediation Effort")
      .setDomain(DOMAIN_RELIABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .create();

  /**
   * @since 5.5
   */
  public static final String NEW_RELIABILITY_REMEDIATION_EFFORT_KEY = "new_reliability_remediation_effort";

  /**
   * @since 5.5
   */
  public static final Metric<Long> NEW_RELIABILITY_REMEDIATION_EFFORT = new Metric.Builder(NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, "Reliability Remediation Effort on New Code",
    Metric.ValueType.WORK_DUR)
      .setDescription("Reliability remediation effort on new code")
      .setDomain(DOMAIN_RELIABILITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 5.5
   */
  public static final String RELIABILITY_RATING_KEY = "reliability_rating";

  /**
   * @since 5.5
   */
  public static final Metric<Integer> RELIABILITY_RATING = new Metric.Builder(RELIABILITY_RATING_KEY, "Reliability Rating", Metric.ValueType.RATING)
    .setDescription("Reliability rating")
    .setDomain(DOMAIN_RELIABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  /**
   * @since 6.2
   */
  public static final String NEW_RELIABILITY_RATING_KEY = "new_reliability_rating";

  /**
   * @since 6.2
   */
  public static final Metric<Integer> NEW_RELIABILITY_RATING = new Metric.Builder(NEW_RELIABILITY_RATING_KEY, "Reliability Rating on New Code", Metric.ValueType.RATING)
    .setDescription("Reliability rating on new code")
    .setDomain(DOMAIN_RELIABILITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // SECURITY CHARACTERISTIC
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 5.5
   */
  public static final String SECURITY_REMEDIATION_EFFORT_KEY = "security_remediation_effort";

  /**
   * @since 5.5
   */
  public static final Metric<Long> SECURITY_REMEDIATION_EFFORT = new Metric.Builder(SECURITY_REMEDIATION_EFFORT_KEY, "Security Remediation Effort", Metric.ValueType.WORK_DUR)
    .setDescription("Security remediation effort")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setOptimizedBestValue(true)
    .setBestValue(0.0)
    .setQualitative(true)
    .create();

  /**
   * @since 5.5
   */
  public static final String NEW_SECURITY_REMEDIATION_EFFORT_KEY = "new_security_remediation_effort";

  /**
   * @since 5.5
   */
  public static final Metric<Long> NEW_SECURITY_REMEDIATION_EFFORT = new Metric.Builder(NEW_SECURITY_REMEDIATION_EFFORT_KEY, "Security Remediation Effort on New Code",
    Metric.ValueType.WORK_DUR)
      .setDescription("Security remediation effort on new code")
      .setDomain(DOMAIN_SECURITY)
      .setDirection(Metric.DIRECTION_WORST)
      .setOptimizedBestValue(true)
      .setBestValue(0.0)
      .setQualitative(true)
      .setDeleteHistoricalData(true)
      .create();

  /**
   * @since 5.5
   */
  public static final String SECURITY_RATING_KEY = "security_rating";

  /**
   * @since 5.5
   */
  public static final Metric<Integer> SECURITY_RATING = new Metric.Builder(SECURITY_RATING_KEY, "Security Rating", Metric.ValueType.RATING)
    .setDescription("Security rating")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  /**
   * @since 6.2
   */
  public static final String NEW_SECURITY_RATING_KEY = "new_security_rating";

  /**
   * @since 6.2
   */
  public static final Metric<Integer> NEW_SECURITY_RATING = new Metric.Builder(NEW_SECURITY_RATING_KEY, "Security Rating on New Code", Metric.ValueType.RATING)
    .setDescription("Security rating on new code")
    .setDomain(DOMAIN_SECURITY)
    .setDirection(Metric.DIRECTION_WORST)
    .setDeleteHistoricalData(true)
    .setOptimizedBestValue(true)
    .setQualitative(true)
    .setBestValue(1.0)
    .setWorstValue(5.0)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // FILE DATA
  //
  // --------------------------------------------------------------------------------------------------------------------

  /**
   * @since 2.14
   */
  public static final String NCLOC_DATA_KEY = "ncloc_data";

  /**
   * Information about lines of code in file.
   * Key-value pairs, where key - is a number of line, and value - is an indicator of whether line contains code (1) or not (0).
   * If a line number is missing in the map it is equivalent to the default value (0).
   *
   * @see org.sonar.api.measures.FileLinesContext
   * @since 2.14
   */
  public static final Metric<String> NCLOC_DATA = new Metric.Builder(NCLOC_DATA_KEY, "ncloc_data", Metric.ValueType.DATA)
    .setHidden(true)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * @since 2.14
   */
  public static final String COMMENT_LINES_DATA_KEY = "comment_lines_data";

  /**
   * Information about comments in file.
   * Key-value pairs, where key - is a number of line, and value - is an indicator of whether line contains comment (1) or not (0).
   * If a line number is missing in the map it is equivalent to the default value (0).
   *
   * @see org.sonar.api.measures.FileLinesContext
   * @since 2.14
   */
  public static final Metric<String> COMMENT_LINES_DATA = new Metric.Builder(COMMENT_LINES_DATA_KEY, "comment_lines_data", Metric.ValueType.DATA)
    .setHidden(true)
    .setDomain(DOMAIN_SIZE)
    .create();

  /**
   * @since 5.5
   */
  public static final String EXECUTABLE_LINES_DATA_KEY = "executable_lines_data";

  /**
   * Information about executable lines of code in file.
   * Key-value pairs, where key - is a number of line, and value - is an indicator of whether line contains executable code (1) or not (0).
   * If a line number is missing in the map it is equivalent to the default value (0).
   *
   * @see org.sonar.api.measures.FileLinesContext
   * @since 5.5
   */
  public static final Metric<String> EXECUTABLE_LINES_DATA = new Metric.Builder(EXECUTABLE_LINES_DATA_KEY, "executable_lines_data", Metric.ValueType.DATA)
    .setHidden(true)
    .setDomain(DOMAIN_COVERAGE)
    .create();

  // --------------------------------------------------------------------------------------------------------------------
  //
  // OTHERS
  //
  // --------------------------------------------------------------------------------------------------------------------

  public static final String ALERT_STATUS_KEY = "alert_status";
  public static final Metric<Metric.Level> ALERT_STATUS = new Metric.Builder(ALERT_STATUS_KEY, "Quality Gate Status", Metric.ValueType.LEVEL)
    .setDescription("The project status with regard to its quality gate.")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(true)
    .setDomain(DOMAIN_RELEASABILITY)
    .create();

  /**
   * @since 4.4
   */
  public static final String QUALITY_GATE_DETAILS_KEY = "quality_gate_details";
  /**
   * The project detailed status with regard to its quality gate.
   * Storing the global quality gate status, along with all evaluated conditions, into a JSON object.
   * @since 4.4
   */
  public static final Metric<String> QUALITY_GATE_DETAILS = new Metric.Builder(QUALITY_GATE_DETAILS_KEY, "Quality Gate Details", Metric.ValueType.DATA)
    .setDescription("The project detailed status with regard to its quality gate")
    .setDomain(DOMAIN_GENERAL)
    .create();

  /**
   * @since 4.4
   * @deprecated since 5.5
   */
  @Deprecated
  public static final String QUALITY_PROFILES_KEY = "quality_profiles";

  /**
   * @since 4.4
   * @deprecated since 5.5
   */
  @Deprecated
  public static final Metric<String> QUALITY_PROFILES = new Metric.Builder(QUALITY_PROFILES_KEY, "Profiles", Metric.ValueType.DATA)
    .setDescription("Details of quality profiles used during analysis")
    .setQualitative(false)
    .setDomain(DOMAIN_GENERAL)
    .setHidden(true)
    .create();

  /**
   * @since 5.2
   */
  public static final String LAST_COMMIT_DATE_KEY = "last_commit_date";

  /**
   * Date of the most recent commit. Current implementation is based on commits touching lines of source code. It
   * ignores other changes like file renaming or file deletion.
   * @since 5.2
   */
  public static final Metric LAST_COMMIT_DATE = new Metric.Builder(LAST_COMMIT_DATE_KEY, "Date of Last Commit", Metric.ValueType.MILLISEC)
    .setDomain(CoreMetrics.DOMAIN_SCM)
    // waiting for type "datetime" to be correctly handled
    .setHidden(true)
    .create();

  private static final List<Metric> METRICS;

  static {
    METRICS = new LinkedList<>();
    for (Field field : CoreMetrics.class.getFields()) {
      if (!Modifier.isTransient(field.getModifiers()) && Metric.class.isAssignableFrom(field.getType())) {
        try {
          Metric metric = (Metric) field.get(null);
          METRICS.add(metric);
        } catch (IllegalAccessException e) {
          throw new SonarException("can not introspect " + CoreMetrics.class + " to get metrics", e);
        }
      }
    }
  }

  private CoreMetrics() {
    // only static stuff
  }

  public static List<Metric> getMetrics() {
    return METRICS;
  }

  public static Metric getMetric(final String key) {
    return METRICS.stream().filter(metric -> metric != null && metric.getKey().equals(key)).findFirst().orElseThrow(NoSuchElementException::new);
  }
}
