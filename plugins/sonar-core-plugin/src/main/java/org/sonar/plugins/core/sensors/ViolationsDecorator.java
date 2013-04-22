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
package org.sonar.plugins.core.sensors;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.sonar.api.batch.*;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class ViolationsDecorator implements Decorator {

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesViolationsMetrics() {
    return Arrays.asList(CoreMetrics.VIOLATIONS,
      CoreMetrics.BLOCKER_VIOLATIONS,
      CoreMetrics.CRITICAL_VIOLATIONS,
      CoreMetrics.MAJOR_VIOLATIONS,
      CoreMetrics.MINOR_VIOLATIONS,
      CoreMetrics.INFO_VIOLATIONS);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    computeTotalViolations(context);
    computeViolationsPerSeverities(context);
    computeViolationsPerRules(context);
  }

  private void computeTotalViolations(DecoratorContext context) {
    if (context.getMeasure(CoreMetrics.VIOLATIONS) == null) {
      Collection<Measure> childrenViolations = context.getChildrenMeasures(CoreMetrics.VIOLATIONS);
      Double sum = MeasureUtils.sum(true, childrenViolations);
      context.saveMeasure(CoreMetrics.VIOLATIONS, sum + context.getViolations().size());
    }
  }

  private void computeViolationsPerSeverities(DecoratorContext context) {
    Multiset<RulePriority> severitiesBag = HashMultiset.create();
    for (Violation violation : context.getViolations()) {
      severitiesBag.add(violation.getSeverity());
    }

    for (RulePriority severity : RulePriority.values()) {
      Metric metric = SeverityUtils.severityToViolationMetric(severity);
      if (context.getMeasure(metric) == null) {
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
        int sum = MeasureUtils.sum(true, children).intValue() + severitiesBag.count(severity);
        context.saveMeasure(metric, (double) sum);
      }
    }
  }

  private void computeViolationsPerRules(DecoratorContext context) {
    Map<RulePriority, Multiset<Rule>> rulesPerSeverity = Maps.newHashMap();
    for (Violation violation : context.getViolations()) {
      Multiset<Rule> rulesBag = initRules(rulesPerSeverity, violation.getSeverity());
      rulesBag.add(violation.getRule());
    }

    for (RulePriority severity : RulePriority.values()) {
      Metric metric = SeverityUtils.severityToViolationMetric(severity);

      Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(metric));
      for (Measure child : children) {
        RuleMeasure childRuleMeasure = (RuleMeasure) child;
        Rule rule = childRuleMeasure.getRule();
        if (rule != null && MeasureUtils.hasValue(childRuleMeasure)) {
          Multiset<Rule> rulesBag = initRules(rulesPerSeverity, severity);
          rulesBag.add(rule, childRuleMeasure.getIntValue());
        }
      }

      Multiset<Rule> rulesBag = rulesPerSeverity.get(severity);
      if (rulesBag != null) {
        for (Multiset.Entry<Rule> entry : rulesBag.entrySet()) {
          RuleMeasure measure = RuleMeasure.createForRule(metric, entry.getElement(), (double) entry.getCount());
          measure.setSeverity(severity);
          context.saveMeasure(measure);
        }
      }
    }
  }

  private Multiset<Rule> initRules(Map<RulePriority, Multiset<Rule>> rulesPerSeverity, RulePriority severity) {
    Multiset<Rule> rulesBag = rulesPerSeverity.get(severity);
    if (rulesBag == null) {
      rulesBag = HashMultiset.create();
      rulesPerSeverity.put(severity, rulesBag);
    }
    return rulesBag;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
