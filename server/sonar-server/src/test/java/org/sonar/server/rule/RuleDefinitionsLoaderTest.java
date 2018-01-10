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
package org.sonar.server.rule;

import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.server.plugins.ServerPluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RuleDefinitionsLoaderTest {

  @Test
  public void no_definitions() {
    CommonRuleDefinitions commonRulesDefinitions = mock(CommonRuleDefinitions.class);
    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), commonRulesDefinitions, mock(ServerPluginRepository.class)).load();

    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void load_definitions() {
    CommonRuleDefinitions commonRulesDefinitions = mock(CommonRuleDefinitions.class);
    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), commonRulesDefinitions, mock(ServerPluginRepository.class),
      new RulesDefinition[] {
        new FindbugsDefinitions(), new SquidDefinitions()
      }).load();

    assertThat(context.repositories()).hasSize(2);
    assertThat(context.repository("findbugs")).isNotNull();
    assertThat(context.repository("squid")).isNotNull();
  }

  @Test
  public void define_common_rules() {
    CommonRuleDefinitions commonRulesDefinitions = new FakeCommonRuleDefinitions();
    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), commonRulesDefinitions, mock(ServerPluginRepository.class),
      new RulesDefinition[] {
        new SquidDefinitions()
      }).load();

    assertThat(context.repositories()).extracting("key").containsOnly("squid", "common-java");
    assertThat(context.repository("common-java").rules()).extracting("key").containsOnly("InsufficientBranchCoverage");
  }

  /**
   * "common-rules" are merged into core 5.2. Previously they were embedded by some plugins. Only the core definition
   * is taken into account. Others are ignored.
   */
  @Test
  public void plugin_common_rules_are_overridden() {
    CommonRuleDefinitions commonRulesDefinitions = new FakeCommonRuleDefinitions();
    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), commonRulesDefinitions, mock(ServerPluginRepository.class),
      new RulesDefinition[] {
        new PluginCommonRuleDefinitions()
      }).load();

    assertThat(context.repositories()).extracting("key").containsOnly("common-java");
    assertThat(context.repository("common-java").rules()).extracting("key").containsOnly("InsufficientBranchCoverage");
    assertThat(context.repository("common-java").rule("InsufficientBranchCoverage").name()).isEqualTo("The name as defined by core");
  }

  static class FindbugsDefinitions implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("findbugs", "java");
      repo.setName("Findbugs");
      repo.createRule("ABC")
        .setName("ABC")
        .setHtmlDescription("Description of ABC");
      repo.done();
    }
  }

  static class SquidDefinitions implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("squid", "java");
      repo.setName("Squid");
      repo.createRule("DEF")
        .setName("DEF")
        .setHtmlDescription("Description of DEF");
      repo.done();
    }
  }

  static class PluginCommonRuleDefinitions implements RulesDefinition {
    @Override
    public void define(RulesDefinition.Context context) {
      RulesDefinition.NewRepository repo = context.createRepository("common-java", "java");
      repo.createRule("InsufficientBranchCoverage")
        .setName("The name as defined by plugin")
        .setHtmlDescription("The description as defined by plugin");
      repo.done();
    }
  }

  static class FakeCommonRuleDefinitions implements CommonRuleDefinitions {
    @Override
    public void define(RulesDefinition.Context context) {
      RulesDefinition.NewRepository repo = context.createRepository("common-java", "java");
      repo.createRule("InsufficientBranchCoverage")
        .setName("The name as defined by core")
        .setHtmlDescription("The description as defined by core");
      repo.done();
    }
  }
}
