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
package org.sonar.batch.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.WorkUnit;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtCalculatorTest {

  @Mock
  TechnicalDebtModel model;

  WorkUnit tenMinutes = new WorkUnit.Builder().setMinutes(10).build();
  WorkUnit fiveMinutes = new WorkUnit.Builder().setMinutes(5).build();

  TechnicalDebtCalculator remediationCostCalculator;

  @Before
  public void before() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, 8);

    remediationCostCalculator = new TechnicalDebtCalculator(model, settings);
  }

  @Test
  public void calcul_technical_debt() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.function()).thenReturn("constant_issue");
    Mockito.when(requirement.factor()).thenReturn(tenMinutes);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isEqualTo(new WorkUnit.Builder().setMinutes(15).build());
  }

  @Test
  public void calcul_technical_debt_with_effort_to_fix() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.function()).thenReturn("linear_offset");
    Mockito.when(requirement.factor()).thenReturn(tenMinutes);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isEqualTo(new WorkUnit.Builder().setMinutes((10 * 2) + 5).build());
  }

  @Test
  public void calcul_technical_debt_with_no_offset() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.function()).thenReturn("linear");
    Mockito.when(requirement.factor()).thenReturn(tenMinutes);
    Mockito.when(requirement.offset()).thenReturn(null);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isEqualTo(new WorkUnit.Builder().setMinutes(10 * 2).build());
  }

  @Test
  public void calcul_technical_debt_with_no_factor() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.function()).thenReturn("constant_issue");
    Mockito.when(requirement.factor()).thenReturn(null);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isEqualTo(new WorkUnit.Builder().setMinutes(5).build());
  }

  @Test
  public void no_technical_debt_if_requirement_not_found() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);
    when(model.requirementsByRule(ruleKey)).thenReturn(null);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isNull();
  }

  @Test
  public void fail_to_calcul_technical_debt_on_constant_issue_function_with_effort_to_fix() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.function()).thenReturn("constant_issue");
    Mockito.when(requirement.factor()).thenReturn(null);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    try {
      assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isEqualTo(new WorkUnit.Builder().setMinutes(15).build());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requirement for 'squid:AvoidCycle' can not use 'Constant/issue' remediation function because this rule does not have a fixed remediation cost.");
    }
  }

}

