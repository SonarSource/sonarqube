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

import org.junit.Test;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;

public class EffortAggregatorTest {

  static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(TECHNICAL_DEBT)
    .add(RELIABILITY_REMEDIATION_EFFORT)
    .add(SECURITY_REMEDIATION_EFFORT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(PROJECT, metricRepository);

  EffortAggregator underTest = new EffortAggregator(metricRepository, measureRepository);

  @Test
  public void sum_maintainability_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newCodeSmellIssue(10);
    DefaultIssue unresolved2 = newCodeSmellIssue(30);
    DefaultIssue unresolvedWithoutEffort = newCodeSmellIssueWithoutEffort();
    DefaultIssue resolved = newCodeSmellIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutEffort);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    // total maintainability effort
    assertMeasure(FILE, TECHNICAL_DEBT_KEY, 10L + 30L);
  }

  @Test
  public void maintainability_effort_is_only_computed_using_code_smell_issues() {
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    // Issues of type BUG and VULNERABILITY should be ignored
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of CODE SMELL issue is used
    assertMeasure(FILE, TECHNICAL_DEBT_KEY, 10L);
  }

  @Test
  public void sum_reliability_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newBugIssue(10);
    DefaultIssue unresolved2 = newBugIssue(30);
    DefaultIssue unresolvedWithoutEffort = newBugIssueWithoutEffort();
    DefaultIssue resolved = newBugIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutEffort);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertMeasure(FILE, RELIABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
  }

  @Test
  public void reliability_effort_is_only_computed_using_bug_issues() {
    DefaultIssue bugIssue = newBugIssue(10);
    // Issues of type CODE SMELL and VULNERABILITY should be ignored
    DefaultIssue codeSmellIssue = newCodeSmellIssue(15);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of BUG issue is used
    assertMeasure(FILE, RELIABILITY_REMEDIATION_EFFORT_KEY, 10L);
  }

  @Test
  public void sum_security_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newVulnerabilityIssue(10);
    DefaultIssue unresolved2 = newVulnerabilityIssue(30);
    DefaultIssue unresolvedWithoutEffort = newVulnerabilityIssueWithoutEffort();
    DefaultIssue resolved = newVulnerabilityIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutEffort);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertMeasure(FILE, SECURITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
  }

  @Test
  public void security_effort_is_only_computed_using_code_smell_issues() {
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(10);
    // Issues of type BUG and CODE SMELL should be ignored
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue codeSmellIssue = newCodeSmellIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.afterComponent(FILE);

    // Only effort of VULNERABILITY issue is used
    assertMeasure(FILE, SECURITY_REMEDIATION_EFFORT_KEY, 10L);
  }

  @Test
  public void aggregate_maintainability_measures_of_children() {
    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, newCodeSmellIssue(10));
    underTest.onIssue(FILE, newBugIssue(8));
    underTest.onIssue(FILE, newVulnerabilityIssue(12));
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, newCodeSmellIssue(30));
    underTest.onIssue(PROJECT, newBugIssue(38));
    underTest.onIssue(PROJECT, newVulnerabilityIssue(42));
    underTest.afterComponent(PROJECT);

    assertMeasure(PROJECT, TECHNICAL_DEBT_KEY, 10L + 30L);
    assertMeasure(PROJECT, RELIABILITY_REMEDIATION_EFFORT_KEY, 8L + 38L);
    assertMeasure(PROJECT, SECURITY_REMEDIATION_EFFORT_KEY, 12L + 42L);
  }

  @Test
  public void sum_characteristic_measures_of_issues_without_effort() {
    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, newCodeSmellIssueWithoutEffort());
    underTest.onIssue(FILE, newBugIssueWithoutEffort());
    underTest.onIssue(FILE, newVulnerabilityIssueWithoutEffort());
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, newCodeSmellIssueWithoutEffort());
    underTest.afterComponent(PROJECT);

    assertMeasure(PROJECT, TECHNICAL_DEBT_KEY, 0L);
    assertMeasure(PROJECT, RELIABILITY_REMEDIATION_EFFORT_KEY, 0L);
    assertMeasure(PROJECT, SECURITY_REMEDIATION_EFFORT_KEY, 0L);
  }

  private void assertMeasure(Component component, String metricKey, long expectedValue) {
    assertThat(measureRepository.getAddedRawMeasure(component, metricKey).get().getLongValue()).isEqualTo(expectedValue);
  }

  private static DefaultIssue newCodeSmellIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setEffort(Duration.create(effort)).setType(CODE_SMELL);
  }

  private static DefaultIssue newBugIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setEffort(Duration.create(effort)).setType(BUG);
  }

  private static DefaultIssue newVulnerabilityIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setEffort(Duration.create(effort)).setType(VULNERABILITY);
  }

  private static DefaultIssue newCodeSmellIssueWithoutEffort() {
    return new DefaultIssue().setType(CODE_SMELL);
  }

  private static DefaultIssue newBugIssueWithoutEffort() {
    return new DefaultIssue().setType(BUG);
  }

  private static DefaultIssue newVulnerabilityIssueWithoutEffort() {
    return new DefaultIssue().setType(VULNERABILITY);
  }

}
