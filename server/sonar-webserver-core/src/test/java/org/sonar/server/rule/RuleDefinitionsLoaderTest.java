/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import static org.mockito.Mockito.when;

public class RuleDefinitionsLoaderTest {

  @Test
  public void no_definitions() {
    RulesDefinition.Context context = new RuleDefinitionsLoader(mock(ServerPluginRepository.class)).load();

    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void load_returns_definition() {
    var serverPluginRepository = mock(ServerPluginRepository.class);
    var findbugsDefinitions = new FindbugsDefinitions();
    var builtInJavaDefinitions = new JavaDefinitions();
    when(serverPluginRepository.getPluginKey(findbugsDefinitions)).thenReturn("findbugs");
    when(serverPluginRepository.getPluginKey(builtInJavaDefinitions)).thenReturn(null);
    RulesDefinition.Context context = new RuleDefinitionsLoader(serverPluginRepository,
      new RulesDefinition[] {
        findbugsDefinitions, builtInJavaDefinitions
      }).load();

    assertThat(context.repositories()).hasSize(2);
    assertThat(context.repository("findbugs")).isNotNull();
    assertThat(context.repository("java")).isNotNull();
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

  static class JavaDefinitions implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repo = context.createRepository("java", "java");
      repo.setName("Sava");
      repo.createRule("DEF")
        .setName("DEF")
        .setHtmlDescription("Description of DEF");
      repo.done();
    }
  }
}
