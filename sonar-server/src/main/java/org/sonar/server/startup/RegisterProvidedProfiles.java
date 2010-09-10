/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.ProfilePrototype;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleProvider;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.rules.DeprecatedProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class RegisterProvidedProfiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterProvidedProfiles.class);

  private RuleProvider ruleProvider;
  private DatabaseSessionFactory sessionFactory;
  private List<ProfileDefinition> definitions = new ArrayList<ProfileDefinition>();

  public RegisterProvidedProfiles(RuleProvider ruleProvider, DatabaseSessionFactory sessionFactory,
                                  DeprecatedProfiles deprecatedBridge, RegisterRules registerRulesBefore,
                                  ProfileDefinition[] definitions) {
    this.ruleProvider = ruleProvider;
    this.sessionFactory = sessionFactory;
    this.definitions.addAll(Arrays.asList(definitions));
    if (deprecatedBridge != null) {
      this.definitions.addAll(deprecatedBridge.getProfiles());
    }
  }

  public RegisterProvidedProfiles(RuleProvider ruleProvider, DatabaseSessionFactory sessionFactory,
                                  DeprecatedProfiles deprecatedBridge, RegisterRules registerRulesBefore) {
    this.ruleProvider = ruleProvider;
    this.sessionFactory = sessionFactory;
    if (deprecatedBridge != null) {
      this.definitions.addAll(deprecatedBridge.getProfiles());
    }
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load provided profiles");

    List<ProfilePrototype> prototypes = createPrototypes();
    DatabaseSession session = sessionFactory.getSession();
    deleteDeprecatedProfiles(prototypes, session);
    saveProvidedProfiles(prototypes, session);
    profiler.stop();
  }

  List<ProfilePrototype> createPrototypes() {
    List<ProfilePrototype> result = new ArrayList<ProfilePrototype>();
    for (ProfileDefinition definition : definitions) {
      result.add(definition.createPrototype());
    }
    return result;
  }

  void deleteDeprecatedProfiles(List<ProfilePrototype> prototypes, DatabaseSession session) {
    TimeProfiler profiler = new TimeProfiler().start("Delete deprecated profiles");
    List<RulesProfile> existingProfiles = session.getResults(RulesProfile.class, "provided", true);
    for (RulesProfile existingProfile : existingProfiles) {
      boolean isDeprecated = true;
      for (ProfilePrototype profile: prototypes) {
        if (StringUtils.equals(existingProfile.getName(), profile.getName()) && StringUtils.equals(existingProfile.getLanguage(), profile.getLanguage())) {
          isDeprecated = false;
          break;
        }
      }
      if (isDeprecated) {
        session.removeWithoutFlush(existingProfile);
      } else {
        for (ActiveRule activeRule : existingProfile.getActiveRules()) {
          session.removeWithoutFlush(activeRule);
        }
        existingProfile.setActiveRules(new ArrayList<ActiveRule>());
        session.saveWithoutFlush(existingProfile);
      }
      session.commit();
    }
    profiler.stop();
  }


  void saveProvidedProfiles(List<ProfilePrototype> prototypes, DatabaseSession session) {
    for (ProfilePrototype prototype : prototypes) {
      TimeProfiler profiler = new TimeProfiler().start("Save profile " + prototype);
      RulesProfile profile = findOrCreate(prototype, session);

      for (ProfilePrototype.RulePrototype rulePrototype : prototype.getRules()) {
        Rule rule = findRule(rulePrototype);
        if (rule == null) {
          LOGGER.warn("The profile " + prototype + " defines an unknown rule: " + rulePrototype);

        } else {
          ActiveRule activeRule = profile.activateRule(rule, rulePrototype.getPriority());
          for (Map.Entry<String, String> entry : rulePrototype.getParameters().entrySet()) {
            activeRule.setParameter(entry.getKey(), entry.getValue());
          }
        }
      }
      session.saveWithoutFlush(profile);
      session.commit();
      profiler.stop();
    }
  }

  private Rule findRule(ProfilePrototype.RulePrototype rulePrototype) {
    if (StringUtils.isNotBlank(rulePrototype.getKey())) {
      return ruleProvider.findByKey(rulePrototype.getRepositoryKey(), rulePrototype.getKey());
    }
    if (StringUtils.isNotBlank(rulePrototype.getConfigKey())) {
      return ruleProvider.find(RuleQuery.create().withRepositoryKey(rulePrototype.getRepositoryKey()).withConfigKey(rulePrototype.getConfigKey()));
    }
    return null;
  }

  private RulesProfile findOrCreate(ProfilePrototype prototype, DatabaseSession session) {
    RulesProfile profile = session.getSingleResult(RulesProfile.class, "name", prototype.getName(), "language", prototype.getLanguage());
    if (profile == null) {
      profile = RulesProfile.create(prototype.getName(), prototype.getLanguage());
      profile.setProvided(true);
      profile.setDefaultProfile(false);
    }
    return profile;
  }

}
