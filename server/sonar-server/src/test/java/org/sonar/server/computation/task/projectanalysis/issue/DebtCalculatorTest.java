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

import org.junit.Test;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.Durations;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class DebtCalculatorTest {

  DumbRule rule = new DumbRule(RuleTesting.XOO_X1);
  DefaultIssue issue = new DefaultIssue().setRuleKey(rule.getKey());

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(rule);

  DebtCalculator underTest = new DebtCalculator(ruleRepository, new Durations());

  @Test
  public void no_debt_if_function_is_not_defined() {
    DefaultIssue issue = new DefaultIssue().setRuleKey(rule.getKey());

    assertThat(underTest.calculate(issue)).isNull();
  }

  @Test
  public void no_debt_if_no_sqale_characteristic() {
    rule.setFunction(null);

    DefaultIssue issue = new DefaultIssue().setRuleKey(rule.getKey());

    assertThat(underTest.calculate(issue)).isNull();
  }

  @Test
  public void default_effort_to_fix_is_one_for_linear_function() {
    int coefficient = 2;
    rule.setFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, coefficient + "min", null));

    assertThat(underTest.calculate(issue).toMinutes()).isEqualTo(coefficient * 1);
  }

  @Test
  public void linear_function() {
    double effortToFix = 3.0;
    int coefficient = 2;
    issue.setGap(effortToFix);
    rule.setFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, coefficient + "min", null));

    assertThat(underTest.calculate(issue).toMinutes()).isEqualTo((int) (coefficient * effortToFix));
  }

  @Test
  public void constant_function() {
    int constant = 2;
    issue.setGap(null);
    rule.setFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, constant + "min"));

    assertThat(underTest.calculate(issue).toMinutes()).isEqualTo(2);
  }

  @Test(expected = IllegalArgumentException.class)
  public void effort_to_fix_must_not_be_set_with_constant_function() {
    int constant = 2;
    issue.setGap(3.0);
    rule.setFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, constant + "min"));

    underTest.calculate(issue);
  }

  @Test
  public void linear_with_offset_function() {
    double effortToFix = 3.0;
    int coefficient = 2;
    int offset = 5;
    issue.setGap(effortToFix);
    rule.setFunction(new DefaultDebtRemediationFunction(
      DebtRemediationFunction.Type.LINEAR_OFFSET, coefficient + "min", offset + "min"));

    assertThat(underTest.calculate(issue).toMinutes()).isEqualTo((int) ((coefficient * effortToFix) + offset));
  }
}
