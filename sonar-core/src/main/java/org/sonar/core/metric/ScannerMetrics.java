/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.scanner.ScannerSide;
import org.springframework.beans.factory.annotation.Autowired;

import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.COGNITIVE_COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_CLASSES;
import static org.sonar.api.measures.CoreMetrics.COMPLEXITY_IN_FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.EXECUTABLE_LINES_DATA;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES;

/**
 * This class is used to know the list of metrics that can be sent in the analysis report.
 * <p/>
 * Scanners should not send other metrics, and the Compute Engine should not allow other metrics.
 */
@ComputeEngineSide
@ScannerSide
public class ScannerMetrics {

  private static final Set<Metric> ALLOWED_CORE_METRICS = Set.of(
    GENERATED_LINES,
    NCLOC,
    NCLOC_DATA,
    GENERATED_NCLOC,
    COMMENT_LINES,

    PUBLIC_API,
    PUBLIC_UNDOCUMENTED_API,

    CLASSES,
    FUNCTIONS,
    STATEMENTS,

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

    EXECUTABLE_LINES_DATA);

  private Set<Metric> metrics;

  @Autowired(required = false)
  public ScannerMetrics() {
    this.metrics = ALLOWED_CORE_METRICS;
  }

  @Autowired(required = false)
  public ScannerMetrics(List<Metrics> metricsRepositories) {
    this.metrics = ALLOWED_CORE_METRICS;
    addPluginMetrics(metricsRepositories);
  }

  /**
   * The metrics allowed in scanner analysis reports. The measures that don't relate to
   * these metrics are not loaded by Compute Engine.
   */
  public Set<Metric> getMetrics() {
    return metrics;
  }

  /**
   * Adds the given metrics to the set of allowed metrics
   */
  public void addPluginMetrics(List<Metrics> metricsRepositories) {
    this.metrics = Stream.concat(getPluginMetrics(metricsRepositories.stream()), this.metrics.stream()).collect(Collectors.toSet());
  }

  private static Stream<Metric> getPluginMetrics(Stream<Metrics> metricsStream) {
    return metricsStream
      .map(Metrics::getMetrics)
      .filter(Objects::nonNull)
      .flatMap(List::stream);
  }
}
