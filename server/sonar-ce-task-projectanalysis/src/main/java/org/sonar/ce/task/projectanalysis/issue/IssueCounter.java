/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.rule.RuleType;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.server.measure.ImpactMeasureBuilder;

import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.server.metric.IssueCountMetrics.ISSUES_IN_SANDBOX_KEY;
import static org.sonar.server.metric.IssueCountMetrics.NEW_ISSUES_IN_SANDBOX_KEY;
import static org.sonar.api.measures.CoreMetrics.ACCEPTED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.HIGH_IMPACT_ACCEPTED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_ACCEPTED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_INFO_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES_KEY;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.REOPENED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.core.rule.RuleType.CODE_SMELL;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.core.rule.RuleType.VULNERABILITY;

/**
 * For each component, computes the measures related to number of issues:
 * <ul>
 * <li>issues per status (open, reopen, confirmed)</li>
 * <li>issues per resolution (unresolved, false-positives, won't fix)</li>
 * <li>issues per severity (from info to blocker)</li>
 * <li>issues per impact severity (from info to blocker)</li>
 * <li>issues per type (code smell, bug, vulnerability, security hotspots)</li>
 * <li>issues per impact</li>
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

  private static final Map<Severity, String> IMPACT_SEVERITY_TO_METRIC_KEY = ImmutableMap.of(
    Severity.BLOCKER, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY,
    Severity.HIGH, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES_KEY,
    Severity.MEDIUM, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY,
    Severity.LOW, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_LOW_ISSUES_KEY,
    Severity.INFO, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_INFO_ISSUES_KEY);

  private static final Map<Severity, String> IMPACT_SEVERITY_TO_NEW_METRIC_KEY = ImmutableMap.of(
    Severity.BLOCKER, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY,
    Severity.HIGH, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY,
    Severity.MEDIUM, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY,
    Severity.LOW, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY,
    Severity.INFO, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY);

  static final Map<String, String> IMPACT_TO_JSON_METRIC_KEY = Map.of(
    SoftwareQuality.SECURITY.name(), SECURITY_ISSUES_KEY,
    SoftwareQuality.RELIABILITY.name(), RELIABILITY_ISSUES_KEY,
    SoftwareQuality.MAINTAINABILITY.name(), MAINTAINABILITY_ISSUES_KEY);

  static final Map<String, String> IMPACT_TO_NEW_JSON_METRIC_KEY = Map.of(
    SoftwareQuality.SECURITY.name(), NEW_SECURITY_ISSUES_KEY,
    SoftwareQuality.RELIABILITY.name(), NEW_RELIABILITY_ISSUES_KEY,
    SoftwareQuality.MAINTAINABILITY.name(), NEW_MAINTAINABILITY_ISSUES_KEY);

  static final Map<String, String> IMPACT_TO_METRIC_KEY = Map.of(
    SoftwareQuality.SECURITY.name(), SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY,
    SoftwareQuality.RELIABILITY.name(), SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY,
    SoftwareQuality.MAINTAINABILITY.name(), SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY);

  static final Map<String, String> IMPACT_TO_NEW_METRIC_KEY = Map.of(
    SoftwareQuality.SECURITY.name(), SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY,
    SoftwareQuality.RELIABILITY.name(), SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY,
    SoftwareQuality.MAINTAINABILITY.name(), SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY);

  private static final Map<RuleType, String> TYPE_TO_METRIC_KEY = ImmutableMap.<RuleType, String>builder()
    .put(CODE_SMELL, CODE_SMELLS_KEY)
    .put(BUG, BUGS_KEY)
    .put(VULNERABILITY, VULNERABILITIES_KEY)
    .put(SECURITY_HOTSPOT, SECURITY_HOTSPOTS_KEY)
    .build();
  private static final Map<RuleType, String> TYPE_TO_NEW_METRIC_KEY = ImmutableMap.<RuleType, String>builder()
    .put(CODE_SMELL, NEW_CODE_SMELLS_KEY)
    .put(BUG, NEW_BUGS_KEY)
    .put(VULNERABILITY, NEW_VULNERABILITIES_KEY)
    .put(SECURITY_HOTSPOT, NEW_SECURITY_HOTSPOTS_KEY)
    .build();

  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final NewIssueClassifier newIssueClassifier;
  private final Map<String, Counters> countersByComponentUuid = new HashMap<>();

  private Counters currentCounters;

  public IssueCounter(MetricRepository metricRepository, MeasureRepository measureRepository, NewIssueClassifier newIssueClassifier) {
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.newIssueClassifier = newIssueClassifier;
  }

  @Override
  public void beforeComponent(Component component) {
    currentCounters = new Counters();
    countersByComponentUuid.put(component.getUuid(), currentCounters);

    // aggregate children counters
    for (Component child : component.getChildren()) {
      Counters childCounters = countersByComponentUuid.remove(child.getUuid());
      currentCounters.add(childCounters);
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    currentCounters.add(issue);
    if (newIssueClassifier.isNew(component, issue)) {
      currentCounters.addOnPeriod(issue);
    }
  }

  @Override
  public void afterComponent(Component component) {
    addMeasuresBySeverity(component);
    addMeasuresByImpactSeverity(component);
    addMeasuresByStatus(component);
    addMeasuresByType(component);
    addMeasuresByImpact(component);
    addNewMeasures(component);
    currentCounters = null;
  }

  private void addMeasuresBySeverity(Component component) {
    addMeasures(component, SEVERITY_TO_METRIC_KEY, currentCounters.counter().severityBag);
  }

  private void addMeasuresByImpactSeverity(Component component) {
    addMeasures(component, IMPACT_SEVERITY_TO_METRIC_KEY, currentCounters.counter().impactSeverityBag);
  }

  private void addMeasuresByStatus(Component component) {
    addMeasure(component, VIOLATIONS_KEY, currentCounters.counter().unresolved);
    addMeasure(component, OPEN_ISSUES_KEY, currentCounters.counter().open);
    addMeasure(component, REOPENED_ISSUES_KEY, currentCounters.counter().reopened);
    addMeasure(component, CONFIRMED_ISSUES_KEY, currentCounters.counter().confirmed);
    addMeasure(component, FALSE_POSITIVE_ISSUES_KEY, currentCounters.counter().falsePositives);
    addMeasure(component, ACCEPTED_ISSUES_KEY, currentCounters.counter().accepted);
    addMeasure(component, HIGH_IMPACT_ACCEPTED_ISSUES_KEY, currentCounters.counter().highImpactAccepted);
    addMeasure(component, ISSUES_IN_SANDBOX_KEY, currentCounters.counter().inSandbox);
  }

  private void addMeasuresByImpact(Component component) {
    for (Map.Entry<String, Map<String, Long>> impactEntry : currentCounters.counter().impactsBag.entrySet()) {
      String json = ImpactMeasureBuilder.fromMap(impactEntry.getValue()).buildAsString();
      addMeasure(component, IMPACT_TO_JSON_METRIC_KEY.get(impactEntry.getKey()), json);
      addMeasure(component, IMPACT_TO_METRIC_KEY.get(impactEntry.getKey()),
        impactEntry.getValue().get(ImpactMeasureBuilder.TOTAL_KEY).intValue());
    }
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

  private void addMeasure(Component component, String metricKey, String data) {
    Metric metric = metricRepository.getByKey(metricKey);
    measureRepository.add(component, metric, Measure.newMeasureBuilder().create(data));
  }

  private <M> void addMeasures(Component component, Map<M, String> metrics, Multiset<M> countPerMetric) {
    for (Map.Entry<M, String> entry : metrics.entrySet()) {
      M entryKey = entry.getKey();
      String metricKey = entry.getValue();
      addMeasure(component, metricKey, countPerMetric.count(entryKey));
    }
  }


  private void addNewMeasures(Component component) {
    if (!newIssueClassifier.isEnabled()) {
      return;
    }
    int unresolved = currentCounters.counterForPeriod().unresolved;
    measureRepository.add(component, metricRepository.getByKey(NEW_VIOLATIONS_KEY), Measure.newMeasureBuilder()
      .create(unresolved));

    addMeasures(component, SEVERITY_TO_NEW_METRIC_KEY, currentCounters.counterForPeriod().severityBag);

    addMeasures(component, IMPACT_SEVERITY_TO_NEW_METRIC_KEY, currentCounters.counterForPeriod().impactSeverityBag);

    addMeasures(component, TYPE_TO_NEW_METRIC_KEY, currentCounters.counterForPeriod().typeBag);

    for (Map.Entry<String, Map<String, Long>> impactEntry : currentCounters.counterForPeriod().impactsBag.entrySet()) {
      String json = ImpactMeasureBuilder.fromMap(impactEntry.getValue()).buildAsString();
      addMeasure(component, IMPACT_TO_NEW_JSON_METRIC_KEY.get(impactEntry.getKey()), json);
      addMeasure(component, IMPACT_TO_NEW_METRIC_KEY.get(impactEntry.getKey()),
        impactEntry.getValue().get(ImpactMeasureBuilder.TOTAL_KEY).intValue());
    }

    addMeasure(component, NEW_ACCEPTED_ISSUES_KEY, currentCounters.counterForPeriod().accepted);
    addMeasure(component, NEW_ISSUES_IN_SANDBOX_KEY, currentCounters.counterForPeriod().inSandbox);
  }

  /**
   * Count issues by status, resolutions, rules, impacts and severities
   */
  private static class Counter {
    private int unresolved = 0;
    private int open = 0;
    private int reopened = 0;
    private int confirmed = 0;
    private int falsePositives = 0;
    private int accepted = 0;
    private int highImpactAccepted = 0;
    private int inSandbox = 0;
    private final Multiset<String> severityBag = HashMultiset.create();
    private final Multiset<Severity> impactSeverityBag = HashMultiset.create();
    /**
     * This map contains the number of issues per software quality along with their distribution based on (new) severity.
     */
    private final Map<String, Map<String, Long>> impactsBag = new HashMap<>();
    private final EnumMultiset<RuleType> typeBag = EnumMultiset.create(RuleType.class);

    public Counter() {
      initImpactsBag();
    }

    private void initImpactsBag() {
      for (SoftwareQuality quality : SoftwareQuality.values()) {
        impactsBag.put(quality.name(), ImpactMeasureBuilder.createEmpty().buildAsMap());
      }
    }

    void add(Counter counter) {
      unresolved += counter.unresolved;
      open += counter.open;
      reopened += counter.reopened;
      confirmed += counter.confirmed;
      falsePositives += counter.falsePositives;
      accepted += counter.accepted;
      highImpactAccepted += counter.highImpactAccepted;
      inSandbox += counter.inSandbox;
      severityBag.addAll(counter.severityBag);
      impactSeverityBag.addAll(counter.impactSeverityBag);
      typeBag.addAll(counter.typeBag);

      // Add impacts
      for (Map.Entry<String, Map<String, Long>> impactEntry : counter.impactsBag.entrySet()) {
        Map<String, Long> severityMap = impactsBag.get(impactEntry.getKey());
        for (Map.Entry<String, Long> severityEntry : impactEntry.getValue().entrySet()) {
          severityMap.compute(severityEntry.getKey(), (key, value) -> value + severityEntry.getValue());
        }
      }
    }

    void add(DefaultIssue issue) {
      if (IssueStatus.IN_SANDBOX.equals(issue.issueStatus())) {
        inSandbox++;
        return;
      }
      if (issue.type() == SECURITY_HOTSPOT) {
        if (issue.resolution() == null) {
          typeBag.add(SECURITY_HOTSPOT);
        }
        return;
      }
      if (issue.resolution() == null) {
        unresolved++;
        typeBag.add(issue.type());
        severityBag.add(issue.severity());
      } else if (IssueStatus.FALSE_POSITIVE.equals(issue.issueStatus())) {
        falsePositives++;
      } else if (IssueStatus.ACCEPTED.equals(issue.issueStatus())) {
        accepted++;
        if (issue.impacts().values().stream().anyMatch(severity -> severity == Severity.HIGH || severity == Severity.BLOCKER)) {
          highImpactAccepted++;
        }
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
      countIssueImpacts(issue);
    }

    private void countIssueImpacts(DefaultIssue issue) {
      if (IssueStatus.OPEN == issue.issueStatus() || IssueStatus.CONFIRMED == issue.issueStatus()) {
        for (Map.Entry<SoftwareQuality, Severity> impact : issue.impacts().entrySet()) {
          impactsBag.compute(impact.getKey().name(), (key, value) -> {
            value.compute(impact.getValue().name(), (severity, count) -> count == null ? 1 : count + 1);
            value.compute(ImpactMeasureBuilder.TOTAL_KEY, (total, count) -> count == null ? 1 : count + 1);
            return value;
          });
        }
        issue.impacts().values().stream().distinct().forEach(impactSeverityBag::add);
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
