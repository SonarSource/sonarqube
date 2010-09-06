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

import org.apache.commons.collections.CollectionUtils;
import org.sonar.api.Plugin;
import org.sonar.api.Plugins;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.api.resources.Language;

import java.util.*;

/**
 * A class to manage and access rules defined in Sonar.
 *
 *  @deprecated UGLY CLASS - WILL BE COMPLETELY REFACTORED IN SONAR 2.3 
 */
@Deprecated
public class DefaultRulesManager extends RulesManager {

  private final Set<Language> languages;
  private final RulesRepository<?>[] repositories;
  private final Map<Language, List<RulesRepository<?>>> rulesByLanguage;
  private final Map<Language, List<Plugin>> pluginsByLanguage;
  private final Map<String, Map<String, Rule>> rulesByPluginAndKey = new HashMap<String, Map<String, Rule>>();
  private final RulesDao rulesDao;
  private final Plugins plugins;

  /**
   * Creates a RuleManager
   * @param plugins the plugins dictionnary
   * @param repositories the repositories of rules
   * @param dao the dao object
   */
  public DefaultRulesManager(Plugins plugins, RulesRepository[] repositories, RulesDao dao) {
    this.plugins = plugins;
    this.rulesDao = dao;

    languages = new HashSet<Language>();
    rulesByLanguage = new HashMap<Language, List<RulesRepository<?>>>();
    pluginsByLanguage = new HashMap<Language, List<Plugin>>();
    this.repositories = repositories;

    for (RulesRepository<?> repository : repositories) {
      languages.add(repository.getLanguage());

      List<RulesRepository<?>> list = rulesByLanguage.get(repository.getLanguage());
      if (list == null) {
        list = new ArrayList<RulesRepository<?>>();
        rulesByLanguage.put(repository.getLanguage(), list);
      }
      list.add(repository);

      List<Plugin> languagePlugins = pluginsByLanguage.get(repository.getLanguage());
      if (languagePlugins == null) {
        languagePlugins = new ArrayList<Plugin>();
        pluginsByLanguage.put(repository.getLanguage(), languagePlugins);
      }
      languagePlugins.add(plugins.getPluginByExtension(repository));
    }
  }

  /**
   * Constructor for tests only
   *
   * @param dao the dao
   */
  protected DefaultRulesManager(RulesDao dao, Plugins plugins) {
    this.rulesDao = dao;
    this.plugins = plugins;
    languages = new HashSet<Language>();
    rulesByLanguage = new HashMap<Language, List<RulesRepository<?>>>();
    pluginsByLanguage = new HashMap<Language, List<Plugin>>();
    repositories = null;

  }

  /**
   * Returns the list of languages for which there is a rule repository
   *
   * @return a Set of languages
   */
  public Set<Language> getLanguages() {
    return languages;
  }

  /**
   * Gets the list of Rules Repositories available for a language
   *
   * @param language the language
   * @return the list of rules repositories
   */
  public List<RulesRepository<?>> getRulesRepositories(Language language) {
    List<RulesRepository<?>> rulesRepositories = rulesByLanguage.get(language);
    if (CollectionUtils.isNotEmpty(rulesRepositories)) {
      return rulesRepositories;
    }
    return Collections.emptyList();
  }

  /**
   * Gets the complete list of Rules Repositories in the Sonar instance
   *
   * @return the list of rules repositories
   */
  public List<RulesRepository<?>> getRulesRepositories() {
    return Arrays.asList(repositories);
  }

  /**
   * Gets the list of rules plugins for a given language
   * @param language  the language
   * @return the list of plugins
   */
  public List<Plugin> getPlugins(Language language) {
    List<Plugin> result = pluginsByLanguage.get(language);
    if (!CollectionUtils.isEmpty(result)) {
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Gets count of rules by categories defined for a given language
   *
   * @param language the language
   * @return a Map with the category as key and the count as value
   */
  public Map<String, Long> countRulesByCategory(Language language) {
    return countRulesByCategory(language, rulesDao);
  }

  protected Map<String, Long> countRulesByCategory(Language language, RulesDao rulesDao) {
    Map<String, Long> countByCategory = new HashMap<String, Long>();
    List<Plugin> result = getPlugins(language);
    if (!CollectionUtils.isEmpty(result)) {
      List<String> keys = getPluginKeys(getPlugins(language));
      for (RulesCategory rulesCategory : rulesDao.getCategories()) {
        Long rulesCount = rulesDao.countRules(keys, rulesCategory.getName());
        countByCategory.put(rulesCategory.getName(), rulesCount);
      }
    }
    return countByCategory;
  }

  private List<String> getPluginKeys(List<Plugin> plugins) {
    ArrayList<String> keys = new ArrayList<String>();
    for (Plugin plugin : plugins) {
      keys.add(plugin.getKey());
    }
    return keys;
  }

  /**
   * Get the list of rules plugin that implement a mechanism of export for a given language
   *
   * @param language the language
   * @return the list of plugins
   */
  public List<Plugin> getExportablePlugins(Language language) {
    List<Plugin> targets = new ArrayList<Plugin>();
    List<RulesRepository<?>> rulesRepositories = getRulesRepositories(language);
    for (RulesRepository<?> repository : rulesRepositories) {
      if (repository instanceof ConfigurationExportable) {
        targets.add(plugins.getPluginByExtension(repository));
      }
    }
    return targets;
  }

  /**
   * Get the list of rules plugin that implement a mechanism of import for a given language
   *
   * @param language the language
   * @return the list of plugins
   */
  public List<Plugin> getImportablePlugins(Language language) {
    List<Plugin> targets = new ArrayList<Plugin>();
    for (RulesRepository<?> repository : getRulesRepositories(language)) {
      if (repository instanceof ConfigurationImportable) {
        targets.add(plugins.getPluginByExtension(repository));
      }
    }
    return targets;
  }

  /**
   * Gets a list of rules indexed by their key for a given plugin
   * @param pluginKey the plugin key
   * @return a Map with the rule key and the rule
   */
  public Map<String, Rule> getPluginRulesIndexedByKey(String pluginKey) {
    Map<String, Rule> rulesByKey = rulesByPluginAndKey.get(pluginKey);
    if (rulesByKey == null) {
      rulesByKey = new HashMap<String, Rule>();
      List<Rule> rules = rulesDao.getRulesByPlugin(pluginKey);
      if (rules != null) {
        for (Rule rule : rules) {
          rulesByKey.put(rule.getKey(), rule);
        }
      }
      rulesByPluginAndKey.put(pluginKey, rulesByKey);
    }
    return rulesByKey;
  }

  /**
   * Gets a collection of rules belonging to a plugin
   *
   * @param pluginKey the plugin key
   * @return the collection of rules
   */
  public Collection<Rule> getPluginRules(String pluginKey) {
    Map<String, Rule> rulesByKey = getPluginRulesIndexedByKey(pluginKey);
    return rulesByKey.values();
  }

  /**
   * Gets a rule belonging to a defined plugin based on its key
   *
   * @param pluginKey the plugin key
   * @param ruleKey the rule key
   * @return the rule
   */
  public Rule getPluginRule(String pluginKey, String ruleKey) {
    Map<String, Rule> rulesByKey = getPluginRulesIndexedByKey(pluginKey);
    return rulesByKey.get(ruleKey);
  }

}
