/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import java.util.Arrays;
import java.util.List;

public class ViolationsDensityDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependsUpon
  public List<Metric> dependsUponWeightedViolationsAndNcloc() {
    return Arrays.asList(CoreMetrics.WEIGHTED_VIOLATIONS, CoreMetrics.NCLOC);
  }

  @DependedUpon
  public Metric generatesViolationsDensity() {
    return CoreMetrics.VIOLATIONS_DENSITY;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(context)) {
      decorateDensity(context);
    }
  }

  protected boolean shouldDecorateResource(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.VIOLATIONS_DENSITY) == null;
  }

  private void decorateDensity(DecoratorContext context) {
    Measure ncloc = context.getMeasure(CoreMetrics.NCLOC);
    if (MeasureUtils.hasValue(ncloc) && ncloc.getValue() > 0.0) {
      saveDensity(context, ncloc.getValue().intValue());
    }
  }

  private void saveDensity(DecoratorContext context, int ncloc) {
    Measure debt = context.getMeasure(CoreMetrics.WEIGHTED_VIOLATIONS);
    Integer debtValue = 0;
    if (MeasureUtils.hasValue(debt)) {
      debtValue = debt.getValue().intValue();
    }
    double density = calculate(debtValue, ncloc);
    context.saveMeasure(CoreMetrics.VIOLATIONS_DENSITY, density);
  }

  protected static double calculate(int debt, int ncloc) {
    double rci = (1.0 - ((double) debt / (double) ncloc)) * 100.0;
    rci = Math.max(rci, 0.0);
    return rci;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
