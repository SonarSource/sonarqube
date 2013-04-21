/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.dao;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;

import java.util.List;

public class RulesDao extends BaseDao {

  public RulesDao(DatabaseSession session) {
    super(session);
  }

  public List<Rule> getRules() {
    return getSession().createQuery("FROM " + Rule.class.getSimpleName() + " r WHERE r.status<>:status")
        .setParameter("status", Rule.STATUS_REMOVED)
        .getResultList();
  }

  public List<Rule> getRulesByRepository(String repositoryKey) {
    return getSession().createQuery("FROM " + Rule.class.getSimpleName() + " r WHERE r.pluginName=:pluginName and r.status<>:status")
        .setParameter("pluginName", repositoryKey)
        .setParameter("status", Rule.STATUS_REMOVED)
        .getResultList();
  }

  /**
   * @deprecated since 2.5 use {@link #getRulesByRepository(String)} instead.
   */
  @Deprecated
  public List<Rule> getRulesByPlugin(String pluginKey) {
    return getRulesByRepository(pluginKey);
  }

  public Rule getRuleByKey(String repositoryKey, String ruleKey) {
    DatabaseSession session = getSession();
    return (Rule) session.getSingleResult(
        session.createQuery("FROM " + Rule.class.getSimpleName() + " r WHERE r.key=:key and r.pluginName=:pluginName and r.status<>:status")
            .setParameter("key", ruleKey)
            .setParameter("pluginName", repositoryKey)
            .setParameter("status", Rule.STATUS_REMOVED
        ),
        null);
  }

  public RuleParam getRuleParam(Rule rule, String paramKey) {
    return getSession().getSingleResult(RuleParam.class, "rule", rule, "key", paramKey);
  }

}
