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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;

import java.util.*;

@DependsUpon(classes = ViolationPersisterDecorator.class)
public class NewViolationsDecorator implements Decorator {

  private TimeMachineConfiguration timeMachineConfiguration;

  // temporary data for current resource
  private Map<Rule, RulePriority> ruleToLevel = Maps.newHashMap();

  public NewViolationsDecorator(TimeMachineConfiguration timeMachineConfiguration) {
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @DependedUpon
  public List<Metric> generatesMetric() {
    return Arrays.asList(CoreMetrics.NEW_VIOLATIONS,
      CoreMetrics.NEW_BLOCKER_VIOLATIONS, CoreMetrics.NEW_CRITICAL_VIOLATIONS, CoreMetrics.NEW_MAJOR_VIOLATIONS, CoreMetrics.NEW_MINOR_VIOLATIONS, CoreMetrics.NEW_INFO_VIOLATIONS);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    resetCache();
    saveNewViolations(context);
    saveNewViolationsByPriority(context);
    saveNewViolationsByRule(context);
  }

  private void resetCache() {
    ruleToLevel.clear();
  }

  private void saveNewViolations(DecoratorContext context) {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
    for (PastSnapshot variationSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      int variationIndex = variationSnapshot.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS);
      int value = countViolations(context.getViolations(), variationSnapshot.getDate());
      double sum = sumChildren(variationIndex, children) + value;
      measure.setVariation(variationIndex, sum);
    }
    context.saveMeasure(measure);
  }

  private void saveNewViolationsByPriority(DecoratorContext context) {
    for (RulePriority priority : RulePriority.values()) {
      Measure measure1 = new Measure(getMetricForPriority(priority));
      Measure measure2 = RuleMeasure.createForPriority(CoreMetrics.NEW_VIOLATIONS, priority, null);
      for (PastSnapshot variationSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
        int variationIndex = variationSnapshot.getIndex();
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rulePriority(CoreMetrics.NEW_VIOLATIONS, priority));
        int count = countViolations(context.getViolations(), variationSnapshot.getDate(), priority);
        double sum = sumChildren(variationIndex, children) + count;
        measure1.setVariation(variationIndex, sum);
        measure2.setVariation(variationIndex, sum);
      }
      context.saveMeasure(measure1);
      context.saveMeasure(measure2);
    }
  }

  private void saveNewViolationsByRule(DecoratorContext context) {
    ArrayListMultimap<Rule, Measure> childrenByRule = ArrayListMultimap.create();
    Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(CoreMetrics.NEW_VIOLATIONS));
    for (Measure childMeasure : children) {
      RuleMeasure childRuleMeasure = (RuleMeasure) childMeasure;
      Rule rule = childRuleMeasure.getRule();
      if (rule != null && MeasureUtils.hasValue(childRuleMeasure)) {
        childrenByRule.put(rule, childMeasure);
        ruleToLevel.put(childRuleMeasure.getRule(), childRuleMeasure.getRulePriority());
      }
    }

    for (Rule rule : childrenByRule.keys()) {
      RuleMeasure measure = RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, rule, null);
      measure.setRulePriority(ruleToLevel.get(rule));
      for (PastSnapshot variationSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
        int variationIndex = variationSnapshot.getIndex();
        int value = countViolations(context.getViolations(), variationSnapshot.getDate(), rule);
        double sum = sumChildren(variationIndex, childrenByRule.get(rule)) + value;
        measure.setVariation(variationIndex, sum);
      }
      context.saveMeasure(measure);
    }
  }

  int countViolations(Collection<Violation> violations, Date targetDate) {
    int count = 0;
    for (Violation violation : violations) {
      if (isAfter(violation, targetDate)) {
        count++;
      }
    }
    return count;
  }

  int countViolations(List<Violation> violations, Date targetDate, RulePriority priority) {
    int count = 0;
    for (Violation violation : violations) {
      if (isAfter(violation, targetDate) && ObjectUtils.equals(violation.getPriority(), priority)) {
        count++;
      }
    }
    return count;
  }

  int countViolations(List<Violation> violations, Date targetDate, Rule rule) {
    int count = 0;
    for (Violation violation : violations) {
      if (isAfter(violation, targetDate) && ObjectUtils.equals(violation.getRule(), rule)) {
        count++;
      }
    }
    return count;
  }

  private boolean isAfter(Violation violation, Date date) {
    return !violation.getCreatedAt().before(date);
  }

  int sumChildren(int variationIndex, Collection<Measure> measures) {
    int sum = 0;
    for (Measure measure : measures) {
      Double var = measure.getVariation(variationIndex);
      if (var != null) {
        sum += var.intValue();
      }
    }
    return sum;
  }

  private Metric getMetricForPriority(RulePriority priority) {
    Metric metric = null;
    if (priority.equals(RulePriority.BLOCKER)) {
      metric = CoreMetrics.NEW_BLOCKER_VIOLATIONS;
    } else if (priority.equals(RulePriority.CRITICAL)) {
      metric = CoreMetrics.NEW_CRITICAL_VIOLATIONS;
    } else if (priority.equals(RulePriority.MAJOR)) {
      metric = CoreMetrics.NEW_MAJOR_VIOLATIONS;
    } else if (priority.equals(RulePriority.MINOR)) {
      metric = CoreMetrics.NEW_MINOR_VIOLATIONS;
    } else if (priority.equals(RulePriority.INFO)) {
      metric = CoreMetrics.NEW_INFO_VIOLATIONS;
    }
    return metric;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
