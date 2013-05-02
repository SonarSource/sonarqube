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
package org.sonar.plugins.core.issue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.*;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Computes metrics related to number of issues.
 *
 * @since 3.6
 */
@DependsUpon(DecoratorBarriers.END_OF_ISSUES_UPDATES)
public class IssueCountersDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final RuleFinder rulefinder;
  private final TimeMachineConfiguration timeMachineConfiguration;

  public IssueCountersDecorator(ResourcePerspectives perspectives, RuleFinder rulefinder, TimeMachineConfiguration timeMachineConfiguration) {
    this.perspectives = perspectives;
    this.rulefinder = rulefinder;
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesIssuesMetrics() {
    return ImmutableList.of(
      CoreMetrics.ISSUES,
      CoreMetrics.BLOCKER_ISSUES,
      CoreMetrics.CRITICAL_ISSUES,
      CoreMetrics.MAJOR_ISSUES,
      CoreMetrics.MINOR_ISSUES,
      CoreMetrics.INFO_ISSUES,
      CoreMetrics.NEW_ISSUES,
      CoreMetrics.NEW_BLOCKER_ISSUES,
      CoreMetrics.NEW_CRITICAL_ISSUES,
      CoreMetrics.NEW_MAJOR_ISSUES,
      CoreMetrics.NEW_MINOR_ISSUES,
      CoreMetrics.NEW_INFO_ISSUES,
      CoreMetrics.FALSE_POSITIVE_ISSUES,
      CoreMetrics.UNASSIGNED_ISSUES
    );
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null) {
      Collection<Issue> issues = getOpenIssues(issuable.issues());
      boolean shouldSaveNewMetrics = shouldSaveNewMetrics(context);

      Multiset<RulePriority> severitiesBag = HashMultiset.create();
      Map<RulePriority, Multiset<Rule>> rulesPerSeverity = Maps.newHashMap();
      ListMultimap<RulePriority, Issue> issuesPerSeverities = ArrayListMultimap.create();
      int countUnassigned = 0;
      int falsePositives = 0;

      for (Issue issue : issues) {
        severitiesBag.add(RulePriority.valueOf(issue.severity()));
        Multiset<Rule> rulesBag = initRules(rulesPerSeverity, RulePriority.valueOf(issue.severity()));
        rulesBag.add(rulefinder.findByKey(issue.ruleKey().repository(), issue.ruleKey().rule()));
        issuesPerSeverities.put(RulePriority.valueOf(issue.severity()), issue);

        if (issue.assignee() == null) {
          countUnassigned++;
        }
        if (Issue.RESOLUTION_FALSE_POSITIVE.equals(issue.resolution())) {
          falsePositives++;
        }
      }

      for (RulePriority ruleSeverity : RulePriority.values()) {
        saveIssuesForSeverity(context, ruleSeverity, severitiesBag);
        saveIssuesPerRules(context, ruleSeverity, rulesPerSeverity);
        saveNewIssuesForSeverity(context, ruleSeverity, issuesPerSeverities, shouldSaveNewMetrics);
        saveNewIssuesPerRule(context, ruleSeverity, issues, shouldSaveNewMetrics);
      }

      saveTotalIssues(context, issues);
      saveNewIssues(context, issues, shouldSaveNewMetrics);
      saveUnassignedIssues(context, countUnassigned);
      saveFalsePositiveIssues(context, falsePositives);
    }
  }

  private void saveTotalIssues(DecoratorContext context, Collection<Issue> issues) {
    if (context.getMeasure(CoreMetrics.ISSUES) == null) {
      Collection<Measure> childrenIssues = context.getChildrenMeasures(CoreMetrics.ISSUES);
      Double sum = MeasureUtils.sum(true, childrenIssues);
      context.saveMeasure(CoreMetrics.ISSUES, sum + issues.size());
    }
  }

  private void saveNewIssues(DecoratorContext context, Collection<Issue> issues, boolean shouldSaveNewMetrics) {
    if (shouldSaveNewMetrics) {
      Measure measure = new Measure(CoreMetrics.NEW_ISSUES);
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

  private void saveIssuesPerRules(DecoratorContext context, RulePriority severity, Map<RulePriority, Multiset<Rule>> rulesPerSeverity) {
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

  private void saveNewIssuesPerRule(DecoratorContext context, RulePriority severity, Collection<Issue> issues, boolean shouldSaveNewMetrics) {
    if (shouldSaveNewMetrics) {
      Metric metric = SeverityUtils.severityToNewMetricIssue(severity);
      ListMultimap<Rule, Measure> childMeasuresPerRule = ArrayListMultimap.create();
      ListMultimap<Rule, Issue> issuesPerRule = ArrayListMultimap.create();
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

      for (Issue issue : issues) {
        if (RulePriority.valueOf(issue.severity()).equals(severity)) {
          Rule rule = rulefinder.findByKey(issue.ruleKey().repository(), issue.ruleKey().rule());
          rules.add(rule);
          issuesPerRule.put(rule, issue);
        }
      }

      for (Rule rule : rules) {
        RuleMeasure measure = RuleMeasure.createForRule(metric, rule, null);
        measure.setSeverity(severity);
        for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
          int variationIndex = pastSnapshot.getIndex();
          int count = countIssuesAfterDate(issuesPerRule.get(rule), pastSnapshot.getTargetDate());
          double sum = MeasureUtils.sumOnVariation(true, variationIndex, childMeasuresPerRule.get(rule)) + count;
          measure.setVariation(variationIndex, sum);
        }
        context.saveMeasure(measure);
      }
    }
  }

  private void saveNewIssues(DecoratorContext context, Measure measure, Collection<Issue> issues) {
    for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      int variationIndex = pastSnapshot.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(measure.getMetric());
      int count = countIssuesAfterDate(issues, pastSnapshot.getTargetDate());
      double sum = MeasureUtils.sumOnVariation(true, variationIndex, children) + count;
      measure.setVariation(variationIndex, sum);
    }
    context.saveMeasure(measure);
  }

  private void saveUnassignedIssues(DecoratorContext context, int countUnassigned) {
    context.saveMeasure(CoreMetrics.UNASSIGNED_ISSUES, (double) (countUnassigned + sumChildren(context, CoreMetrics.UNASSIGNED_ISSUES)));
  }

  private void saveFalsePositiveIssues(DecoratorContext context, int falsePositives) {
    context.saveMeasure(CoreMetrics.FALSE_POSITIVE_ISSUES, (double) (falsePositives + sumChildren(context, CoreMetrics.FALSE_POSITIVE_ISSUES)));
  }

  private int sumChildren(DecoratorContext context, Metric metric) {
    int sum = 0;
    if (!ResourceUtils.isFile(context.getResource())) {
      sum = MeasureUtils.sum(true, context.getChildrenMeasures(metric)).intValue();
    }
    return sum;
  }

  private Multiset<Rule> initRules(Map<RulePriority, Multiset<Rule>> rulesPerSeverity, RulePriority severity) {
    Multiset<Rule> rulesBag = rulesPerSeverity.get(severity);
    if (rulesBag == null) {
      rulesBag = HashMultiset.create();
      rulesPerSeverity.put(severity, rulesBag);
    }
    return rulesBag;
  }

  @VisibleForTesting
  int countIssuesAfterDate(Collection<Issue> issues, Date targetDate) {
    if (issues == null) {
      return 0;
    }
    int count = 0;
    for (Issue issue : issues) {
      if (isAfter(issue, targetDate)) {
        count++;
      }
    }
    return count;
  }

  private boolean isAfter(Issue issue, @Nullable Date date) {
    return date == null || issue.creationDate() != null && issue.creationDate().after(date);
  }

  private boolean shouldSaveNewMetrics(DecoratorContext context) {
    return context.getProject().isLatestAnalysis() && context.getMeasure(CoreMetrics.NEW_ISSUES) == null;
  }

  private Collection<Issue> getOpenIssues(Collection<Issue> issues) {
    return newArrayList(Iterables.filter(issues, new Predicate<Issue>() {
      @Override
      public boolean apply(final Issue issue) {
        return !Issue.STATUS_CLOSED.equals(issue.status());
      }
    }));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
