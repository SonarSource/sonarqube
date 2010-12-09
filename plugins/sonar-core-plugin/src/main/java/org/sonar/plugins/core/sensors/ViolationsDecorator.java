/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.sonar.api.batch.*;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@DependsUpon(value = DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class ViolationsDecorator implements Decorator {

  // temporary data for current resource
  private Multiset<Rule> rules = HashMultiset.create();
  private Multiset<RulePriority> severities = HashMultiset.create();
  private Map<Rule, RulePriority> ruleToSeverity = Maps.newHashMap();
  private int total = 0;

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesViolationsMetrics() {
    return Arrays.asList(CoreMetrics.VIOLATIONS,
        CoreMetrics.BLOCKER_VIOLATIONS, CoreMetrics.CRITICAL_VIOLATIONS, CoreMetrics.MAJOR_VIOLATIONS, CoreMetrics.MINOR_VIOLATIONS, CoreMetrics.INFO_VIOLATIONS);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource)) {
      resetCounters();
      countViolations(context);
      saveTotalViolations(context);
      saveViolationsBySeverity(context);
      saveViolationsByRule(context);
    }
  }

  private boolean shouldDecorateResource(Resource resource) {
    return !ResourceUtils.isUnitTestClass(resource);
  }

  private void resetCounters() {
    rules.clear();
    severities.clear();
    ruleToSeverity.clear();
    total = 0;
  }

  private void saveViolationsBySeverity(DecoratorContext context) {
    for (RulePriority severity : RulePriority.values()) {
      Metric metric = getMetricForSeverity(severity);
      if (context.getMeasure(metric) == null) {
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
        double sum = MeasureUtils.sum(true, children) + severities.count(severity);
        context.saveMeasure(new Measure(metric, sum));
      }
    }
  }

  private Metric getMetricForSeverity(RulePriority severity) {
    Metric metric = null;
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
    }
    return metric;
  }

  private void saveViolationsByRule(DecoratorContext context) {
    // See SONAR-1729
    // Extrapolation : assume that the measure with key [metric "violations", rule] does not exist when the measure "violations" does not exist as well.
    if (context.getMeasure(CoreMetrics.VIOLATIONS) == null) {
      Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(CoreMetrics.VIOLATIONS));
      for (Measure childMeasure : children) {
        RuleMeasure childRuleMeasure = (RuleMeasure) childMeasure;
        Rule rule = childRuleMeasure.getRule();
        if (rule != null && MeasureUtils.hasValue(childRuleMeasure)) {
          rules.add(rule, childRuleMeasure.getValue().intValue());
          ruleToSeverity.put(childRuleMeasure.getRule(), childRuleMeasure.getRulePriority());
        }
      }
      for (Multiset.Entry<Rule> entry : rules.entrySet()) {
        Rule rule = entry.getElement();
        RuleMeasure measure = RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, rule, (double) entry.getCount());
        measure.setRulePriority(ruleToSeverity.get(rule));
        context.saveMeasure(measure);
      }
    }
  }

  private void saveTotalViolations(DecoratorContext context) {
    if (context.getMeasure(CoreMetrics.VIOLATIONS) == null) {
      Collection<Measure> childrenViolations = context.getChildrenMeasures(CoreMetrics.VIOLATIONS);
      Double sum = MeasureUtils.sum(true, childrenViolations) + total;
      context.saveMeasure(new Measure(CoreMetrics.VIOLATIONS, sum));
    }
  }

  private void countViolations(DecoratorContext context) {
    List<Violation> violations = context.getViolations();
    for (Violation violation : violations) {
      rules.add(violation.getRule());
      severities.add(violation.getSeverity());
      ruleToSeverity.put(violation.getRule(), violation.getSeverity());
    }
    total = violations.size();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
