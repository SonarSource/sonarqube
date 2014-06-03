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

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.Rule;

import java.util.Collection;

public class RuleActivationContextFactory implements ServerComponent {

  private final DbClient db;

  public RuleActivationContextFactory(DbClient db) {
    this.db = db;
  }

  public RuleActivationContext create(ActiveRuleKey key, DbSession session) {
    RuleActivationContext context = new RuleActivationContext();

    RuleDto rule = initRule(key.ruleKey(), context, session);

    QualityProfileDto profile = initProfile(key, context, session, false);
    initActiveRules(key, context, session, false);
    if (!profile.getLanguage().equals(rule.getLanguage())) {
      throw new IllegalArgumentException(String.format("Rule %s and profile %s have different languages", rule.getKey(), profile.getKey()));
    }

    if (profile.getParent() != null) {
      ActiveRuleKey parentKey = ActiveRuleKey.of(
        QualityProfileKey.of(profile.getParent(), profile.getLanguage()), rule.getKey());
      initProfile(parentKey, context, session, true);
      initActiveRules(parentKey, context, session, true);
    }
    return context;
  }

  private RuleDto initRule(RuleKey ruleKey, RuleActivationContext context, DbSession dbSession) {
    RuleDto rule = db.ruleDao().getNullableByKey(dbSession, ruleKey);
    if (rule == null) {
      throw new IllegalArgumentException("Rule not found: " + ruleKey);
    }
    if (RuleStatus.REMOVED == rule.getStatus()) {
      throw new IllegalArgumentException("Rule was removed: " + ruleKey);
    }
    if (Cardinality.MULTIPLE.equals(rule.getCardinality())) {
      throw new IllegalArgumentException("Rule template can't be activated on a Quality profile: " + ruleKey);
    }
    if (Rule.MANUAL_REPOSITORY_KEY.equals(rule.getRepositoryKey())) {
      throw new IllegalArgumentException("Manual rule can't be activated on a Quality profile: " + ruleKey);
    }
    context.setRule(rule);
    context.setRuleParams(db.ruleDao().findRuleParamsByRuleKey(dbSession, rule.getKey()));
    return rule;
  }

  private QualityProfileDto initProfile(ActiveRuleKey key, RuleActivationContext context, DbSession session, boolean parent) {
    QualityProfileDto profile = db.qualityProfileDao().getByKey(key.qProfile(),session);
    if (profile == null) {
      throw new IllegalArgumentException("Quality profile not found: " + key.qProfile());
    }
    if (parent) {
      context.setParentProfile(profile);
    } else {
      context.setProfile(profile);
    }
    return profile;
  }

  private void initActiveRules(ActiveRuleKey key, RuleActivationContext context, DbSession session, boolean parent) {
    ActiveRuleDto activeRule = db.activeRuleDao().getNullableByKey(session, key);
    Collection<ActiveRuleParamDto> activeRuleParams = null;
    if (activeRule != null) {
      activeRuleParams = db.activeRuleDao().findParamsByActiveRuleKey(session, key);
    }
    if (parent) {
      context.setParentActiveRule(activeRule);
      context.setParentActiveRuleParams(activeRuleParams);
    } else {
      context.setActiveRule(activeRule);
      context.setActiveRuleParams(activeRuleParams);
    }
  }
}
