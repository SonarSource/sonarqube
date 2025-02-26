/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.qualityprofile.builtin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.RulesProfileDto;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

public class BuiltInQProfileUpdateImpl implements BuiltInQProfileUpdate {

  private final DbClient dbClient;
  private final RuleActivator ruleActivator;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final QualityProfileChangeEventService qualityProfileChangeEventService;

  public BuiltInQProfileUpdateImpl(DbClient dbClient, RuleActivator ruleActivator, ActiveRuleIndexer activeRuleIndexer,
    QualityProfileChangeEventService qualityProfileChangeEventService) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.activeRuleIndexer = activeRuleIndexer;
    this.qualityProfileChangeEventService = qualityProfileChangeEventService;
  }

  public List<ActiveRuleChange> update(DbSession dbSession, BuiltInQProfile builtInDefinition, RulesProfileDto initialRuleProfile) {
    // Keep reference to all the activated rules before update
    Set<String> previousBuiltinActiveRuleUuids = dbClient.activeRuleDao().selectByRuleProfile(dbSession, initialRuleProfile)
      .stream()
      .map(ActiveRuleDto::getRuleUuid)
      .collect(Collectors.toUnmodifiableSet());

    Set<String> deactivatedRuleUuids = new HashSet<>(previousBuiltinActiveRuleUuids);

    // all rules, including those which are removed from built-in profile
    Set<String> ruleUuids = Stream.concat(
        deactivatedRuleUuids.stream(),
        builtInDefinition.getActiveRules().stream().map(BuiltInQProfile.ActiveRule::getRuleUuid))
      .collect(Collectors.toSet());

    Collection<RuleActivation> activations = new ArrayList<>();
    for (BuiltInQProfile.ActiveRule ar : builtInDefinition.getActiveRules()) {
      RuleActivation activation = convert(ar);
      activations.add(activation);
      deactivatedRuleUuids.remove(activation.getRuleUuid());
    }

    RuleActivationContext context = ruleActivator.createContextForBuiltInProfile(dbSession, initialRuleProfile, ruleUuids, previousBuiltinActiveRuleUuids);
    List<ActiveRuleChange> changes = new ArrayList<>();

    changes.addAll(ruleActivator.activate(dbSession, activations, context));

    // these rules are no longer part of the built-in profile
    deactivatedRuleUuids.forEach(ruleUuid -> changes.addAll(ruleActivator.deactivate(dbSession, context, ruleUuid, false)));

    if (!changes.isEmpty()) {
      qualityProfileChangeEventService.distributeRuleChangeEvent(context.getProfiles(), changes, initialRuleProfile.getLanguage());
    }

    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  private static RuleActivation convert(BuiltInQProfile.ActiveRule ar) {
    Map<String, String> params = ar.getParams().stream()
      .collect(Collectors.toMap(BuiltInQualityProfilesDefinition.OverriddenParam::key, BuiltInQualityProfilesDefinition.OverriddenParam::overriddenValue));
    return RuleActivation.create(ar.getRuleUuid(), ar.getSeverity(), params);
  }

}
