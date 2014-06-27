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

package org.sonar.server.rule;

import com.google.common.collect.Lists;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryOptions;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.List;

@Deprecated
/**
 * Will be removed in the future. Please use {@link org.sonar.server.rule.RuleService}
 */
public class DefaultRuleFinder implements RuleFinder {

  private final RuleIndex index;

  public DefaultRuleFinder(IndexClient indexes) {
    this.index = indexes.get(RuleIndex.class);
  }

  @CheckForNull
  public org.sonar.api.rules.Rule findById(int ruleId) {
    return toRule(index.getById(ruleId));
  }

  @CheckForNull
  public Collection<org.sonar.api.rules.Rule> findByIds(Collection<Integer> ruleIds) {
    List<org.sonar.api.rules.Rule> rules = Lists.newArrayList();
    if (ruleIds.isEmpty()) {
      return rules;
    }
    for (Rule rule : index.getByIds(ruleIds)) {
      rules.add(toRule(rule));
    }
    return rules;
  }

  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(RuleKey key) {
    return toRule(index.getByKey(key));
  }

  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(String repositoryKey, String key) {
    return findByKey(RuleKey.of(repositoryKey, key));
  }

  public final org.sonar.api.rules.Rule find(org.sonar.api.rules.RuleQuery query) {
    return toRule(index.search(toQuery(query), new QueryOptions()).getHits().get(0));
  }

  public final Collection<org.sonar.api.rules.Rule> findAll(org.sonar.api.rules.RuleQuery query) {
    List<org.sonar.api.rules.Rule> rules = Lists.newArrayList();
    for(Rule rule:index.search(toQuery(query), new QueryOptions()).getHits()){
      rules.add(toRule(rule));
    }
    return rules;
  }

  private org.sonar.api.rules.Rule toRule(Rule rule) {
    org.sonar.api.rules.Rule apiRule = new org.sonar.api.rules.Rule();
    apiRule.setCharacteristicId(rule.)
    System.out.println("rule = " + rule);
    return null;
  }

  private RuleQuery toQuery(org.sonar.api.rules.RuleQuery query) {
    return null;
  }
//
//  private Query createHqlQuery(DatabaseSession session, org.sonar.api.rules.RuleQuery query) {
//    StringBuilder hql = new StringBuilder().append("from ").append(Rule.class.getSimpleName()).append(" where status<>:status ");
//    Map<String, Object> params = new HashMap<String, Object>();
//    params.put("status", Rule.STATUS_REMOVED);
//    if (StringUtils.isNotBlank(query.getRepositoryKey())) {
//      hql.append("AND pluginName=:repositoryKey ");
//      params.put("repositoryKey", query.getRepositoryKey());
//    }
//    if (StringUtils.isNotBlank(query.getKey())) {
//      hql.append("AND key=:key ");
//      params.put("key", query.getKey());
//    }
//    if (StringUtils.isNotBlank(query.getConfigKey())) {
//      hql.append("AND configKey=:configKey ");
//      params.put("configKey", query.getConfigKey());
//    }
//
//    Query hqlQuery = session.createQuery(hql.toString());
//    for (Map.Entry<String, Object> entry : params.entrySet()) {
//      hqlQuery.setParameter(entry.getKey(), entry.getValue());
//    }
//    return hqlQuery;
//  }
}
