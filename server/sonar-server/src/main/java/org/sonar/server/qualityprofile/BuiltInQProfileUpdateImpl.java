/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

public class BuiltInQProfileUpdateImpl implements BuiltInQProfileUpdate {

  private final DbClient dbClient;
  private final RuleActivator ruleActivator;
  private final ActiveRuleIndexer activeRuleIndexer;

  public BuiltInQProfileUpdateImpl(DbClient dbClient, RuleActivator ruleActivator, ActiveRuleIndexer activeRuleIndexer) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  public List<ActiveRuleChange> update(DbSession dbSession, BuiltInQProfile builtIn, RulesProfileDto rulesProfile) {
    // Keep reference to all the activated rules before update
    Set<RuleKey> deactivatedKeys = dbClient.activeRuleDao().selectByRuleProfile(dbSession, rulesProfile)
      .stream()
      .map(ActiveRuleDto::getRuleKey)
      .collect(MoreCollectors.toHashSet());

    Collection<RuleActivation> activations = new ArrayList<>();
    Collection<RuleKey> ruleKeys = new HashSet<>(deactivatedKeys);
    for (BuiltInActiveRule ar : builtIn.getActiveRules()) {
      RuleActivation activation = convert(ar);
      activations.add(activation);
      ruleKeys.add(activation.getRuleKey());
      deactivatedKeys.remove(activation.getRuleKey());
    }

    RuleActivationContext context = ruleActivator.createContextForBuiltInProfile(dbSession, rulesProfile, ruleKeys);

    List<ActiveRuleChange> changes = new ArrayList<>();
    for (RuleActivation activation : activations) {
      changes.addAll(ruleActivator.activate(dbSession, activation, context));
    }

    // these rules are not part of the built-in profile anymore
    deactivatedKeys.forEach(ruleKey -> changes.addAll(ruleActivator.deactivate(dbSession, context, ruleKey, false)));

    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  private static RuleActivation convert(BuiltInActiveRule ar) {
    Map<String, String> params = ar.overriddenParams().stream()
      .collect(MoreCollectors.uniqueIndex(BuiltInQualityProfilesDefinition.OverriddenParam::key, BuiltInQualityProfilesDefinition.OverriddenParam::overriddenValue));
    return RuleActivation.create(RuleKey.of(ar.repoKey(), ar.ruleKey()), ar.overriddenSeverity(), params);
  }

}
