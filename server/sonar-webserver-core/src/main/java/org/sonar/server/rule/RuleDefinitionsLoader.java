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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.impl.server.RulesDefinitionContext;
import org.sonar.server.plugins.ServerPluginRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Loads all instances of {@link RulesDefinition}. Used during server startup
 * and restore of debt model backup.
 */
public class RuleDefinitionsLoader {

  private final RulesDefinition[] pluginDefs;
  private final ServerPluginRepository serverPluginRepository;

  @Autowired(required = false)
  public RuleDefinitionsLoader(ServerPluginRepository serverPluginRepository, RulesDefinition[] pluginDefs) {
    this.serverPluginRepository = serverPluginRepository;
    this.pluginDefs = pluginDefs;
  }

  /**
   * Used when no definitions at all.
   */
  @Autowired(required = false)
  public RuleDefinitionsLoader(ServerPluginRepository serverPluginRepository) {
    this(serverPluginRepository, new RulesDefinition[0]);
  }

  public RulesDefinition.Context load() {
    RulesDefinition.Context context = new RulesDefinitionContext();
    for (RulesDefinition pluginDefinition : pluginDefs) {
      context.setCurrentPluginKey(serverPluginRepository.getPluginKey(pluginDefinition));
      pluginDefinition.define(context);
    }
    context.setCurrentPluginKey(null);
    return context;
  }
}
