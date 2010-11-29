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
package org.sonar.core.components;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.sonar.api.rules.Rule;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Map;

public final class CacheRuleFinder extends DefaultRuleFinder {

  private BiMap<Integer, Rule> rulesById = HashBiMap.create();
  private Map<String, Map<String, Rule>> rulesByKey = Maps.newHashMap();

  public CacheRuleFinder(DatabaseSessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public Rule findById(int ruleId) {
    Rule rule = rulesById.get(ruleId);
    if (rule==null) {
      rule = doFindById(ruleId);
      addToCache(rule);
    }
    return rule;
  }

  @Override
  public Rule findByKey(String repositoryKey, String key) {
    Map<String,Rule> repoRules = rulesByKey.get(repositoryKey);
    Rule rule = null;
    if (repoRules!=null) {
      rule = repoRules.get(key);
    }
    if (rule == null) {
      rule = doFindByKey(repositoryKey, key);
      addToCache(rule);
    }
    return rule;
  }

  private void addToCache(Rule rule) {
    if (rule != null) {
      rulesById.put(rule.getId(), rule);
      Map<String, Rule> repoRules = rulesByKey.get(rule.getKey());
      if (repoRules==null) {
        repoRules = Maps.newHashMap();
        rulesByKey.put(rule.getRepositoryKey(), repoRules);
      }
      repoRules.put(rule.getKey(), rule);
    }
  }
}
