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
package org.sonar.plugins.core.sensors;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Arrays;
import java.util.List;

/**
 * @deprecated the metric <code>CoreMetrics.UNCOVERED_COMPLEXITY_BY_TESTS</code> is deprecated since v.1.11.
 *             It's replaced by uncovered_line and uncovered_conditions
 */
@Deprecated
public class UncoveredComplexityDecorator implements Decorator {

  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return Arrays.asList(CoreMetrics.COVERAGE, CoreMetrics.COMPLEXITY);
  }

  @DependedUpon
  public Metric generatesMetric() {
    return CoreMetrics.UNCOVERED_COMPLEXITY_BY_TESTS;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Measure coverage = context.getMeasure(CoreMetrics.COVERAGE);
    Measure complexity = context.getMeasure(CoreMetrics.COMPLEXITY);

    if (MeasureUtils.haveValues(coverage, complexity)) {
      double value = complexity.getValue() - (complexity.getValue() * (coverage.getValue() / 100.0));
      String data = KeyValueFormat.format("CMP", complexity.getValue().intValue(), "COV", coverage.getValue());
      context.saveMeasure(new Measure(CoreMetrics.UNCOVERED_COMPLEXITY_BY_TESTS, value, data));
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

