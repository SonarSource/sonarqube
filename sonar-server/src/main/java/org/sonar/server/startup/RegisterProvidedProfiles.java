/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import com.google.common.collect.Sets;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class RegisterProvidedProfiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterProvidedProfiles.class);

  private DatabaseSessionFactory sessionFactory;
  private List<ProfileDefinition> definitions = Lists.newArrayList();
  private RuleFinder ruleFinder;

  public RegisterProvidedProfiles(RuleFinder ruleFinder, DatabaseSessionFactory sessionFactory,// NOSONAR the parameter registerRulesBefore is unused must be declared for execution order of tasks
                                  RegisterRules registerRulesBefore,
                                  ProfileDefinition[] definitions) {
    this.ruleFinder = ruleFinder;
    this.sessionFactory = sessionFactory;
    this.definitions.addAll(Arrays.asList(definitions));
  }

  public RegisterProvidedProfiles(RuleFinder ruleFinder, DatabaseSessionFactory sessionFactory,// NOSONAR the parameter registerRulesBefore is unused must be declared for execution order of tasks
                                  RegisterRules registerRulesBefore) {
    this.ruleFinder = ruleFinder;
    this.sessionFactory = sessionFactory;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load provided profiles");

    List<RulesProfile> profiles = createProfiles();
    DatabaseSession session = sessionFactory.getSession();
    cleanProvidedProfiles(profiles, session);
    saveProvidedProfiles(profiles, session);
    session.commit();
    profiler.stop();
  }

  List<RulesProfile> createProfiles() {
    List<RulesProfile> result = Lists.newArrayList();
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

  private void cleanProvidedProfiles(List<RulesProfile> profiles, DatabaseSession session) {
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
    }
    profiler.stop();
  }

  private void saveProvidedProfiles(List<RulesProfile> profiles, DatabaseSession session) {
    Collection<String> languagesWithDefaultProfile = findLanguagesWithDefaultProfile(session);
    for (RulesProfile profile : profiles) {
      TimeProfiler profiler = new TimeProfiler().start("Save profile " + profile);
      RulesProfile persistedProfile = findOrCreate(profile, session, languagesWithDefaultProfile.contains(profile.getLanguage()));

      for (ActiveRule activeRule : profile.getActiveRules()) {
        Rule rule = getPersistedRule(activeRule);
        ActiveRule persistedRule = persistedProfile.activateRule(rule, activeRule.getSeverity());
        for (RuleParam param : rule.getParams()) {
          String value = StringUtils.defaultString(activeRule.getParameter(param.getKey()), param.getDefaultValue());
          if (value != null) {
            persistedRule.setParameter(param.getKey(), value);
          }
        }
      }

      session.saveWithoutFlush(persistedProfile);
      profiler.stop();
    }
  }

  private Collection<String> findLanguagesWithDefaultProfile(DatabaseSession session) {
    Set<String> languagesWithDefaultProfile = Sets.newHashSet();
    List<RulesProfile> defaultProfiles = session.getResults(RulesProfile.class, "defaultProfile", true);
    for (RulesProfile defaultProfile : defaultProfiles) {
      languagesWithDefaultProfile.add(defaultProfile.getLanguage());
    }
    return languagesWithDefaultProfile;
  }

  private Rule getPersistedRule(ActiveRule activeRule) {
    Rule rule = activeRule.getRule();
    if (rule != null && rule.getId() == null) {
      if (rule.getKey() != null) {
        rule = ruleFinder.findByKey(rule.getRepositoryKey(), rule.getKey());

      } else if (rule.getConfigKey() != null) {
        rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(rule.getRepositoryKey()).withConfigKey(rule.getConfigKey()));
      }
    }
    return rule;
  }

  private RulesProfile findOrCreate(RulesProfile profile, DatabaseSession session, boolean defaultProfileAlreadyExist) {
    RulesProfile persistedProfile = session.getSingleResult(RulesProfile.class, "name", profile.getName(), "language", profile.getLanguage());
    if (persistedProfile == null) {
      persistedProfile = RulesProfile.create(profile.getName(), profile.getLanguage());
      persistedProfile.setProvided(true);
      if (!defaultProfileAlreadyExist) {
        persistedProfile.setDefaultProfile(profile.getDefaultProfile());
      }
    }
    return persistedProfile;
  }

}
