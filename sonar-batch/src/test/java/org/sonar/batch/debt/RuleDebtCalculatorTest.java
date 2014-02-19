/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.debt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.WorkDuration;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleDebtCalculatorTest {

  private static final int HOURS_IN_DAY = 8;

  @Mock
  TechnicalDebtModel model;

  RuleDebtCalculator calculator;

  @Before
  public void before() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, HOURS_IN_DAY);
    calculator = new RuleDebtCalculator(model, settings);
  }

  @Test
  public void calculate_technical_debt() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);

    DefaultRequirement requirement = new DefaultRequirement()
      .setFunction("constant_issue")
      .setFactorValue(10)
      .setFactorUnit(WorkDuration.UNIT.MINUTES)
      .setOffsetValue(5)
      .setOffsetUnit(WorkDuration.UNIT.MINUTES);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(calculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).isEqualTo(15 * 60);
  }

  @Test
  public void calculate_technical_debt_with_effort_to_fix() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = new DefaultRequirement()
      .setFunction("linear_offset")
      .setFactorValue(10)
      .setFactorUnit(WorkDuration.UNIT.MINUTES)
      .setOffsetValue(5)
      .setOffsetUnit(WorkDuration.UNIT.MINUTES);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(calculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).isEqualTo(((10 * 2) + 5) * 60);
  }

  @Test
  public void calculate_technical_debt_with_no_offset() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = new DefaultRequirement()
      .setFunction("linear")
      .setFactorValue(10)
      .setFactorUnit(WorkDuration.UNIT.HOURS);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(calculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).isEqualTo((10 * 2) * 60 * 60);
  }

  @Test
  public void calculate_technical_debt_with_no_factor() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);

    DefaultRequirement requirement = new DefaultRequirement()
      .setFunction("constant_issue")
      .setOffsetValue(5)
      .setOffsetUnit(WorkDuration.UNIT.DAYS);

    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(calculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).isEqualTo(5 * HOURS_IN_DAY * 60 * 60);
  }

  @Test
  public void no_technical_debt_if_requirement_not_found() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);
    when(model.requirementsByRule(ruleKey)).thenReturn(null);

    assertThat(calculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).isNull();
  }

  @Test
  public void fail_to_calculate_technical_debt_on_constant_issue_function_with_effort_to_fix() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = new DefaultRequirement()
      .setFunction("constant_issue")
      .setOffsetValue(5)
      .setOffsetUnit(WorkDuration.UNIT.MINUTES);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    try {
      assertThat(calculator.calculateTechnicalDebt(issue.ruleKey(), issue.effortToFix())).isEqualTo(15 * 60);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requirement for 'squid:AvoidCycle' can not use 'Constant/issue' remediation function because this rule does not have a fixed remediation cost.");
    }
  }

}

