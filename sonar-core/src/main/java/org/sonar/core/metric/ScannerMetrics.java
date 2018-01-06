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
package org.sonar.core.metric;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.concurrent.Immutable;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import static org.sonar.api.measures.CoreMetrics.ACCESSORS;
import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_DATA;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.DIRECTORIES;
import static org.sonar.api.measures.CoreMetrics.EXECUTABLE_LINES_DATA;
import static org.sonar.api.measures.CoreMetrics.FILES;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

/**
 * This class is used to know the list of metrics that can be sent in the analysis report.
 * <p/>
 * Scanners should not send other metrics, and the Compute Engine should not allow other metrics.
 */
@Immutable
@ComputeEngineSide
@ScannerSide
public class ScannerMetrics {

  private static final Set<Metric> ALLOWED_CORE_METRICS = ImmutableSet.of(
    GENERATED_LINES,
    NCLOC,
    NCLOC_DATA,
    GENERATED_NCLOC,
    COMMENT_LINES,
    COMMENT_LINES_DATA,
    NCLOC_LANGUAGE_DISTRIBUTION,

    PUBLIC_API,
    PUBLIC_UNDOCUMENTED_API,

    FILES,
    DIRECTORIES,
    CLASSES,
    FUNCTIONS,
    STATEMENTS,
    ACCESSORS,

    COMPLEXITY,
    COMPLEXITY_IN_CLASSES,
    COMPLEXITY_IN_FUNCTIONS,
    COGNITIVE_COMPLEXITY,
    FILE_COMPLEXITY_DISTRIBUTION,
    FUNCTION_COMPLEXITY_DISTRIBUTION,

    TESTS,
    SKIPPED_TESTS,
    TEST_ERRORS,
    TEST_FAILURES,
    TEST_EXECUTION_TIME,

    LINES_TO_COVER,
    UNCOVERED_LINES,
    COVERAGE_LINE_HITS_DATA,
    CONDITIONS_TO_COVER,
    UNCOVERED_CONDITIONS,
    COVERED_CONDITIONS_BY_LINE,
    CONDITIONS_BY_LINE,

    EXECUTABLE_LINES_DATA);

  private final Set<Metric> metrics;

  public ScannerMetrics() {
    this.metrics = ALLOWED_CORE_METRICS;
  }

  public ScannerMetrics(Metrics[] metricsRepositories) {
    this.metrics = Stream.concat(getPluginMetrics(metricsRepositories), ALLOWED_CORE_METRICS.stream()).collect(toSet());
  }

  public Set<Metric> getMetrics() {
    return metrics;
  }

  private static Stream<Metric> getPluginMetrics(Metrics[] metricsRepositories) {
    return Arrays.stream(metricsRepositories)
      .map(Metrics::getMetrics)
      .filter(Objects::nonNull)
      .flatMap(List::stream);
  }
}
