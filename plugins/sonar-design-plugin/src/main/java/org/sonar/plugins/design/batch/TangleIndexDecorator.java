/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.design.batch;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.List;

public abstract class TangleIndexDecorator implements Decorator {

  private Metric tanglesMetric;
  private Metric edgesWeightMetric;
  private Metric tangleIndexMetric;

  protected TangleIndexDecorator(Metric tanglesMetric, Metric edgesWeightMetric, Metric tangleIndexMetric) {
    this.tanglesMetric = tanglesMetric;
    this.edgesWeightMetric = edgesWeightMetric;
    this.tangleIndexMetric = tangleIndexMetric;
  }

  @DependsUpon
  public final List<Metric> dependsUponMetrics() {
    return Arrays.asList(tanglesMetric, edgesWeightMetric);
  }

  /**
   * Used to define downstream dependencies
   */
  @DependedUpon
  public final Metric generatesMetric() {
    return tangleIndexMetric;
  }

  public final boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public final void decorate(Resource resource, DecoratorContext context) {
    if (!shouldDecorateResource(context)) {
      return;
    }
    Measure tangles = context.getMeasure(tanglesMetric);
    Measure totalweight = context.getMeasure(edgesWeightMetric);

    if (MeasureUtils.hasValue(totalweight)) {
      context.saveMeasure(new Measure(tangleIndexMetric, compute(MeasureUtils.getValue(tangles, 0.0), totalweight.getValue())));
    }
  }

  private boolean shouldDecorateResource(DecoratorContext context) {
    return context.getMeasure(tangleIndexMetric) == null;
  }


  private double compute(double tangles, double totalWeight) {
    if (totalWeight==0.0) {
      return 0.0;
    }
    double result = 2 * tangles / totalWeight;
    return result * 100;
  }
}
