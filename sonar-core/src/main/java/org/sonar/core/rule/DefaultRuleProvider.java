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
package org.sonar.core.rule;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleProvider;
import org.sonar.api.rules.RuleQuery;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultRuleProvider implements RuleProvider {

  private DatabaseSessionFactory sessionFactory;

  public DefaultRuleProvider(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public Rule findByKey(String repositoryKey, String key) {
    return sessionFactory.getSession().getSingleResult(Rule.class, "pluginName", repositoryKey, "key", key, "enabled", true);
  }

  public Rule find(RuleQuery query) {
    return (Rule) createHqlQuery(query).getSingleResult();
  }

  public Collection<Rule> findAll(RuleQuery query) {
    return createHqlQuery(query).getResultList();
  }

  private Query createHqlQuery(RuleQuery query) {
    StringBuilder hql = new StringBuilder().append("from ").append(Rule.class.getSimpleName()).append(" where enabled=true ");
    Map<String,Object> params = new HashMap<String,Object>();
    if (StringUtils.isNotBlank(query.getRepositoryKey())) {
      hql.append("AND pluginName=:repositoryKey");
      params.put("repositoryKey", query.getRepositoryKey());
    }
    if (StringUtils.isNotBlank(query.getKey())) {
      hql.append("AND key=:key");
      params.put("key", query.getKey());
    }
    if (StringUtils.isNotBlank(query.getConfigKey())) {
      hql.append("AND configKey=:configKey");
      params.put("configKey", query.getConfigKey());
    }

    Query hqlQuery = sessionFactory.getSession().createQuery(hql.toString());
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      hqlQuery.setParameter(entry.getKey(), entry.getValue());
    }
    return hqlQuery;
  }
}
