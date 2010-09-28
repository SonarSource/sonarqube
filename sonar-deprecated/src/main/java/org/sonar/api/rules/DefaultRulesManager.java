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

import com.google.common.collect.Maps;
import org.sonar.jpa.dao.RulesDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to manage and access rules defined in Sonar.
 *
 * @deprecated UGLY CLASS 
 */
@Deprecated
public class DefaultRulesManager extends RulesManager {

  private final Map<String, Map<String, Rule>> rulesByPluginAndKey = Maps.newHashMap();
  private final RulesDao rulesDao;

  public DefaultRulesManager(RulesDao dao) {
    this.rulesDao = dao;
  }

  /**
   * Gets a list of rules indexed by their key for a given plugin
   *
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
   * Gets a rule belonging to a defined plugin based on its key
   *
   * @param pluginKey the plugin key
   * @param ruleKey   the rule key
   * @return the rule
   */
  public Rule getPluginRule(String pluginKey, String ruleKey) {
    Map<String, Rule> rulesByKey = getPluginRulesIndexedByKey(pluginKey);
    return rulesByKey.get(ruleKey);
  }

}
