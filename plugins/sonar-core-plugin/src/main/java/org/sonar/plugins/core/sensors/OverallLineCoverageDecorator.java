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
package org.sonar.plugins.core.sensors;

import com.google.common.collect.ImmutableList;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;

import java.util.List;

public final class OverallLineCoverageDecorator extends AbstractCoverageDecorator {

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return ImmutableList.<Metric>of(CoreMetrics.OVERALL_UNCOVERED_LINES, CoreMetrics.OVERALL_LINES_TO_COVER, CoreMetrics.NEW_OVERALL_UNCOVERED_LINES,
      CoreMetrics.NEW_OVERALL_LINES_TO_COVER);
  }

  @Override
  protected Metric getGeneratedMetric() {
    return CoreMetrics.OVERALL_LINE_COVERAGE;
  }

  @Override
  protected Long countElements(DecoratorContext context) {
    return MeasureUtils.getValueAsLong(context.getMeasure(CoreMetrics.OVERALL_LINES_TO_COVER), 0L);
  }

  @Override
  protected long countCoveredElements(DecoratorContext context) {
    long uncoveredLines = MeasureUtils.getValueAsLong(context.getMeasure(CoreMetrics.OVERALL_UNCOVERED_LINES), 0L);
    long lines = MeasureUtils.getValueAsLong(context.getMeasure(CoreMetrics.OVERALL_LINES_TO_COVER), 0L);

    return lines - uncoveredLines;
  }

  @Override
  protected Metric getGeneratedMetricForNewCode() {
    return CoreMetrics.NEW_OVERALL_LINE_COVERAGE;
  }

  @Override
  protected Long countElementsForNewCode(DecoratorContext context, int periodIndex) {
    return MeasureUtils.getVariationAsLong(context.getMeasure(CoreMetrics.NEW_OVERALL_LINES_TO_COVER), periodIndex);
  }

  @Override
  protected long countCoveredElementsForNewCode(DecoratorContext context, int periodIndex) {
    long uncoveredLines = MeasureUtils.getVariationAsLong(context.getMeasure(CoreMetrics.NEW_OVERALL_UNCOVERED_LINES), periodIndex, 0L);
    long lines = MeasureUtils.getVariationAsLong(context.getMeasure(CoreMetrics.NEW_OVERALL_LINES_TO_COVER), periodIndex, 0L);

    return lines - uncoveredLines;
  }
}
