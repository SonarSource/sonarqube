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
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.WildcardPattern;

import java.util.Collection;
import java.util.Iterator;

public class CoverageExclusions {

  private static final Logger LOG = LoggerFactory.getLogger(CoverageExclusions.class);

  private final Settings settings;
  private final ImmutableSet<Metric> coverageMetrics;
  private Collection<WildcardPattern> resourcePatterns;

  public CoverageExclusions(Settings settings) {
    this.settings = settings;
    this.coverageMetrics = ImmutableSet.<Metric>builder()
      .add(CoreMetrics.COVERAGE)
      .add(CoreMetrics.COVERAGE_LINE_HITS_DATA)
      .add(CoreMetrics.CONDITIONS_BY_LINE)
      .add(CoreMetrics.COVERED_CONDITIONS_BY_LINE)
      .addAll(CoverageConstants.COVERAGE_METRICS)
      .add(CoreMetrics.LINE_COVERAGE)
      .addAll(CoverageConstants.LINE_COVERAGE_METRICS)
      .add(CoreMetrics.BRANCH_COVERAGE)
      .addAll(CoverageConstants.BRANCH_COVERAGE_METRICS)
      .build();

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

  private boolean hasMatchingPattern(Resource resource) {
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

  private void log(String title, Collection<WildcardPattern> patterns) {
    if (!patterns.isEmpty()) {
      LOG.info(title);
      for (WildcardPattern pattern : patterns) {
        LOG.info("  " + pattern);
      }
    }
  }
}
