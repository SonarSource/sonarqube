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
package org.sonar.api.server.rule;

import org.junit.Test;
import org.sonar.api.i18n.RuleI18n;
import org.sonar.api.impl.server.RulesDefinitionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RulesDefinitionI18nLoaderTest {

  RuleI18n i18n = mock(RuleI18n.class);
  RulesDefinitionI18nLoader loader = new RulesDefinitionI18nLoader(i18n);

  @Test
  public void complete_rule_name_and_description() {
    when(i18n.getName("squid", "S0001")).thenReturn("SOne");
    when(i18n.getDescription("squid", "S0001")).thenReturn("S One");

    RulesDefinition.Context context = new RulesDefinitionContext();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java");
    // rule without description
    repo.createRule("S0001");

    loader.load(repo);
    repo.done();

    RulesDefinition.Rule rule = context.repository("squid").rule("S0001");
    assertThat(rule.name()).isEqualTo("SOne");
    assertThat(rule.htmlDescription()).isEqualTo("S One");
  }

  @Test
  public void do_not_override_if_no_bundle() {
    // i18n returns null values

    RulesDefinition.Context context = new RulesDefinitionContext();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java");
    repo.createRule("S0001").setName("SOne").setHtmlDescription("S One");

    loader.load(repo);
    repo.done();

    RulesDefinition.Rule rule = context.repository("squid").rule("S0001");
    assertThat(rule.name()).isEqualTo("SOne");
    assertThat(rule.htmlDescription()).isEqualTo("S One");
  }

  @Test
  public void override_existing() {
    when(i18n.getName("squid", "S0001")).thenReturn("SOne");
    when(i18n.getDescription("squid", "S0001")).thenReturn("S One");

    RulesDefinition.Context context = new RulesDefinitionContext();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java");
    repo.createRule("S0001").setName("Bad").setHtmlDescription("Bad");

    loader.load(repo);
    repo.done();

    RulesDefinition.Rule rule = context.repository("squid").rule("S0001");
    assertThat(rule.name()).isEqualTo("SOne");
    assertThat(rule.htmlDescription()).isEqualTo("S One");
  }

  @Test
  public void complete_param_description() {
    when(i18n.getParamDescription("squid", "S0001", "max")).thenReturn("Maximum");

    RulesDefinition.Context context = new RulesDefinitionContext();
    RulesDefinition.NewRepository repo = context.createRepository("squid", "java");
    repo.createRule("S0001").setName("SOne").setHtmlDescription("S One").createParam("max");

    loader.load(repo);
    repo.done();

    RulesDefinition.Rule rule = context.repository("squid").rule("S0001");
    assertThat(rule.param("max").description()).isEqualTo("Maximum");
  }
}
