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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileReset implements ServerComponent {

  private final DbClient db;
  private final RuleActivator activator;
  private final BuiltInProfiles builtInProfiles;
  private final ProfileDefinition[] definitions;

  public QProfileReset(DbClient db, RuleActivator activator, BuiltInProfiles builtInProfiles, ProfileDefinition[] definitions) {
    this.db = db;
    this.activator = activator;
    this.builtInProfiles = builtInProfiles;
    this.definitions = definitions;
  }

  public QProfileReset(DbClient db, RuleActivator activator, BuiltInProfiles builtInProfiles) {
    this(db, activator, builtInProfiles, new ProfileDefinition[0]);
  }

  public Collection<String> builtInProfileNamesForLanguage(String language) {
    return builtInProfiles.byLanguage(language);
  }

  void resetLanguage(String language) {
    ListMultimap<String, RulesProfile> profilesByName = loadDefinitionsGroupedByName(language);
    for (Map.Entry<String, Collection<RulesProfile>> entry : profilesByName.asMap().entrySet()) {
      QualityProfileKey profileKey = QualityProfileKey.of(entry.getKey(), language);
      List<RuleActivation> activations = Lists.newArrayList();
      for (RulesProfile def : entry.getValue()) {
        for (ActiveRule activeRule : def.getActiveRules()) {
          RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey())));
          activation.setSeverity(activeRule.getSeverity().name());
          for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
            activation.setParameter(param.getParamKey(), param.getValue());
          }
          activations.add(activation);
        }
      }
      reset(profileKey, activations);
    }
  }

  /**
   * Create the profile if needed.
   */
  void reset(QualityProfileKey profileKey, Collection<RuleActivation> activations) {
    Set<RuleKey> rulesToDeactivate = Sets.newHashSet();
    DbSession dbSession = db.openSession(false);
    try {
      // find or create profile
      if (db.qualityProfileDao().getByKey(dbSession, profileKey) == null) {
        // create new profile
        db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(profileKey));
      } else {
        // already exists. Keep reference to all the activated rules before backup restore
        for (ActiveRuleDto activeRuleDto : db.activeRuleDao().findByProfileKey(dbSession, profileKey)) {
          if (activeRuleDto.getInheritance() == null) {
            // inherited rules can't be deactivated
            rulesToDeactivate.add(activeRuleDto.getKey().ruleKey());
          }
        }
      }

      for (RuleActivation activation : activations) {
        try {
          activator.activate(dbSession, activation);
          rulesToDeactivate.remove(activation.getKey().ruleKey());
        } catch (BadRequestException e) {
          // TODO should return warnings instead of logging warnings
          LoggerFactory.getLogger(getClass()).warn(e.getMessage());
        }
      }

      for (RuleKey ruleKey : rulesToDeactivate) {
        activator.deactivate(dbSession, ActiveRuleKey.of(profileKey, ruleKey));
      }
      dbSession.commit();

    } finally {
      dbSession.close();
    }
  }

  private ListMultimap<String, RulesProfile> loadDefinitionsGroupedByName(String language) {
    ListMultimap<String, RulesProfile> profilesByName = ArrayListMultimap.create();
    for (ProfileDefinition definition : definitions) {
      ValidationMessages validation = ValidationMessages.create();
      RulesProfile profile = definition.createProfile(validation);
      if (language.equals(profile.getLanguage())) {
        processValidationMessages(validation);
        profilesByName.put(profile.getName(), profile);
      }
    }
    return profilesByName;
  }

  private void processValidationMessages(ValidationMessages messages) {
    if (!messages.getErrors().isEmpty()) {
      List<BadRequestException.Message> errors = newArrayList();
      for (String error : messages.getErrors()) {
        errors.add(BadRequestException.Message.of(error));
      }
      throw BadRequestException.of("Fail to restore profile", errors);
    }
  }
}

