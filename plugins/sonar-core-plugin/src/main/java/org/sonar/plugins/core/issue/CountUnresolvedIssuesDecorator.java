/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.plugins.core.issue;

import org.sonar.batch.components.Period;

import org.sonar.batch.components.TimeMachineConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RulePriority;

import javax.annotation.Nullable;

import java.util.*;

/**
 * Computes metrics related to number of issues.
 *
 * @since 3.6
 */
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public class CountUnresolvedIssuesDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final TimeMachineConfiguration timeMachineConfiguration;

  public CountUnresolvedIssuesDecorator(ResourcePerspectives perspectives, TimeMachineConfiguration timeMachineConfiguration) {
    this.perspectives = perspectives;
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesIssuesMetrics() {
    return ImmutableList.<Metric>of(
      CoreMetrics.VIOLATIONS,
      CoreMetrics.BLOCKER_VIOLATIONS,
      CoreMetrics.CRITICAL_VIOLATIONS,
      CoreMetrics.MAJOR_VIOLATIONS,
      CoreMetrics.MINOR_VIOLATIONS,
      CoreMetrics.INFO_VIOLATIONS,
      CoreMetrics.NEW_VIOLATIONS,
      CoreMetrics.NEW_BLOCKER_VIOLATIONS,
      CoreMetrics.NEW_CRITICAL_VIOLATIONS,
      CoreMetrics.NEW_MAJOR_VIOLATIONS,
      CoreMetrics.NEW_MINOR_VIOLATIONS,
      CoreMetrics.NEW_INFO_VIOLATIONS,
      CoreMetrics.OPEN_ISSUES,
      CoreMetrics.REOPENED_ISSUES,
      CoreMetrics.CONFIRMED_ISSUES
      );
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null) {
      Collection<Issue> issues = issuable.issues();
      boolean shouldSaveNewMetrics = shouldSaveNewMetrics(context);

      Multiset<RulePriority> severityBag = HashMultiset.create();
      Map<RulePriority, Multiset<RuleKey>> rulesPerSeverity = Maps.newHashMap();
      ListMultimap<RulePriority, Issue> issuesPerSeverity = ArrayListMultimap.create();
      int countOpen = 0;
      int countReopened = 0;
      int countConfirmed = 0;

      for (Issue issue : issues) {
        severityBag.add(RulePriority.valueOf(issue.severity()));
        Multiset<RuleKey> rulesBag = initRules(rulesPerSeverity, RulePriority.valueOf(issue.severity()));
        rulesBag.add(issue.ruleKey());
        issuesPerSeverity.put(RulePriority.valueOf(issue.severity()), issue);

        if (Issue.STATUS_OPEN.equals(issue.status())) {
          countOpen++;
        } else if (Issue.STATUS_REOPENED.equals(issue.status())) {
          countReopened++;
        } else if (Issue.STATUS_CONFIRMED.equals(issue.status())) {
          countConfirmed++;
        }
      }

      for (RulePriority ruleSeverity : RulePriority.values()) {
        saveIssuesForSeverity(context, ruleSeverity, severityBag);
        saveIssuesPerRules(context, ruleSeverity, rulesPerSeverity);
        saveNewIssuesForSeverity(context, ruleSeverity, issuesPerSeverity, shouldSaveNewMetrics);
        saveNewIssuesPerRule(context, ruleSeverity, issues, shouldSaveNewMetrics);
      }

      saveTotalIssues(context, issues);
      saveNewIssues(context, issues, shouldSaveNewMetrics);

      saveMeasure(context, CoreMetrics.OPEN_ISSUES, countOpen);
      saveMeasure(context, CoreMetrics.REOPENED_ISSUES, countReopened);
      saveMeasure(context, CoreMetrics.CONFIRMED_ISSUES, countConfirmed);
    }
  }

  private void saveTotalIssues(DecoratorContext context, Collection<Issue> issues) {
    if (context.getMeasure(CoreMetrics.VIOLATIONS) == null) {
      Collection<Measure> childrenIssues = context.getChildrenMeasures(CoreMetrics.VIOLATIONS);
      Double sum = MeasureUtils.sum(true, childrenIssues);
      context.saveMeasure(CoreMetrics.VIOLATIONS, sum + issues.size());
    }
  }

  private void saveNewIssues(DecoratorContext context, Collection<Issue> issues, boolean shouldSaveNewMetrics) {
    if (shouldSaveNewMetrics) {
      Measure measure = new Measure(CoreMetrics.NEW_VIOLATIONS);
      saveNewIssues(context, measure, issues);
    }
  }

  private void saveIssuesForSeverity(DecoratorContext context, RulePriority ruleSeverity, Multiset<RulePriority> severitiesBag) {
    Metric metric = SeverityUtils.severityToIssueMetric(ruleSeverity);
    if (context.getMeasure(metric) == null) {
      Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
      int sum = MeasureUtils.sum(true, children).intValue() + severitiesBag.count(ruleSeverity);
      context.saveMeasure(metric, (double) sum);
    }
  }

  private void saveNewIssuesForSeverity(DecoratorContext context, RulePriority severity, ListMultimap<RulePriority, Issue> issuesPerSeverities, boolean shouldSaveNewMetrics) {
    if (shouldSaveNewMetrics) {
      Metric metric = SeverityUtils.severityToNewMetricIssue(severity);
      Measure measure = new Measure(metric);
      saveNewIssues(context, measure, issuesPerSeverities.get(severity));
    }
  }

  private void saveIssuesPerRules(DecoratorContext context, RulePriority severity, Map<RulePriority, Multiset<RuleKey>> rulesPerSeverity) {
    Metric metric = SeverityUtils.severityToIssueMetric(severity);

    Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(metric));
    for (Measure child : children) {
      RuleMeasure childRuleMeasure = (RuleMeasure) child;
      RuleKey ruleKey = childRuleMeasure.ruleKey();
      if (ruleKey != null && MeasureUtils.hasValue(childRuleMeasure)) {
        Multiset<RuleKey> rulesBag = initRules(rulesPerSeverity, severity);
        rulesBag.add(ruleKey, childRuleMeasure.getIntValue());
      }
    }

    Multiset<RuleKey> rulesBag = rulesPerSeverity.get(severity);
    if (rulesBag != null) {
      for (Multiset.Entry<RuleKey> entry : rulesBag.entrySet()) {
        RuleMeasure measure = RuleMeasure.createForRule(metric, entry.getElement(), (double) entry.getCount());
        measure.setSeverity(severity);
        context.saveMeasure(measure);
      }
    }
  }

  private void saveNewIssuesPerRule(DecoratorContext context, RulePriority severity, Collection<Issue> issues, boolean shouldSaveNewMetrics) {
    if (shouldSaveNewMetrics) {
      Metric metric = SeverityUtils.severityToNewMetricIssue(severity);
      ListMultimap<RuleKey, Measure> childMeasuresPerRuleKeys = ArrayListMultimap.create();
      ListMultimap<RuleKey, Issue> issuesPerRuleKeys = ArrayListMultimap.create();
      Set<RuleKey> ruleKeys = Sets.newHashSet();

      Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.rules(metric));
      for (Measure child : children) {
        RuleMeasure childRuleMeasure = (RuleMeasure) child;
        RuleKey ruleKey = childRuleMeasure.ruleKey();
        if (ruleKey != null) {
          childMeasuresPerRuleKeys.put(ruleKey, childRuleMeasure);
          ruleKeys.add(ruleKey);
        }
      }

      for (Issue issue : issues) {
        if (RulePriority.valueOf(issue.severity()).equals(severity)) {
          ruleKeys.add(issue.ruleKey());
          issuesPerRuleKeys.put(issue.ruleKey(), issue);
        }
      }

      for (RuleKey ruleKey : ruleKeys) {
        RuleMeasure measure = RuleMeasure.createForRule(metric, ruleKey, null);
        measure.setSeverity(severity);
        for (Period period : timeMachineConfiguration.periods()) {
          int variationIndex = period.getIndex();
          double sum = MeasureUtils.sumOnVariation(true, variationIndex, childMeasuresPerRuleKeys.get(ruleKey)) + countIssues(issuesPerRuleKeys.get(ruleKey), period);
          measure.setVariation(variationIndex, sum);
        }
        context.saveMeasure(measure);
      }
    }
  }

  private void saveNewIssues(DecoratorContext context, Measure measure, Collection<Issue> issues) {
    for (Period period : timeMachineConfiguration.periods()) {
      int variationIndex = period.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(measure.getMetric());
      double sum = MeasureUtils.sumOnVariation(true, variationIndex, children) + countIssues(issues, period);
      measure.setVariation(variationIndex, sum);
    }
    context.saveMeasure(measure);
  }

  private void saveMeasure(DecoratorContext context, Metric metric, int value) {
    context.saveMeasure(metric, (double) (value + sumChildren(context, metric)));
  }

  private int sumChildren(DecoratorContext context, Metric metric) {
    int sum = 0;
    if (!ResourceUtils.isFile(context.getResource())) {
      sum = MeasureUtils.sum(true, context.getChildrenMeasures(metric)).intValue();
    }
    return sum;
  }

  private Multiset<RuleKey> initRules(Map<RulePriority, Multiset<RuleKey>> rulesPerSeverity, RulePriority severity) {
    Multiset<RuleKey> rulesBag = rulesPerSeverity.get(severity);
    if (rulesBag == null) {
      rulesBag = HashMultiset.create();
      rulesPerSeverity.put(severity, rulesBag);
    }
    return rulesBag;
  }

  private int countIssues(Collection<Issue> issues, Period period) {
    // SONAR-3647 Use real snapshot date and not target date in order to stay consistent with other measure variations
    Date datePlusOneSecond = period.getDate() != null ? DateUtils.addSeconds(period.getDate(), 1) : null;
    return countIssuesAfterDate(issues, datePlusOneSecond);
  }

  @VisibleForTesting
  int countIssuesAfterDate(Collection<Issue> issues, @Nullable Date date) {
    if (issues == null) {
      return 0;
    }
    int count = 0;
    for (Issue issue : issues) {
      if (isAfter(issue, date)) {
        count++;
      }
    }
    return count;
  }

  private boolean isAfter(Issue issue, @Nullable Date date) {
    return date == null || (issue.creationDate() != null && DateUtils.truncatedCompareTo(issue.creationDate(), date, Calendar.SECOND) > 0);
  }

  private boolean shouldSaveNewMetrics(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.NEW_VIOLATIONS) == null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
