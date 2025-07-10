/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.RuleActivationContext;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;

import static com.google.common.base.Preconditions.checkArgument;

public class QProfileRulesImpl implements QProfileRules {

  private final Logger logger = LoggerFactory.getLogger(QProfileRulesImpl.class);

  private final DbClient db;
  private final RuleActivator ruleActivator;
  private final RuleIndex ruleIndex;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final QualityProfileChangeEventService qualityProfileChangeEventService;

  public QProfileRulesImpl(DbClient db, RuleActivator ruleActivator, RuleIndex ruleIndex, ActiveRuleIndexer activeRuleIndexer,
    QualityProfileChangeEventService qualityProfileChangeEventService) {
    this.db = db;
    this.ruleActivator = ruleActivator;
    this.ruleIndex = ruleIndex;
    this.activeRuleIndexer = activeRuleIndexer;
    this.qualityProfileChangeEventService = qualityProfileChangeEventService;
  }

  @Override
  public List<ActiveRuleChange> activateAndCommit(DbSession dbSession, QProfileDto profile, Collection<RuleActivation> activations) {
    verifyNotBuiltIn(profile);

    Set<String> ruleUuids = activations.stream().map(RuleActivation::getRuleUuid).collect(Collectors.toSet());
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleUuids);

    List<ActiveRuleChange> changes = new ArrayList<>();
    for (RuleActivation activation : activations) {
      changes.addAll(ruleActivator.activate(dbSession, activation, context));
    }
    qualityProfileChangeEventService.distributeRuleChangeEvent(List.of(profile), changes, profile.getLanguage());
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  @Override
  public BulkChangeResult bulkActivateAndCommit(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery, @Nullable String severity,
    @Nullable Boolean prioritizedRule) {
    verifyNotBuiltIn(profile);
    BulkChangeResult bulkChangeResult = doBulk(dbSession, profile, ruleQuery, (context, ruleDto) -> {
      RuleActivation activation = RuleActivation.create(ruleDto.getUuid(), severity, prioritizedRule, null);
      return ruleActivator.activate(dbSession, activation, context);
    });
    qualityProfileChangeEventService.distributeRuleChangeEvent(List.of(profile), bulkChangeResult.getChanges(), profile.getLanguage());
    return bulkChangeResult;
  }

  @Override
  public List<ActiveRuleChange> deactivateAndCommit(DbSession dbSession, QProfileDto profile, Collection<String> ruleUuids) {
    verifyNotBuiltIn(profile);
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleUuids);

    List<ActiveRuleChange> changes = new ArrayList<>();
    for (String ruleUuid : ruleUuids) {
      changes.addAll(ruleActivator.deactivate(dbSession, context, ruleUuid, false));
    }
    logger.debug("Rule Deactivated for qProfileUuid :{}, organizationUuid : {} ", profile.getRulesProfileUuid(),
            profile.getOrganizationUuid());
    qualityProfileChangeEventService.distributeRuleChangeEvent(List.of(profile), changes, profile.getLanguage());

    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  @Override
  public BulkChangeResult bulkDeactivateAndCommit(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery) {
    verifyNotBuiltIn(profile);
    BulkChangeResult bulkChangeResult = doBulk(dbSession, profile, ruleQuery, (context, ruleDto) -> ruleActivator.deactivate(dbSession, context, ruleDto.getUuid(), false));

    qualityProfileChangeEventService.distributeRuleChangeEvent(List.of(profile), bulkChangeResult.getChanges(), profile.getLanguage());

    return bulkChangeResult;
  }

  @Override
  public List<ActiveRuleChange> deleteRule(DbSession dbSession, RuleDto rule) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    List<String> activeRuleUuids = new ArrayList<>();
    db.activeRuleDao().selectByRuleUuid(dbSession, rule.getUuid()).forEach(ar -> {
      activeRuleUuids.add(ar.getUuid());
      changes.add(ruleActivator.doDelete(dbSession, ar, rule));
    });

    return changes;
  }

  private static void verifyNotBuiltIn(QProfileDto profile) {
    checkArgument(!profile.isBuiltIn(), "The built-in profile %s is read-only and can't be updated", profile.getName());
  }

  private BulkChangeResult doBulk(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery, BiFunction<RuleActivationContext, RuleDto, List<ActiveRuleChange>> fn) {
    BulkChangeResult result = new BulkChangeResult();
    Collection<String> ruleUuids = Sets.newHashSet(ruleIndex.searchAll(ruleQuery));
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleUuids);

    for (String ruleUuid : ruleUuids) {
      try {
        context.reset(ruleUuid);
        RuleDto ruleDto = context.getRule().get();
        List<ActiveRuleChange> changes = fn.apply(context, ruleDto);
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
