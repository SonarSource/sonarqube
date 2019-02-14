/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.core.issue.DefaultIssue;

import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
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
import static org.sonar.api.measures.CoreMetrics.WONT_FIX_ISSUES_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.utils.DateUtils.truncateToSeconds;

/**
 * For each component, computes the measures related to number of issues:
 * <ul>
 * <li>issues per status (open, reopen, confirmed)</li>
 * <li>issues per resolution (unresolved, false-positives, won't fix)</li>
 * <li>issues per severity (from info to blocker)</li>
 * <li>issues per type (code smell, bug, vulnerability)</li>
 * </ul>
 * For each value, the variation on configured periods is also computed.
 */
public class IssueCounter extends IssueVisitor {

  private static final Map<String, String> SEVERITY_TO_METRIC_KEY = ImmutableMap.of(
    BLOCKER, BLOCKER_VIOLATIONS_KEY,
    CRITICAL, CRITICAL_VIOLATIONS_KEY,
    MAJOR, MAJOR_VIOLATIONS_KEY,
    MINOR, MINOR_VIOLATIONS_KEY,
    INFO, INFO_VIOLATIONS_KEY);

  private static final Map<String, String> SEVERITY_TO_NEW_METRIC_KEY = ImmutableMap.of(
    BLOCKER, NEW_BLOCKER_VIOLATIONS_KEY,
    CRITICAL, NEW_CRITICAL_VIOLATIONS_KEY,
    MAJOR, NEW_MAJOR_VIOLATIONS_KEY,
    MINOR, NEW_MINOR_VIOLATIONS_KEY,
    INFO, NEW_INFO_VIOLATIONS_KEY);

  private static final Map<RuleType, String> TYPE_TO_METRIC_KEY = ImmutableMap.<RuleType, String>builder()
    .put(RuleType.CODE_SMELL, CoreMetrics.CODE_SMELLS_KEY)
    .put(RuleType.BUG, CoreMetrics.BUGS_KEY)
    .put(RuleType.VULNERABILITY, CoreMetrics.VULNERABILITIES_KEY)
    .build();
  private static final Map<RuleType, String> TYPE_TO_NEW_METRIC_KEY = ImmutableMap.<RuleType, String>builder()
    .put(RuleType.CODE_SMELL, CoreMetrics.NEW_CODE_SMELLS_KEY)
    .put(RuleType.BUG, CoreMetrics.NEW_BUGS_KEY)
    .put(RuleType.VULNERABILITY, CoreMetrics.NEW_VULNERABILITIES_KEY)
    .build();

  private final PeriodHolder periodHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final Map<String, Counters> countersByComponentUuid = new HashMap<>();

  private Counters currentCounters;

  public IssueCounter(PeriodHolder periodHolder, AnalysisMetadataHolder analysisMetadataHolder,
    MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.periodHolder = periodHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void beforeComponent(Component component) {
    currentCounters = new Counters();
    countersByComponentUuid.put(component.getUuid(), currentCounters);

    // aggregate children counters
    for (Component child : component.getChildren()) {
      // no need to keep the children in memory. They can be garbage-collected.
      Counters childCounters = countersByComponentUuid.remove(child.getUuid());
      currentCounters.add(childCounters);
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.type() == RuleType.SECURITY_HOTSPOT) {
      return;
    }

    currentCounters.add(issue);
    if (analysisMetadataHolder.isSLBorPR()) {
      currentCounters.addOnPeriod(issue);
    } else if (periodHolder.hasPeriod()) {
      Period period = periodHolder.getPeriod();
      if (issue.creationDate().getTime() > truncateToSeconds(period.getSnapshotDate())) {
        currentCounters.addOnPeriod(issue);
      }
    }
  }

  @Override
  public void afterComponent(Component component) {
    addMeasuresBySeverity(component);
    addMeasuresByStatus(component);
    addMeasuresByType(component);
    addNewMeasures(component);
    currentCounters = null;
  }

  private void addMeasuresBySeverity(Component component) {
    for (Map.Entry<String, String> entry : SEVERITY_TO_METRIC_KEY.entrySet()) {
      String severity = entry.getKey();
      String metricKey = entry.getValue();
      addMeasure(component, metricKey, currentCounters.counter().severityBag.count(severity));
    }
  }

  private void addMeasuresByStatus(Component component) {
    addMeasure(component, VIOLATIONS_KEY, currentCounters.counter().unresolved);
    addMeasure(component, OPEN_ISSUES_KEY, currentCounters.counter().open);
    addMeasure(component, REOPENED_ISSUES_KEY, currentCounters.counter().reopened);
    addMeasure(component, CONFIRMED_ISSUES_KEY, currentCounters.counter().confirmed);
    addMeasure(component, FALSE_POSITIVE_ISSUES_KEY, currentCounters.counter().falsePositives);
    addMeasure(component, WONT_FIX_ISSUES_KEY, currentCounters.counter().wontFix);
  }

  private void addMeasuresByType(Component component) {
    for (Map.Entry<RuleType, String> entry : TYPE_TO_METRIC_KEY.entrySet()) {
      addMeasure(component, entry.getValue(), currentCounters.counter().typeBag.count(entry.getKey()));
    }
  }

  private void addMeasure(Component component, String metricKey, int value) {
    Metric metric = metricRepository.getByKey(metricKey);
    measureRepository.add(component, metric, Measure.newMeasureBuilder().create(value));
  }

  private void addNewMeasures(Component component) {
    if (!periodHolder.hasPeriod() && !analysisMetadataHolder.isSLBorPR()) {
      return;
    }
    double unresolvedVariations = (double) currentCounters.counterForPeriod().unresolved;
    measureRepository.add(component, metricRepository.getByKey(NEW_VIOLATIONS_KEY), Measure.newMeasureBuilder()
      .setVariation(unresolvedVariations)
      .createNoValue());

    for (Map.Entry<String, String> entry : SEVERITY_TO_NEW_METRIC_KEY.entrySet()) {
      String severity = entry.getKey();
      String metricKey = entry.getValue();
      Multiset<String> bag = currentCounters.counterForPeriod().severityBag;
      Metric metric = metricRepository.getByKey(metricKey);
      measureRepository.add(component, metric, Measure.newMeasureBuilder()
        .setVariation((double) bag.count(severity))
        .createNoValue());
    }

    // waiting for Java 8 lambda in order to factor this loop with the previous one
    // (see call currentCounters.counterForPeriod(period.getIndex()).xxx with xxx as severityBag or typeBag)
    for (Map.Entry<RuleType, String> entry : TYPE_TO_NEW_METRIC_KEY.entrySet()) {
      RuleType type = entry.getKey();
      String metricKey = entry.getValue();
      Multiset<RuleType> bag = currentCounters.counterForPeriod().typeBag;
      Metric metric = metricRepository.getByKey(metricKey);
      measureRepository.add(component, metric, Measure.newMeasureBuilder()
        .setVariation((double) bag.count(type))
        .createNoValue());
    }
  }

  /**
   * Count issues by status, resolutions, rules and severities
   */
  private static class Counter {
    private int unresolved = 0;
    private int open = 0;
    private int reopened = 0;
    private int confirmed = 0;
    private int falsePositives = 0;
    private int wontFix = 0;
    private final Multiset<String> severityBag = HashMultiset.create();
    private final EnumMultiset<RuleType> typeBag = EnumMultiset.create(RuleType.class);

    void add(Counter counter) {
      unresolved += counter.unresolved;
      open += counter.open;
      reopened += counter.reopened;
      confirmed += counter.confirmed;
      falsePositives += counter.falsePositives;
      wontFix += counter.wontFix;
      severityBag.addAll(counter.severityBag);
      typeBag.addAll(counter.typeBag);
    }

    void add(DefaultIssue issue) {
      if (issue.resolution() == null) {
        unresolved++;
        typeBag.add(issue.type());
        severityBag.add(issue.severity());
      } else if (RESOLUTION_FALSE_POSITIVE.equals(issue.resolution())) {
        falsePositives++;
      } else if (RESOLUTION_WONT_FIX.equals(issue.resolution())) {
        wontFix++;
      }
      switch (issue.status()) {
        case STATUS_OPEN:
          open++;
          break;
        case STATUS_REOPENED:
          reopened++;
          break;
        case STATUS_CONFIRMED:
          confirmed++;
          break;
        default:
          // Other statuses are ignored
      }
    }
  }

  /**
   * List of {@link Counter} for regular value and period.
   */
  private static class Counters {
    private final Counter counter = new Counter();
    private final Counter counterForPeriod = new Counter();

    void add(@Nullable Counters other) {
      if (other != null) {
        counter.add(other.counter);
        counterForPeriod.add(other.counterForPeriod);
      }
    }

    void addOnPeriod(DefaultIssue issue) {
      counterForPeriod.add(issue);
    }

    void add(DefaultIssue issue) {
      counter.add(issue);
    }

    Counter counter() {
      return counter;
    }

    Counter counterForPeriod() {
      return counterForPeriod;
    }
  }
}
