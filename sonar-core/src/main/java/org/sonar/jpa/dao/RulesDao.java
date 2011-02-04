/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.jpa.dao;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;

import java.util.Iterator;
import java.util.List;

public class RulesDao extends BaseDao {

  public RulesDao(DatabaseSession session) {
    super(session);
  }

  public List<Rule> getRules() {
    return getSession().getResults(Rule.class, "enabled", true);
  }

  public List<Rule> getRulesByRepository(String repositoryKey) {
    return getSession().getResults(Rule.class, "pluginName", repositoryKey, "enabled", true);
  }

  /**
   * @deprecated since 2.5 use {@link #getRulesByRepository(String)} instead.
   */
  @Deprecated
  public List<Rule> getRulesByPlugin(String pluginKey) {
    return getRulesByRepository(pluginKey);
  }

  public Rule getRuleByKey(String repositoryKey, String ruleKey) {
    return getSession().getSingleResult(Rule.class, "key", ruleKey, "pluginName", repositoryKey, "enabled", true);
  }


  public RuleParam getRuleParam(Rule rule, String paramKey) {
    return getSession().getSingleResult(RuleParam.class, "rule", rule, "key", paramKey);
  }

}
