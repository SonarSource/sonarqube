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
package org.sonar.batch.issue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class IssuesDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final RuleFinder rulefinder;

  public IssuesDecorator(ResourcePerspectives perspectives, RuleFinder rulefinder) {
    this.perspectives = perspectives;
    this.rulefinder = rulefinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesIssuesMetrics() {
    return Arrays.asList(CoreMetrics.ISSUES,
      CoreMetrics.BLOCKER_ISSUES,
      CoreMetrics.CRITICAL_ISSUES,
      CoreMetrics.MAJOR_ISSUES,
      CoreMetrics.MINOR_ISSUES,
      CoreMetrics.INFO_ISSUES);
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null) {
      Collection<Issue> issues = issuable.issues();
      computeTotalIssues(context, issues);
      computeIssuesPerSeverities(context, issues);
      computeIssuesPerRules(context, issues);
    }
  }

  private void computeTotalIssues(DecoratorContext context, Collection<Issue> issues) {
    if (context.getMeasure(CoreMetrics.ISSUES) == null) {
      Collection<Measure> childrenIssues = context.getChildrenMeasures(CoreMetrics.ISSUES);
      Double sum = MeasureUtils.sum(true, childrenIssues);
      context.saveMeasure(CoreMetrics.ISSUES, sum + issues.size());
    }
  }

  private void computeIssuesPerSeverities(DecoratorContext context, Collection<Issue> issues) {
    Multiset<RulePriority> severitiesBag = HashMultiset.create();
    for (Issue issue : issues) {
      severitiesBag.add(RulePriority.valueOf(issue.severity()));
    }

    for (RulePriority ruleSeverity : RulePriority.values()) {
      Metric metric = SeverityUtils.severityToIssueMetric(ruleSeverity);
      if (context.getMeasure(metric) == null) {
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
        int sum = MeasureUtils.sum(true, children).intValue() + severitiesBag.count(ruleSeverity);
        context.saveMeasure(metric, (double) sum);
      }
    }
  }

  private void computeIssuesPerRules(DecoratorContext context, Collection<Issue> issues) {
    Map<RulePriority, Multiset<Rule>> rulesPerSeverity = Maps.newHashMap();
    for (Issue issue : issues) {
      Multiset<Rule> rulesBag = initRules(rulesPerSeverity, RulePriority.valueOf(issue.severity()));
      rulesBag.add(rulefinder.findByKey(issue.ruleRepositoryKey(), issue.ruleKey()));
    }

    for (RulePriority severity : RulePriority.values()) {
      Metric metric = SeverityUtils.severityToIssueMetric(severity);

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
