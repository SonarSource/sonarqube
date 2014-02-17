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
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.utils.WorkDuration;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultTechnicalDebtManagerTest {

  @Mock
  CharacteristicDao dao;

  @Mock
  RuleFinder ruleFinder;

  DefaultTechnicalDebtManager finder;

  @Before
  public void setUp() throws Exception {
    finder = new DefaultTechnicalDebtManager(dao, ruleFinder);
  }

  @Test
  public void find_root_characteristics() throws Exception {
    CharacteristicDto rootCharacteristicDto = new CharacteristicDto()
      .setId(1)
      .setKey("MEMORY_EFFICIENCY")
      .setName("Memory use");
    when(dao.selectEnabledRootCharacteristics()).thenReturn(newArrayList(rootCharacteristicDto));

    List<Characteristic> result = finder.findRootCharacteristics();
    assertThat(result).hasSize(1);

    Characteristic rootCharacteristic = result.get(0);
    assertThat(rootCharacteristic.key()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(rootCharacteristic.name()).isEqualTo("Memory use");
    assertThat(rootCharacteristic.parentId()).isNull();
    assertThat(rootCharacteristic.rootId()).isNull();
  }

  @Test
  public void find_requirement() throws Exception {
    Rule rule = Rule.create("repo", "key");
    rule.setId(1);

    when(dao.selectByRuleId(rule.getId())).thenReturn(
      new CharacteristicDto().setId(3).setRuleId(10).setParentId(2).setRootId(1).setFunction("linear")
        .setFactorValue(30.0).setFactorUnit("mn")
        .setOffsetValue(0.0).setOffsetUnit("d")
    );

    Characteristic result = finder.findRequirementByRule(rule);

    assertThat(result.id()).isEqualTo(3);
    assertThat(result.parentId()).isEqualTo(2);
    assertThat(result.rootId()).isEqualTo(1);
    assertThat(result.ruleKey()).isEqualTo(RuleKey.of("repo", "key"));
    assertThat(result.function()).isEqualTo("linear");
    assertThat(result.factorValue()).isEqualTo(30);
    assertThat(result.factorUnit()).isEqualTo(WorkDuration.UNIT.MINUTES);
    assertThat(result.offsetValue()).isEqualTo(0);
    assertThat(result.offsetUnit()).isEqualTo(WorkDuration.UNIT.DAYS);
  }

  @Test
  public void not_find_requirement() throws Exception {
    Rule rule = Rule.create("repo", "key");
    rule.setId(1);

    when(dao.selectByRuleId(rule.getId())).thenReturn(null);

    Characteristic result = finder.findRequirementByRule(rule);
    assertThat(result).isNull();
  }

  @Test
  public void find_characteristic() throws Exception {
    Rule rule = Rule.create("repo", "key");
    rule.setId(1);

    when(dao.selectById(2)).thenReturn(
      new CharacteristicDto().setId(2).setKey("COMPILER_RELATED_PORTABILITY").setName("Compiler").setParentId(1).setRootId(1));

    Characteristic result = finder.findCharacteristicById(2);

    assertThat(result.id()).isEqualTo(2);
    assertThat(result.parentId()).isEqualTo(1);
    assertThat(result.rootId()).isEqualTo(1);
    assertThat(result.key()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(result.name()).isEqualTo("Compiler");
  }

  @Test
  public void not_find_characteristic() throws Exception {
    Rule rule = Rule.create("repo", "key");
    rule.setId(1);

    when(dao.selectById(rule.getId())).thenReturn(null);

    Characteristic result = finder.findCharacteristicById(2);
    assertThat(result).isNull();
  }

  @Test
  public void find_requirement_by_rule_id() throws Exception {
    Rule rule = Rule.create("repo", "key");
    rule.setId(1);

    when(ruleFinder.findById(1)).thenReturn(rule);

    when(dao.selectByRuleId(rule.getId())).thenReturn(
      new CharacteristicDto().setId(3).setRuleId(10).setParentId(2).setRootId(1).setFunction("linear")
        .setFactorValue(30.0).setFactorUnit("mn")
        .setOffsetValue(0.0).setOffsetUnit("d")
    );

    Characteristic result = finder.findRequirementByRuleId(1);

    assertThat(result.id()).isEqualTo(3);
    assertThat(result.parentId()).isEqualTo(2);
    assertThat(result.rootId()).isEqualTo(1);
    assertThat(result.ruleKey()).isEqualTo(RuleKey.of("repo", "key"));
    assertThat(result.function()).isEqualTo("linear");
    assertThat(result.factorValue()).isEqualTo(30);
    assertThat(result.factorUnit()).isEqualTo(WorkDuration.UNIT.MINUTES);
    assertThat(result.offsetValue()).isEqualTo(0);
    assertThat(result.offsetUnit()).isEqualTo(WorkDuration.UNIT.DAYS);

  }

  @Test
  public void not_find_requirement_by_rule_id_on_unknown_requirement() throws Exception {
    Rule rule = Rule.create("repo", "key");
    rule.setId(1);

    when(ruleFinder.findById(1)).thenReturn(rule);

    when(dao.selectByRuleId(rule.getId())).thenReturn(null);

    assertThat(finder.findRequirementByRuleId(1)).isNull();
  }

  @Test
  public void fail_to_find_requirement_by_rule_id_if_unknown_rule_id() throws Exception {
    when(dao.selectByRuleId(1)).thenReturn(
      new CharacteristicDto().setId(3).setRuleId(10).setParentId(2).setRootId(1).setFunction("linear").setFactorValue(30.0).setFactorUnit("mn"));
    when(ruleFinder.findById(1)).thenReturn(null);
    try {
      finder.findRequirementByRuleId(1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
