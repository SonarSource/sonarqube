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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.*;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegisterQualityProfiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegisterQualityProfiles.class);
  private static final String DEFAULT_PROFILE_NAME = "Sonar way";

  private final PersistentSettings settings;
  private final List<ProfileDefinition> definitions;
  private final DefaultProfilesCache defaultProfilesCache;
  private final DatabaseSessionFactory sessionFactory;
  private final DbClient dbClient;

  public RegisterQualityProfiles(DatabaseSessionFactory sessionFactory,
                                 PersistentSettings settings,
                                 DefaultProfilesCache defaultProfilesCache, DbClient dbClient) {
    this(sessionFactory, settings, defaultProfilesCache, dbClient,
      Collections.<ProfileDefinition>emptyList());
  }

  public RegisterQualityProfiles(DatabaseSessionFactory sessionFactory,
                                 PersistentSettings settings,
                                 DefaultProfilesCache defaultProfilesCache,
                                 DbClient dbClient,
                                 List<ProfileDefinition> definitions) {
    this.sessionFactory = sessionFactory;
    this.settings = settings;
    this.defaultProfilesCache = defaultProfilesCache;
    this.definitions = definitions;
    this.dbClient = dbClient;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler(LOGGER).start("Register Quality Profiles");

    // Hibernate session can contain an invalid cache of rules.
    // As long ProfileDefinition API will be used, then we'll have to use this commit as Hibernate is used by plugin to load rules when creating their profiles.
    sessionFactory.getSession().commit();

    DbSession session = dbClient.openSession(false);
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
          defaultProfilesCache.put(language, name);
        }
        setDefault(language, profiles, session);
      }
      session.commit();
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
      throw new IllegalStateException("Several Quality Profiles are flagged as default for the language " + language + ": " + defaultProfileNames);
    }
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private QualityProfileDto newQualityProfileDto(String name, String language, DbSession session) {
    checkPermission(UserSession.get());
      if (dbClient.qualityProfileDao().selectByNameAndLanguage(name, language, session) != null) {
        throw BadRequestException.ofL10n("quality_profiles.profile_x_already_exists", name);
      }

    QualityProfileDto profile =  QualityProfileDto.createFor(name, language)
      .setVersion(1).setUsed(false);
    dbClient.qualityProfileDao().insert(profile, session);
    return profile;
  }

  private void register(String language, String name, Collection<RulesProfile> profiles, DbSession session) {
    LOGGER.info("Register " + language + " profile: " + name);

    QualityProfileDto profile = dbClient.qualityProfileDao().selectByNameAndLanguage(name, language, session);
    if (profile != null) {
      dbClient.activeRuleDao().deleteByProfileKey(profile.getKey(), session);
      dbClient.qualityProfileDao().delete(profile, session);
    }
    profile = newQualityProfileDto(name, language, session);

    for (RulesProfile currentRulesProfile : profiles) {
      //TODO trapped on legacy Hibernate object.
      for (org.sonar.api.rules.ActiveRule activeRule : currentRulesProfile.getActiveRules()) {
        RuleKey ruleKey = RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey());
        RuleDto rule = dbClient.ruleDao().getByKey(ruleKey, session);
        if (rule == null) {
          throw new NotFoundException(String.format("Rule '%s' does not exists.", ruleKey));
        }

        ActiveRuleDto activeRuleDto = dbClient.activeRuleDao().createActiveRule(
          profile.getKey(), ruleKey, activeRule.getSeverity().name(), session);

        //TODO trapped on legacy Hibernate object.
        for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
          String paramKey = param.getKey();
          String value = param.getValue();
          ActiveRuleParamDto paramDto = dbClient.activeRuleDao()
            .getParamsByActiveRuleAndKey(activeRuleDto, paramKey, session);
          if (value != null && !paramDto.getValue().equals(value)) {
            paramDto.setValue(value);
            dbClient.activeRuleDao().updateParam(activeRuleDto, paramDto, session);
          }
        }
      }
    }

    dbClient.getDao(LoadedTemplateDao.class)
      .insert(new LoadedTemplateDto(templateKey(language, name), LoadedTemplateDto.QUALITY_PROFILE_TYPE), session);
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
    return dbClient.getDao(LoadedTemplateDao.class)
      .countByTypeAndKey(LoadedTemplateDto.QUALITY_PROFILE_TYPE, templateKey(language, profileName), session) == 0;
  }

  private static String templateKey(String language, String profileName) {
    return StringUtils.lowerCase(language) + ":" + profileName;
  }
}
