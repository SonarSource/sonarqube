/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.rule;

import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ActiveRulesBuilderTest {

  @Test
  public void no_rules() {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    ActiveRules rules = builder.build();
    assertThat(rules.findAll()).isEmpty();
  }

  @Test
  public void build_rules() {
    NewActiveRule activeRule = new NewActiveRule.Builder()
      .setRuleKey(RuleKey.of("java", "S0001"))
      .setName("My Rule")
      .setSeverity(Severity.CRITICAL)
      .setInternalKey("__S0001__")
      .setParam("min", "20")
      .build();

    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(activeRule)
      // most simple rule
      .addRule(new NewActiveRule.Builder().setRuleKey(RuleKey.of("java", "S0002")).build())
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of("findbugs", "NPE"))
        .setInternalKey(null)
        .setSeverity(null)
        .setParam("foo", null)
        .build())
      .build();

    assertThat(activeRules.findAll()).hasSize(3);
    assertThat(activeRules.findByRepository("java")).hasSize(2);
    assertThat(activeRules.findByRepository("findbugs")).hasSize(1);
    assertThat(activeRules.findByInternalKey("java", "__S0001__")).isNotNull();
    assertThat(activeRules.findByRepository("unknown")).isEmpty();

    ActiveRule java1 = activeRules.find(RuleKey.of("java", "S0001"));
    assertThat(java1.ruleKey().repository()).isEqualTo("java");
    assertThat(java1.ruleKey().rule()).isEqualTo("S0001");
    assertThat(java1.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(java1.internalKey()).isEqualTo("__S0001__");
    assertThat(java1.params()).hasSize(1);
    assertThat(java1.param("min")).isEqualTo("20");

    ActiveRule java2 = activeRules.find(RuleKey.of("java", "S0002"));
    assertThat(java2.ruleKey().repository()).isEqualTo("java");
    assertThat(java2.ruleKey().rule()).isEqualTo("S0002");
    assertThat(java2.severity()).isEqualTo(Severity.defaultSeverity());
    assertThat(java2.params()).isEmpty();

    ActiveRule findbugsRule = activeRules.find(RuleKey.of("findbugs", "NPE"));
    assertThat(findbugsRule.severity()).isEqualTo(Severity.defaultSeverity());
    assertThat(findbugsRule.internalKey()).isNull();
    assertThat(findbugsRule.params()).isEmpty();
  }

  @Test
  public void fail_to_add_twice_the_same_rule() {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    NewActiveRule rule = new NewActiveRule.Builder()
      .setRuleKey(RuleKey.of("java", "S0001"))
      .build();
    builder.addRule(rule);

    assertThatThrownBy(() -> builder.addRule(rule))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Rule 'java:S0001' is already activated");
  }
}
