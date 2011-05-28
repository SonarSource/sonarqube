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
package org.sonar.plugins.squid.decorators;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

public final class ChidamberKemererDistributionBuilder implements Decorator {

  private static final Integer[] LCOM4_LIMITS = { 2, 3, 4, 5, 10 }; // 1 is excluded
  private static final Integer[] RFC_LIMITS = { 0, 5, 10, 20, 30, 50, 90, 150 };

  @DependedUpon
  public Metric generatesLcom4Distribution() {
    return CoreMetrics.LCOM4_DISTRIBUTION;
  }

  @DependsUpon
  public Metric dependsInLcom4() {
    return CoreMetrics.LCOM4;
  }

  @DependedUpon
  public Metric generatesRfcDistribution() {
    return CoreMetrics.RFC_DISTRIBUTION;
  }

  @DependsUpon
  public Metric dependsInRfc() {
    return CoreMetrics.RFC;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldExecuteOn(resource)) {
      RangeDistributionBuilder lcom4Distribution = new RangeDistributionBuilder(CoreMetrics.LCOM4_DISTRIBUTION, LCOM4_LIMITS);
      RangeDistributionBuilder rfcDistribution = new RangeDistributionBuilder(CoreMetrics.RFC_DISTRIBUTION, RFC_LIMITS);

      for (DecoratorContext childContext : context.getChildren()) {
        if (Scopes.isFile(childContext.getResource())) {
          addMeasureToDistribution(childContext, lcom4Distribution, CoreMetrics.LCOM4);
          addMeasureToDistribution(childContext, rfcDistribution, CoreMetrics.RFC);
        }
      }

      saveDistribution(context, lcom4Distribution);
      saveDistribution(context, rfcDistribution);
    }
  }

  private void addMeasureToDistribution(DecoratorContext childContext, RangeDistributionBuilder distribution, Metric metric) {
    Measure measure = childContext.getMeasure(metric);
    if (measure != null) {
      distribution.add(measure.getIntValue());
    }
  }

  private void saveDistribution(DecoratorContext context, RangeDistributionBuilder distribution) {
    Measure measure = distribution.build(false);
    if (measure != null) {
      context.saveMeasure(measure);
    }
  }

  boolean shouldExecuteOn(Resource resource) {
    return Scopes.isDirectory(resource);
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey());
  }
}