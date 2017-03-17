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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.ValidationMessages;
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
import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
public class QProfileResetImpl implements QProfileReset {

  private final DbClient db;
  private final QProfileFactory factory;
  private final RuleActivator activator;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final ProfileDefinition[] definitions;

  public QProfileResetImpl(DbClient db, RuleActivator activator, ActiveRuleIndexer activeRuleIndexer, QProfileFactory factory, ProfileDefinition... definitions) {
    this.db = db;
    this.activator = activator;
    this.activeRuleIndexer = activeRuleIndexer;
    this.factory = factory;
    this.definitions = definitions;
  }

  public QProfileResetImpl(DbClient db, RuleActivator activator, ActiveRuleIndexer activeRuleIndexer, QProfileFactory factory) {
    this(db, activator, activeRuleIndexer, factory, new ProfileDefinition[0]);
  }

  @Override
  public void resetLanguage(DbSession dbSession, OrganizationDto organization, String language) {
    ListMultimap<QProfileName, RulesProfile> profilesByName = loadDefinitionsGroupedByName(language);
    for (Map.Entry<QProfileName, Collection<RulesProfile>> entry : profilesByName.asMap().entrySet()) {
      QProfileName profileName = entry.getKey();
      QualityProfileDto profile = factory.getOrCreate(dbSession, organization, profileName);
      List<RuleActivation> activations = Lists.newArrayList();
      for (RulesProfile def : entry.getValue()) {
        for (ActiveRule activeRule : def.getActiveRules()) {
          activations.add(getRuleActivation(dbSession, activeRule));
        }
      }
      reset(dbSession, profile, activations);
    }
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

  private ListMultimap<QProfileName, RulesProfile> loadDefinitionsGroupedByName(String language) {
    ListMultimap<QProfileName, RulesProfile> profilesByName = ArrayListMultimap.create();
    for (ProfileDefinition definition : definitions) {
      ValidationMessages validation = ValidationMessages.create();
      RulesProfile profile = definition.createProfile(validation);
      if (language.equals(profile.getLanguage())) {
        processValidationMessages(validation);
        profilesByName.put(new QProfileName(profile.getLanguage(), profile.getName()), profile);
      }
    }
    return profilesByName;
  }

  private static void processValidationMessages(ValidationMessages messages) {
    checkRequest(messages.getErrors().isEmpty(), messages.getErrors());
  }
}
