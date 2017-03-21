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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static java.util.Objects.requireNonNull;

@ServerSide
public class QProfileResetImpl implements QProfileReset {

  private final DbClient db;
  private final QProfileFactory factory;
  private final RuleActivator activator;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final DefinedQProfileRepository definedQProfileRepositories;

  public QProfileResetImpl(DbClient db, RuleActivator activator, ActiveRuleIndexer activeRuleIndexer, QProfileFactory factory,
    DefinedQProfileRepository definedQProfileRepository) {
    this.db = db;
    this.activator = activator;
    this.activeRuleIndexer = activeRuleIndexer;
    this.factory = factory;
    this.definedQProfileRepositories = definedQProfileRepository;
  }

  @Override
  public void resetLanguage(DbSession dbSession, OrganizationDto organization, String language) {
    definedQProfileRepositories.getQProfilesByLanguage()
      .entrySet()
      .stream()
      .filter(entry -> entry.getKey().equals(language))
      .map(Map.Entry::getValue)
      .flatMap(List::stream)
      .forEach(definedQProfile -> resetProfile(dbSession, organization, definedQProfile));
  }

  private void resetProfile(DbSession dbSession, OrganizationDto organization, DefinedQProfile definedQProfile) {
    QualityProfileDto profile = factory.getOrCreate(dbSession, organization, definedQProfile.getQProfileName());

    List<RuleActivation> activations = Lists.newArrayList();
    definedQProfile.getActiveRules().forEach(activeRule -> activations.add(getRuleActivation(dbSession, activeRule)));
    reset(dbSession, profile, activations);
  }

  private RuleActivation getRuleActivation(DbSession dbSession, ActiveRule activeRule) {
    RuleActivation activation = new RuleActivation(RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey()));
    activation.setSeverity(activeRule.getSeverity().name());
    if (!activeRule.getActiveRuleParams().isEmpty()) {
      for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
        activation.setParameter(param.getParamKey(), param.getValue());
      }
    } else {
      for (RuleParamDto param : db.ruleDao().selectRuleParamsByRuleKey(dbSession, activeRule.getRule().ruleKey())) {
        activation.setParameter(param.getName(), param.getDefaultValue());
      }
    }
    return activation;
  }

  @Override
  public BulkChangeResult reset(DbSession dbSession, QualityProfileDto profile, Collection<RuleActivation> activations) {
    requireNonNull(profile.getId(), "Quality profile must be persisted");
    BulkChangeResult result = new BulkChangeResult();
    Set<RuleKey> ruleToBeDeactivated = Sets.newHashSet();
    // Keep reference to all the activated rules before backup restore
    for (ActiveRuleDto activeRuleDto : db.activeRuleDao().selectByProfileKey(dbSession, profile.getKee())) {
      if (activeRuleDto.getInheritance() == null) {
        // inherited rules can't be deactivated
        ruleToBeDeactivated.add(activeRuleDto.getKey().ruleKey());
      }
    }

    for (RuleActivation activation : activations) {
      try {
        List<ActiveRuleChange> changes = activator.activate(dbSession, activation, profile.getKey());
        ruleToBeDeactivated.remove(activation.getRuleKey());
        result.incrementSucceeded();
        result.addChanges(changes);
      } catch (BadRequestException e) {
        result.incrementFailed();
        result.getErrors().addAll(e.errors());
      }
    }

    List<ActiveRuleChange> changes = new ArrayList<>();
    changes.addAll(result.getChanges());
    for (RuleKey ruleKey : ruleToBeDeactivated) {
      try {
        changes.addAll(activator.deactivate(dbSession, ActiveRuleKey.of(profile.getKee(), ruleKey)));
      } catch (BadRequestException e) {
        // ignore, probably a rule inherited from parent that can't be deactivated
      }
    }
    dbSession.commit();
    activeRuleIndexer.index(changes);
    return result;
  }

}
