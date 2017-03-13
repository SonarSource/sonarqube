/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;

import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
public class RuleActivatorContextFactory {

  private final DbClient db;

  public RuleActivatorContextFactory(DbClient db) {
    this.db = db;
  }

  RuleActivatorContext create(String profileKey, RuleKey ruleKey, DbSession session) {
    RuleActivatorContext context = new RuleActivatorContext();
    QualityProfileDto profile = db.qualityProfileDao().selectByKey(session, profileKey);
    checkRequest(profile != null, "Quality profile not found: %s", profileKey);
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
    Optional<RuleDto> rule = getRule(dbSession, ruleKey);
    checkRequest(rule.isPresent(), "Rule not found: %s", ruleKey);
    context.setRule(rule.get());
    context.setRuleParams(db.ruleDao().selectRuleParamsByRuleKey(dbSession, rule.get().getKey()));
    return rule.get();
  }

  private void initActiveRules(String profileKey, RuleKey ruleKey, RuleActivatorContext context, DbSession session, boolean parent) {
    ActiveRuleKey key = ActiveRuleKey.of(profileKey, ruleKey);
    Optional<ActiveRuleDto> activeRule = getActiveRule(session, key);
    Collection<ActiveRuleParamDto> activeRuleParams = null;
    if (activeRule.isPresent()) {
      activeRuleParams = getActiveRuleParams(session, activeRule.get());
    }
    if (parent) {
      context.setParentActiveRule(activeRule.orElse(null));
      context.setParentActiveRuleParams(activeRuleParams);
    } else {
      context.setActiveRule(activeRule.orElse(null));
      context.setActiveRuleParams(activeRuleParams);
    }
  }

  Optional<RuleDto> getRule(DbSession dbSession, RuleKey ruleKey) {
    return Optional.ofNullable(db.ruleDao().selectByKey(dbSession, ruleKey).orNull());
  }

  Optional<ActiveRuleDto> getActiveRule(DbSession session, ActiveRuleKey key) {
    return Optional.ofNullable(db.activeRuleDao().selectByKey(session, key).orNull());
  }

  List<ActiveRuleParamDto> getActiveRuleParams(DbSession session, ActiveRuleDto activeRuleDto) {
    return db.activeRuleDao().selectParamsByActiveRuleId(session, activeRuleDto.getId());
  }
}
