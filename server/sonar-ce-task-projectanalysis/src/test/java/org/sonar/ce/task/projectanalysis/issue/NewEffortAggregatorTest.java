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

import java.util.Date;
import java.util.Random;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.component.BranchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;

public class NewEffortAggregatorTest {

  private static final Period PERIOD = new Period(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, 1_500_000_000L, "U1");
  private static final long[] OLD_ISSUES_DATES = new long[] {
    PERIOD.getSnapshotDate(),
    PERIOD.getSnapshotDate() - 1,
    PERIOD.getSnapshotDate() - 1_200_000L,
  };

  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).setUuid("FILE").build();
  private static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  @org.junit.Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();
  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_TECHNICAL_DEBT)
    .add(NEW_RELIABILITY_REMEDIATION_EFFORT)
    .add(NEW_SECURITY_REMEDIATION_EFFORT);
  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create();
  @org.junit.Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private final Random random = new Random();
  private NewEffortAggregator underTest = new NewEffortAggregator(periodsHolder, analysisMetadataHolder, metricRepository, measureRepository);

  @Test
  public void sum_new_maintainability_effort_of_issues() {
    periodsHolder.setPeriod(PERIOD);
    DefaultIssue unresolved1 = newCodeSmellIssue(10L);
    DefaultIssue old1 = oldCodeSmellIssue(100L);
    DefaultIssue unresolved2 = newCodeSmellIssue(30L);
    DefaultIssue old2 = oldCodeSmellIssue(300L);
    DefaultIssue unresolvedWithoutDebt = newCodeSmellIssueWithoutEffort();
    DefaultIssue resolved = newCodeSmellIssue(50L).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, old1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, old2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_TECHNICAL_DEBT_KEY, 10 + 30);
  }

  @Test
  public void new_maintainability_effort_is_only_computed_using_code_smell_issues() {
    periodsHolder.setPeriod(PERIOD);
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    DefaultIssue oldSmellIssue = oldCodeSmellIssue(100);
    // Issues of type BUG and VULNERABILITY should be ignored
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue oldBugIssue = oldBugIssue(150);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of CODE SMELL issue is used
    assertVariation(FILE, NEW_TECHNICAL_DEBT_KEY, 10);
  }

  @Test
  public void sum_new_reliability_effort_of_issues() {
    periodsHolder.setPeriod(PERIOD);
    DefaultIssue unresolved1 = newBugIssue(10L);
    DefaultIssue old1 = oldBugIssue(100L);
    DefaultIssue unresolved2 = newBugIssue(30L);

    DefaultIssue old2 = oldBugIssue(300L);
    DefaultIssue unresolvedWithoutDebt = newBugIssueWithoutEffort();
    DefaultIssue resolved = newBugIssue(50L).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, old1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, old2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 10 + 30);
  }

  @Test
  public void new_reliability_effort_is_only_computed_using_bug_issues() {
    periodsHolder.setPeriod(PERIOD);
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue oldBugIssue = oldBugIssue(150);
    // Issues of type CODE SMELL and VULNERABILITY should be ignored
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    DefaultIssue oldCodeSmellIssue = oldCodeSmellIssue(100);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldCodeSmellIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of BUG issue is used
    assertVariation(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 15);
  }

  @Test
  public void sum_new_vulnerability_effort_of_issues() {
    periodsHolder.setPeriod(PERIOD);
    DefaultIssue unresolved1 = newVulnerabilityIssue(10L);
    DefaultIssue old1 = oldVulnerabilityIssue(100L);
    DefaultIssue unresolved2 = newVulnerabilityIssue(30L);
    DefaultIssue old2 = oldVulnerabilityIssue(300L);
    DefaultIssue unresolvedWithoutDebt = newVulnerabilityIssueWithoutEffort();
    DefaultIssue resolved = newVulnerabilityIssue(50L).setResolution(RESOLUTION_FIXED);
    DefaultIssue oldResolved = oldVulnerabilityIssue(500L).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, old1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, old2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.onIssue(FILE, oldResolved);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 10 + 30);
  }

  @Test
  public void new_security_effort_is_only_computed_using_vulnerability_issues() {
    periodsHolder.setPeriod(PERIOD);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);
    // Issues of type CODE SMELL and BUG should be ignored
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    DefaultIssue oldCodeSmellIssue = oldCodeSmellIssue(100);
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue oldBugIssue = oldBugIssue(150);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldCodeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of VULNERABILITY issue is used
    assertVariation(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 12);
  }

  @Test
  public void aggregate_new_characteristic_measures_of_children() {
    periodsHolder.setPeriod(PERIOD);

    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    DefaultIssue oldCodeSmellIssue = oldCodeSmellIssue(100);
    DefaultIssue bugIssue = newBugIssue(8);
    DefaultIssue oldBugIssue = oldBugIssue(80);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);

    DefaultIssue codeSmellProjectIssue = newCodeSmellIssue(30);
    DefaultIssue oldCodeSmellProjectIssue = oldCodeSmellIssue(300);
    DefaultIssue bugProjectIssue = newBugIssue(28);
    DefaultIssue oldBugProjectIssue = oldBugIssue(280);
    DefaultIssue vulnerabilityProjectIssue = newVulnerabilityIssue(32);
    DefaultIssue oldVulnerabilityProjectIssue = oldVulnerabilityIssue(320);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldCodeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, codeSmellProjectIssue);
    underTest.onIssue(PROJECT, oldCodeSmellProjectIssue);
    underTest.onIssue(PROJECT, bugProjectIssue);
    underTest.onIssue(PROJECT, oldBugProjectIssue);
    underTest.onIssue(PROJECT, vulnerabilityProjectIssue);
    underTest.onIssue(PROJECT, oldVulnerabilityProjectIssue);
    underTest.afterComponent(PROJECT);

    assertVariation(PROJECT, NEW_TECHNICAL_DEBT_KEY, 10 + 30);
    assertVariation(PROJECT, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 8 + 28);
    assertVariation(PROJECT, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 12 + 32);
  }

  @Test
  public void no_measures_if_no_periods() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.LONG);
    analysisMetadataHolder.setBranch(branch);
    periodsHolder.setPeriod(null);
    DefaultIssue unresolved = newCodeSmellIssue(10);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved);
    underTest.afterComponent(FILE);

    assertThat(measureRepository.getRawMeasures(FILE)).isEmpty();
  }

  @Test
  public void should_have_empty_measures_if_no_issues() {
    periodsHolder.setPeriod(PERIOD);

    underTest.beforeComponent(FILE);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_TECHNICAL_DEBT_KEY, 0);
    assertVariation(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 0);
    assertVariation(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 0);
  }

  private void assertVariation(Component component, String metricKey, int variation) {
    Measure newMeasure = measureRepository.getRawMeasure(component, metricRepository.getByKey(metricKey)).get();
    assertThat(newMeasure.getVariation()).isEqualTo(variation);
    assertThat(newMeasure.getValueType()).isEqualTo(Measure.ValueType.NO_VALUE);
  }

  private static DefaultIssue newCodeSmellIssue(long effort) {
    return newCodeSmellIssueWithoutEffort()
      .setEffort(Duration.create(effort))
      .setType(RuleType.CODE_SMELL)
      .setCreationDate(new Date(PERIOD.getSnapshotDate() + 10_000L));
  }

  private DefaultIssue oldCodeSmellIssue(long effort) {
    return newCodeSmellIssueWithoutEffort()
      .setEffort(Duration.create(effort))
      .setType(RuleType.CODE_SMELL)
      .setCreationDate(new Date(OLD_ISSUES_DATES[random.nextInt(OLD_ISSUES_DATES.length)]));
  }

  private static DefaultIssue newBugIssue(long effort) {
    return newCodeSmellIssueWithoutEffort()
      .setEffort(Duration.create(effort))
      .setType(RuleType.BUG)
      .setCreationDate(new Date(PERIOD.getSnapshotDate() + 10_000L));
  }

  private DefaultIssue oldBugIssue(long effort) {
    return newCodeSmellIssueWithoutEffort()
      .setEffort(Duration.create(effort))
      .setType(RuleType.BUG)
      .setCreationDate(new Date(OLD_ISSUES_DATES[random.nextInt(OLD_ISSUES_DATES.length)]));
  }

  private static DefaultIssue newVulnerabilityIssue(long effort) {
    return newCodeSmellIssueWithoutEffort()
      .setEffort(Duration.create(effort))
      .setType(RuleType.VULNERABILITY)
      .setCreationDate(new Date(PERIOD.getSnapshotDate() + 10_000L));
  }

  private DefaultIssue oldVulnerabilityIssue(long effort) {
    return newCodeSmellIssueWithoutEffort()
      .setEffort(Duration.create(effort))
      .setType(RuleType.VULNERABILITY)
      .setCreationDate(new Date(OLD_ISSUES_DATES[random.nextInt(OLD_ISSUES_DATES.length)]));
  }

  private static DefaultIssue newCodeSmellIssueWithoutEffort() {
    return new DefaultIssue()
      .setType(CODE_SMELL)
      .setCreationDate(new Date(PERIOD.getSnapshotDate() + 10_000L));
  }

  private static DefaultIssue newBugIssueWithoutEffort() {
    return new DefaultIssue().setType(BUG);
  }

  private static DefaultIssue newVulnerabilityIssueWithoutEffort() {
    return new DefaultIssue().setType(VULNERABILITY);
  }
}
