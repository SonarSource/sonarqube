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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.BranchType;
import org.sonar.db.rule.RuleTesting;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_INFO_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES_KEY;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.REOPENED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.measures.CoreMetrics.WONT_FIX_ISSUES;
import static org.sonar.api.measures.CoreMetrics.WONT_FIX_ISSUES_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;

public class IssueCounterTest {

  private static final Component FILE1 = builder(Component.Type.FILE, 1).build();
  private static final Component FILE2 = builder(Component.Type.FILE, 2).build();
  private static final Component FILE3 = builder(Component.Type.FILE, 3).build();
  private static final Component PROJECT = builder(Component.Type.PROJECT, 4).addChildren(FILE1, FILE2, FILE3).build();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(VIOLATIONS)
    .add(OPEN_ISSUES)
    .add(REOPENED_ISSUES)
    .add(CONFIRMED_ISSUES)
    .add(BLOCKER_VIOLATIONS)
    .add(CRITICAL_VIOLATIONS)
    .add(MAJOR_VIOLATIONS)
    .add(MINOR_VIOLATIONS)
    .add(INFO_VIOLATIONS)
    .add(NEW_VIOLATIONS)
    .add(NEW_BLOCKER_VIOLATIONS)
    .add(NEW_CRITICAL_VIOLATIONS)
    .add(NEW_MAJOR_VIOLATIONS)
    .add(NEW_MINOR_VIOLATIONS)
    .add(NEW_INFO_VIOLATIONS)
    .add(FALSE_POSITIVE_ISSUES)
    .add(WONT_FIX_ISSUES)
    .add(CODE_SMELLS)
    .add(BUGS)
    .add(VULNERABILITIES)
    .add(SECURITY_HOTSPOTS)
    .add(NEW_CODE_SMELLS)
    .add(NEW_BUGS)
    .add(NEW_VULNERABILITIES)
    .add(NEW_SECURITY_HOTSPOTS);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private IssueCounter underTest = new IssueCounter(periodsHolder, analysisMetadataHolder, metricRepository, measureRepository);

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
    // Security hotspot should be ignored
    underTest.onIssue(FILE3, createSecurityHotspot().setStatus(STATUS_OPEN));
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(VIOLATIONS_KEY, 1), entry(OPEN_ISSUES_KEY, 1), entry(CONFIRMED_ISSUES_KEY, 0));
    assertMeasures(FILE2, entry(VIOLATIONS_KEY, 2), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 2));
    assertMeasures(FILE3, entry(VIOLATIONS_KEY, 0));
    assertMeasures(PROJECT, entry(VIOLATIONS_KEY, 3), entry(OPEN_ISSUES_KEY, 1), entry(CONFIRMED_ISSUES_KEY, 2));
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
    // Security hotspot should be ignored
    underTest.onIssue(FILE3, createSecurityHotspot().setResolution(RESOLUTION_WONT_FIX));
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(VIOLATIONS_KEY, 1), entry(FALSE_POSITIVE_ISSUES_KEY, 1), entry(WONT_FIX_ISSUES_KEY, 1));
    assertMeasures(FILE2, entry(VIOLATIONS_KEY, 2), entry(FALSE_POSITIVE_ISSUES_KEY, 0), entry(WONT_FIX_ISSUES_KEY, 1));
    assertMeasures(FILE3, entry(VIOLATIONS_KEY, 0));
    assertMeasures(PROJECT, entry(VIOLATIONS_KEY, 3), entry(FALSE_POSITIVE_ISSUES_KEY, 1), entry(WONT_FIX_ISSUES_KEY, 2));
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
    // Security hotspot should be ignored
    underTest.onIssue(FILE3, createSecurityHotspot().setSeverity(MAJOR));
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(BLOCKER_VIOLATIONS_KEY, 1), entry(CRITICAL_VIOLATIONS_KEY, 0), entry(MAJOR_VIOLATIONS_KEY, 0));
    assertMeasures(FILE2, entry(BLOCKER_VIOLATIONS_KEY, 1), entry(CRITICAL_VIOLATIONS_KEY, 0), entry(MAJOR_VIOLATIONS_KEY, 1));
    assertMeasures(PROJECT, entry(BLOCKER_VIOLATIONS_KEY, 2), entry(CRITICAL_VIOLATIONS_KEY, 0), entry(MAJOR_VIOLATIONS_KEY, 1));
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

    // file3 : one unresolved security hotspot
    underTest.beforeComponent(FILE3);
    underTest.onIssue(FILE3, createSecurityHotspot());
    underTest.onIssue(FILE3, createSecurityHotspot().setResolution(RESOLUTION_WONT_FIX).setStatus(STATUS_CLOSED));
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(CODE_SMELLS_KEY, 1), entry(BUGS_KEY, 0), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 0));
    assertMeasures(FILE2, entry(CODE_SMELLS_KEY, 0), entry(BUGS_KEY, 1), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 0));
    assertMeasures(FILE3, entry(CODE_SMELLS_KEY, 0), entry(BUGS_KEY, 0), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 1));
    assertMeasures(PROJECT, entry(CODE_SMELLS_KEY, 1), entry(BUGS_KEY, 1), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 1));
  }

  @Test
  public void count_new_issues_if_period_exists() {
    Period period = newPeriod(1500000000000L);
    periodsHolder.setPeriod(period);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues (so ignored)
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER, period.getSnapshotDate() - 1000000L).setType(RuleType.CODE_SMELL));
    // created during the first analysis starting the period -> existing issues (so ignored)
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER, period.getSnapshotDate()).setType(RuleType.BUG));
    // created after -> 4 new issues but 1 is closed
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, CRITICAL, period.getSnapshotDate() + 100000L).setType(RuleType.CODE_SMELL));
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, CRITICAL, period.getSnapshotDate() + 100000L).setType(RuleType.BUG));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR, period.getSnapshotDate() + 200000L).setType(RuleType.BUG));
    underTest.onIssue(FILE1, createSecurityHotspot(period.getSnapshotDate() + 100000L));
    underTest.onIssue(FILE1, createSecurityHotspot(period.getSnapshotDate() + 100000L).setResolution(RESOLUTION_WONT_FIX).setStatus(STATUS_CLOSED));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertVariations(FILE1, entry(NEW_VIOLATIONS_KEY, 2), entry(NEW_CRITICAL_VIOLATIONS_KEY, 2), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0), entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_CODE_SMELLS_KEY, 1), entry(NEW_BUGS_KEY, 1), entry(NEW_VULNERABILITIES_KEY, 0), entry(NEW_SECURITY_HOTSPOTS_KEY, 1));
    assertVariations(PROJECT, entry(NEW_VIOLATIONS_KEY, 2), entry(NEW_CRITICAL_VIOLATIONS_KEY, 2), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0), entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_CODE_SMELLS_KEY, 1), entry(NEW_BUGS_KEY, 1), entry(NEW_VULNERABILITIES_KEY, 0), entry(NEW_SECURITY_HOTSPOTS_KEY, 1));
  }

  @Test
  public void count_all_issues_as_new_issues_if_pr_or_slb() {
    periodsHolder.setPeriod(null);
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.SHORT);
    analysisMetadataHolder.setBranch(branch);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER, 1000000L).setType(RuleType.CODE_SMELL));
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, BLOCKER, 0L).setType(RuleType.BUG));
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, CRITICAL, 100000L).setType(RuleType.CODE_SMELL));
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, CRITICAL, 100000L).setType(RuleType.BUG));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR, 200000L).setType(RuleType.BUG));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertVariations(FILE1, entry(NEW_VIOLATIONS_KEY, 4), entry(NEW_CRITICAL_VIOLATIONS_KEY, 2), entry(NEW_BLOCKER_VIOLATIONS_KEY, 2), entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_CODE_SMELLS_KEY, 2), entry(NEW_BUGS_KEY, 2), entry(NEW_VULNERABILITIES_KEY, 0));
    assertVariations(PROJECT, entry(NEW_VIOLATIONS_KEY, 4), entry(NEW_CRITICAL_VIOLATIONS_KEY, 2), entry(NEW_BLOCKER_VIOLATIONS_KEY, 2), entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_CODE_SMELLS_KEY, 2), entry(NEW_BUGS_KEY, 2), entry(NEW_VULNERABILITIES_KEY, 0));
  }

  @Test
  public void exclude_hotspots_from_issue_counts() {
    periodsHolder.setPeriod(null);

    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createSecurityHotspot());
    underTest.onIssue(FILE1, createSecurityHotspot());
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createSecurityHotspot());
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(FILE3);
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(VIOLATIONS_KEY, 0), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 0));
    assertMeasures(FILE2, entry(VIOLATIONS_KEY, 0), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 0));
    assertMeasures(FILE3, entry(VIOLATIONS_KEY, 0));
    assertMeasures(PROJECT, entry(VIOLATIONS_KEY, 0), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 0));
  }

  @Test
  public void exclude_new_hotspots_from_issue_counts() {
    Period period = newPeriod(1500000000000L);
    periodsHolder.setPeriod(period);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues (so ignored)
    underTest.onIssue(FILE1, createSecurityHotspot(period.getSnapshotDate() - 1000000L));
    // created during the first analysis starting the period -> existing issues (so ignored)
    underTest.onIssue(FILE1, createSecurityHotspot(period.getSnapshotDate()));

    // created after, but closed
    underTest.onIssue(FILE1, createSecurityHotspot(period.getSnapshotDate() + 100000L).setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_WONT_FIX));

    for (String severity : Arrays.asList(CRITICAL, BLOCKER, MAJOR)) {
      DefaultIssue issue = createSecurityHotspot(period.getSnapshotDate() + 100000L);
      issue.setSeverity(severity);
      underTest.onIssue(FILE1, issue);
    }
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertVariations(FILE1, entry(NEW_VIOLATIONS_KEY, 0), entry(NEW_CRITICAL_VIOLATIONS_KEY, 0), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0), entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_VULNERABILITIES_KEY, 0));
    assertVariations(PROJECT, entry(NEW_VIOLATIONS_KEY, 0), entry(NEW_CRITICAL_VIOLATIONS_KEY, 0), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0), entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_VULNERABILITIES_KEY, 0));
  }

  @SafeVarargs
  private final void assertVariations(Component componentRef, MapEntry<String, Integer>... entries) {
    assertThat(measureRepository.getRawMeasures(componentRef).entrySet()
      .stream()
      .filter(e -> e.getValue().hasVariation())
      .map(e -> entry(e.getKey(), (int) e.getValue().getVariation())))
      .contains(entries);
  }

  @SafeVarargs
  private final void assertMeasures(Component componentRef, Map.Entry<String, Integer>... entries) {
    List<MeasureRepoEntry> expected = stream(entries)
      .map(e -> entryOf(e.getKey(), newMeasureBuilder().create(e.getValue())))
      .collect(Collectors.toList());

    assertThat(measureRepository.getRawMeasures(componentRef).entrySet().stream().map(e -> entryOf(e.getKey(), e.getValue())))
      .containsAll(expected);
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity) {
    return createIssue(resolution, status, severity, RuleType.CODE_SMELL, new Date().getTime());
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity, long creationDate) {
    return createIssue(resolution, status, severity, RuleType.CODE_SMELL, creationDate);
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity, RuleType ruleType, long creationDate) {
    return new DefaultIssue()
      .setResolution(resolution).setStatus(status)
      .setSeverity(severity).setRuleKey(RuleTesting.XOO_X1)
      .setType(ruleType)
      .setCreationDate(new Date(creationDate));
  }

  private static DefaultIssue createSecurityHotspot() {
    return createSecurityHotspot(new Date().getTime());
  }

  private static DefaultIssue createSecurityHotspot(long creationDate) {
    return createIssue(null, STATUS_OPEN, "MAJOR", RuleType.SECURITY_HOTSPOT, creationDate);
  }

  private static Period newPeriod(long date) {
    return new Period("mode", null, date, "U1");
  }

}
