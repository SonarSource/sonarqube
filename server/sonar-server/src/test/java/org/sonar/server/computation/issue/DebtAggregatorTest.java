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

public class DebtAggregatorTest {

  static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  static final DumbRule RULE = new DumbRule(RuleTesting.XOO_X1).setId(100);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(RULE);

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(200, CoreMetrics.TECHNICAL_DEBT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(PROJECT, metricRepository);

  DebtAggregator underTest = new DebtAggregator(ruleRepository, metricRepository, measureRepository);

  @Test
  public void sum_debt_of_unresolved_issues() {
    DefaultIssue unresolved1 = newIssue(10);
    DefaultIssue unresolved2 = newIssue(30);
    DefaultIssue unresolvedWithoutDebt = newIssue();
    DefaultIssue resolved = newIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    // total debt
    assertThat(debtMeasure(FILE).get().getLongValue()).isEqualTo(10 + 30);

    // debt by rule
    assertThat(debtRuleMeasure(FILE, RULE.getId()).get().getLongValue()).isEqualTo(10 + 30);
  }

  @Test
  public void aggregate_debt_of_children() {
    DefaultIssue fileIssue = newIssue(10);
    DefaultIssue projectIssue = newIssue(30);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    // total debt of project
    assertThat(debtMeasure(PROJECT).get().getLongValue()).isEqualTo(10 + 30);

    // debt by rule
    assertThat(debtRuleMeasure(PROJECT, RULE.getId()).get().getLongValue()).isEqualTo(10 + 30);
  }

  @Test
  public void sum_debt_of_issues_without_debt() {
    DefaultIssue fileIssue = newIssue();
    DefaultIssue projectIssue = newIssue();

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    // total debt of project
    assertThat(debtMeasure(PROJECT).get().getLongValue()).isZero();

    // debt by rule
    assertThat(debtRuleMeasure(PROJECT, RULE.getId())).isAbsent();
  }

  @CheckForNull
  private Optional<Measure> debtMeasure(Component component) {
    return measureRepository.getAddedRawMeasure(component, TECHNICAL_DEBT_KEY);
  }

  @CheckForNull
  private Optional<Measure> debtRuleMeasure(Component component, int ruleId) {
    return measureRepository.getAddedRawRuleMeasure(component, TECHNICAL_DEBT_KEY, ruleId);
  }

  private static DefaultIssue newIssue(long debt) {
    return newIssue().setDebt(Duration.create(debt));
  }

  private static DefaultIssue newIssue() {
    return new DefaultIssue().setRuleKey(RULE.getKey());
  }
}
