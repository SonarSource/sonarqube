/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolder;

import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;

/**
 * Compute new effort related measures :
 * {@link CoreMetrics#NEW_TECHNICAL_DEBT_KEY}
 * {@link CoreMetrics#NEW_RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link CoreMetrics#NEW_SECURITY_REMEDIATION_EFFORT_KEY}
 */
public class NewEffortAggregator extends IssueVisitor {

  private final NewEffortCalculator calculator;
  private final PeriodHolder periodHolder;
  private final DbClient dbClient;
  private final MeasureRepository measureRepository;

  private final Metric newMaintainabilityEffortMetric;
  private final Metric newReliabilityEffortMetric;
  private final Metric newSecurityEffortMetric;

  private ListMultimap<String, IssueChangeDto> changesByIssueUuid = ArrayListMultimap.create();
  private Map<Integer, NewEffortCounter> counterByComponentRef = new HashMap<>();
  private NewEffortCounter counter = null;

  public NewEffortAggregator(NewEffortCalculator calculator, PeriodHolder periodHolder, DbClient dbClient,
    MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.calculator = calculator;
    this.periodHolder = periodHolder;
    this.dbClient = dbClient;
    this.measureRepository = measureRepository;

    this.newMaintainabilityEffortMetric = metricRepository.getByKey(NEW_TECHNICAL_DEBT_KEY);
    this.newReliabilityEffortMetric = metricRepository.getByKey(NEW_RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.newSecurityEffortMetric = metricRepository.getByKey(NEW_SECURITY_REMEDIATION_EFFORT_KEY);
  }

  @Override
  public void beforeComponent(Component component) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<IssueChangeDto> changes = dbClient.issueChangeDao().selectChangelogOfNonClosedIssuesByComponent(dbSession, component.getUuid());
      for (IssueChangeDto change : changes) {
        changesByIssueUuid.put(change.getIssueKey(), change);
      }
    }

    counter = new NewEffortCounter(calculator);
    counterByComponentRef.put(component.getReportAttributes().getRef(), counter);
    for (Component child : component.getChildren()) {
      NewEffortCounter childSum = counterByComponentRef.remove(child.getReportAttributes().getRef());
      if (childSum != null) {
        counter.add(childSum);
      }
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.resolution() == null && issue.effortInMinutes() != null && periodHolder.hasPeriod()) {
      List<IssueChangeDto> changelog = changesByIssueUuid.get(issue.key());
      counter.add(issue, periodHolder.getPeriod(), changelog);
    }
  }

  @Override
  public void afterComponent(Component component) {
    computeMeasure(component, newMaintainabilityEffortMetric, counter.maintainabilitySum);
    computeMeasure(component, newReliabilityEffortMetric, counter.reliabilitySum);
    computeMeasure(component, newSecurityEffortMetric, counter.securitySum);
    changesByIssueUuid.clear();
    counter = null;
  }

  private void computeMeasure(Component component, Metric metric, EffortSum effortSum) {
    if (!effortSum.isEmpty) {
      measureRepository.add(component, metric, Measure.newMeasureBuilder().setVariation(effortSum.newEffort).createNoValue());
    }
  }

  private static class NewEffortCounter {
    private final NewEffortCalculator calculator;

    private final EffortSum maintainabilitySum = new EffortSum();
    private final EffortSum reliabilitySum = new EffortSum();
    private final EffortSum securitySum = new EffortSum();

    public NewEffortCounter(NewEffortCalculator calculator) {
      this.calculator = calculator;
    }

    void add(NewEffortCounter otherCounter) {
      maintainabilitySum.add(otherCounter.maintainabilitySum);
      reliabilitySum.add(otherCounter.reliabilitySum);
      securitySum.add(otherCounter.securitySum);
    }

    void add(DefaultIssue issue, Period period, List<IssueChangeDto> changelog) {
      long newEffort = calculator.calculate(issue, changelog, period);
      switch (issue.type()) {
        case CODE_SMELL:
          maintainabilitySum.add(newEffort);
          break;
        case BUG:
          reliabilitySum.add(newEffort);
          break;
        case VULNERABILITY:
          securitySum.add(newEffort);
          break;
        default:
          throw new IllegalStateException(String.format("Unknown type '%s'", issue.type()));
      }
    }
  }

  private static class EffortSum {
    private Double newEffort;
    private boolean isEmpty = true;

    void add(long newEffort) {
      double previous = MoreObjects.firstNonNull(this.newEffort, 0d);
      this.newEffort = previous + newEffort;
      isEmpty = false;
    }

    void add(EffortSum other) {
      Double otherValue = other.newEffort;
      if (otherValue != null) {
        add(otherValue.longValue());
      }
    }
  }
}
