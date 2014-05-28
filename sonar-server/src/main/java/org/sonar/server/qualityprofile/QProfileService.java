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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Multimap;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.IndexClient;

import javax.annotation.CheckForNull;
import java.util.List;

public class QProfileService implements ServerComponent {

  private final IndexClient index;
  private final RuleActivator ruleActivator;

  public QProfileService(IndexClient index, RuleActivator ruleActivator) {
    this.index = index;
    this.ruleActivator = ruleActivator;
  }

  @CheckForNull
  public ActiveRule getActiveRule(ActiveRuleKey key) {
    return index.get(ActiveRuleIndex.class).getByKey(key);
  }

  public List<ActiveRule> findActiveRulesByRule(RuleKey key) {
    return index.get(ActiveRuleIndex.class).findByRule(key);
  }

  public List<ActiveRule> findActiveRulesByProfile(QualityProfileKey key) {
    return index.get(ActiveRuleIndex.class).findByProfile(key);
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public List<ActiveRuleChange> activate(RuleActivation activation) {
    return ruleActivator.activate(activation);
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated, but
   * fails (fast) if the rule or the profile does not exist.
   */
  public List<ActiveRuleChange> deactivate(ActiveRuleKey key) {
    return ruleActivator.deactivate(key);
  }


  public Multimap<String, String> bulkActivate(RuleQuery ruleQuery, QualityProfileKey profile) {
    return ruleActivator.activateByRuleQuery(ruleQuery, profile);
  }

  public Multimap<String, String> bulkDeactivate(RuleQuery ruleQuery, QualityProfileKey profile) {
    return ruleActivator.deActivateByRuleQuery(ruleQuery, profile);
  }
}
