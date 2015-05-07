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

import org.sonar.api.ServerSide;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Loads all instances of {@link org.sonar.api.server.rule.RulesDefinition}
 * and initializes {@link org.sonar.server.rule.RuleRepositories}. Used at server startup.
 */
@ServerSide
public class RuleDefinitionsLoader {

  private final DeprecatedRulesDefinitionLoader deprecatedDefinitionConverter;
  private final RulesDefinition[] definitions;
  private final RuleRepositories repositories;

  public RuleDefinitionsLoader(DeprecatedRulesDefinitionLoader deprecatedDefinitionConverter, RuleRepositories repositories, RulesDefinition[] definitions) {
    this.deprecatedDefinitionConverter = deprecatedDefinitionConverter;
    this.repositories = repositories;
    this.definitions = definitions;
  }

  /**
   * Used when no definitions at all.
   */
  public RuleDefinitionsLoader(DeprecatedRulesDefinitionLoader converter, RuleRepositories repositories) {
    this(converter, repositories, new RulesDefinition[0]);
  }

  public RulesDefinition.Context load() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    for (RulesDefinition definition : definitions) {
      definition.define(context);
    }
    deprecatedDefinitionConverter.complete(context);
    repositories.register(context);
    return context;
  }
}
