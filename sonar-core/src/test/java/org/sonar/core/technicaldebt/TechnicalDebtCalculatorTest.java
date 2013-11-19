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
package org.sonar.core.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TechnicalDebtCalculatorTest {

  @Mock
  TechnicalDebtModel technicalDebtModel;

  @Mock
  TechnicalDebtConverter converter;

  WorkUnit tenMinutes = WorkUnit.create(10d, WorkUnit.MINUTES);
  WorkUnit fiveMinutes = WorkUnit.create(5d, WorkUnit.MINUTES);

  TechnicalDebtCalculator remediationCostCalculator;

  @Before
  public void before() {
    when(converter.toMinutes(tenMinutes)).thenReturn(10l);
    when(converter.toMinutes(fiveMinutes)).thenReturn(5l);

    remediationCostCalculator = new TechnicalDebtCalculator(technicalDebtModel, converter);
  }

  @Test
  public void calcul_technical_debt() throws Exception {
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(RuleKey.of("squid", "AvoidCycle"));

    TechnicalDebtRequirement requirement = mock(TechnicalDebtRequirement.class);
    Mockito.when(requirement.getRemediationFactor()).thenReturn(tenMinutes);
    Mockito.when(requirement.getOffset()).thenReturn(fiveMinutes);
    when(technicalDebtModel.getRequirementByRule("squid", "AvoidCycle")).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(10l + 5l);
  }

  @Test
  public void calcul_technical_debt_with_effort_to_fix() throws Exception {
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(RuleKey.of("squid", "AvoidCycle")).setEffortToFix(2d);

    TechnicalDebtRequirement requirement = mock(TechnicalDebtRequirement.class);
    Mockito.when(requirement.getRemediationFactor()).thenReturn(tenMinutes);
    Mockito.when(requirement.getOffset()).thenReturn(fiveMinutes);
    when(technicalDebtModel.getRequirementByRule("squid", "AvoidCycle")).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(10l * 2 + 5l);
  }

  @Test
  public void calcul_technical_debt_with_no_offset() throws Exception {
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(RuleKey.of("squid", "AvoidCycle")).setEffortToFix(2d);

    TechnicalDebtRequirement requirement = mock(TechnicalDebtRequirement.class);
    Mockito.when(requirement.getRemediationFactor()).thenReturn(tenMinutes);
    Mockito.when(requirement.getOffset()).thenReturn(null);
    when(technicalDebtModel.getRequirementByRule("squid", "AvoidCycle")).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(10l * 2 + 0l);
  }

  @Test
  public void calcul_technical_debt_with_no_factor() throws Exception {
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(RuleKey.of("squid", "AvoidCycle")).setEffortToFix(2d);

    TechnicalDebtRequirement requirement = mock(TechnicalDebtRequirement.class);
    Mockito.when(requirement.getRemediationFactor()).thenReturn(null);
    Mockito.when(requirement.getOffset()).thenReturn(fiveMinutes);
    when(technicalDebtModel.getRequirementByRule("squid", "AvoidCycle")).thenReturn(requirement);

    remediationCostCalculator.calculTechnicalDebt(issue);

    verify(converter).fromMinutes(0l * 2 + 5l);
  }

  @Test
  public void no_technical_debt_if_requirement_not_found() throws Exception {
    DefaultIssue issue = new DefaultIssue().setKey("ABCDE").setRuleKey(RuleKey.of("squid", "AvoidCycle"));
    when(technicalDebtModel.getRequirementByRule("squid", "AvoidCycle")).thenReturn(null);

    assertThat(remediationCostCalculator.calculTechnicalDebt(issue)).isNull();
    verify(converter, never()).fromMinutes(anyLong());
  }

}

