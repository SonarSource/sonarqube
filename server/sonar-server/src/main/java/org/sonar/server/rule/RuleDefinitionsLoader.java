/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

/**
 * Loads all instances of {@link RulesDefinition}. Used during server startup
 * and restore of debt model backup.
 */
public class RuleDefinitionsLoader {

  private final DeprecatedRulesDefinitionLoader deprecatedDefConverter;
  private final CommonRuleDefinitions coreCommonDefs;
  private final RulesDefinition[] pluginDefs;

  public RuleDefinitionsLoader(DeprecatedRulesDefinitionLoader deprecatedDefConverter,
    CommonRuleDefinitions coreCommonDefs, RulesDefinition[] pluginDefs) {
    this.deprecatedDefConverter = deprecatedDefConverter;
    this.coreCommonDefs = coreCommonDefs;
    this.pluginDefs = pluginDefs;
  }

  /**
   * Used when no definitions at all.
   */
  public RuleDefinitionsLoader(DeprecatedRulesDefinitionLoader converter,
    CommonRuleDefinitions coreCommonDefs) {
    this(converter, coreCommonDefs, new RulesDefinition[0]);
  }

  public RulesDefinition.Context load() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    for (RulesDefinition pluginDefinition : pluginDefs) {
      pluginDefinition.define(context);
    }
    deprecatedDefConverter.complete(context);
    coreCommonDefs.define(context);
    return context;
  }
}
