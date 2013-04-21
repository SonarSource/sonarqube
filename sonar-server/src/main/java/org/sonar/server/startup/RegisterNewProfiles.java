/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.platform.PersistentSettings;

import java.util.*;

public class RegisterNewProfiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterNewProfiles.class);
  private static final String DEFAULT_PROFILE_NAME = "Sonar way";

  private final List<ProfileDefinition> definitions;
  private final LoadedTemplateDao loadedTemplateDao;
  private final RuleFinder ruleFinder;
  private final DatabaseSessionFactory sessionFactory;
  private final PersistentSettings settings;
  private DatabaseSession session = null;

  public RegisterNewProfiles(List<ProfileDefinition> definitions,
                             PersistentSettings settings,
                             RuleFinder ruleFinder,
                             LoadedTemplateDao loadedTemplateDao,
                             DatabaseSessionFactory sessionFactory,
                             RegisterRules registerRulesBefore) {
    this.settings = settings;
    this.ruleFinder = ruleFinder;
    this.definitions = definitions;
    this.loadedTemplateDao = loadedTemplateDao;
    this.sessionFactory = sessionFactory;
  }

  public RegisterNewProfiles(PersistentSettings settings,
                             RuleFinder ruleFinder,
                             LoadedTemplateDao loadedTemplateDao,
                             DatabaseSessionFactory sessionFactory,
                             RegisterRules registerRulesBefore) {
    this(Collections.<ProfileDefinition>emptyList(), settings, ruleFinder, loadedTemplateDao, sessionFactory, registerRulesBefore);
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler(LOGGER).start("Register Quality Profiles");
    session = sessionFactory.getSession();
    ListMultimap<String, RulesProfile> profilesByLanguage = loadDefinitions();
    for (String language : profilesByLanguage.keySet()) {
      List<RulesProfile> profiles = profilesByLanguage.get(language);
      verifyLanguage(language, profiles);

      for (Map.Entry<String, Collection<RulesProfile>> entry : groupByName(profiles).entrySet()) {
        String name = entry.getKey();
        if (shouldRegister(language, name)) {
          register(language, name, entry.getValue());
        }
      }

      setDefault(language, profiles);
    }
    session.commit();
    profiler.stop();
  }

  private void setDefault(String language, List<RulesProfile> profiles) {
    String propertyKey = "sonar.profile." + language;
    if (settings.getString(propertyKey) == null) {
      String defaultProfileName = defaultProfileName(profiles);
      LOGGER.info("Set default " + language + " profile: " + defaultProfileName);
      settings.saveProperty(propertyKey, defaultProfileName);
    }
  }

  private Map<String, Collection<RulesProfile>> groupByName(List<RulesProfile> profiles) {
    return Multimaps.index(profiles,
      new Function<RulesProfile, String>() {
        public String apply(RulesProfile profile) {
          return profile.getName();
        }
      }).asMap();
  }

  private boolean shouldRegister(String language, String profileName) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.QUALITY_PROFILE_TYPE, templateKey(language, profileName)) == 0;
  }

  private static String templateKey(String language, String profileName) {
    return StringUtils.lowerCase(language) + ":" + profileName;
  }

  private void register(String language, String name, Collection<RulesProfile> profiles) {
    LOGGER.info("Register " + language + " profile: " + name);
    clean(language, name);
    insert(language, name, profiles);
    loadedTemplateDao.insert(new LoadedTemplateDto(templateKey(language, name), LoadedTemplateDto.QUALITY_PROFILE_TYPE));
  }


  private void verifyLanguage(String language, List<RulesProfile> profiles) {
    if (profiles.isEmpty()) {
      LOGGER.warn("No Quality Profile defined for language: " + language);
    }

    Set<String> defaultProfileNames = defaultProfileNames(profiles);
    if (defaultProfileNames.size() > 1) {
      throw new SonarException("Several Quality Profiles are flagged as default for the language " + language + ": " +
        defaultProfileNames);
    }
  }

  /**
   * @return profiles by language
   */
  private ListMultimap<String, RulesProfile> loadDefinitions() {
    ListMultimap<String, RulesProfile> byLang = ArrayListMultimap.create();
    for (ProfileDefinition definition : definitions) {
      ValidationMessages validation = ValidationMessages.create();
      RulesProfile profile = definition.createProfile(validation);
      validation.log(LOGGER);
      if (profile != null && !validation.hasErrors()) {
        byLang.put(StringUtils.lowerCase(profile.getLanguage()), profile);
      }
    }
    return byLang;
  }

  private static String defaultProfileName(List<RulesProfile> profiles) {
    String defaultName = null;
    boolean hasSonarWay = false;

    for (RulesProfile profile : profiles) {
      if (profile.getDefaultProfile()) {
        defaultName = profile.getName();
      } else if (DEFAULT_PROFILE_NAME.equals(profile.getName())) {
        hasSonarWay = true;
      }
    }

    if (StringUtils.isBlank(defaultName) && !hasSonarWay && !profiles.isEmpty()) {
      defaultName = profiles.get(0).getName();
    }

    return StringUtils.defaultIfBlank(defaultName, DEFAULT_PROFILE_NAME);
  }

  private static Set<String> defaultProfileNames(Collection<RulesProfile> profiles) {
    Set<String> names = Sets.newHashSet();
    for (RulesProfile profile : profiles) {
      if (profile.getDefaultProfile()) {
        names.add(profile.getName());
      }
    }
    return names;
  }

  //
  // PERSISTENCE
  //

  private void insert(String language, String name, Collection<RulesProfile> profiles) {
    RulesProfile persisted = RulesProfile.create(name, language);
    for (RulesProfile profile : profiles) {
      for (ActiveRule activeRule : profile.getActiveRules()) {
        Rule rule = persistedRule(activeRule);
        ActiveRule persistedActiveRule = persisted.activateRule(rule, activeRule.getSeverity());
        for (RuleParam param : rule.getParams()) {
          String value = StringUtils.defaultString(activeRule.getParameter(param.getKey()), param.getDefaultValue());
          if (value != null) {
            persistedActiveRule.setParameter(param.getKey(), value);
          }
        }
      }
    }
    session.saveWithoutFlush(persisted);
  }

  private Rule persistedRule(ActiveRule activeRule) {
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

  private void clean(String language, String name) {
    List<RulesProfile> existingProfiles = session.getResults(RulesProfile.class, "language", language, "name", name);
    for (RulesProfile profile : existingProfiles) {
      session.removeWithoutFlush(profile);
    }
  }
}
