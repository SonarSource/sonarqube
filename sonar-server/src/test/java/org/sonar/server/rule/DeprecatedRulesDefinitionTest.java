/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RemediationFunction;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.technicaldebt.DebtRulesXMLImporter;
import org.sonar.core.technicaldebt.TechnicalDebtModelRepository;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeprecatedRulesDefinitionTest {

  @Mock
  RuleI18nManager i18n;

  @Mock
  TechnicalDebtModelRepository debtModelRepository;

  @Mock
  DebtRulesXMLImporter importer;

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
      rule.setTags(new String[]{"style", "security"});
      rule.createParameter("format").setDescription("Regular expression").setDefaultValue("A-Z").setType("REGULAR_EXPRESSION");
      return Arrays.asList(rule);
    }
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
  public void wrap_deprecated_rule_repositories() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new DeprecatedRulesDefinition(i18n, new RuleRepository[]{new CheckstyleRules()}, debtModelRepository, importer).define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository checkstyle = context.repository("checkstyle");
    assertThat(checkstyle).isNotNull();
    assertThat(checkstyle.key()).isEqualTo("checkstyle");
    assertThat(checkstyle.name()).isEqualTo("Checkstyle");
    assertThat(checkstyle.language()).isEqualTo("java");
    assertThat(checkstyle.rules()).hasSize(1);
    RulesDefinition.Rule rule = checkstyle.rule("ConstantName");
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo("ConstantName");
    assertThat(rule.name()).isEqualTo("Constant Name");
    assertThat(rule.htmlDescription()).isEqualTo("Checks that constant names conform to the specified format");
    assertThat(rule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.internalKey()).isEqualTo("Checker/TreeWalker/ConstantName");
    assertThat(rule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(rule.tags()).containsOnly("style", "security");
    assertThat(rule.params()).hasSize(1);
    RulesDefinition.Param param = rule.param("format");
    assertThat(param).isNotNull();
    assertThat(param.key()).isEqualTo("format");
    assertThat(param.name()).isEqualTo("format");
    assertThat(param.description()).isEqualTo("Regular expression");
    assertThat(param.defaultValue()).isEqualTo("A-Z");
  }

  @Test
  public void emulate_the_day_deprecated_api_can_be_dropped() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();

    // no more RuleRepository !
    new DeprecatedRulesDefinition(i18n, debtModelRepository, importer);

    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void use_l10n_bundles() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();
    when(i18n.getName("checkstyle", "ConstantName")).thenReturn("Constant Name");
    when(i18n.getDescription("checkstyle", "ConstantName")).thenReturn("Checks that constant names conform to the specified format");
    when(i18n.getParamDescription("checkstyle", "ConstantName", "format")).thenReturn("Regular expression");

    new DeprecatedRulesDefinition(i18n, new RuleRepository[]{new UseBundles()}, debtModelRepository, importer).define(context);

    RulesDefinition.Repository checkstyle = context.repository("checkstyle");
    RulesDefinition.Rule rule = checkstyle.rule("ConstantName");
    assertThat(rule.key()).isEqualTo("ConstantName");
    assertThat(rule.name()).isEqualTo("Constant Name");
    assertThat(rule.htmlDescription()).isEqualTo("Checks that constant names conform to the specified format");
    RulesDefinition.Param param = rule.param("format");
    assertThat(param.key()).isEqualTo("format");
    assertThat(param.name()).isEqualTo("format");
    assertThat(param.description()).isEqualTo("Regular expression");
  }

  @Test
  public void define_rule_debt() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();

    List<DebtRulesXMLImporter.RuleDebt> ruleDebts = newArrayList(
      new DebtRulesXMLImporter.RuleDebt()
        .setCharacteristicKey("MEMORY_EFFICIENCY")
        .setRuleKey(RuleKey.of("checkstyle", "ConstantName"))
        .setFunction(RemediationFunction.LINEAR_OFFSET)
        .setFactor("1d")
        .setOffset("10min")
    );

    Reader javaModelReader = mock(Reader.class);
    when(debtModelRepository.createReaderForXMLFile("java")).thenReturn(javaModelReader);
    when(debtModelRepository.getContributingPluginList()).thenReturn(newArrayList("java"));
    when(importer.importXML(eq(javaModelReader))).thenReturn(ruleDebts);

    new DeprecatedRulesDefinition(i18n, new RuleRepository[]{new CheckstyleRules()}, debtModelRepository, importer).define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository checkstyle = context.repository("checkstyle");
    assertThat(checkstyle.rules()).hasSize(1);

    RulesDefinition.Rule rule = checkstyle.rule("ConstantName");
    assertThat(rule).isNotNull();
    assertThat(rule.key()).isEqualTo("ConstantName");
    assertThat(rule.characteristicKey()).isEqualTo("MEMORY_EFFICIENCY");
    assertThat(rule.remediationFunction()).isEqualTo(RemediationFunction.LINEAR_OFFSET);
    assertThat(rule.remediationFactor()).isEqualTo("1d");
    assertThat(rule.remediationOffset()).isEqualTo("10min");
  }

}
