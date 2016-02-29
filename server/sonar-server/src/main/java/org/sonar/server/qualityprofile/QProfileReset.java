/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
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
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

@ServerSide
public class QProfileReset {

  private final DbClient db;
  private final QProfileFactory factory;
  private final RuleActivator activator;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final BuiltInProfiles builtInProfiles;
  private final ProfileDefinition[] definitions;

  public QProfileReset(DbClient db, RuleActivator activator, ActiveRuleIndexer activeRuleIndexer, BuiltInProfiles builtInProfiles, QProfileFactory factory,
    ProfileDefinition[] definitions) {
    this.db = db;
    this.activator = activator;
    this.activeRuleIndexer = activeRuleIndexer;
    this.builtInProfiles = builtInProfiles;
    this.factory = factory;
    this.definitions = definitions;
  }

  public QProfileReset(DbClient db, RuleActivator activator, BuiltInProfiles builtInProfiles, QProfileFactory factory, ActiveRuleIndexer activeRuleIndexer) {
    this(db, activator, activeRuleIndexer, builtInProfiles, factory, new ProfileDefinition[0]);
  }

  public Collection<String> builtInProfileNamesForLanguage(String language) {
    return builtInProfiles.byLanguage(language);
  }

  /**
   * Reset built-in profiles for the given language. Missing profiles are created and
   * existing ones are updated.
   */
  void resetLanguage(String language) {
    DbSession dbSession = db.openSession(false);
    try {
      ListMultimap<QProfileName, RulesProfile> profilesByName = loadDefinitionsGroupedByName(language);
      for (Map.Entry<QProfileName, Collection<RulesProfile>> entry : profilesByName.asMap().entrySet()) {
        QProfileName profileName = entry.getKey();
        QualityProfileDto profile = factory.getOrCreate(dbSession, profileName);
        List<RuleActivation> activations = Lists.newArrayList();
        for (RulesProfile def : entry.getValue()) {
          for (ActiveRule activeRule : def.getActiveRules()) {
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
            activations.add(activation);
          }
        }
        doReset(dbSession, profile, activations);
      }
    } finally {
      dbSession.close();
    }
  }

  /**
   * Reset the profile, which is created if it does not exist
   */
  BulkChangeResult reset(QProfileName profileName, Collection<RuleActivation> activations) {
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto profile = factory.getOrCreate(dbSession, profileName);
      return doReset(dbSession, profile, activations);
    } finally {
      dbSession.close();
    }
  }

  /**
   * @param dbSession
   * @param profile must exist
   */
  private BulkChangeResult doReset(DbSession dbSession, QualityProfileDto profile, Collection<RuleActivation> activations) {
    Preconditions.checkNotNull(profile.getId(), "Quality profile must be persisted");
    BulkChangeResult result = new BulkChangeResult(profile);
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
        result.getErrors().add(e.errors());
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

  private void processValidationMessages(ValidationMessages messages) {
    if (!messages.getErrors().isEmpty()) {
      throw new BadRequestException(messages);
    }
  }
}
