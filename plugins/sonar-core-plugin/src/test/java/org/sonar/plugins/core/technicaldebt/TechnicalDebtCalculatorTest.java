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
package org.sonar.plugins.core.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.WorkUnit;
import org.sonar.core.technicaldebt.TechnicalDebtConverter;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtCalculatorTest {

  @Mock
  TechnicalDebtModel model;

  @Mock
  TechnicalDebtConverter converter;

  WorkUnit tenMinutes = WorkUnit.create(10d, WorkUnit.MINUTES);
  WorkUnit fiveMinutes = WorkUnit.create(5d, WorkUnit.MINUTES);

  TechnicalDebtCalculator remediationCostCalculator;

  @Before
  public void before() {
    when(converter.toMinutes(tenMinutes)).thenReturn(10l);
    when(converter.toMinutes(fiveMinutes)).thenReturn(5l);

    remediationCostCalculator = new TechnicalDebtCalculator(model, converter);
  }

  @Test
  public void calcul_technical_debt() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.factor()).thenReturn(tenMinutes);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(10l + 5l);
  }

  @Test
  public void calcul_technical_debt_with_effort_to_fix() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.factor()).thenReturn(tenMinutes);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(10l * 2 + 5l);
  }

  @Test
  public void calcul_technical_debt_with_no_offset() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.factor()).thenReturn(tenMinutes);
    Mockito.when(requirement.offset()).thenReturn(null);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(10l * 2 + 0l);
  }

  @Test
  public void calcul_technical_debt_with_no_factor() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey).setEffortToFix(2d);

    DefaultRequirement requirement = mock(DefaultRequirement.class);
    Mockito.when(requirement.factor()).thenReturn(null);
    Mockito.when(requirement.offset()).thenReturn(fiveMinutes);
    when(model.requirementsByRule(ruleKey)).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(0l * 2 + 5l);
  }

  @Test
  public void no_technical_debt_if_requirement_not_found() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(ruleKey);
    when(model.requirementsByRule(ruleKey)).thenReturn(null);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isNull();
    verify(converter, never()).fromMinutes(anyLong());
  }

}

