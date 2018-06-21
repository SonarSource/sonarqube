/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Date;
import javax.annotation.Nullable;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
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
import static org.sonar.api.measures.CoreMetrics.WONT_FIX_ISSUES_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.computation.task.projectanalysis.metric.Metric.MetricType.INT;

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
  static final Metric WONT_FIX_ISSUES_METRIC = new MetricImpl(23, WONT_FIX_ISSUES_KEY, WONT_FIX_ISSUES_KEY, INT);
  static final Metric CODE_SMELLS_METRIC = new MetricImpl(17, CoreMetrics.CODE_SMELLS_KEY, CoreMetrics.CODE_SMELLS_KEY, INT);
  static final Metric BUGS_METRIC = new MetricImpl(18, CoreMetrics.BUGS_KEY, CoreMetrics.BUGS_KEY, INT);
  static final Metric VULNERABILITIES_METRIC = new MetricImpl(19, CoreMetrics.VULNERABILITIES_KEY, CoreMetrics.VULNERABILITIES_KEY, INT);
  static final Metric NEW_CODE_SMELLS_METRIC = new MetricImpl(20, CoreMetrics.NEW_CODE_SMELLS_KEY, CoreMetrics.NEW_CODE_SMELLS_KEY, INT);
  static final Metric NEW_BUGS_METRIC = new MetricImpl(21, CoreMetrics.NEW_BUGS_KEY, CoreMetrics.NEW_BUGS_KEY, INT);
  static final Metric NEW_VULNERABILITIES_METRIC = new MetricImpl(22, CoreMetrics.NEW_VULNERABILITIES_KEY, CoreMetrics.NEW_VULNERABILITIES_KEY, INT);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(ISSUES_METRIC)
    .add(OPEN_ISSUES_METRIC)
    .add(REOPENED_ISSUES_METRIC)
    .add(CONFIRMED_ISSUES_METRIC)
    .add(BLOCKER_ISSUES_METRIC)
    .add(CRITICAL_ISSUES_METRIC)
    .add(MAJOR_ISSUES_METRIC)
    .add(MINOR_ISSUES_METRIC)
    .add(INFO_ISSUES_METRIC)
    .add(NEW_ISSUES_METRIC)
    .add(NEW_BLOCKER_ISSUES_METRIC)
    .add(NEW_CRITICAL_ISSUES_METRIC)
    .add(NEW_MAJOR_ISSUES_METRIC)
    .add(NEW_MINOR_ISSUES_METRIC)
    .add(NEW_INFO_ISSUES_METRIC)
    .add(FALSE_POSITIVE_ISSUES_METRIC)
    .add(WONT_FIX_ISSUES_METRIC)
    .add(CODE_SMELLS_METRIC)
    .add(BUGS_METRIC)
    .add(VULNERABILITIES_METRIC)
    .add(NEW_CODE_SMELLS_METRIC)
    .add(NEW_BUGS_METRIC)
    .add(NEW_VULNERABILITIES_METRIC);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private IssueCounter underTest = new IssueCounter(periodsHolder, metricRepository, measureRepository);

  @Test
  public void count_issues_by_status() {
    periodsHolder.setPeriod(null);

    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FALSE_POSITIVE, STATUS_RESOLVED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, BLOCKER));
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(FILE3);
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertThat(measureRepository.getRawMeasure(FILE1, ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(FILE2, ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(FILE2, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE2, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);

    assertThat(measureRepository.getRawMeasure(FILE3, ISSUES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getIntValue()).isEqualTo(3);
    assertThat(measureRepository.getRawMeasure(PROJECT, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void count_issues_by_resolution() {
    periodsHolder.setPeriod(null);

    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FALSE_POSITIVE, STATUS_RESOLVED, MAJOR));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_CLOSED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, BLOCKER));
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.onIssue(FILE2, createIssue(RESOLUTION_WONT_FIX, STATUS_CLOSED, MAJOR));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(FILE3);
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertThat(measureRepository.getRawMeasure(FILE1, ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, WONT_FIX_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);

    assertThat(measureRepository.getRawMeasure(FILE2, ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(FILE2, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE2, WONT_FIX_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);

    assertThat(measureRepository.getRawMeasure(FILE3, ISSUES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(PROJECT, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, WONT_FIX_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void count_unresolved_issues_by_severity() {
    periodsHolder.setPeriod(null);

    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER));
    // this resolved issue is ignored
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, BLOCKER));
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

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
  public void count_unresolved_issues_by_type() {
    periodsHolder.setPeriod(null);

    // bottom-up traversal -> from files to project
    // file1 : one open code smell, one closed code smell (which will be excluded from metric)
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER).setType(RuleType.CODE_SMELL));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR).setType(RuleType.CODE_SMELL));
    underTest.afterComponent(FILE1);

    // file2 : one bug
    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, BLOCKER).setType(RuleType.BUG));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertThat(measureRepository.getRawMeasure(FILE1, CODE_SMELLS_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE1, BUGS_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE1, VULNERABILITIES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(FILE2, CODE_SMELLS_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(FILE2, BUGS_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(FILE2, VULNERABILITIES_METRIC).get().getIntValue()).isEqualTo(0);

    assertThat(measureRepository.getRawMeasure(PROJECT, CODE_SMELLS_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, BUGS_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, VULNERABILITIES_METRIC).get().getIntValue()).isEqualTo(0);
  }

  @Test
  public void count_new_issues() {
    Period period = newPeriod(1500000000000L);
    periodsHolder.setPeriod(period);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues (so ignored)
    underTest.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, BLOCKER, period.getSnapshotDate() - 1000000L).setType(RuleType.CODE_SMELL));
    // created during the first analysis starting the period -> existing issues (so ignored)
    underTest.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, BLOCKER, period.getSnapshotDate()).setType(RuleType.BUG));
    // created after -> 3 new issues but 1 is closed
    underTest.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, CRITICAL, period.getSnapshotDate() + 100000L).setType(RuleType.CODE_SMELL));
    underTest.onIssue(FILE1, createIssueAt(null, STATUS_OPEN, CRITICAL, period.getSnapshotDate() + 100000L).setType(RuleType.BUG));
    underTest.onIssue(FILE1, createIssueAt(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR, period.getSnapshotDate() + 200000L).setType(RuleType.BUG));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertVariation(FILE1, NEW_ISSUES_METRIC, 2);
    assertVariation(FILE1, NEW_CRITICAL_ISSUES_METRIC, 2);
    assertVariation(FILE1, NEW_BLOCKER_ISSUES_METRIC, 0);
    assertVariation(FILE1, NEW_MAJOR_ISSUES_METRIC, 0);
    assertVariation(FILE1, NEW_CODE_SMELLS_METRIC, 1);
    assertVariation(FILE1, NEW_BUGS_METRIC, 1);
    assertVariation(FILE1, NEW_VULNERABILITIES_METRIC, 0);

    assertVariation(PROJECT, NEW_ISSUES_METRIC, 2);
    assertVariation(PROJECT, NEW_CRITICAL_ISSUES_METRIC, 2);
    assertVariation(PROJECT, NEW_BLOCKER_ISSUES_METRIC, 0);
    assertVariation(PROJECT, NEW_MAJOR_ISSUES_METRIC, 0);
    assertVariation(PROJECT, NEW_CODE_SMELLS_METRIC, 1);
    assertVariation(PROJECT, NEW_BUGS_METRIC, 1);
    assertVariation(PROJECT, NEW_VULNERABILITIES_METRIC, 0);
  }

  private void assertVariation(Component component, Metric metric, int expectedVariation) {
    Measure measure = measureRepository.getRawMeasure(component, metric).get();
    assertThat(measure.getVariation()).isEqualTo((double) expectedVariation, Offset.offset(0.01));
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity) {
    return new DefaultIssue()
      .setResolution(resolution).setStatus(status)
      .setSeverity(severity).setRuleKey(RuleTesting.XOO_X1)
      .setType(RuleType.CODE_SMELL)
      .setCreationDate(new Date());
  }

  private static DefaultIssue createIssueAt(@Nullable String resolution, String status, String severity, long creationDate) {
    return new DefaultIssue()
      .setResolution(resolution).setStatus(status)
      .setSeverity(severity).setRuleKey(RuleTesting.XOO_X1)
      .setType(RuleType.CODE_SMELL)
      .setCreationDate(new Date(creationDate));
  }

  private static Period newPeriod(long date) {
    return new Period("mode", null, date, "U1");
  }

}
