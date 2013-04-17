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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.*;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.*;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class NewIssuesDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final RuleFinder rulefinder;
  private TimeMachineConfiguration timeMachineConfiguration;
  private NotificationManager notificationManager;

  public NewIssuesDecorator(TimeMachineConfiguration timeMachineConfiguration, NotificationManager notificationManager, ResourcePerspectives perspectives, RuleFinder rulefinder) {
    this.timeMachineConfiguration = timeMachineConfiguration;
    this.notificationManager = notificationManager;
    this.perspectives = perspectives;
    this.rulefinder = rulefinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @DependedUpon
  public List<Metric> generatesMetric() {
    return Arrays.asList(
        CoreMetrics.NEW_ISSUES,
        CoreMetrics.NEW_BLOCKER_ISSUES,
        CoreMetrics.NEW_CRITICAL_ISSUES,
        CoreMetrics.NEW_MAJOR_ISSUES,
        CoreMetrics.NEW_MINOR_ISSUES,
        CoreMetrics.NEW_INFO_ISSUES);
  }

  @SuppressWarnings("rawtypes")
  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource, context)) {
      Issuable issuable = perspectives.as(Issuable.class, context.getResource());
      if (issuable != null) {
        Collection<Issue> issues = issuable.issues();

        computeNewIssues(context, issues);
        computeNewIssuesPerSeverity(context, issues);
        computeNewIssuesPerRule(context, issues);
      }
    }
    if (ResourceUtils.isRootProject(resource)) {
      notifyNewIssues((Project) resource, context);
    }
  }

  private boolean shouldDecorateResource(Resource<?> resource, DecoratorContext context) {
    return (StringUtils.equals(Scopes.PROJECT, resource.getScope()) || StringUtils.equals(Scopes.DIRECTORY, resource.getScope()) || StringUtils
        .equals(Scopes.FILE, resource.getScope()))
        && (context.getMeasure(CoreMetrics.NEW_ISSUES) == null);
  }

  private void computeNewIssues(DecoratorContext context, Collection<Issue> issues) {
    Measure measure = new Measure(CoreMetrics.NEW_ISSUES);
    for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      int variationIndex = pastSnapshot.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(CoreMetrics.NEW_ISSUES);
      int count = countIssues(issues, pastSnapshot.getTargetDate());
      double sum = MeasureUtils.sumOnVariation(true, variationIndex, children) + count;
      measure.setVariation(variationIndex, sum);
    }
    context.saveMeasure(measure);
  }

  private void computeNewIssuesPerSeverity(DecoratorContext context, Collection<Issue> issues) {
    ListMultimap<RulePriority, Issue> issuesPerSeverities = ArrayListMultimap.create();
    for (Issue issue : issues) {
      issuesPerSeverities.put(RulePriority.valueOf(issue.severity()), issue);
    }

    for (RulePriority severity : RulePriority.values()) {
      Metric metric = severityToMetric(severity);
      Measure measure = new Measure(metric);
      for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
        int variationIndex = pastSnapshot.getIndex();
        int count = countIssues(issuesPerSeverities.get(severity), pastSnapshot.getTargetDate());
        Collection<Measure> children = context.getChildrenMeasures(MeasuresFilters.metric(metric));
        double sum = MeasureUtils.sumOnVariation(true, variationIndex, children) + count;
        measure.setVariation(variationIndex, sum);
      }
      context.saveMeasure(measure);
    }
  }

  private void computeNewIssuesPerRule(DecoratorContext context, Collection<Issue> issues) {
    for (RulePriority severity : RulePriority.values()) {
      Metric metric = severityToMetric(severity);
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
          int count = countIssues(issuesPerRule.get(rule), pastSnapshot.getTargetDate());
          double sum = MeasureUtils.sumOnVariation(true, variationIndex, childMeasuresPerRule.get(rule)) + count;
          measure.setVariation(variationIndex, sum);
        }
        context.saveMeasure(measure);
      }
    }
  }

  int countIssues(Collection<Issue> issues, Date targetDate) {
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

  private boolean isAfter(Issue issue, Date date) {
    if (date == null) {
      return true;
    }
    return issue.createdAt() != null && issue.createdAt().after(date);
  }

  private Metric severityToMetric(RulePriority severity) {
    Metric metric;
    if (severity.equals(RulePriority.BLOCKER)) {
      metric = CoreMetrics.NEW_BLOCKER_ISSUES;
    } else if (severity.equals(RulePriority.CRITICAL)) {
      metric = CoreMetrics.NEW_CRITICAL_ISSUES;
    } else if (severity.equals(RulePriority.MAJOR)) {
      metric = CoreMetrics.NEW_MAJOR_ISSUES;
    } else if (severity.equals(RulePriority.MINOR)) {
      metric = CoreMetrics.NEW_MINOR_ISSUES;
    } else if (severity.equals(RulePriority.INFO)) {
      metric = CoreMetrics.NEW_INFO_ISSUES;
    } else {
      throw new IllegalArgumentException("Unsupported severity: " + severity);
    }
    return metric;
  }

  protected void notifyNewIssues(Project project, DecoratorContext context) {
    List<PastSnapshot> projectPastSnapshots = timeMachineConfiguration.getProjectPastSnapshots();
    if (projectPastSnapshots.size() >= 1) {
      // we always check new issues against period1
      PastSnapshot pastSnapshot = projectPastSnapshots.get(0);
      Double newIssuesCount = context.getMeasure(CoreMetrics.NEW_ISSUES).getVariation1();
      // Do not send notification if this is the first analysis or if there's no violation
      if (pastSnapshot.getTargetDate() != null && newIssuesCount != null && newIssuesCount > 0) {
        // Maybe we should check if this is the first analysis or not?
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        Notification notification = new Notification("new-issues")
            .setDefaultMessage(newIssuesCount.intValue() + " new issues on " + project.getLongName() + ".")
            .setFieldValue("count", String.valueOf(newIssuesCount.intValue()))
            .setFieldValue("projectName", project.getLongName())
            .setFieldValue("projectKey", project.getKey())
            .setFieldValue("projectId", String.valueOf(project.getId()))
            .setFieldValue("fromDate", dateformat.format(pastSnapshot.getTargetDate()));
        notificationManager.scheduleForSending(notification);
      }
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
