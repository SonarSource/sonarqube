/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.issue;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;

public class EffortAggregatorTest {

  static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  static final DumbRule RULE = new DumbRule(RuleTesting.XOO_X1).setId(100);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(RULE);

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(200, CoreMetrics.TECHNICAL_DEBT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(PROJECT, metricRepository);

  EffortAggregator underTest = new EffortAggregator(ruleRepository, metricRepository, measureRepository);

  @Test
  public void sum_maintainability_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newCodeSmellIssue(10);
    DefaultIssue unresolved2 = newCodeSmellIssue(30);
    DefaultIssue unresolvedWithoutDebt = newCodeSmellIssueWithoutEffort();
    DefaultIssue resolved = newCodeSmellIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    // total maintainability effort
    assertThat(maintainabilityEffortMeasure(FILE).get().getLongValue()).isEqualTo(10 + 30);

    // maintainability effort by rule
    assertThat(debtRuleMeasure(FILE, RULE.getId()).get().getLongValue()).isEqualTo(10 + 30);
  }

  @Test
  public void maintainability_effort_is_only_computed_using_code_smell_issues() {
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    // Issue of type BUG should be ignored
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of code smell issue is used
    assertThat(maintainabilityEffortMeasure(FILE).get().getLongValue()).isEqualTo(10);
  }

  @Test
  public void aggregate_maintainability_effort_of_children() {
    DefaultIssue fileIssue = newCodeSmellIssue(10);
    DefaultIssue projectIssue = newCodeSmellIssue(30);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    // total maintainability effort of project
    assertThat(maintainabilityEffortMeasure(PROJECT).get().getLongValue()).isEqualTo(10 + 30);

    // maintainability effort by rule
    assertThat(debtRuleMeasure(PROJECT, RULE.getId()).get().getLongValue()).isEqualTo(10 + 30);
  }

  @Test
  public void sum_maintainability_effort_of_issues_without_effort() {
    DefaultIssue fileIssue = newCodeSmellIssueWithoutEffort();
    DefaultIssue projectIssue = newCodeSmellIssueWithoutEffort();

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    // total maintainability effort of project
    assertThat(maintainabilityEffortMeasure(PROJECT).get().getLongValue()).isZero();

    // maintainability effort by rule
    assertThat(debtRuleMeasure(PROJECT, RULE.getId())).isAbsent();
  }

  @CheckForNull
  private Optional<Measure> maintainabilityEffortMeasure(Component component) {
    return measureRepository.getAddedRawMeasure(component, TECHNICAL_DEBT_KEY);
  }

  @CheckForNull
  private Optional<Measure> debtRuleMeasure(Component component, int ruleId) {
    return measureRepository.getAddedRawRuleMeasure(component, TECHNICAL_DEBT_KEY, ruleId);
  }

  private static DefaultIssue newCodeSmellIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setDebt(Duration.create(effort)).setType(RuleType.CODE_SMELL);
  }

  private static DefaultIssue newBugIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setDebt(Duration.create(effort)).setType(RuleType.BUG);
  }

  private static DefaultIssue newVulnerabilityIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setDebt(Duration.create(effort)).setType(RuleType.VULNERABILITY);
  }

  private static DefaultIssue newCodeSmellIssueWithoutEffort() {
    return new DefaultIssue().setRuleKey(RULE.getKey()).setType(RuleType.CODE_SMELL);
  }
}
