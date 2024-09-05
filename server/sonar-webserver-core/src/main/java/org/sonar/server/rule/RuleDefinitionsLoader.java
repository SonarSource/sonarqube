/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Objects;
import java.util.function.Predicate;
import org.sonar.api.impl.server.RulesDefinitionContext;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.server.plugins.ServerPluginRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Loads all instances of {@link RulesDefinition}. Used during server startup
 * and restore of debt model backup.
 */
public class RuleDefinitionsLoader {

  private final RulesDefinition[] rulesDefinitions;
  private final ServerPluginRepository serverPluginRepository;

  @Autowired(required = false)
  public RuleDefinitionsLoader(ServerPluginRepository serverPluginRepository, RulesDefinition[] rulesDefinitions) {
    this.serverPluginRepository = serverPluginRepository;
    this.rulesDefinitions = rulesDefinitions;
  }

  /**
   * Used when no definitions at all.
   */
  @Autowired(required = false)
  public RuleDefinitionsLoader(ServerPluginRepository serverPluginRepository) {
    this(serverPluginRepository, new RulesDefinition[0]);
  }

  public RulesDefinition.Context loadFromPlugins() {
    return load(Predicate.not(Objects::isNull));
  }

  public RulesDefinition.Context loadBuiltIn() {
    return load(Objects::isNull);
  }

  private RulesDefinition.Context load(Predicate<String> pluginKeyPredicate) {
    RulesDefinition.Context context = new RulesDefinitionContext();
    for (RulesDefinition rulesDefinition : rulesDefinitions) {
      var pluginKey = serverPluginRepository.getPluginKey(rulesDefinition);
      if (pluginKeyPredicate.test(pluginKey)) {
        context.setCurrentPluginKey(pluginKey);
        rulesDefinition.define(context);
      }
    }
    context.setCurrentPluginKey(null);
    return context;
  }
}
