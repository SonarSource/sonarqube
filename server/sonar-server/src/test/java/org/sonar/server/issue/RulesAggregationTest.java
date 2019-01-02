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
package org.sonar.server.issue;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesAggregationTest {

  @Test
  public void empty() {
    RulesAggregation rulesAggregation = new RulesAggregation();
    assertThat(rulesAggregation.rules()).isEmpty();
  }

  @Test
  public void count_rules() {
    RulesAggregation rulesAggregation = new RulesAggregation();
    RuleKey ruleKey = RuleKey.of("xoo", "S001");
    RuleDefinitionDto ruleDto = RuleTesting.newRule(ruleKey).setName("Rule name");
    rulesAggregation.add(ruleDto);
    rulesAggregation.add(ruleDto);

    RulesAggregation.Rule rule = new RulesAggregation.Rule(ruleKey, "Rule name");

    assertThat(rulesAggregation.rules()).hasSize(1);
    assertThat(rulesAggregation.rules().iterator().next().name()).isEqualTo("Rule name");
    assertThat(rulesAggregation.countRule(rule)).isEqualTo(2);
  }

  @Test
  public void count_rules_with_different_rules() {
    RulesAggregation rulesAggregation = new RulesAggregation();

    RuleDefinitionDto ruleDto = RuleTesting.newRule(RuleKey.of("xoo", "S001")).setName("Rule name 1");
    rulesAggregation.add(ruleDto);
    rulesAggregation.add(ruleDto);
    rulesAggregation.add(RuleTesting.newRule(RuleKey.of("xoo", "S002")).setName("Rule name 2"));

    assertThat(rulesAggregation.rules()).hasSize(2);
  }

  @Test
  public void test_equals_and_hash_code() {
    RulesAggregation.Rule rule = new RulesAggregation.Rule(RuleKey.of("xoo", "S001"), "S001");
    RulesAggregation.Rule ruleSameRuleKey = new RulesAggregation.Rule(RuleKey.of("xoo", "S001"), "S001");
    RulesAggregation.Rule ruleWithDifferentRuleKey = new RulesAggregation.Rule(RuleKey.of("xoo", "S002"), "S002");

    assertThat(rule).isEqualTo(rule);
    assertThat(rule).isEqualTo(ruleSameRuleKey);
    assertThat(rule).isNotEqualTo(ruleWithDifferentRuleKey);

    assertThat(rule.hashCode()).isEqualTo(rule.hashCode());
    assertThat(rule.hashCode()).isEqualTo(ruleSameRuleKey.hashCode());
    assertThat(rule.hashCode()).isNotEqualTo(ruleWithDifferentRuleKey.hashCode());
  }
}
