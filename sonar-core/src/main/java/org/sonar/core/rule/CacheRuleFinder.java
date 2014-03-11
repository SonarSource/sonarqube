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
package org.sonar.core.rule;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleQuery;
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
    if (rule == null) {
      rule = doFindById(ruleId);
      if (rule != null) {
        loadRepository(rule.getRepositoryKey());
      }
    }
    return rule;
  }

  @Override
  public Rule findByKey(String repositoryKey, String ruleKey) {
    Map<String, Rule> repository = loadRepository(repositoryKey);
    return repository.get(ruleKey);
  }

  private Map<String, Rule> loadRepository(String repositoryKey) {
    Map<String, Rule> repository = rulesByKey.get(repositoryKey);
    if (repository == null) {
      repository = Maps.newHashMap();
      rulesByKey.put(repositoryKey, repository);

      for (Rule rule : findAll(RuleQuery.create().withRepositoryKey(repositoryKey))) {
        repository.put(rule.getKey(), rule);
        rulesById.put(rule.getId(), rule);
      }
    }
    return repository;
  }
}
