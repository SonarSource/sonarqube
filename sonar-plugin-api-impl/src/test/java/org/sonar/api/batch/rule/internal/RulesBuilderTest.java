/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.batch.rule.internal;

import org.junit.Test;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RulesBuilderTest {

  @Test
  public void no_rules() {
    RulesBuilder builder = new RulesBuilder();
    Rules rules = builder.build();
    assertThat(rules.findAll()).isEmpty();
  }

  @Test
  public void build_rules() {
    RulesBuilder builder = new RulesBuilder();
    NewRule newJava1 = builder.add(RuleKey.of("java", "S0001"));
    newJava1.setName("Detect bug");
    newJava1.setDescription("Detect potential bug");
    newJava1.setInternalKey("foo=bar");
    newJava1.setSeverity(org.sonar.api.rule.Severity.CRITICAL);
    newJava1.setStatus(RuleStatus.BETA);
    newJava1.addParam("min");
    newJava1.addParam("max").setDescription("Maximum");
    // most simple rule
    builder.add(RuleKey.of("java", "S0002"));
    builder.add(RuleKey.of("findbugs", "NPE"));

    Rules rules = builder.build();

    assertThat(rules.findAll()).hasSize(3);
    assertThat(rules.findByRepository("java")).hasSize(2);
    assertThat(rules.findByRepository("findbugs")).hasSize(1);
    assertThat(rules.findByRepository("unknown")).isEmpty();

    Rule java1 = rules.find(RuleKey.of("java", "S0001"));
    assertThat(java1.key().repository()).isEqualTo("java");
    assertThat(java1.key().rule()).isEqualTo("S0001");
    assertThat(java1.name()).isEqualTo("Detect bug");
    assertThat(java1.description()).isEqualTo("Detect potential bug");
    assertThat(java1.internalKey()).isEqualTo("foo=bar");
    assertThat(java1.status()).isEqualTo(RuleStatus.BETA);
    assertThat(java1.severity()).isEqualTo(org.sonar.api.rule.Severity.CRITICAL);
    assertThat(java1.params()).hasSize(2);
    assertThat(java1.param("min").key()).isEqualTo("min");
    assertThat(java1.param("min").description()).isNull();
    assertThat(java1.param("max").key()).isEqualTo("max");
    assertThat(java1.param("max").description()).isEqualTo("Maximum");

    Rule java2 = rules.find(RuleKey.of("java", "S0002"));
    assertThat(java2.key().repository()).isEqualTo("java");
    assertThat(java2.key().rule()).isEqualTo("S0002");
    assertThat(java2.description()).isNull();
    assertThat(java2.internalKey()).isNull();
    assertThat(java2.status()).isEqualTo(RuleStatus.defaultStatus());
    assertThat(java2.severity()).isEqualTo(Severity.defaultSeverity());
    assertThat(java2.params()).isEmpty();
  }

  @Test
  public void fail_to_add_twice_the_same_rule() {
    RulesBuilder builder = new RulesBuilder();
    builder.add(RuleKey.of("java", "S0001"));

    assertThatThrownBy(() -> builder.add(RuleKey.of("java", "S0001")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Rule 'java:S0001' already exists");
  }

  @Test
  public void fail_to_add_twice_the_same_param() {
    RulesBuilder builder = new RulesBuilder();
    NewRule newRule = builder.add(RuleKey.of("java", "S0001"));
    newRule.addParam("min");
    newRule.addParam("max");

    assertThatThrownBy(() -> newRule.addParam("min"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Parameter 'min' already exists on rule 'java:S0001'");
  }
}
