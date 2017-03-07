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
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.lowerCase;

/**
 * Synchronize Quality profiles during server startup
 */
@ServerSide
public class RegisterQualityProfiles {

  private static final Logger LOGGER = Loggers.get(RegisterQualityProfiles.class);
  private static final String DEFAULT_PROFILE_NAME = "Sonar way";

  private final List<ProfileDefinition> definitions;
  private final DbClient dbClient;
  private final QProfileFactory profileFactory;
  private final RuleActivator ruleActivator;
  private final Languages languages;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  /**
   * To be kept when no ProfileDefinition are injected
   */
  public RegisterQualityProfiles(DbClient dbClient,
    QProfileFactory profileFactory, CachingRuleActivator ruleActivator, Languages languages, ActiveRuleIndexer activeRuleIndexer,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this(dbClient, profileFactory, ruleActivator, Collections.emptyList(), languages, activeRuleIndexer, defaultOrganizationProvider);
  }

  public RegisterQualityProfiles(DbClient dbClient,
    QProfileFactory profileFactory, CachingRuleActivator ruleActivator,
    List<ProfileDefinition> definitions, Languages languages, ActiveRuleIndexer activeRuleIndexer,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.profileFactory = profileFactory;
    this.ruleActivator = ruleActivator;
    this.definitions = definitions;
    this.languages = languages;
    this.activeRuleIndexer = activeRuleIndexer;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register quality profiles");
    List<ActiveRuleChange> changes = new ArrayList<>();
    ListMultimap<String, RulesProfile> profilesByLanguage = profilesByLanguage();
    validateAndClean(profilesByLanguage);
    try (DbSession session = dbClient.openSession(false)) {
      Multimaps.asMap(profilesByLanguage).entrySet()
        .forEach(entry -> registerProfilesForLanguage(session, entry.getKey(), entry.getValue(), changes));
      activeRuleIndexer.index(changes);
      profiler.stopDebug();
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
        byLang.put(lowerCase(profile.getLanguage(), Locale.ENGLISH), profile);
      }
    }
    return byLang;
  }

  private void validateAndClean(ListMultimap<String, RulesProfile> byLang) {
    byLang.asMap().entrySet()
      .removeIf(entry -> {
        String language = entry.getKey();
        if (languages.get(language) == null) {
          LOGGER.info("Language {} is not installed, related Quality profiles are ignored", language);
          return true;
        }
        Collection<RulesProfile> profiles = entry.getValue();
        if (profiles.isEmpty()) {
          LOGGER.warn("No Quality profiles defined for language: {}", language);
          return true;
        }
        profiles.forEach(profile -> checkArgument(isNotEmpty(profile.getName()), "Profile name can not be blank"));
        Set<String> defaultProfileNames = defaultProfileNames(profiles);
        checkState(defaultProfileNames.size() <= 1, "Several Quality profiles are flagged as default for the language %s: %s", language, defaultProfileNames);
        return false;
      });
  }

  private void registerProfilesForLanguage(DbSession session, String language, List<RulesProfile> defs, List<ActiveRuleChange> changes) {
    OrganizationDto organization = dbClient.organizationDao().selectByUuid(session, defaultOrganizationProvider.get().getUuid())
      .orElseThrow(() -> new IllegalStateException("Failed to retrieve default organization"));
    defs.stream().collect(Collectors.index(RulesProfile::getName)).asMap().entrySet()
      .forEach(entry -> {
        String name = entry.getKey();
        QProfileName profileName = new QProfileName(language, name);
        if (shouldRegister(profileName, session)) {
          register(session, organization, profileName, entry.getValue(), changes);
        }
      });
    setDefault(language, organization, defs, session);
    session.commit();
  }

  private void register(DbSession session, OrganizationDto organization, QProfileName name, Collection<RulesProfile> profiles, List<ActiveRuleChange> changes) {
    LOGGER.info("Register profile " + name);

    QualityProfileDto profileDto = dbClient.qualityProfileDao().selectByNameAndLanguage(organization, name.getName(), name.getLanguage(), session);
    if (profileDto != null) {
      changes.addAll(profileFactory.delete(session, profileDto.getKey(), true));
    }
    QualityProfileDto newQProfileDto = profileFactory.create(session, organization, name);
    for (RulesProfile profile : profiles) {
      for (org.sonar.api.rules.ActiveRule activeRule : profile.getActiveRules()) {
        RuleKey ruleKey = RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey());
        RuleActivation activation = new RuleActivation(ruleKey);
        activation.setSeverity(activeRule.getSeverity() != null ? activeRule.getSeverity().name() : null);
        for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
          activation.setParameter(param.getKey(), param.getValue());
        }
        changes.addAll(ruleActivator.activate(session, activation, newQProfileDto));
      }
    }

    LoadedTemplateDto template = new LoadedTemplateDto(templateKey(name), LoadedTemplateDto.QUALITY_PROFILE_TYPE);
    dbClient.loadedTemplateDao().insert(template, session);
    session.commit();
  }

  private void setDefault(String language, OrganizationDto organization, List<RulesProfile> profileDefs, DbSession session) {
    QualityProfileDto currentDefault = dbClient.qualityProfileDao().selectDefaultProfile(session, organization, language);

    if (currentDefault == null) {
      String defaultProfileName = nameOfDefaultProfile(profileDefs);
      LOGGER.info("Set default " + language + " profile: " + defaultProfileName);
      QualityProfileDto newDefaultProfile = dbClient.qualityProfileDao().selectByNameAndLanguage(defaultProfileName, language, session);
      if (newDefaultProfile == null) {
        // Must not happen, we just registered it
        throw new IllegalStateException("Could not find declared default profile '%s' for language '%s'");
      } else {
        dbClient.qualityProfileDao().update(session, newDefaultProfile.setDefault(true));
      }
    }
  }

  private static String nameOfDefaultProfile(List<RulesProfile> profiles) {
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
    return profiles.stream()
      .filter(RulesProfile::getDefaultProfile)
      .map(RulesProfile::getName)
      .collect(Collectors.toSet());
  }

  private boolean shouldRegister(QProfileName key, DbSession session) {
    // check if the profile was already registered in the past
    return dbClient.loadedTemplateDao()
      .countByTypeAndKey(LoadedTemplateDto.QUALITY_PROFILE_TYPE, templateKey(key), session) == 0;
  }

  static String templateKey(QProfileName key) {
    return lowerCase(key.getLanguage(), Locale.ENGLISH) + ":" + key.getName();
  }
}
