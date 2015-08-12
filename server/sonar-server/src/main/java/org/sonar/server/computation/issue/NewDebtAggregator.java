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
package org.sonar.server.computation.issue;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;
import org.sonar.db.DbClient;

public class NewDebtAggregator extends IssueVisitor {

  private final NewDebtCalculator calculator;
  private final PeriodsHolder periodsHolder;
  private final DbClient dbClient;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  private ListMultimap<String, IssueChangeDto> changesByIssueUuid = ArrayListMultimap.create();
  private Map<Integer, DebtSum> sumsByComponentRef = new HashMap<>();
  private DebtSum currentSum = null;

  public NewDebtAggregator(NewDebtCalculator calculator, PeriodsHolder periodsHolder, DbClient dbClient,
    MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.calculator = calculator;
    this.periodsHolder = periodsHolder;
    this.dbClient = dbClient;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void beforeComponent(Component component) {
    currentSum = new DebtSum();
    sumsByComponentRef.put(component.getReportAttributes().getRef(), currentSum);
    List<IssueChangeDto> changes = dbClient.issueChangeDao().selectChangelogOfNonClosedIssuesByComponent(component.getUuid());
    for (IssueChangeDto change : changes) {
      changesByIssueUuid.put(change.getIssueKey(), change);
    }
    for (Component child : component.getChildren()) {
      DebtSum childSum = sumsByComponentRef.remove(child.getReportAttributes().getRef());
      if (childSum != null) {
        currentSum.add(childSum);
      }
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.resolution() == null && issue.debtInMinutes() != null && !periodsHolder.getPeriods().isEmpty()) {
      List<IssueChangeDto> changelog = changesByIssueUuid.get(issue.key());
      for (Period period : periodsHolder.getPeriods()) {
        long newDebt = calculator.calculate(issue, changelog, period);
        currentSum.add(period.getIndex(), newDebt);
      }
    }
  }

  @Override
  public void afterComponent(Component component) {
    if (!currentSum.isEmpty) {
      MeasureVariations variations = new MeasureVariations(currentSum.sums);
      Metric metric = metricRepository.getByKey(CoreMetrics.NEW_TECHNICAL_DEBT_KEY);
      measureRepository.add(component, metric, Measure.newMeasureBuilder().setVariations(variations).createNoValue());
    }
    changesByIssueUuid.clear();
    currentSum = null;
  }

  private static class DebtSum {
    private final Double[] sums = new Double[PeriodsHolder.MAX_NUMBER_OF_PERIODS];
    private boolean isEmpty = true;

    void add(int periodIndex, long newDebt) {
      double previous = Objects.firstNonNull(sums[periodIndex - 1], 0d);
      sums[periodIndex - 1] = previous + newDebt;
      isEmpty = false;
    }

    void add(DebtSum other) {
      for (int i = 0; i < sums.length; i++) {
        Double otherValue = other.sums[i];
        if (otherValue != null) {
          add(i + 1, otherValue.longValue());
        }
      }
    }
  }
}
