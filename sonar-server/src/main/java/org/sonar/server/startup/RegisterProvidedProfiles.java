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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.rules.DeprecatedProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RegisterProvidedProfiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterProvidedProfiles.class);

  private DatabaseSessionFactory sessionFactory;
  private List<ProfileDefinition> definitions = Lists.newArrayList();
  private DeprecatedProfiles deprecatedProfiles = null;
  private RuleFinder ruleFinder;

  public RegisterProvidedProfiles(RuleFinder ruleFinder, DatabaseSessionFactory sessionFactory,
                                  DeprecatedProfiles deprecatedBridge, RegisterRules registerRulesBefore,
                                  ProfileDefinition[] definitions) {
    this.ruleFinder = ruleFinder;
    this.sessionFactory = sessionFactory;
    this.definitions.addAll(Arrays.asList(definitions));
    this.deprecatedProfiles = deprecatedBridge;
  }

  public RegisterProvidedProfiles(RuleFinder ruleFinder, DatabaseSessionFactory sessionFactory,
                                  DeprecatedProfiles deprecatedBridge, RegisterRules registerRulesBefore) {
    this.ruleFinder = ruleFinder;
    this.sessionFactory = sessionFactory;
    this.deprecatedProfiles = deprecatedBridge;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load provided profiles");

    List<RulesProfile> profiles = createProfiles();
    DatabaseSession session = sessionFactory.getSession();
    cleanProvidedProfiles(profiles, session);
    saveProvidedProfiles(profiles, session);
    profiler.stop();
  }

  List<RulesProfile> createProfiles() {
    List<RulesProfile> result = Lists.newArrayList();

    // this must not be moved in the constructor, because rules are still not saved
    definitions.addAll(deprecatedProfiles.getProfiles());

    for (ProfileDefinition definition : definitions) {
      ValidationMessages validation = ValidationMessages.create();
      RulesProfile profile = definition.createProfile(validation);
      validation.log(LOGGER);
      if (profile != null && !validation.hasErrors()) {
        result.add(profile);
      }
    }
    return result;
  }

  void cleanProvidedProfiles(List<RulesProfile> profiles, DatabaseSession session) {
    TimeProfiler profiler = new TimeProfiler().start("Clean provided profiles");
    List<RulesProfile> existingProfiles = session.getResults(RulesProfile.class, "provided", true);
    for (RulesProfile existingProfile : existingProfiles) {
      boolean isDeprecated = true;
      for (RulesProfile profile : profiles) {
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


  void saveProvidedProfiles(List<RulesProfile> profiles, DatabaseSession session) {
    for (RulesProfile profile : profiles) {
      TimeProfiler profiler = new TimeProfiler().start("Save profile " + profile);
      RulesProfile persistedProfile = findOrCreate(profile.getName(), profile.getLanguage(), session);

      for (ActiveRule activeRule : profile.getActiveRules()) {
        Rule rule = getPersistedRule(activeRule);
        ActiveRule persistedRule = persistedProfile.activateRule(rule, activeRule.getPriority());
        for (RuleParam param : rule.getParams()) {
          String value = StringUtils.defaultString(activeRule.getParameter(param.getKey()), param.getDefaultValue());
          if (value != null) {
            persistedRule.setParameter(param.getKey(), value);
          }
        }
      }

      session.saveWithoutFlush(persistedProfile);
      session.commit();
      profiler.stop();
    }

  }

  Rule getPersistedRule(ActiveRule activeRule) {
    Rule rule = activeRule.getRule();
    if (rule!=null && rule.getId()==null) {
      if (rule.getKey()!=null) {
        rule = ruleFinder.findByKey(rule.getRepositoryKey(), rule.getKey());

      } else if (rule.getConfigKey()!=null) {
        rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(rule.getRepositoryKey()).withConfigKey(rule.getConfigKey()));
      }
    }
    return rule;
  }

  private RulesProfile findOrCreate(String name, String language, DatabaseSession session) {
    RulesProfile profile = session.getSingleResult(RulesProfile.class, "name", name, "language", language);
    if (profile == null) {
      profile = RulesProfile.create(name, language);
      profile.setProvided(true);
      profile.setDefaultProfile(false);
    }
    return profile;
  }

}
