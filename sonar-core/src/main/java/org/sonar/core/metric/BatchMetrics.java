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

package org.sonar.core.metric;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.server.ServerSide;

import static org.sonar.api.measures.CoreMetrics.ACCESSORS;
import static org.sonar.api.measures.CoreMetrics.CLASSES;
import static org.sonar.api.measures.CoreMetrics.COMMENTED_OUT_CODE_LINES;
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
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_BLOCKS;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_FILES;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY;
import static org.sonar.api.measures.CoreMetrics.FILES;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTIONS;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.GENERATED_LINES;
import static org.sonar.api.measures.CoreMetrics.GENERATED_NCLOC;
import static org.sonar.api.measures.CoreMetrics.IT_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.IT_COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.IT_LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.IT_UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.IT_UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.LINES;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.NCLOC;
import static org.sonar.api.measures.CoreMetrics.NCLOC_DATA;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.OVERALL_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_CONDITIONS_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA;
import static org.sonar.api.measures.CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE;
import static org.sonar.api.measures.CoreMetrics.OVERALL_LINES_TO_COVER;
import static org.sonar.api.measures.CoreMetrics.OVERALL_UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.OVERALL_UNCOVERED_LINES;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_API;
import static org.sonar.api.measures.CoreMetrics.PUBLIC_UNDOCUMENTED_API;
import static org.sonar.api.measures.CoreMetrics.QUALITY_PROFILES;
import static org.sonar.api.measures.CoreMetrics.SKIPPED_TESTS;
import static org.sonar.api.measures.CoreMetrics.STATEMENTS;
import static org.sonar.api.measures.CoreMetrics.TESTS;
import static org.sonar.api.measures.CoreMetrics.TEST_ERRORS;
import static org.sonar.api.measures.CoreMetrics.TEST_EXECUTION_TIME;
import static org.sonar.api.measures.CoreMetrics.TEST_FAILURES;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_CONDITIONS;
import static org.sonar.api.measures.CoreMetrics.UNCOVERED_LINES;

/**
 * This class is used to know the list of metrics that can be send in the batch report.
 * <p/>
 * The batch should not send other metrics, and the Compute Engine should not allow other metrics.
 */
@ServerSide
@BatchSide
public class BatchMetrics {

  private static final Set<Metric> ALLOWED_CORE_METRICS = ImmutableSet.<Metric>of(
    LINES,
    GENERATED_LINES,
    NCLOC,
    NCLOC_DATA,
    GENERATED_NCLOC,
    COMMENT_LINES,
    COMMENT_LINES_DATA,
    NCLOC_LANGUAGE_DISTRIBUTION,
    COMMENTED_OUT_CODE_LINES,

    PUBLIC_API,
    PUBLIC_UNDOCUMENTED_API,

    FILES,
    DIRECTORIES,
    CLASSES,
    FUNCTIONS,
    STATEMENTS,
    ACCESSORS,

    DUPLICATED_LINES,
    DUPLICATED_BLOCKS,
    DUPLICATED_FILES,
    DUPLICATED_LINES_DENSITY,

    COMPLEXITY,
    COMPLEXITY_IN_CLASSES,
    COMPLEXITY_IN_FUNCTIONS,
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

    IT_LINES_TO_COVER,
    IT_UNCOVERED_LINES,
    IT_COVERAGE_LINE_HITS_DATA,
    IT_CONDITIONS_TO_COVER,
    IT_UNCOVERED_CONDITIONS,
    IT_COVERED_CONDITIONS_BY_LINE,
    IT_CONDITIONS_BY_LINE,

    OVERALL_LINES_TO_COVER,
    OVERALL_UNCOVERED_LINES,
    OVERALL_COVERAGE_LINE_HITS_DATA,
    OVERALL_CONDITIONS_TO_COVER,
    OVERALL_UNCOVERED_CONDITIONS,
    OVERALL_COVERED_CONDITIONS_BY_LINE,
    OVERALL_CONDITIONS_BY_LINE,

    QUALITY_PROFILES
    );

  private final Set<Metric> metrics;

  public BatchMetrics(Metrics[] metricsRepositories) {
    this.metrics = ImmutableSet.copyOf(Iterables.concat(getPluginMetrics(metricsRepositories), ALLOWED_CORE_METRICS));
  }

  public Set<Metric> getMetrics() {
    return metrics;
  }

  private static Iterable<Metric> getPluginMetrics(Metrics[] metricsRepositories) {
    return FluentIterable.from(Arrays.asList(metricsRepositories))
      .transformAndConcat(FlattenMetrics.INSTANCE);
  }

  private enum FlattenMetrics implements Function<Metrics, List<Metric>> {
    INSTANCE;

    @Nullable
    @Override
    public List<Metric> apply(Metrics input) {
      return input.getMetrics();
    }
  }
}
