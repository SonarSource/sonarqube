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
package org.sonar.server.startup;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.qualityprofile.*;
import org.sonar.server.rule.RegisterRules;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.*;

public class RegisterQualityProfiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterQualityProfiles.class);
  private static final String DEFAULT_PROFILE_NAME = "Sonar way";

  private final LoadedTemplateDao loadedTemplateDao;
  private final QProfileBackup qProfileBackup;
  private final QProfileOperations qProfileOperations;
  private final QProfileLookup qProfileLookup;
  private final ESActiveRule esActiveRule;
  private final PersistentSettings settings;
  private final List<ProfileDefinition> definitions;
  private final DatabaseSessionFactory sessionFactory;
  private final MyBatis myBatis;

  public RegisterQualityProfiles(DatabaseSessionFactory sessionFactory,
                                 MyBatis myBatis,
                                 PersistentSettings settings,
                                 ESActiveRule esActiveRule,
                                 LoadedTemplateDao loadedTemplateDao,
                                 QProfileBackup qProfileBackup,
                                 QProfileOperations qProfileOperations,
                                 QProfileLookup qProfileLookup,
                                 RegisterRules registerRulesBefore) {
    this(sessionFactory, myBatis, settings, esActiveRule, loadedTemplateDao, qProfileBackup, qProfileOperations, qProfileLookup, registerRulesBefore,
      Collections.<ProfileDefinition>emptyList());
  }

  public RegisterQualityProfiles(DatabaseSessionFactory sessionFactory,
                                 MyBatis myBatis,
                                 PersistentSettings settings,
                                 ESActiveRule esActiveRule,
                                 LoadedTemplateDao loadedTemplateDao,
                                 QProfileBackup qProfileBackup,
                                 QProfileOperations qProfileOperations,
                                 QProfileLookup qProfileLookup,
                                 RegisterRules registerRulesBefore,
                                 List<ProfileDefinition> definitions) {
    this.sessionFactory = sessionFactory;
    this.myBatis = myBatis;
    this.settings = settings;
    this.esActiveRule = esActiveRule;
    this.qProfileBackup = qProfileBackup;
    this.qProfileOperations = qProfileOperations;
    this.qProfileLookup = qProfileLookup;
    this.definitions = definitions;
    this.loadedTemplateDao = loadedTemplateDao;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler(LOGGER).start("Register Quality Profiles");

    // Hibernate session can contain an invalid cache of rules.
    // As long ProfileDefinition API will be used, then we'll have to use this commit as Hibernate is used by plugin to load rules when creating their profiles.
    sessionFactory.getSession().commit();

    SqlSession session = myBatis.openSession();
    try {
      ListMultimap<String, RulesProfile> profilesByLanguage = profilesByLanguage();
      for (String language : profilesByLanguage.keySet()) {
        List<RulesProfile> profiles = profilesByLanguage.get(language);
        verifyLanguage(language, profiles);

        for (Map.Entry<String, Collection<RulesProfile>> entry : profilesByName(profiles).entrySet()) {
          String name = entry.getKey();
          if (shouldRegister(language, name, session)) {
            register(language, name, entry.getValue(), session);
          }
        }
        setDefault(language, profiles, session);
      }
      session.commit();
      esActiveRule.bulkRegisterActiveRules();
    } finally {
      MyBatis.closeQuietly(session);
      profiler.stop();
    }
  }

  private static void verifyLanguage(String language, List<RulesProfile> profiles) {
    if (profiles.isEmpty()) {
      LOGGER.warn("No Quality Profile defined for language: " + language);
    }

    Set<String> defaultProfileNames = defaultProfileNames(profiles);
    if (defaultProfileNames.size() > 1) {
      throw new SonarException("Several Quality Profiles are flagged as default for the language " + language + ": " + defaultProfileNames);
    }
  }

  private void register(String language, String name, Collection<RulesProfile> profiles, SqlSession session) {
    LOGGER.info("Register " + language + " profile: " + name);

    QProfile profile = qProfileLookup.profile(name, language, session);
    if (profile != null) {
      qProfileOperations.deleteProfile(profile.id(), session);
    }
    profile = qProfileOperations.newProfile(name, language, true, UserSession.get(), session);

    for (RulesProfile currentRulesProfile : profiles) {
      qProfileBackup.restoreFromActiveRules(profile, currentRulesProfile, session);
    }

    loadedTemplateDao.insert(new LoadedTemplateDto(templateKey(language, name), LoadedTemplateDto.QUALITY_PROFILE_TYPE), session);
  }

  private void setDefault(String language, List<RulesProfile> profiles, SqlSession session) {
    String propertyKey = "sonar.profile." + language;
    if (settings.getString(propertyKey) == null) {
      String defaultProfileName = defaultProfileName(profiles);
      LOGGER.info("Set default " + language + " profile: " + defaultProfileName);
      settings.saveProperty(propertyKey, defaultProfileName);
    }
  }

  /**
   * @return profiles by language
   */
  private ListMultimap<String, RulesProfile> profilesByLanguage() {
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

  private static Map<String, Collection<RulesProfile>> profilesByName(List<RulesProfile> profiles) {
    return Multimaps.index(profiles, new Function<RulesProfile, String>() {
      public String apply(@Nullable RulesProfile profile) {
        return profile != null ? profile.getName() : null;
      }
    }).asMap();
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

  private boolean shouldRegister(String language, String profileName, SqlSession session) {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.QUALITY_PROFILE_TYPE, templateKey(language, profileName), session) == 0;
  }

  private static String templateKey(String language, String profileName) {
    return StringUtils.lowerCase(language) + ":" + profileName;
  }
}
