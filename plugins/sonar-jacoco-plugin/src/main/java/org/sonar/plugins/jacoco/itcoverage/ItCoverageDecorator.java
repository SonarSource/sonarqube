/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.jacoco.itcoverage;

import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;

import java.util.Arrays;
import java.util.List;

/**
 * Copied from org.sonar.plugins.core.sensors.CoverageDecorator
 */
public class ItCoverageDecorator extends AbstractCoverageDecorator {

  @Override
  protected Metric getTargetMetric() {
    return JaCoCoItMetrics.IT_COVERAGE;
  }

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return Arrays.asList(JaCoCoItMetrics.IT_LINES_TO_COVER, JaCoCoItMetrics.IT_UNCOVERED_LINES, JaCoCoItMetrics.IT_CONDITIONS_TO_COVER, JaCoCoItMetrics.IT_UNCOVERED_CONDITIONS);
  }

  @Override
  protected Double countCoveredElements(DecoratorContext context) {
    double uncoveredLines = MeasureUtils.getValue(context.getMeasure(JaCoCoItMetrics.IT_UNCOVERED_LINES), 0.0);
    double lines = MeasureUtils.getValue(context.getMeasure(JaCoCoItMetrics.IT_LINES_TO_COVER), 0.0);
    double uncoveredConditions = MeasureUtils.getValue(context.getMeasure(JaCoCoItMetrics.IT_UNCOVERED_CONDITIONS), 0.0);
    double conditions = MeasureUtils.getValue(context.getMeasure(JaCoCoItMetrics.IT_CONDITIONS_TO_COVER), 0.0);
    return lines + conditions - uncoveredConditions - uncoveredLines;
  }

  @Override
  protected Double countElements(DecoratorContext context) {
    double lines = MeasureUtils.getValue(context.getMeasure(JaCoCoItMetrics.IT_LINES_TO_COVER), 0.0);
    double conditions = MeasureUtils.getValue(context.getMeasure(JaCoCoItMetrics.IT_CONDITIONS_TO_COVER), 0.0);
    return lines + conditions;
  }

}
