/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rules;

import org.sonar.api.Plugin;
import org.sonar.api.resources.Language;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manage and access rules defined in Sonar.
 *
 *  @deprecated UGLY COMPONENT - WILL BE COMPLETELY REFACTORED IN SONAR 2.3
 */
@Deprecated
public abstract class RulesManager {

  /**
   * Returns the list of languages for which there is a rule repository
   *
   * @return a Set of languages
   */
  public abstract Set<Language> getLanguages();

  /**
   * Gets the list of Rules Repositories available for a language
   *
   * @param language the language
   * @return the list of rules repositories
   */
  public abstract List<RulesRepository<?>> getRulesRepositories(Language language);

  /**
   * Gets the complete list of Rules Repositories in the Sonar instance
   *
   * @return the list of rules repositories
   */
  public abstract List<RulesRepository<?>> getRulesRepositories();

  /**
   * Gets the list of rules plugins for a given language
   * @param language  the language
   * @return the list of plugins
   */
  public abstract List<Plugin> getPlugins(Language language);

  /**
   * Get the list of rules plugin that implement a mechanism of import for a given language
   *
   * @param language the language
   * @return the list of plugins
   */
  public abstract List<Plugin> getImportablePlugins(Language language);

  /**
   * Gets a list of rules indexed by their key for a given plugin
   * @param pluginKey the plugin key
   * @return a Map with the rule key and the rule
   */
  public abstract Map<String, Rule> getPluginRulesIndexedByKey(String pluginKey);

  /**
   * Gets a collection of rules belonging to a plugin
   *
   * @param pluginKey the plugin key
   * @return the collection of rules
   */
  public abstract Collection<Rule> getPluginRules(String pluginKey);

  /**
   * Gets a rule belonging to a defined plugin based on its key
   *
   * @param pluginKey the plugin key
   * @param ruleKey the rule key
   * @return the rule
   */
  public abstract Rule getPluginRule(String pluginKey, String ruleKey);
}
