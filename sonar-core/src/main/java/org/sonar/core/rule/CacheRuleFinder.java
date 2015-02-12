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
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.annotation.CheckForNull;
import javax.persistence.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated since 4.5
 */
@Deprecated
public final class CacheRuleFinder implements RuleFinder {

  private BiMap<Integer, Rule> rulesById = HashBiMap.create();
  private Map<String, Map<String, Rule>> rulesByKey = Maps.newHashMap();

  private DatabaseSessionFactory sessionFactory;

  public CacheRuleFinder(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
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
        hibernateHack(rule);
        repository.put(rule.getKey(), rule);
        rulesById.put(rule.getId(), rule);
      }
    }
    return repository;
  }

  private void hibernateHack(Rule rule) {
    Hibernate.initialize(rule.getParams());
  }

  private Rule doFindById(int ruleId) {
    DatabaseSession session = sessionFactory.getSession();
    return session.getSingleResult(
      session.createQuery("FROM " + Rule.class.getSimpleName() + " r WHERE r.id=:id and r.status<>:status")
        .setParameter("id", ruleId)
        .setParameter("status", Rule.STATUS_REMOVED
        ),
      null);
  }

  @Override
  @CheckForNull
  public Rule findByKey(RuleKey key) {
    return findByKey(key.repository(), key.rule());
  }

  @Override
  public final Rule find(RuleQuery query) {
    DatabaseSession session = sessionFactory.getSession();
    return session.getSingleResult(createHqlQuery(session, query), null);
  }

  @Override
  public final Collection<Rule> findAll(RuleQuery query) {
    DatabaseSession session = sessionFactory.getSession();
    return createHqlQuery(session, query).getResultList();
  }

  private Query createHqlQuery(DatabaseSession session, RuleQuery query) {
    StringBuilder hql = new StringBuilder().append("from ").append(Rule.class.getSimpleName()).append(" where status<>:status ");
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("status", Rule.STATUS_REMOVED);
    if (StringUtils.isNotBlank(query.getRepositoryKey())) {
      hql.append("AND pluginName=:repositoryKey ");
      params.put("repositoryKey", query.getRepositoryKey());
    }
    if (StringUtils.isNotBlank(query.getKey())) {
      hql.append("AND key=:key ");
      params.put("key", query.getKey());
    }
    if (StringUtils.isNotBlank(query.getConfigKey())) {
      hql.append("AND configKey=:configKey ");
      params.put("configKey", query.getConfigKey());
    }

    Query hqlQuery = session.createQuery(hql.toString());
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      hqlQuery.setParameter(entry.getKey(), entry.getValue());
    }
    return hqlQuery;
  }
}
