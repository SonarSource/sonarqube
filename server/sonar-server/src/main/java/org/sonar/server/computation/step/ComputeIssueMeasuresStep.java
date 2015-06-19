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

package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.rule.Severity;
import org.sonar.batch.protocol.output.BatchReport.Issue;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.REOPENED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.POST_ORDER;

/**
 * Computes metrics related to number of issues.
 * - Total number of issues and new issues
 * - Number of issues by severity, and new issues by severity
 * - Number of false-positives
 */
public class ComputeIssueMeasuresStep implements ComputationStep {

  private final BatchReportReader reportReader;
  private final TreeRootHolder treeRootHolder;
  private final PeriodsHolder periodsHolder;
  private final MeasureRepository measureRepository;
  private final MetricRepository metricRepository;

  private final static Map<String, String> SEVERITY_METRIC_KEY_BY_SEVERITY = ImmutableMap.of(
    Severity.BLOCKER, BLOCKER_VIOLATIONS_KEY,
    Severity.CRITICAL, CRITICAL_VIOLATIONS_KEY,
    Severity.MAJOR, MAJOR_VIOLATIONS_KEY,
    Severity.MINOR, MINOR_VIOLATIONS_KEY,
    Severity.INFO, INFO_VIOLATIONS_KEY
  );

  private final static Map<String, String> NEW_SEVERITY_METRIC_KEY_BY_SEVERITY = ImmutableMap.of(
    Severity.BLOCKER, NEW_BLOCKER_VIOLATIONS_KEY,
    Severity.CRITICAL, NEW_CRITICAL_VIOLATIONS_KEY,
    Severity.MAJOR, NEW_MAJOR_VIOLATIONS_KEY,
    Severity.MINOR, NEW_MINOR_VIOLATIONS_KEY,
    Severity.INFO, NEW_INFO_VIOLATIONS_KEY
  );

  public ComputeIssueMeasuresStep(PeriodsHolder periodsHolder, BatchReportReader reportReader, TreeRootHolder treeRootHolder, MeasureRepository measureRepository,
                                  MetricRepository metricRepository) {
    this.periodsHolder = periodsHolder;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
    this.measureRepository = measureRepository;
    this.metricRepository = metricRepository;
  }

  @Override
  public void execute() {
    new DepthTraversalTypeAwareVisitor(FILE, POST_ORDER) {
      @Override
      public void visitAny(Component component) {
        List<Issue> issues = reportReader.readComponentIssues(component.getRef());
        List<Issue> unresolvedIssues = from(issues)
          .filter(UnresolvedIssue.INSTANCE)
          .toList();
        CountIssues countIssues = new CountIssues(unresolvedIssues);
        addIssuesMeasures(component, unresolvedIssues);
        addIssuesStatusMeasures(component, countIssues);
        addIssuesSeverityMeasures(component, countIssues);
        addFalsePositiveMeasures(component, issues);
      }
    }.visit(treeRootHolder.getRoot());
  }

  private void addIssuesMeasures(Component component, List<Issue> unresolvedIssues) {
    addMeasure(component, VIOLATIONS_KEY, unresolvedIssues.size());
    addNewMeasures(component, NEW_VIOLATIONS_KEY, unresolvedIssues);
  }

  private void addIssuesStatusMeasures(Component component, CountIssues countIssues) {
    addMeasure(component, OPEN_ISSUES_KEY, countIssues.openIssues);
    addMeasure(component, REOPENED_ISSUES_KEY, countIssues.reopenedIssues);
    addMeasure(component, CONFIRMED_ISSUES_KEY, countIssues.confirmedIssues);
  }

  private void addIssuesSeverityMeasures(Component component, CountIssues countIssues) {
    for (Map.Entry<String, String> entry : SEVERITY_METRIC_KEY_BY_SEVERITY.entrySet()) {
      String severity = entry.getKey();
      String metricKey = entry.getValue();
      addMeasure(component, metricKey, countIssues.severityBag.count(severity));
    }
    for (Map.Entry<String, String> entry : NEW_SEVERITY_METRIC_KEY_BY_SEVERITY.entrySet()) {
      String severity = entry.getKey();
      String metricKey = entry.getValue();
      addNewMeasures(component, metricKey, countIssues.issuesPerSeverity.get(severity));
    }
  }

  private void addFalsePositiveMeasures(Component component, List<Issue> issues) {
    addMeasure(component, FALSE_POSITIVE_ISSUES_KEY, from(issues).filter(FalsePositiveIssue.INSTANCE).size());
  }

  private void addNewMeasures(Component component, String metricKey, List<Issue> issues) {
    if (periodsHolder.getPeriods().isEmpty()) {
      return;
    }
    Metric metric = metricRepository.getByKey(metricKey);
    Double[] periodValues = new Double[]{null, null, null, null, null};
    for (Period period : periodsHolder.getPeriods()) {
      Collection<Measure> childrenMeasures = getChildrenMeasures(component, metric);
      double periodValue = sumIssuesOnPeriod(issues, period.getSnapshotDate()) + sumChildrenMeasuresOnPeriod(childrenMeasures, period.getIndex());
      periodValues[period.getIndex() - 1] = periodValue;
    }
    measureRepository.add(component, metric, Measure.newMeasureBuilder()
      .setVariations(new MeasureVariations(periodValues))
      .createNoValue());
  }

  private void addMeasure(Component component, String metricKey, int value) {
    Metric metric = metricRepository.getByKey(metricKey);
    int totalValue = value + sumChildrenMeasures(getChildrenMeasures(component, metric));
    measureRepository.add(component, metric, Measure.newMeasureBuilder().create(totalValue, null));
  }

  private Collection<Measure> getChildrenMeasures(Component component, final Metric metric) {
    return from(component.getChildren()).transform(new ComponentChildrenMeasures(metric)).toList();
  }

  private static int sumChildrenMeasures(Collection<Measure> measures) {
    SumMeasure sumMeasures = new SumMeasure();
    from(measures).filter(sumMeasures).size();
    return sumMeasures.getSum();
  }

  private static double sumChildrenMeasuresOnPeriod(Collection<Measure> measures, int periodIndex) {
    SumVariationMeasure sumMeasures = new SumVariationMeasure(periodIndex);
    from(measures).filter(sumMeasures).size();
    return sumMeasures.getSum();
  }

  private static int sumIssuesOnPeriod(Collection<Issue> issues, long periodDate) {
    // Add one second to not take into account issues created during current analysis
    long datePlusOneSecond = periodDate + 1000L;
    SumIssueAfterDate sumIssues = new SumIssueAfterDate(datePlusOneSecond);
    from(issues).filter(sumIssues).toList();
    return sumIssues.getSum();
  }

  private static class CountIssues {
    int openIssues = 0;
    int reopenedIssues = 0;
    int confirmedIssues = 0;
    Multiset<String> severityBag = HashMultiset.create();
    ListMultimap<String, Issue> issuesPerSeverity = ArrayListMultimap.create();

    public CountIssues(Iterable<Issue> issues) {
      for (Issue issue : issues) {
        countByStatus(issue.getStatus());
        severityBag.add(issue.getSeverity().name());
        issuesPerSeverity.put(issue.getSeverity().name(), issue);
      }
    }

    private void countByStatus(String status) {
      switch (status) {
        case STATUS_OPEN:
          openIssues++;
          break;
        case STATUS_REOPENED:
          reopenedIssues++;
          break;
        case STATUS_CONFIRMED:
          confirmedIssues++;
          break;
        default:
          // Other statuses are ignored
      }
    }
  }

  private static class SumMeasure implements Predicate<Measure> {

    private int sum = 0;

    @Override
    public boolean apply(@Nonnull Measure input) {
      sum += input.getIntValue();
      return true;
    }

    public int getSum() {
      return sum;
    }
  }

  private static class SumVariationMeasure implements Predicate<Measure> {

    private final int periodIndex;
    private double sum = 0d;

    public SumVariationMeasure(int periodIndex) {
      this.periodIndex = periodIndex;
    }

    @Override
    public boolean apply(@Nonnull Measure input) {
      if (input.hasVariations() && input.getVariations().hasVariation(periodIndex)) {
        sum += input.getVariations().getVariation(periodIndex);
      }
      return true;
    }

    public double getSum() {
      return sum;
    }
  }

  private static class SumIssueAfterDate implements Predicate<Issue> {

    private final long date;
    private int sum = 0;

    public SumIssueAfterDate(long date) {
      this.date = date;
    }

    @Override
    public boolean apply(@Nonnull Issue issue) {
      if (isAfter(issue, date)) {
        sum++;
      }
      return true;
    }

    public int getSum() {
      return sum;
    }

    private static boolean isAfter(Issue issue, long date) {
      // TODO should we truncate the date to the second as it was done in batch ?
      return issue.getCreationDate() > date;
    }
  }

  private enum FalsePositiveIssue implements Predicate<Issue> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Issue issue) {
      return issue.hasResolution() && issue.getResolution().equals(RESOLUTION_FALSE_POSITIVE);
    }
  }

  private enum UnresolvedIssue implements Predicate<Issue> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Issue issue) {
      return !issue.hasResolution();
    }
  }

  private class ComponentChildrenMeasures implements Function<Component, Measure> {
    private final Metric metric;

    public ComponentChildrenMeasures(Metric metric) {
      this.metric = metric;
    }

    @Nullable
    @Override
    public Measure apply(@Nonnull Component input) {
      Optional<Measure> childMeasure = measureRepository.getRawMeasure(input, metric);
      if (childMeasure.isPresent()) {
        return childMeasure.get();
      }
      return null;
    }
  }

  @Override
  public String getDescription() {
    return "Compute measures on issues";
  }
}
