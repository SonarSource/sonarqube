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
import org.sonar.api.server.rule.RulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RulesDefinitionLoaderTest {
  @Test
  public void no_definitions() {
    RuleRepositories repositories = new RuleRepositories();

    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), repositories).load();

    assertThat(context.repositories()).isEmpty();
    assertThat(repositories.repositories()).isEmpty();
  }

  @Test
  public void load_definitions() {
    RuleRepositories repositories = new RuleRepositories();

    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(DeprecatedRulesDefinitionLoader.class), repositories, new RulesDefinition[]{
      new FindbugsDefinitions(), new SquidDefinitions()
    }).load();

    assertThat(context.repositories()).hasSize(2);
    assertThat(context.repository("findbugs")).isNotNull();
    assertThat(context.repository("squid")).isNotNull();
    assertThat(repositories.repositories()).hasSize(2);
    assertThat(repositories.repository("findbugs")).isNotNull();
    assertThat(repositories.repository("squid")).isNotNull();
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
}
