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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultNewRuleTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  private DefaultNewRule rule = new DefaultNewRule("plugin", "repo", "key");

  @Test
  public void testSimpleSetGet() {
    assertThat(rule.pluginKey()).isEqualTo("plugin");
    assertThat(rule.repoKey()).isEqualTo("repo");
    assertThat(rule.key()).isEqualTo("key");

    rule.setScope(RuleScope.MAIN);
    assertThat(rule.scope()).isEqualTo(RuleScope.MAIN);

    rule.setName("   name  ");
    assertThat(rule.name()).isEqualTo("name");

    rule.setHtmlDescription("   html  ");
    assertThat(rule.htmlDescription()).isEqualTo("html");

    rule.setTemplate(true);
    assertThat(rule.template()).isTrue();

    rule.setActivatedByDefault(true);
    assertThat(rule.activatedByDefault()).isTrue();

    RulesDefinition.NewParam param1 = rule.createParam("param1");
    assertThat(rule.param("param1")).isEqualTo(param1);
    assertThat(rule.params()).containsOnly(param1);

    rule.setTags("tag1", "tag2");
    rule.addTags("tag3");
    assertThat(rule.tags()).containsExactly("tag1", "tag2", "tag3");

    rule.setEffortToFixDescription("effort");
    assertThat(rule.gapDescription()).isEqualTo("effort");

    rule.setGapDescription("gap");
    assertThat(rule.gapDescription()).isEqualTo("gap");

    rule.setInternalKey("internal");
    assertThat(rule.internalKey()).isEqualTo("internal");

    rule.addDeprecatedRuleKey("deprecatedrepo", "deprecatedkey");
    assertThat(rule.deprecatedRuleKeys()).containsOnly(RuleKey.of("deprecatedrepo", "deprecatedkey"));

    rule.setStatus(RuleStatus.READY);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);

    rule.addCwe(12);
    rule.addCwe(10);
    assertThat(rule.securityStandards()).containsOnly("cwe:10", "cwe:12");

    rule.setType(RuleType.SECURITY_HOTSPOT);
    assertThat(rule.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);

    DebtRemediationFunction f = mock(DebtRemediationFunction.class);
    rule.setDebtRemediationFunction(f);
    assertThat(rule.debtRemediationFunction()).isEqualTo(f);

    rule.setSeverity("MAJOR");
    assertThat(rule.severity()).isEqualTo("MAJOR");
  }

  @Test
  public void validate_fails() {
    rule.setHtmlDescription("html");
    exception.expect(IllegalStateException.class);
    rule.validate();
  }

  @Test
  public void validate_succeeds() {
    rule.setHtmlDescription("html");
    rule.setName("name");
    rule.validate();
  }

  @Test
  public void set_markdown_description() {
    rule.setMarkdownDescription("markdown");
    assertThat(rule.markdownDescription()).isEqualTo("markdown");
  }
  @Test
  public void fail_if_severity_is_invalid() {
    exception.expect(IllegalArgumentException.class);
    rule.setSeverity("invalid");
  }

  @Test
  public void fail_setting_markdown_if_html_is_set() {
    exception.expect(IllegalStateException.class);
    rule.setHtmlDescription("html");
    rule.setMarkdownDescription("markdown");
  }

  @Test
  public void fail_if_set_status_to_removed() {
    exception.expect(IllegalArgumentException.class);
    rule.setStatus(RuleStatus.REMOVED);
  }
}
