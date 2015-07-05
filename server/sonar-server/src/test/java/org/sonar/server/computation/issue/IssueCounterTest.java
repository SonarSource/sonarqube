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

import java.util.Date;
import javax.annotation.Nullable;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
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
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.metric.Metric.MetricType.INT;

public class IssueCounterTest {

  static final Component FILE1 = builder(Component.Type.FILE, 1).build();
  static final Component FILE2 = builder(Component.Type.FILE, 2).build();
  static final Component FILE3 = builder(Component.Type.FILE, 3).build();
  static final Component PROJECT = builder(Component.Type.PROJECT, 4).addChildren(FILE1, FILE2, FILE3).build();

  static final Metric ISSUES_METRIC = new MetricImpl(1, VIOLATIONS_KEY, VIOLATIONS_KEY, INT);
  static final Metric OPEN_ISSUES_METRIC = new MetricImpl(2, OPEN_ISSUES_KEY, OPEN_ISSUES_KEY, INT);
  static final Metric REOPENED_ISSUES_METRIC = new MetricImpl(3, REOPENED_ISSUES_KEY, REOPENED_ISSUES_KEY, INT);
  static final Metric CONFIRMED_ISSUES_METRIC = new MetricImpl(4, CONFIRMED_ISSUES_KEY, CONFIRMED_ISSUES_KEY, INT);
  static final Metric BLOCKER_ISSUES_METRIC = new MetricImpl(5, BLOCKER_VIOLATIONS_KEY, BLOCKER_VIOLATIONS_KEY, INT);
  static final Metric CRITICAL_ISSUES_METRIC = new MetricImpl(6, CRITICAL_VIOLATIONS_KEY, CRITICAL_VIOLATIONS_KEY, INT);
  static final Metric MAJOR_ISSUES_METRIC = new MetricImpl(7, MAJOR_VIOLATIONS_KEY, MAJOR_VIOLATIONS_KEY, INT);
  static final Metric MINOR_ISSUES_METRIC = new MetricImpl(8, MINOR_VIOLATIONS_KEY, MINOR_VIOLATIONS_KEY, INT);
  static final Metric INFO_ISSUES_METRIC = new MetricImpl(9, INFO_VIOLATIONS_KEY, INFO_VIOLATIONS_KEY, INT);
  static final Metric NEW_ISSUES_METRIC = new MetricImpl(10, NEW_VIOLATIONS_KEY, NEW_VIOLATIONS_KEY, INT);
  static final Metric NEW_BLOCKER_ISSUES_METRIC = new MetricImpl(11, NEW_BLOCKER_VIOLATIONS_KEY, NEW_BLOCKER_VIOLATIONS_KEY, INT);
  static final Metric NEW_CRITICAL_ISSUES_METRIC = new MetricImpl(12, NEW_CRITICAL_VIOLATIONS_KEY, NEW_CRITICAL_VIOLATIONS_KEY, INT);
  static final Metric NEW_MAJOR_ISSUES_METRIC = new MetricImpl(13, NEW_MAJOR_VIOLATIONS_KEY, NEW_MAJOR_VIOLATIONS_KEY, INT);
  static final Metric NEW_MINOR_ISSUES_METRIC = new MetricImpl(14, NEW_MINOR_VIOLATIONS_KEY, NEW_MINOR_VIOLATIONS_KEY, INT);
  static final Metric NEW_INFO_ISSUES_METRIC = new MetricImpl(15, NEW_INFO_VIOLATIONS_KEY, NEW_INFO_VIOLATIONS_KEY, INT);
  static final Metric FALSE_POSITIVE_ISSUES_METRIC = new MetricImpl(16, FALSE_POSITIVE_ISSUES_KEY, FALSE_POSITIVE_ISSUES_KEY, INT);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  MetricRepository metricRepository = mock(MetricRepository.class);
  MeasureRepository measureRepository;
  IssueCounter sut;

  @Before
  public void setUp() throws Exception {
    initMetrics();
    measureRepository = new MeasureRepositoryImpl(null, reportReader, metricRepository);

    sut = new IssueCounter(periodsHolder, metricRepository, measureRepository);
  }

  @Test
  public void count_issues_by_status() throws Exception {
    periodsHolder.setPeriods();

    // bottom-up traversal -> from files to project
    sut.beforeComponent(FILE1);
    sut.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER));
    sut.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    sut.onIssue(FILE1, createIssue(RESOLUTION_FALSE_POSITIVE, STATUS_RESOLVED, MAJOR));
    sut.afterComponent(FILE1);

    sut.beforeComponent(FILE2);
    sut.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, BLOCKER));
    sut.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    sut.afterComponent(FILE2);

    sut.beforeComponent(FILE3);
    sut.afterComponent(FILE3);

    sut.beforeComponent(PROJECT);
    sut.afterComponent(PROJECT);

    // count by status
    assertThat(measureRepository.getRawMeasure(FILE1, ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(FILE2, ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(FILE2, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE2, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE2, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);

    assertThat(measureRepository.getRawMeasure(FILE3, ISSUES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getIntValue()).isEqualTo(3);
    assertThat(measureRepository.getRawMeasure(PROJECT, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void count_unresolved_issues_by_severity() throws Exception {
    periodsHolder.setPeriods();

    // bottom-up traversal -> from files to project
    sut.beforeComponent(FILE1);
    sut.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER));
    // this resolved issue is ignored
    sut.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    sut.afterComponent(FILE1);

    sut.beforeComponent(FILE2);
    sut.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, BLOCKER));
    sut.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    sut.afterComponent(FILE2);

    sut.beforeComponent(PROJECT);
    sut.afterComponent(PROJECT);

    assertThat(measureRepository.getRawMeasure(FILE1, BLOCKER_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, CRITICAL_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE1, MAJOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(FILE2, BLOCKER_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE2, CRITICAL_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE2, MAJOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);

    assertThat(measureRepository.getRawMeasure(PROJECT, BLOCKER_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(PROJECT, CRITICAL_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, MAJOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
  }

  @Test
  public void count_new_issues() throws Exception {
    Period period = newPeriod(3, 1500000000000L);
    periodsHolder.setPeriods(period);

    sut.beforeComponent(FILE1);
    // created before -> existing issues
    sut.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, BLOCKER, period.getSnapshotDate() - 1000000L));
    // created during the first analysis starting the period -> existing issues
    sut.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, BLOCKER, period.getSnapshotDate()));
    // created after -> new issues
    sut.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, CRITICAL, period.getSnapshotDate() + 100000L));
    sut.onIssue(FILE1, createIssueAt(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR, period.getSnapshotDate() + 200000L));
    sut.afterComponent(FILE1);

    sut.beforeComponent(FILE2);
    sut.afterComponent(FILE2);

    sut.beforeComponent(PROJECT);
    sut.afterComponent(PROJECT);

    assertVariation(FILE1, NEW_ISSUES_METRIC, period.getIndex(), 1);
    assertVariation(FILE1, NEW_CRITICAL_ISSUES_METRIC, period.getIndex(), 1);
    assertVariation(FILE1, NEW_BLOCKER_ISSUES_METRIC, period.getIndex(), 0);
    assertVariation(FILE1, NEW_MAJOR_ISSUES_METRIC, period.getIndex(), 0);

    assertVariation(PROJECT, NEW_ISSUES_METRIC, period.getIndex(), 1);
    assertVariation(PROJECT, NEW_CRITICAL_ISSUES_METRIC, period.getIndex(), 1);
    assertVariation(PROJECT, NEW_BLOCKER_ISSUES_METRIC, period.getIndex(), 0);
    assertVariation(PROJECT, NEW_MAJOR_ISSUES_METRIC, period.getIndex(), 0);
  }

  private void assertVariation(Component component, Metric metric, int periodIndex, int expectedVariation) {
    MeasureVariations variations = measureRepository.getRawMeasure(component, metric).get().getVariations();
    assertThat(variations.getVariation(periodIndex)).isEqualTo((double) expectedVariation, Offset.offset(0.01));
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity) {
    return new DefaultIssue()
      .setResolution(resolution).setStatus(status)
      .setSeverity(severity).setRuleKey(RuleTesting.XOO_X1)
      .setCreationDate(new Date());
  }

  private static DefaultIssue createIssueAt(@Nullable String resolution, String status, String severity, long creationDate) {
    return new DefaultIssue()
      .setResolution(resolution).setStatus(status)
      .setSeverity(severity).setRuleKey(RuleTesting.XOO_X1)
      .setCreationDate(new Date(creationDate));
  }

  private static Period newPeriod(int index, long date) {
    return new Period(index, "mode", null, date, 42l);
  }

  private void initMetrics() {
    when(metricRepository.getByKey(ISSUES_METRIC.getKey())).thenReturn(ISSUES_METRIC);
    when(metricRepository.getByKey(OPEN_ISSUES_METRIC.getKey())).thenReturn(OPEN_ISSUES_METRIC);
    when(metricRepository.getByKey(REOPENED_ISSUES_METRIC.getKey())).thenReturn(REOPENED_ISSUES_METRIC);
    when(metricRepository.getByKey(CONFIRMED_ISSUES_METRIC.getKey())).thenReturn(CONFIRMED_ISSUES_METRIC);
    when(metricRepository.getByKey(BLOCKER_ISSUES_METRIC.getKey())).thenReturn(BLOCKER_ISSUES_METRIC);
    when(metricRepository.getByKey(CRITICAL_ISSUES_METRIC.getKey())).thenReturn(CRITICAL_ISSUES_METRIC);
    when(metricRepository.getByKey(MAJOR_ISSUES_METRIC.getKey())).thenReturn(MAJOR_ISSUES_METRIC);
    when(metricRepository.getByKey(MINOR_ISSUES_METRIC.getKey())).thenReturn(MINOR_ISSUES_METRIC);
    when(metricRepository.getByKey(INFO_ISSUES_METRIC.getKey())).thenReturn(INFO_ISSUES_METRIC);
    when(metricRepository.getByKey(NEW_ISSUES_METRIC.getKey())).thenReturn(NEW_ISSUES_METRIC);
    when(metricRepository.getByKey(NEW_BLOCKER_ISSUES_METRIC.getKey())).thenReturn(NEW_BLOCKER_ISSUES_METRIC);
    when(metricRepository.getByKey(NEW_CRITICAL_ISSUES_METRIC.getKey())).thenReturn(NEW_CRITICAL_ISSUES_METRIC);
    when(metricRepository.getByKey(NEW_MAJOR_ISSUES_METRIC.getKey())).thenReturn(NEW_MAJOR_ISSUES_METRIC);
    when(metricRepository.getByKey(NEW_MINOR_ISSUES_METRIC.getKey())).thenReturn(NEW_MINOR_ISSUES_METRIC);
    when(metricRepository.getByKey(NEW_INFO_ISSUES_METRIC.getKey())).thenReturn(NEW_INFO_ISSUES_METRIC);
    when(metricRepository.getByKey(FALSE_POSITIVE_ISSUES_METRIC.getKey())).thenReturn(FALSE_POSITIVE_ISSUES_METRIC);
  }
}
