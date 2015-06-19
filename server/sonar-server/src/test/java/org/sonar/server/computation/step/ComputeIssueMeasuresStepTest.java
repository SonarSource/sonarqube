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

import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Issue;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.issue.RuleCache;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepository;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.server.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.internal.DefaultIssue.RESOLUTION_FIXED;
import static org.sonar.api.issue.internal.DefaultIssue.RESOLUTION_REMOVED;
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
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.server.computation.component.DumbComponent.builder;
import static org.sonar.server.computation.metric.Metric.MetricType.INT;

public class ComputeIssueMeasuresStepTest {

  static final Component FILE = builder(Component.Type.FILE, 2).build();
  static final Component PROJECT = builder(Component.Type.PROJECT, 1).addChildren(FILE).build();

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

  static final RuleDto RULE_1 = RuleTesting.newDto(RuleKey.of("xoo", "x1")).setId(1);
  static final RuleDto RULE_2 = RuleTesting.newDto(RuleKey.of("xoo", "x2")).setId(2);
  static final RuleDto RULE_3 = RuleTesting.newDto(RuleKey.of("xoo", "x3")).setId(3);
  static final RuleDto RULE_4 = RuleTesting.newDto(RuleKey.of("xoo", "x4")).setId(4);
  static final RuleDto RULE_5 = RuleTesting.newDto(RuleKey.of("xoo", "x5")).setId(5);
  static final RuleDto RULE_6 = RuleTesting.newDto(RuleKey.of("xoo", "x6")).setId(6);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  MetricRepository metricRepository = mock(MetricRepository.class);
  RuleCache ruleCache = mock(RuleCache.class);
  MeasureRepository measureRepository;

  ComputeIssueMeasuresStep sut;

  @Before
  public void setUp() throws Exception {
    initMetrics();
    measureRepository = new MeasureRepositoryImpl(null, reportReader, metricRepository, ruleCache);

    sut = new ComputeIssueMeasuresStep(periodsHolder, reportReader, treeRootHolder, measureRepository, metricRepository);
  }

  @Test
  public void compute_total_issues_measure() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();
    addIssues(FILE.getRef(), createIssue(STATUS_OPEN, BLOCKER, RULE_1.getKey()));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
  }

  @Test
  public void compute_measures_on_all_levels() throws Exception {
    Component file1 = builder(Component.Type.FILE, 5).build();
    Component file2 = builder(Component.Type.FILE, 4).build();
    Component file3 = builder(Component.Type.FILE, 3).build();
    Component directory = builder(Component.Type.DIRECTORY, 2).addChildren(file1, file2, file3).build();
    Component project = builder(Component.Type.PROJECT, 1).addChildren(directory).build();
    treeRootHolder.setRoot(project);
    periodsHolder.setPeriods();

    addIssues(file1.getRef(), createIssue(STATUS_OPEN, BLOCKER, RULE_1.getKey()));
    addIssues(file2.getRef(), createIssue(STATUS_REOPENED, CRITICAL, RULE_2.getKey()));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(file1, ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(file2, ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(directory, ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(project, ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void compute_measures_on_issue_statuses() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();
    addIssues(FILE.getRef(),
      createIssue(STATUS_OPEN, BLOCKER, RULE_1.getKey()),
      createIssue(STATUS_REOPENED, BLOCKER, RULE_2.getKey()),
      createIssue(STATUS_CONFIRMED, BLOCKER, RULE_3.getKey()),
      createIssue(STATUS_CONFIRMED, BLOCKER, RULE_4.getKey()));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, REOPENED_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, CONFIRMED_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void compute_measures_on_issue_severities() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();
    addIssues(FILE.getRef(),
      createIssue(STATUS_OPEN, BLOCKER, RULE_1.getKey()),
      createIssue(STATUS_OPEN, CRITICAL, RULE_2.getKey()),
      createIssue(STATUS_OPEN, MAJOR, RULE_3.getKey()),
      createIssue(STATUS_OPEN, MINOR, RULE_4.getKey()),
      createIssue(STATUS_OPEN, INFO, RULE_5.getKey()),
      createIssue(STATUS_OPEN, INFO, RULE_6.getKey()));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, BLOCKER_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, CRITICAL_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, MAJOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, MINOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, INFO_ISSUES_METRIC).get().getIntValue()).isEqualTo(2);
  }

  @Test
  public void compute_measures_on_false_positive_issue() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();
    addIssues(FILE.getRef(),
      createIssue(STATUS_OPEN, BLOCKER, RULE_1.getKey()),
      createIssue(STATUS_CLOSED, BLOCKER, RESOLUTION_FALSE_POSITIVE, RULE_2.getKey()),
      createIssue(STATUS_RESOLVED, BLOCKER, RESOLUTION_FIXED, RULE_3.getKey()),
      createIssue(STATUS_RESOLVED, BLOCKER, RESOLUTION_REMOVED, RULE_4.getKey()));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, FALSE_POSITIVE_ISSUES_METRIC).get().getIntValue()).isEqualTo(1);
  }

  @Test
  public void compute_measures_on_new_issue() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    addIssues(FILE.getRef(),
      // issue created before the period 3
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1388552400000L),
      // issue created after period 3 but before current analysis
      createIssue(STATUS_OPEN, BLOCKER, null, RULE_1.getKey(), 1433131200000L));
    periodsHolder.setPeriods(newPeriod(3, 1420088400000L));

    sut.execute();

    // Only 1 new issues for period 3
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation3()).isEqualTo(1);

    // No variation on other periods
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().hasVariation1()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().hasVariation2()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().hasVariation4()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().hasVariation5()).isFalse();
  }

  @Test
  public void do_not_take_into_account_issue_from_current_analysis_when_computing_measures_on_new_issue() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    addIssues(FILE.getRef(),
      // issue created during current analysis -> should not be taking into account
      createIssue(STATUS_OPEN, BLOCKER, null, RULE_1.getKey(), 1420088400000L));
    periodsHolder.setPeriods(newPeriod(1, 1420088400000L));

    sut.execute();

    // No new issues
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(0);
  }

  @Test
  public void compute_measures_on_new_issue_on_every_variations() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    addIssues(FILE.getRef(),
      // issue created the 2014-01-01, before all periods
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1388552400000L),
      // issue created the 2015-01-15, before period 2
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1421298000000L),
      // issue created the 2015-02-15, before period 3
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1423976400000L),
      // issue created the 2015-03-15, before period 4
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1426392000000L),
      // issue created the 2015-04-15, before period 5
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1429070400000L),
      // issue created the 2015-06-01 -> Should not been taken into account by any period
      createIssue(STATUS_OPEN, BLOCKER, null, RULE_1.getKey(), 1433131200000L));
    periodsHolder.setPeriods(
      // 2015-01-01
      newPeriod(1, 1420088400000L),
      // 2015-02-01
      newPeriod(2, 1422766800000L),
      // 2015-03-01
      newPeriod(3, 1425186000000L),
      // 2015-04-01
      newPeriod(4, 1427860800000L),
      // 2015-05-01
      newPeriod(5, 1430452800000L));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(5);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation2()).isEqualTo(4);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation3()).isEqualTo(3);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation4()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).get().getVariations().getVariation5()).isEqualTo(1);
  }

  @Test
  public void compute_measures_on_new_issue_severities() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    addIssues(FILE.getRef(),
      // issue created before the period 1
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1388552400000L),
      // issues created after period 1 but before current analysis
      createIssue(STATUS_OPEN, BLOCKER, null, RULE_1.getKey(), 1433131200000L),
      createIssue(STATUS_OPEN, BLOCKER, null, RULE_2.getKey(), 1433131200000L),
      createIssue(STATUS_OPEN, CRITICAL, null, RULE_1.getKey(), 1433131200000L),
      createIssue(STATUS_OPEN, MAJOR, null, RULE_1.getKey(), 1433131200000L),
      createIssue(STATUS_OPEN, MINOR, null, RULE_1.getKey(), 1433131200000L),
      createIssue(STATUS_OPEN, INFO, null, RULE_1.getKey(), 1433131200000L));
    periodsHolder.setPeriods(newPeriod(1, 1420088400000L));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_BLOCKER_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(2);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_CRITICAL_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_MAJOR_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_MINOR_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(1);
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_INFO_ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(1);
  }

  @Test
  public void compute_no_new_measures_when_no_period() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();
    addIssues(FILE.getRef(),
      createIssue(STATUS_CONFIRMED, BLOCKER, null, RULE_1.getKey(), 1388552400000L));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_ISSUES_METRIC).isPresent()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_BLOCKER_ISSUES_METRIC).isPresent()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_CRITICAL_ISSUES_METRIC).isPresent()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_MAJOR_ISSUES_METRIC).isPresent()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_MINOR_ISSUES_METRIC).isPresent()).isFalse();
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_INFO_ISSUES_METRIC).isPresent()).isFalse();
  }

  @Test
  public void compute_measures_having_zero_value_if_no_issue() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, OPEN_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, REOPENED_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, BLOCKER_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, CRITICAL_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, MAJOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, MINOR_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
    assertThat(measureRepository.getRawMeasure(PROJECT, INFO_ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
  }

  @Test
  public void ignore_resolved_issues() throws Exception {
    treeRootHolder.setRoot(PROJECT);
    periodsHolder.setPeriods();
    addIssues(FILE.getRef(),
      createIssue(STATUS_CLOSED, BLOCKER, RESOLUTION_FALSE_POSITIVE, RULE_1.getKey()),
      createIssue(STATUS_RESOLVED, BLOCKER, RESOLUTION_FIXED, RULE_2.getKey()),
      createIssue(STATUS_RESOLVED, BLOCKER, RESOLUTION_REMOVED, RULE_3.getKey()));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getIntValue()).isEqualTo(0);
  }

  private void addIssues(int componentRef, Issue... issues) {
    reportReader.putIssues(componentRef, Arrays.asList(issues));
  }

  private static Issue createIssue(String status, String severity, RuleKey ruleKey) {
    return createIssue(status, severity, null, ruleKey, 1000L);
  }

  private static Issue createIssue(String status, String severity, @Nullable String resolution, RuleKey ruleKey) {
    return createIssue(status, severity, resolution, ruleKey, 1000L);
  }

  private static Issue createIssue(String status, String severity, @Nullable String resolution, RuleKey ruleKey, long creationDate) {
    BatchReport.Issue.Builder issueBuilder = Issue.newBuilder()
      .setUuid(Uuids.create())
      .setStatus(status)
      .setRuleKey(ruleKey.rule())
      .setRuleRepository(ruleKey.repository())
      .setSeverity(Constants.Severity.valueOf(severity))
      .setCreationDate(creationDate);
    if (resolution != null) {
      issueBuilder.setResolution(resolution);
    }
    return issueBuilder.build();
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
