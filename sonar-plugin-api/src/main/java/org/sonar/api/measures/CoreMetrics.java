/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.sonar.api.utils.SonarException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @since 1.10
 */
public final class CoreMetrics {

  private CoreMetrics() {
    // only static stuff
  }

  public static final String DOMAIN_SIZE = "Size";
  public static final String DOMAIN_TESTS = "Tests";
  public static final String DOMAIN_COMPLEXITY = "Complexity";
  public static final String DOMAIN_DOCUMENTATION = "Documentation";
  public static final String DOMAIN_RULES = "Rules";
  public static final String DOMAIN_RULE_CATEGORIES = "Rule categories";
  public static final String DOMAIN_GENERAL = "General";
  public static final String DOMAIN_DUPLICATION = "Duplication";
  public static final String DOMAIN_DESIGN = "Design";

  public static final String LINES_KEY = "lines";
  public static final Metric LINES = new Metric(LINES_KEY, "Lines", "Lines", Metric.ValueType.INT, Metric.DIRECTION_WORST, false,
      DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String GENERATED_LINES_KEY = "generated_lines";
  public static final Metric GENERATED_LINES = new Metric(GENERATED_LINES_KEY, "Generated Lines", "Number of generated lines",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_SIZE).setBestValue(0.0).setOptimizedBestValue(true).setFormula(
      new SumChildValuesFormula(false));

  public static final String NCLOC_KEY = "ncloc";
  public static final Metric NCLOC = new Metric(NCLOC_KEY, "Lines of code", "Non Commenting Lines of Code", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String GENERATED_NCLOC_KEY = "generated_ncloc";
  public static final Metric GENERATED_NCLOC = new Metric(GENERATED_NCLOC_KEY, "Generated lines of code",
      "Generated non Commenting Lines of Code", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_SIZE).setBestValue(0.0)
      .setOptimizedBestValue(true).setFormula(new SumChildValuesFormula(false));

  public static final String CLASSES_KEY = "classes";
  public static final Metric CLASSES = new Metric(CLASSES_KEY, "Classes", "Classes", Metric.ValueType.INT, Metric.DIRECTION_WORST, false,
      DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String FILES_KEY = "files";
  public static final Metric FILES = new Metric(FILES_KEY, "Files", "Number of files", Metric.ValueType.INT, Metric.DIRECTION_WORST, false,
      DOMAIN_SIZE);

  public static final String DIRECTORIES_KEY = "directories";
  public static final Metric DIRECTORIES = new Metric(DIRECTORIES_KEY, "Directories", "Directories", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_SIZE);

  public static final String PACKAGES_KEY = "packages";
  public static final Metric PACKAGES = new Metric(PACKAGES_KEY, "Packages", "Packages", Metric.ValueType.INT, Metric.DIRECTION_WORST,
      false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String FUNCTIONS_KEY = "functions";
  public static final Metric FUNCTIONS = new Metric(FUNCTIONS_KEY, "Methods", "Methods", Metric.ValueType.INT, Metric.DIRECTION_WORST,
      false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String ACCESSORS_KEY = "accessors";
  public static final Metric ACCESSORS = new Metric(ACCESSORS_KEY, "Accessors", "Accessors", Metric.ValueType.INT, Metric.DIRECTION_WORST,
      false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String PARAGRAPHS_KEY = "paragraphs";
  public static final Metric PARAGRAPHS = new Metric(PARAGRAPHS_KEY, "Paragraphs", "Number of paragraphs", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String STATEMENTS_KEY = "statements";
  public static final Metric STATEMENTS = new Metric(STATEMENTS_KEY, "Statements", "Number of statements", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String PUBLIC_API_KEY = "public_api";
  public static final Metric PUBLIC_API = new Metric(PUBLIC_API_KEY, "Public API", "Public API", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_SIZE).setFormula(new SumChildValuesFormula(false));

  public static final String COMPLEXITY_KEY = "complexity";
  public static final Metric COMPLEXITY = new Metric(COMPLEXITY_KEY, "Complexity", "Cyclomatic complexity", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_COMPLEXITY).setFormula(new SumChildValuesFormula(false));

  public static final String CLASS_COMPLEXITY_KEY = "class_complexity";
  public static final Metric CLASS_COMPLEXITY = new Metric(CLASS_COMPLEXITY_KEY, "Complexity /class", "Complexity average by class",
      Metric.ValueType.FLOAT, Metric.DIRECTION_WORST, true, DOMAIN_COMPLEXITY)
      .setFormula(new AverageComplexityFormula(CoreMetrics.CLASSES));

  public static final String FUNCTION_COMPLEXITY_KEY = "function_complexity";
  public static final Metric FUNCTION_COMPLEXITY = new Metric(FUNCTION_COMPLEXITY_KEY, "Complexity /method",
      "Complexity average by method", Metric.ValueType.FLOAT, Metric.DIRECTION_WORST, true, DOMAIN_COMPLEXITY)
      .setFormula(new AverageComplexityFormula(CoreMetrics.FUNCTIONS));

  public static final String FILE_COMPLEXITY_KEY = "file_complexity";
  public static final Metric FILE_COMPLEXITY = new Metric(FILE_COMPLEXITY_KEY, "Complexity /file", "Complexity average by file",
      Metric.ValueType.FLOAT, Metric.DIRECTION_WORST, true, DOMAIN_COMPLEXITY).setFormula(new AverageComplexityFormula(CoreMetrics.FILES));

  public static final String PARAGRAPH_COMPLEXITY_KEY = "paragraph_complexity";
  public static final Metric PARAGRAPH_COMPLEXITY = new Metric(PARAGRAPH_COMPLEXITY_KEY, "Complexity /paragraph",
      "Complexity average by paragraph", Metric.ValueType.FLOAT, Metric.DIRECTION_WORST, true, DOMAIN_COMPLEXITY)
      .setFormula(new AverageComplexityFormula(CoreMetrics.PARAGRAPHS));

  public static final String CLASS_COMPLEXITY_DISTRIBUTION_KEY = "class_complexity_distribution";
  public static final Metric CLASS_COMPLEXITY_DISTRIBUTION = new Metric(CLASS_COMPLEXITY_DISTRIBUTION_KEY,
      "Classes distribution /complexity", "Classes distribution /complexity", Metric.ValueType.DISTRIB, Metric.DIRECTION_NONE, true,
      DOMAIN_COMPLEXITY).setFormula(new SumChildDistributionFormula());

  public static final String FUNCTION_COMPLEXITY_DISTRIBUTION_KEY = "function_complexity_distribution";
  public static final Metric FUNCTION_COMPLEXITY_DISTRIBUTION = new Metric(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY,
      "Functions distribution /complexity", "Functions distribution /complexity", Metric.ValueType.DISTRIB, Metric.DIRECTION_NONE, true,
      DOMAIN_COMPLEXITY).setFormula(new SumChildDistributionFormula());

  public static final String FILE_COMPLEXITY_DISTRIBUTION_KEY = "file_complexity_distribution";
  public static final Metric FILE_COMPLEXITY_DISTRIBUTION = new Metric(FILE_COMPLEXITY_DISTRIBUTION_KEY, "Files distribution /complexity",
      "Files distribution /complexity", Metric.ValueType.DISTRIB, Metric.DIRECTION_NONE, true, DOMAIN_COMPLEXITY)
      .setFormula(new SumChildDistributionFormula());

  public static final String PARAGRAPH_COMPLEXITY_DISTRIBUTION_KEY = "paragraph_complexity_distribution";
  public static final Metric PARAGRAPH_COMPLEXITY_DISTRIBUTION = new Metric(PARAGRAPH_COMPLEXITY_DISTRIBUTION_KEY,
      "Paragraph distribution /complexity", "Paragraph distribution /complexity", Metric.ValueType.DISTRIB, Metric.DIRECTION_NONE, true,
      DOMAIN_COMPLEXITY).setFormula(new SumChildDistributionFormula());

  public static final String COMMENT_LINES_KEY = "comment_lines";
  public static final Metric COMMENT_LINES = new Metric(COMMENT_LINES_KEY, "Comment lines", "Number of comment lines",
      Metric.ValueType.INT, Metric.DIRECTION_BETTER, false, DOMAIN_DOCUMENTATION).setFormula(new SumChildValuesFormula(false));

  public static final String COMMENT_LINES_DENSITY_KEY = "comment_lines_density";
  public static final Metric COMMENT_LINES_DENSITY = new Metric(COMMENT_LINES_DENSITY_KEY, "Comments (%)",
      "Comments balanced by ncloc + comment lines", Metric.ValueType.PERCENT, Metric.DIRECTION_BETTER, true, DOMAIN_DOCUMENTATION);

  public static final String COMMENT_BLANK_LINES_KEY = "comment_blank_lines";
  public static final Metric COMMENT_BLANK_LINES = new Metric(COMMENT_BLANK_LINES_KEY, "Blank comments",
      "Comments that do not contain comments", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, CoreMetrics.DOMAIN_DOCUMENTATION)
      .setFormula(new SumChildValuesFormula(false)).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String PUBLIC_DOCUMENTED_API_DENSITY_KEY = "public_documented_api_density";
  public static final Metric PUBLIC_DOCUMENTED_API_DENSITY = new Metric(PUBLIC_DOCUMENTED_API_DENSITY_KEY, "Public documented API (%)",
      "Public documented classes and methods balanced by ncloc", Metric.ValueType.PERCENT, Metric.DIRECTION_BETTER, true,
      DOMAIN_DOCUMENTATION).setWorstValue(0.0).setBestValue(100.0).setOptimizedBestValue(true);

  public static final String PUBLIC_UNDOCUMENTED_API_KEY = "public_undocumented_api";
  public static final Metric PUBLIC_UNDOCUMENTED_API = new Metric(PUBLIC_UNDOCUMENTED_API_KEY, "Public undocumented API",
      "Public undocumented classes, methods and variables", Metric.ValueType.INT, Metric.DIRECTION_WORST, true, DOMAIN_DOCUMENTATION)
      .setWorstValue(100.0).setBestValue(0.0).setDirection(Metric.DIRECTION_WORST).setOptimizedBestValue(true).setFormula(
          new SumChildValuesFormula(false));

  public static final String COMMENTED_OUT_CODE_LINES_KEY = "commented_out_code_lines";
  public static final Metric COMMENTED_OUT_CODE_LINES = new Metric(COMMENTED_OUT_CODE_LINES_KEY, "Commented LOCs",
      "Commented lines of code", Metric.ValueType.INT, Metric.DIRECTION_WORST, true, DOMAIN_DOCUMENTATION).setFormula(
      new SumChildValuesFormula(false)).setBestValue(0.0).setOptimizedBestValue(true);

  /* unit tests */
  public static final String TESTS_KEY = "tests";
  public static final Metric TESTS = new Metric(TESTS_KEY, "Unit tests", "Number of unit tests", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_TESTS);

  public static final String TEST_EXECUTION_TIME_KEY = "test_execution_time";
  public static final Metric TEST_EXECUTION_TIME = new Metric(TEST_EXECUTION_TIME_KEY, "Unit tests duration",
      "Execution duration of unit tests ", Metric.ValueType.MILLISEC, Metric.DIRECTION_WORST, false, DOMAIN_TESTS);

  public static final String TEST_ERRORS_KEY = "test_errors";
  public static final Metric TEST_ERRORS = new Metric(TEST_ERRORS_KEY, "Unit test errors", "Number of unit test errors",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_TESTS).setBestValue(0.0).setOptimizedBestValue(true);
  public static final String SKIPPED_TESTS_KEY = "skipped_tests";
  public static final Metric SKIPPED_TESTS = new Metric(SKIPPED_TESTS_KEY, "Skipped unit tests", "Number of skipped unit tests",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_TESTS).setBestValue(0.0).setOptimizedBestValue(true);
  public static final String TEST_FAILURES_KEY = "test_failures";
  public static final Metric TEST_FAILURES = new Metric(TEST_FAILURES_KEY, "Unit test failures", "Number of unit test failures",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_TESTS).setBestValue(0.0).setOptimizedBestValue(true);
  public static final String TEST_SUCCESS_DENSITY_KEY = "test_success_density";
  public static final Metric TEST_SUCCESS_DENSITY = new Metric(TEST_SUCCESS_DENSITY_KEY, "Unit test success (%)",
      "Density of successful unit tests", Metric.ValueType.PERCENT, Metric.DIRECTION_BETTER, true, DOMAIN_TESTS).setWorstValue(0.0)
      .setBestValue(100.0).setOptimizedBestValue(true);
  public static final String TEST_DATA_KEY = "test_data";
  public static final Metric TEST_DATA = new Metric(TEST_DATA_KEY, "Unit tests details", "Unit tests details", Metric.ValueType.DATA,
      Metric.DIRECTION_WORST, false, DOMAIN_TESTS);

  public static final String COVERAGE_KEY = "coverage";
  public static final Metric COVERAGE = new Metric(COVERAGE_KEY, "Coverage", "Coverage by unit tests", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_TESTS).setWorstValue(0.0).setBestValue(100.0);

  public static final String LINES_TO_COVER_KEY = "lines_to_cover";
  public static final Metric LINES_TO_COVER = new Metric(LINES_TO_COVER_KEY, "Lines to cover", "Lines to cover", Metric.ValueType.INT,
      Metric.DIRECTION_BETTER, false, DOMAIN_TESTS).setFormula(new SumChildValuesFormula(false)).setHidden(true);

  public static final String UNCOVERED_LINES_KEY = "uncovered_lines";
  public static final Metric UNCOVERED_LINES = new Metric(UNCOVERED_LINES_KEY, "Uncovered lines", "Uncovered lines", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_TESTS).setFormula(new SumChildValuesFormula(false));

  public static final String LINE_COVERAGE_KEY = "line_coverage";
  public static final Metric LINE_COVERAGE = new Metric(LINE_COVERAGE_KEY, "Line coverage", "Line coverage", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_TESTS);

  public static final String COVERAGE_LINE_HITS_DATA_KEY = "coverage_line_hits_data";
  public static final Metric COVERAGE_LINE_HITS_DATA = new Metric(COVERAGE_LINE_HITS_DATA_KEY, "Coverage hits data",
      "Code coverage line hits data", Metric.ValueType.DATA, Metric.DIRECTION_NONE, false, DOMAIN_TESTS);

  public static final String CONDITIONS_TO_COVER_KEY = "conditions_to_cover";
  public static final Metric CONDITIONS_TO_COVER = new Metric(CONDITIONS_TO_COVER_KEY, "Conditions to cover", "Conditions to cover",
      Metric.ValueType.INT, Metric.DIRECTION_BETTER, false, DOMAIN_TESTS).setFormula(new SumChildValuesFormula(false)).setHidden(true);

  public static final String UNCOVERED_CONDITIONS_KEY = "uncovered_conditions";
  public static final Metric UNCOVERED_CONDITIONS = new Metric(UNCOVERED_CONDITIONS_KEY, "Uncovered conditions", "Uncovered conditions",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_TESTS).setFormula(new SumChildValuesFormula(false));

  public static final String BRANCH_COVERAGE_KEY = "branch_coverage";
  public static final Metric BRANCH_COVERAGE = new Metric(BRANCH_COVERAGE_KEY, "Branch coverage", "Branch coverage",
      Metric.ValueType.PERCENT, Metric.DIRECTION_BETTER, true, DOMAIN_TESTS).setWorstValue(0.0).setBestValue(100.0);

  public static final String BRANCH_COVERAGE_HITS_DATA_KEY = "branch_coverage_hits_data";
  public static final Metric BRANCH_COVERAGE_HITS_DATA = new Metric(BRANCH_COVERAGE_HITS_DATA_KEY, "Branch coverage hits",
      "Branch coverage hits", Metric.ValueType.DATA, Metric.DIRECTION_NONE, false, DOMAIN_TESTS);

  /**
   * @deprecated replaced since 1.11 by UNCOVERED_LINES and UNCOVERED_CONDITIONS
   */
  @Deprecated
  public static final String UNCOVERED_COMPLEXITY_BY_TESTS_KEY = "uncovered_complexity_by_tests";
  /**
   * @deprecated replaced since 1.11 by UNCOVERED_LINES and UNCOVERED_CONDITIONS
   */
  @Deprecated
  public static final Metric UNCOVERED_COMPLEXITY_BY_TESTS = new Metric(UNCOVERED_COMPLEXITY_BY_TESTS_KEY, "Uncovered complexity",
      "Uncovered complexity", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_COMPLEXITY).setFormula(new SumChildValuesFormula(
      false));

  public static final String DUPLICATED_LINES_KEY = "duplicated_lines";
  public static final Metric DUPLICATED_LINES = new Metric(DUPLICATED_LINES_KEY, "Duplicated lines", "Duplicated lines",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DUPLICATION).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String DUPLICATED_BLOCKS_KEY = "duplicated_blocks";
  public static final Metric DUPLICATED_BLOCKS = new Metric(DUPLICATED_BLOCKS_KEY, "Duplicated blocks", "Duplicated blocks",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DUPLICATION).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String DUPLICATED_FILES_KEY = "duplicated_files";
  public static final Metric DUPLICATED_FILES = new Metric(DUPLICATED_FILES_KEY, "Duplicated files", "Duplicated files",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, true, DOMAIN_DUPLICATION).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String DUPLICATED_LINES_DENSITY_KEY = "duplicated_lines_density";
  public static final Metric DUPLICATED_LINES_DENSITY = new Metric(DUPLICATED_LINES_DENSITY_KEY, "Duplicated lines (%)",
      "Duplicated lines balanced by statements", Metric.ValueType.PERCENT, Metric.DIRECTION_WORST, true, DOMAIN_DUPLICATION).setWorstValue(
      50.0).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String DUPLICATIONS_DATA_KEY = "duplications_data";
  public static final Metric DUPLICATIONS_DATA = new Metric(DUPLICATIONS_DATA_KEY, "Duplications details", "Duplications details",
      Metric.ValueType.DATA, Metric.DIRECTION_NONE, false, DOMAIN_DUPLICATION);

  /* coding rules */
  public static final String USABILITY_KEY = "usability";
  public static final Metric USABILITY = new Metric(USABILITY_KEY, "Usability", "Usability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);
  public static final String RELIABILITY_KEY = "reliability";
  public static final Metric RELIABILITY = new Metric(RELIABILITY_KEY, "Reliability", "Reliability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);
  public static final String EFFICIENCY_KEY = "efficiency";
  public static final Metric EFFICIENCY = new Metric(EFFICIENCY_KEY, "Efficiency", "Efficiency", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);
  public static final String PORTABILITY_KEY = "portability";
  public static final Metric PORTABILITY = new Metric(PORTABILITY_KEY, "Portability", "Portability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);
  public static final String MAINTAINABILITY_KEY = "maintainability";
  public static final Metric MAINTAINABILITY = new Metric(MAINTAINABILITY_KEY, "Maintainability", "Maintainability",
      Metric.ValueType.PERCENT, Metric.DIRECTION_BETTER, true, DOMAIN_RULE_CATEGORIES).setBestValue(100.0).setOptimizedBestValue(true);

  public static final String WEIGHTED_VIOLATIONS_KEY = "weighted_violations";
  public static final Metric WEIGHTED_VIOLATIONS = new Metric(WEIGHTED_VIOLATIONS_KEY, "Weighted violations", "Weighted Violations",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, true, DOMAIN_RULES).setHidden(true);

  public static final String VIOLATIONS_DENSITY_KEY = "violations_density";
  public static final Metric VIOLATIONS_DENSITY = new Metric(VIOLATIONS_DENSITY_KEY, "Rules compliance", "Rules compliance",
      Metric.ValueType.PERCENT, Metric.DIRECTION_BETTER, true, DOMAIN_RULES);

  public static final String VIOLATIONS_KEY = "violations";
  public static final Metric VIOLATIONS = new Metric(VIOLATIONS_KEY, "Violations", "Violations", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_RULES).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String BLOCKER_VIOLATIONS_KEY = "blocker_violations";
  public static final Metric BLOCKER_VIOLATIONS = new Metric(BLOCKER_VIOLATIONS_KEY, "Blocker violations", "Blocker violations",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_RULES).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String CRITICAL_VIOLATIONS_KEY = "critical_violations";
  public static final Metric CRITICAL_VIOLATIONS = new Metric(CRITICAL_VIOLATIONS_KEY, "Critical violations", "Critical violations",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_RULES).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String MAJOR_VIOLATIONS_KEY = "major_violations";
  public static final Metric MAJOR_VIOLATIONS = new Metric(MAJOR_VIOLATIONS_KEY, "Major violations", "Major violations",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_RULES).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String MINOR_VIOLATIONS_KEY = "minor_violations";
  public static final Metric MINOR_VIOLATIONS = new Metric(MINOR_VIOLATIONS_KEY, "Minor violations", "Minor violations",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_RULES).setBestValue(0.0).setOptimizedBestValue(true);

  public static final String INFO_VIOLATIONS_KEY = "info_violations";
  public static final Metric INFO_VIOLATIONS = new Metric(INFO_VIOLATIONS_KEY, "Info violations", "Info violations", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_RULES).setBestValue(0.0).setOptimizedBestValue(true);

  /* Design */

  public static final String ABSTRACTNESS_KEY = "abstractness";
  public static final Metric ABSTRACTNESS = new Metric(ABSTRACTNESS_KEY, "Abstractness", "Abstractness", Metric.ValueType.PERCENT,
      Metric.DIRECTION_NONE, false, DOMAIN_DESIGN);
  public static final String INSTABILITY_KEY = "instability";
  public static final Metric INSTABILITY = new Metric(INSTABILITY_KEY, "Instability", "Instability", Metric.ValueType.PERCENT,
      Metric.DIRECTION_NONE, false, DOMAIN_DESIGN);
  public static final String DISTANCE_KEY = "distance";
  public static final Metric DISTANCE = new Metric(DISTANCE_KEY, "Distance", "Distance", Metric.ValueType.FLOAT, Metric.DIRECTION_NONE,
      false, DOMAIN_DESIGN);

  public static final String DEPTH_IN_TREE_KEY = "dit";
  public static final Metric DEPTH_IN_TREE = new Metric(DEPTH_IN_TREE_KEY, "Depth in Tree", "Depth in Inheritance Tree",
      Metric.ValueType.INT, Metric.DIRECTION_NONE, false, DOMAIN_DESIGN);

  public static final String NUMBER_OF_CHILDREN_KEY = "noc";
  public static final Metric NUMBER_OF_CHILDREN = new Metric(NUMBER_OF_CHILDREN_KEY, "Number of Children", "Number of Children",
      Metric.ValueType.INT, Metric.DIRECTION_NONE, false, DOMAIN_DESIGN);

  public static final String RFC_KEY = "rfc";
  public static final Metric RFC = new Metric(RFC_KEY, "RFC", "Response for Class", Metric.ValueType.INT, Metric.DIRECTION_WORST, false,
      DOMAIN_DESIGN).setFormula(new WeightedMeanAggregationFormula(CoreMetrics.FILES, false));

  public static final String RFC_DISTRIBUTION_KEY = "rfc_distribution";
  public static final Metric RFC_DISTRIBUTION = new Metric(RFC_DISTRIBUTION_KEY, "Class distribution /RFC", "Class distribution /RFC",
      Metric.ValueType.DISTRIB, Metric.DIRECTION_NONE, true, DOMAIN_DESIGN).setFormula(new SumChildDistributionFormula());

  public static final String LCOM4_KEY = "lcom4";
  public static final Metric LCOM4 = new Metric(LCOM4_KEY, "LCOM4", "Lack of Cohesion of Methods", Metric.ValueType.FLOAT,
      Metric.DIRECTION_WORST, true, DOMAIN_DESIGN).setFormula(new WeightedMeanAggregationFormula(CoreMetrics.FILES, false));

  public static final String LCOM4_BLOCKS_KEY = "lcom4_blocks";
  public static final Metric LCOM4_BLOCKS = new Metric(LCOM4_BLOCKS_KEY, "LCOM4 blocks", "LCOM4 blocks", Metric.ValueType.DATA,
      Metric.DIRECTION_NONE, false, DOMAIN_DESIGN).setHidden(true);

  public static final String LCOM4_DISTRIBUTION_KEY = "lcom4_distribution";
  public static final Metric LCOM4_DISTRIBUTION = new Metric(LCOM4_DISTRIBUTION_KEY, "Class distribution /LCOM4",
      "Class distribution /LCOM4", Metric.ValueType.DISTRIB, Metric.DIRECTION_NONE, true, DOMAIN_DESIGN)
      .setFormula(new SumChildDistributionFormula());

  public static final String SUSPECT_LCOM4_DENSITY_KEY = "suspect_lcom4_density";
  public static final Metric SUSPECT_LCOM4_DENSITY = new Metric(SUSPECT_LCOM4_DENSITY_KEY, "Suspect LCOM4 density",
      "Density of classes having LCOM4>1", Metric.ValueType.PERCENT, Metric.DIRECTION_WORST, true, DOMAIN_DESIGN);

  public static final String AFFERENT_COUPLINGS_KEY = "ca";
  public static final Metric AFFERENT_COUPLINGS = new Metric(AFFERENT_COUPLINGS_KEY, "Afferent couplings", "Afferent couplings",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DESIGN);
  public static final String EFFERENT_COUPLINGS_KEY = "ce";
  public static final Metric EFFERENT_COUPLINGS = new Metric(EFFERENT_COUPLINGS_KEY, "Efferent couplings", "Efferent couplings",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DESIGN);

  public static final String DEPENDENCY_MATRIX_KEY = "dsm";
  public static final Metric DEPENDENCY_MATRIX = new Metric(DEPENDENCY_MATRIX_KEY, "Dependency Matrix", "Dependency Matrix",
      Metric.ValueType.DATA, Metric.DIRECTION_NONE, false, DOMAIN_DESIGN);

  public static final String PACKAGE_CYCLES_KEY = "package_cycles";
  public static final Metric PACKAGE_CYCLES = new Metric(PACKAGE_CYCLES_KEY, "Package cycles", "Package cycles", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, true, DOMAIN_DESIGN).setFormula(new SumChildValuesFormula(false));

  public static final String PACKAGE_TANGLE_INDEX_KEY = "package_tangle_index";
  public static final Metric PACKAGE_TANGLE_INDEX = new Metric(PACKAGE_TANGLE_INDEX_KEY, "Package tangle index", "Package tangle index",
      Metric.ValueType.PERCENT, Metric.DIRECTION_WORST, true, DOMAIN_DESIGN);

  public static final String PACKAGE_TANGLES_KEY = "package_tangles";
  public static final Metric PACKAGE_TANGLES = new Metric(PACKAGE_TANGLES_KEY, "File dependencies to cut", "File dependencies to cut",
      Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DESIGN).setFormula(new SumChildValuesFormula(false));

  public static final String PACKAGE_FEEDBACK_EDGES_KEY = "package_feedback_edges";
  public static final Metric PACKAGE_FEEDBACK_EDGES = new Metric(PACKAGE_FEEDBACK_EDGES_KEY, "Package dependencies to cut",
      "Package dependencies to cut", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DESIGN)
      .setFormula(new SumChildValuesFormula(false));

  public static final String PACKAGE_EDGES_WEIGHT_KEY = "package_edges_weight";
  public static final Metric PACKAGE_EDGES_WEIGHT = new Metric(PACKAGE_EDGES_WEIGHT_KEY, "Package edges weight", "Package edges weight",
      Metric.ValueType.INT, Metric.DIRECTION_BETTER, false, DOMAIN_DESIGN).setFormula(new SumChildValuesFormula(false)).setHidden(true);

  public static final String FILE_CYCLES_KEY = "file_cycles";
  public static final Metric FILE_CYCLES = new Metric(FILE_CYCLES_KEY, "File cycles", "File cycles", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, true, DOMAIN_DESIGN).setHidden(true);

  public static final String FILE_TANGLE_INDEX_KEY = "file_tangle_index";
  public static final Metric FILE_TANGLE_INDEX = new Metric(FILE_TANGLE_INDEX_KEY, "File tangle index", "File tangle index",
      Metric.ValueType.PERCENT, Metric.DIRECTION_WORST, true, DOMAIN_DESIGN).setHidden(true);

  public static final String FILE_TANGLES_KEY = "file_tangles";
  public static final Metric FILE_TANGLES = new Metric(FILE_TANGLES_KEY, "File tangles", "Files tangles", Metric.ValueType.INT,
      Metric.DIRECTION_WORST, false, DOMAIN_DESIGN).setHidden(true);

  public static final String FILE_FEEDBACK_EDGES_KEY = "file_feedback_edges";
  public static final Metric FILE_FEEDBACK_EDGES = new Metric(FILE_FEEDBACK_EDGES_KEY, "Suspect file dependencies",
      "Suspect file dependencies", Metric.ValueType.INT, Metric.DIRECTION_WORST, false, DOMAIN_DESIGN).setHidden(true);

  public static final String FILE_EDGES_WEIGHT_KEY = "file_edges_weight";
  public static final Metric FILE_EDGES_WEIGHT = new Metric(FILE_EDGES_WEIGHT_KEY, "File edges weight", "File edges weight",
      Metric.ValueType.INT, Metric.DIRECTION_BETTER, false, DOMAIN_DESIGN).setHidden(true);

  /* alerts */
  public static final String ALERT_STATUS_KEY = "alert_status";
  public static final Metric ALERT_STATUS = new Metric(ALERT_STATUS_KEY, "Alert", "Alert", Metric.ValueType.LEVEL, Metric.DIRECTION_BETTER,
      true, DOMAIN_GENERAL);

  /* quality profile */
  public static final String PROFILE_KEY = "profile";
  public static final Metric PROFILE = new Metric(PROFILE_KEY, "Profile", "Selected quality profile", Metric.ValueType.DATA,
      Metric.DIRECTION_NONE, false, DOMAIN_GENERAL);

  public static List<Metric> metrics = new ArrayList<Metric>();

  public static Set<String> metricKeys = new HashSet<String>();

  public static List<Metric> getMetrics() {
    if (metrics.isEmpty()) {
      for (Field field : CoreMetrics.class.getFields()) {
        if (Metric.class.isAssignableFrom(field.getType())) {
          try {
            metrics.add((Metric) field.get(null));
          } catch (IllegalAccessException e) {
            throw new SonarException("can not load metrics from " + CoreMetrics.class.getSimpleName(), e);
          }
        }
      }
    }
    return metrics;
  }
}
