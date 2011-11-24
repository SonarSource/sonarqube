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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleUtils;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.KeyValueFormat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WeightedViolationsDecorator implements Decorator {

  @DependsUpon
  public List<Metric> dependsUponViolations() {
    return Arrays.asList(CoreMetrics.BLOCKER_VIOLATIONS, CoreMetrics.CRITICAL_VIOLATIONS,
      CoreMetrics.MAJOR_VIOLATIONS, CoreMetrics.MINOR_VIOLATIONS, CoreMetrics.INFO_VIOLATIONS);
  }

  @DependedUpon
  public Metric generatesWeightedViolations() {
    return CoreMetrics.WEIGHTED_VIOLATIONS;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    decorate(context, RuleUtils.getPriorityWeights(context.getProject().getConfiguration()));
  }

  void decorate(DecoratorContext context, Map<RulePriority, Integer> weights) {
    double debt = 0.0;
    Multiset<RulePriority> distribution = TreeMultiset.create();

    for (RulePriority severity : RulePriority.values()) {
      Measure measure = context.getMeasure(severityToMetric(severity));
      if (measure != null && MeasureUtils.hasValue(measure)) {
        distribution.add(severity, measure.getIntValue());
        double add = weights.get(severity) * measure.getIntValue();
        debt += add;
      }
    }

    Measure debtMeasure = new Measure(CoreMetrics.WEIGHTED_VIOLATIONS, debt, KeyValueFormat.format(distribution));
    saveMeasure(context, debtMeasure);
  }

  static Metric severityToMetric(RulePriority severity) {
    Metric metric;
    if (severity.equals(RulePriority.BLOCKER)) {
      metric = CoreMetrics.BLOCKER_VIOLATIONS;
    } else if (severity.equals(RulePriority.CRITICAL)) {
      metric = CoreMetrics.CRITICAL_VIOLATIONS;
    } else if (severity.equals(RulePriority.MAJOR)) {
      metric = CoreMetrics.MAJOR_VIOLATIONS;
    } else if (severity.equals(RulePriority.MINOR)) {
      metric = CoreMetrics.MINOR_VIOLATIONS;
    } else if (severity.equals(RulePriority.INFO)) {
      metric = CoreMetrics.INFO_VIOLATIONS;
    } else {
      throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
    return metric;
  }


  private void saveMeasure(DecoratorContext context, Measure debtMeasure) {
    if (debtMeasure.getValue() > 0.0) {
      context.saveMeasure(debtMeasure);
    }
  }

}
