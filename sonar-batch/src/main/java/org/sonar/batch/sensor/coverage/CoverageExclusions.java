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
package org.sonar.batch.sensor.coverage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.WildcardPattern;

public class CoverageExclusions {

  private static final Logger LOG = LoggerFactory.getLogger(CoverageExclusions.class);

  private final Settings settings;
  private final Set<Metric> coverageMetrics;
  private Collection<WildcardPattern> resourcePatterns;

  public CoverageExclusions(Settings settings) {
    this.settings = settings;
    this.coverageMetrics = new HashSet<>();
    // UT
    coverageMetrics.add(CoreMetrics.COVERAGE);
    coverageMetrics.add(CoreMetrics.LINE_COVERAGE);
    coverageMetrics.add(CoreMetrics.BRANCH_COVERAGE);
    coverageMetrics.add(CoreMetrics.UNCOVERED_LINES);
    coverageMetrics.add(CoreMetrics.LINES_TO_COVER);
    coverageMetrics.add(CoreMetrics.UNCOVERED_CONDITIONS);
    coverageMetrics.add(CoreMetrics.CONDITIONS_TO_COVER);
    coverageMetrics.add(CoreMetrics.CONDITIONS_BY_LINE);
    coverageMetrics.add(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
    coverageMetrics.add(CoreMetrics.COVERAGE_LINE_HITS_DATA);
    coverageMetrics.add(CoreMetrics.NEW_LINES_TO_COVER);
    coverageMetrics.add(CoreMetrics.NEW_UNCOVERED_LINES);
    coverageMetrics.add(CoreMetrics.NEW_UNCOVERED_CONDITIONS);
    // IT
    coverageMetrics.add(CoreMetrics.IT_COVERAGE);
    coverageMetrics.add(CoreMetrics.IT_LINE_COVERAGE);
    coverageMetrics.add(CoreMetrics.IT_BRANCH_COVERAGE);
    coverageMetrics.add(CoreMetrics.IT_UNCOVERED_LINES);
    coverageMetrics.add(CoreMetrics.IT_LINES_TO_COVER);
    coverageMetrics.add(CoreMetrics.IT_UNCOVERED_CONDITIONS);
    coverageMetrics.add(CoreMetrics.IT_CONDITIONS_TO_COVER);
    coverageMetrics.add(CoreMetrics.IT_CONDITIONS_BY_LINE);
    coverageMetrics.add(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE);
    coverageMetrics.add(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA);
    coverageMetrics.add(CoreMetrics.NEW_IT_LINES_TO_COVER);
    coverageMetrics.add(CoreMetrics.NEW_IT_UNCOVERED_LINES);
    coverageMetrics.add(CoreMetrics.NEW_IT_UNCOVERED_CONDITIONS);
    // OVERALL
    coverageMetrics.add(CoreMetrics.OVERALL_COVERAGE);
    coverageMetrics.add(CoreMetrics.OVERALL_LINE_COVERAGE);
    coverageMetrics.add(CoreMetrics.OVERALL_BRANCH_COVERAGE);
    coverageMetrics.add(CoreMetrics.OVERALL_UNCOVERED_LINES);
    coverageMetrics.add(CoreMetrics.OVERALL_LINES_TO_COVER);
    coverageMetrics.add(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS);
    coverageMetrics.add(CoreMetrics.OVERALL_CONDITIONS_TO_COVER);
    coverageMetrics.add(CoreMetrics.OVERALL_CONDITIONS_BY_LINE);
    coverageMetrics.add(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE);
    coverageMetrics.add(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA);
    coverageMetrics.add(CoreMetrics.NEW_OVERALL_LINES_TO_COVER);
    coverageMetrics.add(CoreMetrics.NEW_OVERALL_UNCOVERED_LINES);
    coverageMetrics.add(CoreMetrics.NEW_OVERALL_UNCOVERED_CONDITIONS);

    initPatterns();
  }

  public boolean accept(Resource resource, Measure measure) {
    if (isCoverageMetric(measure.getMetric())) {
      return !hasMatchingPattern(resource);
    } else {
      return true;
    }
  }

  private boolean isCoverageMetric(Metric metric) {
    return this.coverageMetrics.contains(metric);
  }

  public boolean hasMatchingPattern(Resource resource) {
    boolean found = false;
    Iterator<WildcardPattern> iterator = resourcePatterns.iterator();
    while (!found && iterator.hasNext()) {
      found = resource.matchFilePattern(iterator.next().toString());
    }
    return found;
  }

  @VisibleForTesting
  final void initPatterns() {
    Builder<WildcardPattern> builder = ImmutableList.builder();
    for (String pattern : settings.getStringArray(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY)) {
      builder.add(WildcardPattern.create(pattern));
    }
    resourcePatterns = builder.build();
    log("Excluded sources for coverage: ", resourcePatterns);
  }

  private static void log(String title, Collection<WildcardPattern> patterns) {
    if (!patterns.isEmpty()) {
      LOG.info(title);
      for (WildcardPattern pattern : patterns) {
        LOG.info("  " + pattern);
      }
    }
  }
}
