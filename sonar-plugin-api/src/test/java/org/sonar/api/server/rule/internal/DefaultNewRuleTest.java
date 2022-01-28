/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.server.rule.RulesDefinition.OwaspTop10;
import org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DefaultNewRuleTest {

  private final DefaultNewRule rule = new DefaultNewRule("plugin", "repo", "key");

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

    rule.setGapDescription("effort");
    assertThat(rule.gapDescription()).isEqualTo("effort");

    rule.setInternalKey("internal");
    assertThat(rule.internalKey()).isEqualTo("internal");

    rule.addDeprecatedRuleKey("deprecatedrepo", "deprecatedkey");
    assertThat(rule.deprecatedRuleKeys()).containsOnly(RuleKey.of("deprecatedrepo", "deprecatedkey"));

    rule.setStatus(RuleStatus.READY);
    assertThat(rule.status()).isEqualTo(RuleStatus.READY);

    rule.addCwe(12);
    rule.addCwe(10);
    assertThat(rule.securityStandards()).containsOnly("cwe:10", "cwe:12");

    rule.addOwaspTop10(OwaspTop10.A1, OwaspTop10.A2);
    rule.addOwaspTop10(OwaspTop10Version.Y2017, OwaspTop10.A4);
    rule.addOwaspTop10(OwaspTop10Version.Y2021, OwaspTop10.A5, OwaspTop10.A3);
    assertThat(rule.securityStandards())
      .contains("owaspTop10:a1", "owaspTop10:a2", "owaspTop10:a4", "owaspTop10-2021:a3", "owaspTop10-2021:a5");

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

    assertThatThrownBy(() -> rule.validate())
      .isInstanceOf(IllegalStateException.class);
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
    assertThatThrownBy(() -> rule.setSeverity("invalid"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_setting_markdown_if_html_is_set() {
    rule.setHtmlDescription("html");

    assertThatThrownBy(() -> rule.setMarkdownDescription("markdown"))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void fail_if_set_status_to_removed() {
    assertThatThrownBy(() -> rule.setStatus(RuleStatus.REMOVED))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_if_null_owasp_version() {
    assertThatThrownBy(() -> rule.addOwaspTop10((OwaspTop10Version) null , OwaspTop10.A1))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Owasp version must not be null");
  }
}
