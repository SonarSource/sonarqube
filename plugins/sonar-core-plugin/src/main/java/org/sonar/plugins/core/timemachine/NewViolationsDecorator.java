/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.core.timemachine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class NewViolationsDecorator implements Decorator {

  private TimeMachineConfiguration timeMachineConfiguration;

  public NewViolationsDecorator(TimeMachineConfiguration timeMachineConfiguration) {
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @DependedUpon
  public List<Metric> generatesMetric() {
    return Arrays.asList(
        CoreMetrics.NEW_VIOLATIONS,
        CoreMetrics.NEW_BLOCKER_VIOLATIONS,
        CoreMetrics.NEW_CRITICAL_VIOLATIONS,
        CoreMetrics.NEW_MAJOR_VIOLATIONS,
        CoreMetrics.NEW_MINOR_VIOLATIONS,
        CoreMetrics.NEW_INFO_VIOLATIONS);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource, context)) {
      computeNewViolations(context);
      computeNewViolationsPerSeverity(context);
      computeNewViolationsPerRule(context);
    }
  }

  private boolean shouldDecorateResource(Resource resource, DecoratorContext context) {
    return (StringUtils.equals(Scopes.PROJECT, resource.getScope()) || StringUtils.equals(Scopes.DIRECTORY, resource.getScope()) || StringUtils
        .equals(Scopes.FILE, resource.getScope()))
      && !ResourceUtils.isUnitTestClass(resource)
      && context.getMeasure(CoreMetrics.NEW_VIOLATIONS) == null;
  }

  private void computeNewViolations(DecoratorContext context) {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
    for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      int variationIndex = pastSnapshot.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS);
      int count = countViolations(context.getViolations(), pastSnapshot.getTargetDate());
      double sum = MeasureUtils.sumOnVariation(true, variationIndex, children) + count;
      measure.setVariation(variationIndex, sum);
    }
    context.saveMeasure(measure);
  }

  private void computeNewViolationsPerSeverity(DecoratorContext context) {
    ListMultimap<RulePriority, Violation> violationsPerSeverities = ArrayListMultimap.create();
    for (Violation violation : context.getViolations()) {
      violationsPerSeverities.put(violation.getSeverity(), violation);
    }

    for (RulePriority severity : RulePriority.values()) {
      Metric metric = severityToMetric(severity);
      Measure measure = new Measure(metric);
      for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
        int variationIndex = pastSnapshot.getIndex();
        int count = countViolations(violationsPerSeverities.get(severity), pastSnapshot.getTargetDate());
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
        double sum = MeasureUtils.sumOnVariation(true, variationIndex, children) + count;
        measure.setVariation(variationIndex, sum);
      }
      context.saveMeasure(measure);
    }
  }

  private void computeNewViolationsPerRule(DecoratorContext context) {
    for (RulePriority severity : RulePriority.values()) {
      Metric metric = severityToMetric(severity);
      ListMultimap<Rule, Measure> childMeasuresPerRule = ArrayListMultimap.create();
      ListMultimap<Rule, Violation> violationsPerRule = ArrayListMultimap.create();
      Set<Rule> rules = Sets.newHashSet();

      Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(metric));
      for (Measure child : children) {
        RuleMeasure childRuleMeasure = (RuleMeasure) child;
        Rule rule = childRuleMeasure.getRule();
        if (rule != null) {
          childMeasuresPerRule.put(rule, childRuleMeasure);
          rules.add(rule);
        }
      }

      for (Violation violation : context.getViolations()) {
        if (violation.getSeverity().equals(severity)) {
          rules.add(violation.getRule());
          violationsPerRule.put(violation.getRule(), violation);
        }
      }

      for (Rule rule : rules) {
        RuleMeasure measure = RuleMeasure.createForRule(metric, rule, null);
        measure.setRulePriority(severity);
        for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
          int variationIndex = pastSnapshot.getIndex();
          int count = countViolations(violationsPerRule.get(rule), pastSnapshot.getTargetDate());
          double sum = MeasureUtils.sumOnVariation(true, variationIndex, childMeasuresPerRule.get(rule)) + count;
          measure.setVariation(variationIndex, sum);
        }
        context.saveMeasure(measure);
      }
    }
  }

  int countViolations(Collection<Violation> violations, Date targetDate) {
    if (violations == null) {
      return 0;
    }
    int count = 0;
    for (Violation violation : violations) {
      if (isAfter(violation, targetDate)) {
        count++;
      }
    }
    return count;
  }

  private boolean isAfter(Violation violation, Date date) {
    if (date == null) {
      return true;
    }
    return violation.getCreatedAt() != null && violation.getCreatedAt().after(date);
  }

  private Metric severityToMetric(RulePriority severity) {
    Metric metric;
    if (severity.equals(RulePriority.BLOCKER)) {
      metric = CoreMetrics.NEW_BLOCKER_VIOLATIONS;
    } else if (severity.equals(RulePriority.CRITICAL)) {
      metric = CoreMetrics.NEW_CRITICAL_VIOLATIONS;
    } else if (severity.equals(RulePriority.MAJOR)) {
      metric = CoreMetrics.NEW_MAJOR_VIOLATIONS;
    } else if (severity.equals(RulePriority.MINOR)) {
      metric = CoreMetrics.NEW_MINOR_VIOLATIONS;
    } else if (severity.equals(RulePriority.INFO)) {
      metric = CoreMetrics.NEW_INFO_VIOLATIONS;
    } else {
      throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
    return metric;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
