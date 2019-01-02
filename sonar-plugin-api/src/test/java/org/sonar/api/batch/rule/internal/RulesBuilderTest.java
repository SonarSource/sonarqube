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
package org.sonar.api.batch.rule.internal;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;

public class RulesBuilderTest {
  @org.junit.Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void no_rules() {
    RulesBuilder builder = new RulesBuilder();
    Rules rules = builder.build();
    assertThat(rules.findAll()).isEmpty();
  }

  @Test
  public void build_rules() {
    RulesBuilder builder = new RulesBuilder();
    NewRule newSquid1 = builder.add(RuleKey.of("squid", "S0001"));
    newSquid1.setName("Detect bug");
    newSquid1.setDescription("Detect potential bug");
    newSquid1.setInternalKey("foo=bar");
    newSquid1.setSeverity(Severity.CRITICAL);
    newSquid1.setStatus(RuleStatus.BETA);
    newSquid1.addParam("min");
    newSquid1.addParam("max").setDescription("Maximum");
    // most simple rule
    builder.add(RuleKey.of("squid", "S0002"));
    builder.add(RuleKey.of("findbugs", "NPE"));

    Rules rules = builder.build();

    assertThat(rules.findAll()).hasSize(3);
    assertThat(rules.findByRepository("squid")).hasSize(2);
    assertThat(rules.findByRepository("findbugs")).hasSize(1);
    assertThat(rules.findByRepository("unknown")).isEmpty();

    Rule squid1 = rules.find(RuleKey.of("squid", "S0001"));
    assertThat(squid1.key().repository()).isEqualTo("squid");
    assertThat(squid1.key().rule()).isEqualTo("S0001");
    assertThat(squid1.name()).isEqualTo("Detect bug");
    assertThat(squid1.description()).isEqualTo("Detect potential bug");
    assertThat(squid1.internalKey()).isEqualTo("foo=bar");
    assertThat(squid1.status()).isEqualTo(RuleStatus.BETA);
    assertThat(squid1.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(squid1.params()).hasSize(2);
    assertThat(squid1.param("min").key()).isEqualTo("min");
    assertThat(squid1.param("min").description()).isNull();
    assertThat(squid1.param("max").key()).isEqualTo("max");
    assertThat(squid1.param("max").description()).isEqualTo("Maximum");

    Rule squid2 = rules.find(RuleKey.of("squid", "S0002"));
    assertThat(squid2.key().repository()).isEqualTo("squid");
    assertThat(squid2.key().rule()).isEqualTo("S0002");
    assertThat(squid2.description()).isNull();
    assertThat(squid2.internalKey()).isNull();
    assertThat(squid2.status()).isEqualTo(RuleStatus.defaultStatus());
    assertThat(squid2.severity()).isEqualTo(Severity.defaultSeverity());
    assertThat(squid2.params()).isEmpty();
  }

  @Test
  public void fail_to_add_twice_the_same_rule() {
    RulesBuilder builder = new RulesBuilder();
    builder.add(RuleKey.of("squid", "S0001"));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Rule 'squid:S0001' already exists");

    builder.add(RuleKey.of("squid", "S0001"));
  }

  @Test
  public void fail_to_add_twice_the_same_param() {
    RulesBuilder builder = new RulesBuilder();
    NewRule newRule = builder.add(RuleKey.of("squid", "S0001"));
    newRule.addParam("min");
    newRule.addParam("max");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Parameter 'min' already exists on rule 'squid:S0001'");

    newRule.addParam("min");
  }
}
