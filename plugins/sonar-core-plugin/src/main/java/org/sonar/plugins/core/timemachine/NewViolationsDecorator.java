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
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
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
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class NewViolationsDecorator implements Decorator {

  private TimeMachineConfiguration timeMachineConfiguration;
  private NotificationManager notificationManager;

  public NewViolationsDecorator(TimeMachineConfiguration timeMachineConfiguration, NotificationManager notificationManager) {
    this.timeMachineConfiguration = timeMachineConfiguration;
    this.notificationManager = notificationManager;
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

  @SuppressWarnings("rawtypes")
  public void decorate(Resource resource, DecoratorContext context) {
    if (shouldDecorateResource(resource, context)) {
      computeNewViolations(context);
      computeNewViolationsPerSeverity(context);
      computeNewViolationsPerRule(context);
    }
    if (ResourceUtils.isRootProject(resource)) {
      notifyNewViolations((Project) resource, context);
    }
  }

  private boolean shouldDecorateResource(Resource<?> resource, DecoratorContext context) {
    return (StringUtils.equals(Scopes.PROJECT, resource.getScope()) || StringUtils.equals(Scopes.DIRECTORY, resource.getScope()) || StringUtils
        .equals(Scopes.FILE, resource.getScope()))
      && (context.getMeasure(CoreMetrics.NEW_VIOLATIONS) == null);
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
        measure.setSeverity(severity);
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

  protected void notifyNewViolations(Project project, DecoratorContext context) {
    List<PastSnapshot> projectPastSnapshots = timeMachineConfiguration.getProjectPastSnapshots();
    if (projectPastSnapshots.size() >= 1) {
      // we always check new violations against period1
      PastSnapshot pastSnapshot = projectPastSnapshots.get(0);
      Double newViolationsCount = context.getMeasure(CoreMetrics.NEW_VIOLATIONS).getVariation1();
      // Do not send notification if this is the first analysis or if there's no violation
      if (pastSnapshot.getTargetDate() != null && newViolationsCount != null && newViolationsCount > 0) {
        // Maybe we should check if this is the first analysis or not?
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        Notification notification = new Notification("new-violations")
            .setDefaultMessage(newViolationsCount.intValue() + " new violations on " + project.getLongName() + ".")
            .setFieldValue("count", String.valueOf(newViolationsCount.intValue()))
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
