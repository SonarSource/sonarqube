/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.core.rule;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.persistence.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultRuleFinder implements RuleFinder {

  private DatabaseSessionFactory sessionFactory;

  public DefaultRuleFinder(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public Rule findById(int ruleId) {
    return doFindById(ruleId);
  }

  protected final Rule doFindById(int ruleId) {
    DatabaseSession session = sessionFactory.getSession();
    return (Rule) session.getSingleResult(
        session.createQuery("FROM " + Rule.class.getSimpleName() + " r WHERE r.id=:id and r.status<>:status")
            .setParameter("id", ruleId)
            .setParameter("status", RuleStatus.REMOVED.name()
            ),
        null);
  }

  public Rule findByKey(String repositoryKey, String key) {
    return doFindByKey(repositoryKey, key);
  }

  protected final Rule doFindByKey(String repositoryKey, String key) {
    DatabaseSession session = sessionFactory.getSession();
    return (Rule) session.getSingleResult(
        session.createQuery("FROM " + Rule.class.getSimpleName() + " r WHERE r.key=:key and r.pluginName=:pluginName and r.status<>:status")
            .setParameter("key", key)
            .setParameter("pluginName", repositoryKey)
            .setParameter("status", RuleStatus.REMOVED.name()
            ),
        null);
  }

  public final Rule find(RuleQuery query) {
    DatabaseSession session = sessionFactory.getSession();
    return (Rule) session.getSingleResult(createHqlQuery(session, query), null);

  }

  public final Collection<Rule> findAll(RuleQuery query) {
    DatabaseSession session = sessionFactory.getSession();
    return createHqlQuery(session, query).getResultList();
  }

  private Query createHqlQuery(DatabaseSession session, RuleQuery query) {
    StringBuilder hql = new StringBuilder().append("from ").append(Rule.class.getSimpleName()).append(" where status<>:status ");
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("status", RuleStatus.REMOVED.name());
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
