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

import org.sonar.api.server.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Collection;

@ServerSide
public class RuleActivatorContextFactory {

  private final DbClient db;

  public RuleActivatorContextFactory(DbClient db) {
    this.db = db;
  }

  RuleActivatorContext create(String profileKey, RuleKey ruleKey, DbSession session) {
    RuleActivatorContext context = new RuleActivatorContext();
    QualityProfileDto profile = db.qualityProfileDao().selectByKey(session, profileKey);
    if (profile == null) {
      throw new BadRequestException("Quality profile not found: " + profileKey);
    }
    context.setProfile(profile);
    return create(ruleKey, session, context);
  }

  RuleActivatorContext create(QProfileName profileName, RuleKey ruleKey, DbSession session) {
    RuleActivatorContext context = new RuleActivatorContext();
    QualityProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(profileName.getName(), profileName.getLanguage(), session);
    if (profile == null) {
      throw new BadRequestException("Quality profile not found: " + profileName);
    }
    context.setProfile(profile);
    return create(ruleKey, session, context);
  }

  RuleActivatorContext create(QualityProfileDto profile, RuleKey ruleKey, DbSession session) {
    return create(ruleKey, session, new RuleActivatorContext().setProfile(profile));
  }

  private RuleActivatorContext create(RuleKey ruleKey, DbSession session, RuleActivatorContext context) {
    initRule(ruleKey, context, session);
    initActiveRules(context.profile().getKey(), ruleKey, context, session, false);
    String parentKee = context.profile().getParentKee();
    if (parentKee != null) {
      initActiveRules(parentKee, ruleKey, context, session, true);
    }
    return context;
  }

  private RuleDto initRule(RuleKey ruleKey, RuleActivatorContext context, DbSession dbSession) {
    RuleDto rule = db.deprecatedRuleDao().getNullableByKey(dbSession, ruleKey);
    if (rule == null) {
      throw new BadRequestException("Rule not found: " + ruleKey);
    }
    context.setRule(rule);
    context.setRuleParams(db.deprecatedRuleDao().selectRuleParamsByRuleKey(dbSession, rule.getKey()));
    return rule;
  }

  private void initActiveRules(String profileKey, RuleKey ruleKey, RuleActivatorContext context, DbSession session, boolean parent) {
    ActiveRuleKey key = ActiveRuleKey.of(profileKey, ruleKey);
    ActiveRuleDto activeRule = db.activeRuleDao().getNullableByKey(session, key);
    Collection<ActiveRuleParamDto> activeRuleParams = null;
    if (activeRule != null) {
      activeRuleParams = db.activeRuleDao().selectParamsByActiveRuleKey(session, key);
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
