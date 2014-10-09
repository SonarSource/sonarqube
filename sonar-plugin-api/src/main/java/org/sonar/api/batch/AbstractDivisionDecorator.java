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
package org.sonar.api.batch;

import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.List;

/**
 * A pre-implementation to decorate metrics that are the result of a division
 *
 * @since 1.10
 */
public abstract class AbstractDivisionDecorator implements Decorator {

  protected abstract Metric getQuotientMetric();

  protected abstract Metric getDivisorMetric();

  protected abstract Metric getDividendMetric();

  /**
   * Used to define upstream dependencies
   */
  @DependsUpon
  public List<Metric> dependsUponMetrics() {
    return Arrays.asList(getDividendMetric(), getDivisorMetric());
  }

  /**
   * Used to define downstream dependencies
   */
  @DependedUpon
  public Metric generatesMetric() {
    return getQuotientMetric();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (!shouldDecorateResource(context)) {
      return;
    }
    Measure dividend = context.getMeasure(getDividendMetric());
    Measure divisor = context.getMeasure(getDivisorMetric());

    if (MeasureUtils.hasValue(dividend) && MeasureUtils.hasValue(divisor) && divisor.getValue() > 0.0) {
      context.saveMeasure(new Measure(getQuotientMetric(), compute(dividend, divisor, getQuotientMetric().isPercentageType())));
    }
  }

  protected boolean shouldDecorateResource(DecoratorContext context) {
    return context.getMeasure(getQuotientMetric()) == null;
  }


  protected double compute(Measure dividend, Measure divisor, boolean shouldResultBeInPercent) {
    double result = dividend.getValue() / divisor.getValue();
    return shouldResultBeInPercent ? result * 100 : result;
  }

}
