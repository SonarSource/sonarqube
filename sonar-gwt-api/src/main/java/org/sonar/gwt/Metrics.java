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
package org.sonar.gwt;

/**
 * Keys of core metrics
 */
public interface Metrics {

  String LINES = "lines";
  String NCLOC = "ncloc";
  String GENERATED_NCLOC = "generated_ncloc";
  String GENERATED_LINES = "generated_lines";
  String CLASSES = "classes";
  String FILES = "files";
  String DIRECTORIES = "directories";
  String PACKAGES = "packages";
  String FUNCTIONS = "functions";
  String PARAGRAPHS = "paragraphs";
  String ACCESSORS = "accessors";
  String STATEMENTS = "statements";
  String PUBLIC_API = "public_api";
  String COMPLEXITY = "complexity";
  String CLASS_COMPLEXITY = "class_complexity";
  String FUNCTION_COMPLEXITY = "function_complexity";
  String PARAGRAPH_COMPLEXITY = "paragraph_complexity";
  String FILE_COMPLEXITY = "file_complexity";
  String CLASS_COMPLEXITY_DISTRIBUTION = "class_complexity_distribution";
  String FUNCTION_COMPLEXITY_DISTRIBUTION = "function_complexity_distribution";
  String COMMENT_LINES = "comment_lines";
  String COMMENT_LINES_DENSITY = "comment_lines_density";
  String COMMENT_BLANK_LINES = "comment_blank_lines";
  String PUBLIC_DOCUMENTED_API_DENSITY = "public_documented_api_density";
  String PUBLIC_UNDOCUMENTED_API = "public_undocumented_api";
  String COMMENTED_OUT_CODE_LINES = "commented_out_code_lines";
  String TESTS = "tests";
  String TEST_EXECUTION_TIME = "test_execution_time";
  String TEST_ERRORS = "test_errors";
  String SKIPPED_TESTS = "skipped_tests";
  String TEST_FAILURES = "test_failures";
  String TEST_SUCCESS_DENSITY = "test_success_density";
  String TEST_DATA = "test_data";
  String COVERAGE = "coverage";
  String LINES_TO_COVER = "lines_to_cover";
  String UNCOVERED_LINES = "uncovered_lines";
  String LINE_COVERAGE = "line_coverage";
  String COVERAGE_LINE_HITS_DATA = "coverage_line_hits_data";
  String CONDITIONS_TO_COVER = "conditions_to_cover";
  String UNCOVERED_CONDITIONS = "uncovered_conditions";
  String BRANCH_COVERAGE = "branch_coverage";
  String BRANCH_COVERAGE_HITS_DATA = "branch_coverage_hits_data";
  String DUPLICATED_LINES = "duplicated_lines";
  String DUPLICATED_BLOCKS = "duplicated_blocks";
  String DUPLICATED_FILES = "duplicated_files";
  String DUPLICATED_LINES_DENSITY = "duplicated_lines_density";
  String DUPLICATIONS_DATA = "duplications_data";
  String USABILITY = "usability";
  String RELIABILITY = "reliability";
  String EFFICIENCY = "efficiency";
  String PORTABILITY = "portability";
  String MAINTAINABILITY = "maintainability";
  String WEIGHTED_VIOLATIONS = "weighted_violations";
  String VIOLATIONS_DENSITY = "violations_density";
  String VIOLATIONS = "violations";
  String BLOCKER_VIOLATIONS = "blocker_violations";
  String CRITICAL_VIOLATIONS = "critical_violations";
  String MAJOR_VIOLATIONS = "major_violations";
  String MINOR_VIOLATIONS = "minor_violations";
  String INFO_VIOLATIONS = "info_violations";
  String DEPTH_IN_TREE = "dit";
  String NUMBER_OF_CHILDREN = "noc";
  String RFC = "rfc";
  String RFC_DISTRIBUTION = "rfc_distribution";
  String LCOM4 = "lcom4";
  String LCOM4_DISTRIBUTION = "lcom4_distribution";
  String AFFERENT_COUPLINGS = "ca";
  String EFFERENT_COUPLINGS = "ce";
  String ABSTRACTNESS = "abstractness";
  String INSTABILITY = "instability";
  String DISTANCE = "distance";
  String DEPENDENCY_MATRIX = "dsm";
  String PACKAGE_CYCLES = "package_cycles";
  String PACKAGE_TANGLE_INDEX = "package_tangle_index";
  String PACKAGE_TANGLES = "package_tangles";
  String PACKAGE_FEEDBACK_EDGES = "package_feedback_edges";
  String PACKAGE_EDGES_WEIGHT = "package_edges_weight";
  String FILE_CYCLES = "file_cycles";
  String FILE_TANGLE_INDEX = "file_tangle_index";
  String FILE_TANGLES = "file_tangles";
  String FILE_FEEDBACK_EDGES = "file_feedback_edges";
  String FILE_EDGES_WEIGHT = "file_edges_weight";
  String ALERT_STATUS = "alert_status";
}
