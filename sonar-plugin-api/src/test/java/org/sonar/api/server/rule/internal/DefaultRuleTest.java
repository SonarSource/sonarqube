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

package org.sonar.api.server.rule.internal;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultRuleTest {
  @Test
  public void getters() {
    DefaultRepository repo = mock(DefaultRepository.class);
    DefaultNewRule rule = new DefaultNewRule("plugin", "repo", "key");

    rule.setScope(RuleScope.MAIN);
    rule.setName("   name  ");
    rule.setHtmlDescription("   html  ");
    rule.setTemplate(true);
    rule.setActivatedByDefault(true);
    RulesDefinition.NewParam param1 = rule.createParam("param1");
    rule.setTags("tag1", "tag2");
    rule.addTags("tag3");
    rule.setEffortToFixDescription("effort");
    rule.setGapDescription("gap");
    rule.setInternalKey("internal");
    rule.addDeprecatedRuleKey("deprecatedrepo", "deprecatedkey");
    rule.setStatus(RuleStatus.READY);
    rule.addCwe(12);
    rule.addCwe(10);
    rule.setType(RuleType.SECURITY_HOTSPOT);
    DebtRemediationFunction f = mock(DebtRemediationFunction.class);
    rule.setDebtRemediationFunction(f);
    rule.setSeverity("MAJOR");

    DefaultRule defaultRule = new DefaultRule(repo, rule);
    assertThat(defaultRule.scope()).isEqualTo(RuleScope.MAIN);
    assertThat(defaultRule.name()).isEqualTo("name");
    assertThat(defaultRule.htmlDescription()).isEqualTo("html");
    assertThat(defaultRule.template()).isTrue();
    assertThat(defaultRule.activatedByDefault()).isTrue();
    assertThat(defaultRule.params()).containsOnly(new DefaultParam(new DefaultNewParam("param1")));
    assertThat(defaultRule.tags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(defaultRule.effortToFixDescription()).isEqualTo("gap");
    assertThat(defaultRule.gapDescription()).isEqualTo("gap");
    assertThat(defaultRule.internalKey()).isEqualTo("internal");
    assertThat(defaultRule.deprecatedRuleKeys()).containsOnly(RuleKey.of("deprecatedrepo", "deprecatedkey"));
    assertThat(defaultRule.status()).isEqualTo(RuleStatus.READY);
    assertThat(rule.securityStandards()).containsOnly("cwe:10", "cwe:12");
    assertThat(defaultRule.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(defaultRule.debtRemediationFunction()).isEqualTo(f);
    assertThat(defaultRule.markdownDescription()).isNull();
    assertThat(defaultRule.severity()).isEqualTo("MAJOR");
  }

  @Test
  public void to_string() {
    DefaultRepository repo = mock(DefaultRepository.class);
    DefaultNewRule rule = new DefaultNewRule("plugin", "repo", "key");
    DefaultRule defaultRule = new DefaultRule(repo, rule);

    assertThat(defaultRule.toString()).isEqualTo("[repository=repo, key=key]");
  }
}
