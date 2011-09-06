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

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleUtils;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Map;

public class WeightedViolationsDecorator implements Decorator {

  private Map<RulePriority, Double> weights;


  @DependsUpon
  public Metric dependsUponViolations() {
    return CoreMetrics.VIOLATIONS;
  }

  @DependedUpon
  public Metric generatesWeightedViolations() {
    return CoreMetrics.WEIGHTED_VIOLATIONS;
  }

  public WeightedViolationsDecorator() {
  }

  /**
   * for unit tests
   */
  protected WeightedViolationsDecorator(Map<RulePriority, Double> weights) {
    this.weights = weights;
  }

  private void loadWeights(DecoratorContext context) {
    if (weights == null && context != null) {
      weights = RuleUtils.getPriorityWeights(context.getProject().getConfiguration());
    }
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    loadWeights(context);

    double debt = 0.0;
    Multiset<RulePriority> violationsByPriority = TreeMultiset.create();

    for (RuleMeasure violations : context.getMeasures(MeasuresFilters.rules(CoreMetrics.VIOLATIONS))) {
      if (MeasureUtils.hasValue(violations)) {
        violationsByPriority.add(violations.getRulePriority(), violations.getValue().intValue());
        double add = (double) weights.get(violations.getRulePriority()) * violations.getValue();
        debt += add;
      }
    }

    Measure debtMeasure = new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, debt, KeyValueFormat.format(violationsByPriority));
    saveMeasure(context, debtMeasure);
  }

  private void saveMeasure(DecoratorContext context, Measure debtMeasure) {
    if (debtMeasure.getValue() > 0.0) {
      context.saveMeasure(debtMeasure);
    }
  }

}
