/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;

import static com.google.common.base.Preconditions.checkArgument;

public class QProfileRulesImpl implements QProfileRules {

  private final DbClient db;
  private final RuleActivator ruleActivator;
  private final RuleIndex ruleIndex;
  private final ActiveRuleIndexer activeRuleIndexer;

  public QProfileRulesImpl(DbClient db, RuleActivator ruleActivator, RuleIndex ruleIndex, ActiveRuleIndexer activeRuleIndexer) {
    this.db = db;
    this.ruleActivator = ruleActivator;
    this.ruleIndex = ruleIndex;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  @Override
  public List<ActiveRuleChange> activateAndCommit(DbSession dbSession, QProfileDto profile, Collection<RuleActivation> activations) {
    verifyNotBuiltIn(profile);

    Set<Integer> ruleIds = activations.stream().map(RuleActivation::getRuleId).collect(MoreCollectors.toHashSet(activations.size()));
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleIds);

    List<ActiveRuleChange> changes = new ArrayList<>();
    for (RuleActivation activation : activations) {
      changes.addAll(ruleActivator.activate(dbSession, activation, context));
    }
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  @Override
  public BulkChangeResult bulkActivateAndCommit(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery, @Nullable String severity) {
    verifyNotBuiltIn(profile);
    return doBulk(dbSession, profile, ruleQuery, (context, ruleDefinition) -> {
      RuleActivation activation = RuleActivation.create(ruleDefinition.getId(), severity, null);
      return ruleActivator.activate(dbSession, activation, context);
    });
  }

  @Override
  public List<ActiveRuleChange> deactivateAndCommit(DbSession dbSession, QProfileDto profile, Collection<Integer> ruleIds) {
    verifyNotBuiltIn(profile);
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleIds);

    List<ActiveRuleChange> changes = new ArrayList<>();
    for (Integer ruleId : ruleIds) {
      changes.addAll(ruleActivator.deactivate(dbSession, context, ruleId, false));
    }
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  @Override
  public BulkChangeResult bulkDeactivateAndCommit(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery) {
    verifyNotBuiltIn(profile);
    return doBulk(dbSession, profile, ruleQuery, (context, ruleDefinition) -> ruleActivator.deactivate(dbSession, context, ruleDefinition.getId(), false));
  }

  @Override
  public List<ActiveRuleChange> deleteRule(DbSession dbSession, RuleDefinitionDto rule) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    List<Integer> activeRuleIds = new ArrayList<>();
    db.activeRuleDao().selectByRuleIdOfAllOrganizations(dbSession, rule.getId()).forEach(ar -> {
      activeRuleIds.add(ar.getId());
      changes.add(new ActiveRuleChange(ActiveRuleChange.Type.DEACTIVATED, ar, rule));
    });

    db.activeRuleDao().deleteByIds(dbSession, activeRuleIds);
    db.activeRuleDao().deleteParamsByActiveRuleIds(dbSession, activeRuleIds);

    return changes;
  }

  private static void verifyNotBuiltIn(QProfileDto profile) {
    checkArgument(!profile.isBuiltIn(), "The built-in profile %s is read-only and can't be updated", profile.getName());
  }

  private BulkChangeResult doBulk(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery, BiFunction<RuleActivationContext, RuleDefinitionDto, List<ActiveRuleChange>> fn) {
    BulkChangeResult result = new BulkChangeResult();
    Collection<Integer> ruleIds = Sets.newHashSet(ruleIndex.searchAll(ruleQuery));
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleIds);

    for (Integer ruleId : ruleIds) {
      try {
        context.reset(ruleId);
        RuleDefinitionDto ruleDefinition = context.getRule().get();
        List<ActiveRuleChange> changes = fn.apply(context, ruleDefinition);
        result.addChanges(changes);
        if (!changes.isEmpty()) {
          result.incrementSucceeded();
        }
      } catch (BadRequestException e) {
        // other exceptions stop the bulk activation
        result.incrementFailed();
        result.getErrors().addAll(e.errors());
      }
    }
    activeRuleIndexer.commitAndIndex(dbSession, result.getChanges());
    return result;
  }
}
