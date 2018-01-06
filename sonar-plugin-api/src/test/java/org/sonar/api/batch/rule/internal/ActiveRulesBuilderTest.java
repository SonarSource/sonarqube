/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRulesBuilderTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void no_rules() {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    ActiveRules rules = builder.build();
    assertThat(rules.findAll()).isEmpty();
  }

  @Test
  public void build_rules() {
    ActiveRules activeRules = new ActiveRulesBuilder()
      .create(RuleKey.of("squid", "S0001"))
      .setName("My Rule")
      .setSeverity(Severity.CRITICAL)
      .setInternalKey("__S0001__")
      .setParam("min", "20")
      .activate()
      // most simple rule
      .create(RuleKey.of("squid", "S0002")).activate()
      .create(RuleKey.of("findbugs", "NPE")).setInternalKey(null).setSeverity(null).setParam("foo", null).activate()
      .build();

    assertThat(activeRules.findAll()).hasSize(3);
    assertThat(activeRules.findByRepository("squid")).hasSize(2);
    assertThat(activeRules.findByRepository("findbugs")).hasSize(1);
    assertThat(activeRules.findByInternalKey("squid", "__S0001__")).isNotNull();
    assertThat(activeRules.findByRepository("unknown")).isEmpty();

    ActiveRule squid1 = activeRules.find(RuleKey.of("squid", "S0001"));
    assertThat(squid1.ruleKey().repository()).isEqualTo("squid");
    assertThat(squid1.ruleKey().rule()).isEqualTo("S0001");
    assertThat(squid1.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(squid1.internalKey()).isEqualTo("__S0001__");
    assertThat(squid1.params()).hasSize(1);
    assertThat(squid1.param("min")).isEqualTo("20");

    ActiveRule squid2 = activeRules.find(RuleKey.of("squid", "S0002"));
    assertThat(squid2.ruleKey().repository()).isEqualTo("squid");
    assertThat(squid2.ruleKey().rule()).isEqualTo("S0002");
    assertThat(squid2.severity()).isEqualTo(Severity.defaultSeverity());
    assertThat(squid2.params()).isEmpty();

    ActiveRule findbugsRule = activeRules.find(RuleKey.of("findbugs", "NPE"));
    assertThat(findbugsRule.severity()).isEqualTo(Severity.defaultSeverity());
    assertThat(findbugsRule.internalKey()).isNull();
    assertThat(findbugsRule.params()).isEmpty();
  }

  @Test
  public void fail_to_add_twice_the_same_rule() {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    builder.create(RuleKey.of("squid", "S0001")).activate();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Rule 'squid:S0001' is already activated");

    builder.create(RuleKey.of("squid", "S0001")).activate();
  }
}
