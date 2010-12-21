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

import com.google.common.collect.*;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;

import java.util.*;

@DependsUpon(classes = ViolationPersisterDecorator.class)
public class NewViolationsDecorator implements Decorator {

  private TimeMachineConfiguration timeMachineConfiguration;

  // temporary data for current resource
  private Map<Rule, RulePriority> ruleToLevel = Maps.newHashMap();
  private Multimap<RulePriority, Violation> violationsBySeverity = ArrayListMultimap.create();
  private Multimap<Rule, Violation> violationsByRule = ArrayListMultimap.create();

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
    if (shouldDecorateResource(resource, context)) {
      prepareCurrentResourceViolations(context);
      saveNewViolations(context);
      saveNewViolationsBySeverity(context);
      saveNewViolationsByRule(context);
      clearCache();
    }
  }

  private boolean shouldDecorateResource(Resource resource, DecoratorContext context) {
    return !ResourceUtils.isUnitTestClass(resource) && context.getMeasure(CoreMetrics.NEW_VIOLATIONS) == null;
  }


  private void clearCache() {
    ruleToLevel.clear();
    violationsBySeverity.clear();
    violationsByRule.clear();
  }

  private void prepareCurrentResourceViolations(DecoratorContext context) {
    for (Violation violation : context.getViolations()) {
      violationsBySeverity.put(violation.getSeverity(), violation);
      violationsByRule.put(violation.getRule(), violation);
      ruleToLevel.put(violation.getRule(), violation.getSeverity());
    }
  }

  private void saveNewViolations(DecoratorContext context) {
    Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
    for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      int variationIndex = pastSnapshot.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(CoreMetrics.NEW_VIOLATIONS);
      int count = countViolations(context.getViolations(), pastSnapshot.getTargetDate());
      double sum = sumChildren(variationIndex, children) + count;
      measure.setVariation(variationIndex, sum);
    }
    context.saveMeasure(measure);
  }

  private void saveNewViolationsBySeverity(DecoratorContext context) {
    for (RulePriority priority : RulePriority.values()) {
      Metric metric = getMetricForSeverity(priority);
      Measure measure = new Measure(metric);
      for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
        int variationIndex = pastSnapshot.getIndex();
        int count = countViolations(violationsBySeverity.get(priority), pastSnapshot.getTargetDate());
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
        double sum = sumChildren(variationIndex, children) + count;
        measure.setVariation(variationIndex, sum);
      }
      context.saveMeasure(measure);
    }
  }

  private void saveNewViolationsByRule(DecoratorContext context) {
    ListMultimap<Rule, Measure> childrenByRule = ArrayListMultimap.create();
    Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(CoreMetrics.NEW_VIOLATIONS));
    for (Measure childMeasure : children) {
      RuleMeasure childRuleMeasure = (RuleMeasure) childMeasure;
      Rule rule = childRuleMeasure.getRule();
      if (rule != null) {
        childrenByRule.put(rule, childMeasure);
        ruleToLevel.put(childRuleMeasure.getRule(), childRuleMeasure.getRulePriority());
      }
    }

    Set<Rule> rules = Sets.newHashSet(violationsByRule.keys());
    rules.addAll(childrenByRule.keys());

    for (Rule rule : rules) {
      RuleMeasure measure = RuleMeasure.createForRule(CoreMetrics.NEW_VIOLATIONS, rule, null);
      measure.setRulePriority(ruleToLevel.get(rule));
      for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
        int variationIndex = pastSnapshot.getIndex();
        int count = countViolations(violationsByRule.get(rule), pastSnapshot.getTargetDate());
        double sum = sumChildren(variationIndex, childrenByRule.get(rule)) + count;
        measure.setVariation(variationIndex, sum);
      }
      context.saveMeasure(measure);
    }
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
    return violation.getCreatedAt()!= null && violation.getCreatedAt().after(date);
  }

  private Metric getMetricForSeverity(RulePriority severity) {
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
      throw new IllegalArgumentException("Not supported severity: " + severity);
    }
    return metric;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
