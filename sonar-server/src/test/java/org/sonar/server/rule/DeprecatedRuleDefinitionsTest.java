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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RuleDefinitions;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.core.i18n.RuleI18nManager;

import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeprecatedRuleDefinitionsTest {

  static class CheckstyleRules extends RuleRepository {
    public CheckstyleRules() {
      super("checkstyle", "java");
      setName("Checkstyle");
    }

    @Override
    public List<Rule> createRules() {
      Rule rule = Rule.create("checkstyle", "ConstantName", "Constant Name");
      rule.setDescription("Checks that constant names conform to the specified format");
      rule.setConfigKey("Checker/TreeWalker/ConstantName");
      rule.setSeverity(RulePriority.BLOCKER);
      rule.setStatus(Rule.STATUS_BETA);
      rule.createParameter("format").setDescription("Regular expression").setDefaultValue("A-Z").setType("REGULAR_EXPRESSION");
      return Arrays.asList(rule);
    }
  }

  @Test
  public void wrap_deprecated_rule_repositories() throws Exception {
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    RuleI18nManager i18n = mock(RuleI18nManager.class);

    new DeprecatedRuleDefinitions(i18n, new RuleRepository[]{new CheckstyleRules()}).define(context);

    assertThat(context.repositories()).hasSize(1);
    RuleDefinitions.Repository checkstyle = context.repository("checkstyle");
    assertThat(checkstyle).isNotNull();
    assertThat(checkstyle.key()).isEqualTo("checkstyle");
    assertThat(checkstyle.name()).isEqualTo("Checkstyle");
    assertThat(checkstyle.language()).isEqualTo("java");
    assertThat(checkstyle.rules()).hasSize(1);
    RuleDefinitions.Rule rule = checkstyle.rule("ConstantName");
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo("ConstantName");
    assertThat(rule.name()).isEqualTo("Constant Name");
    assertThat(rule.htmlDescription()).isEqualTo("Checks that constant names conform to the specified format");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.metadata()).isEqualTo("Checker/TreeWalker/ConstantName");
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.tags()).isEmpty();
    assertThat(rule.params()).hasSize(1);
    RuleDefinitions.Param param = rule.param("format");
    assertThat(param).isNotNull();
    assertThat(param.key()).isEqualTo("format");
    assertThat(param.name()).isEqualTo("format");
    assertThat(param.description()).isEqualTo("Regular expression");
    assertThat(param.defaultValue()).isEqualTo("A-Z");
  }

  @Test
  public void emulate_the_day_deprecated_api_can_be_dropped() throws Exception {
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    RuleI18nManager i18n = mock(RuleI18nManager.class);

    // no more RuleRepository !
    new DeprecatedRuleDefinitions(i18n);

    assertThat(context.repositories()).isEmpty();
  }


  static class UseBundles extends RuleRepository {
    public UseBundles() {
      super("checkstyle", "java");
      setName("Checkstyle");
    }

    @Override
    public List<Rule> createRules() {
      Rule rule = Rule.create("checkstyle", "ConstantName");
      rule.createParameter("format");
      return Arrays.asList(rule);
    }
  }

  @Test
  public void use_l10n_bundles() throws Exception {
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    RuleI18nManager i18n = mock(RuleI18nManager.class);
    when(i18n.getName("checkstyle", "ConstantName")).thenReturn("Constant Name");
    when(i18n.getDescription("checkstyle", "ConstantName")).thenReturn("Checks that constant names conform to the specified format");
    when(i18n.getParamDescription("checkstyle", "ConstantName", "format")).thenReturn("Regular expression");

    new DeprecatedRuleDefinitions(i18n, new RuleRepository[]{new UseBundles()}).define(context);

    RuleDefinitions.Repository checkstyle = context.repository("checkstyle");
    RuleDefinitions.Rule rule = checkstyle.rule("ConstantName");
    assertThat(rule.key()).isEqualTo("ConstantName");
    assertThat(rule.name()).isEqualTo("Constant Name");
    assertThat(rule.htmlDescription()).isEqualTo("Checks that constant names conform to the specified format");
    RuleDefinitions.Param param = rule.param("format");
    assertThat(param.key()).isEqualTo("format");
    assertThat(param.name()).isEqualTo("format");
    assertThat(param.description()).isEqualTo("Regular expression");
  }
}
