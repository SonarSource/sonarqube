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
package org.sonar.server.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.core.technicaldebt.DefaultRequirement;
import org.sonar.core.technicaldebt.db.RequirementDao;
import org.sonar.core.technicaldebt.db.RequirementDto;
import org.sonar.server.exceptions.BadRequestException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RubyTechnicalDebtServiceTest {


  private RubyTechnicalDebtService service;

  @Mock
  private RequirementDao requirementDao;

  @Mock
  private RuleFinder rulefinder;

  @Before
  public void before() {
    service = new RubyTechnicalDebtService(requirementDao, rulefinder);
  }

  @Test
  public void return_requirement() {
    RuleKey ruleKey = RuleKey.of("squid", "AvoidCycle");
    Rule rule = Rule.create("squid", "AvoidCycle");
    rule.setId(1);
    when(rulefinder.findByKey(ruleKey)).thenReturn(rule);

    DefaultRequirement defaultRequirement = mock(DefaultRequirement.class);
    RequirementDto requirement = mock(RequirementDto.class);
    when(requirement.toDefaultRequirement()).thenReturn(defaultRequirement);
    when(requirementDao.selectByRuleId(1)).thenReturn(requirement);

    DefaultRequirement result = service.requirement(ruleKey);
    assertThat(result).isEqualTo(defaultRequirement);
  }

  @Test
  public void return_null_requirement_if_rule_does_not_exists() {
    when(rulefinder.findByKey(any(RuleKey.class))).thenReturn(null);

    RequirementDto requirement = new RequirementDto();
    when(requirementDao.selectByRuleId(1)).thenReturn(requirement);

    try {
      service.requirement(RuleKey.of("squid", "AvoidCycle"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Unknown rule: squid:AvoidCycle");
    }
  }

}
