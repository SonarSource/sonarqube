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
package org.sonar.api.batch.rule.internal;

import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ActiveRulesBuilderTest {
  @Test
  public void no_rules() throws Exception {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    ActiveRules rules = builder.build();
    assertThat(rules.findAll()).isEmpty();
  }

  @Test
  public void build_rules() throws Exception {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    NewActiveRule newSquid1 = builder.activate(RuleKey.of("squid", "S0001"));
    newSquid1.setSeverity(Severity.CRITICAL);
    newSquid1.setInternalKey("__S0001__");
    newSquid1.setParam("min", "20");
    // most simple rule
    builder.activate(RuleKey.of("squid", "S0002"));
    builder.activate(RuleKey.of("findbugs", "NPE")).setInternalKey(null).setSeverity(null).setParam("foo", null);

    ActiveRules activeRules = builder.build();

    assertThat(activeRules.findAll()).hasSize(3);
    assertThat(activeRules.findByRepository("squid")).hasSize(2);
    assertThat(activeRules.findByRepository("findbugs")).hasSize(1);
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
  public void fail_to_add_twice_the_same_rule() throws Exception {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    builder.activate(RuleKey.of("squid", "S0001"));
    try {
      builder.activate(RuleKey.of("squid", "S0001"));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Rule 'squid:S0001' is already activated");
    }
  }
}
