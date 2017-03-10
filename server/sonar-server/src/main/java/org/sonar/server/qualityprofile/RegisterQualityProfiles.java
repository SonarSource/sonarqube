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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
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
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.lowerCase;
import static org.sonar.db.loadedtemplate.LoadedTemplateDto.QUALITY_PROFILE_TYPE;

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

    ListMultimap<String, RulesProfile> rulesProfilesByLanguage = buildRulesProfilesByLanguage();
    validateAndClean(rulesProfilesByLanguage);
    Map<String, List<QualityProfile>> qualityProfilesByLanguage = toQualityProfilesByLanguage(rulesProfilesByLanguage);
    if (qualityProfilesByLanguage.isEmpty()) {
      // do not open DB session if there is no quality profile to register
      profiler.stopDebug("No quality profile to register");
      return;
    }

    try (DbSession session = dbClient.openSession(false)) {
      OrganizationDto organization = dbClient.organizationDao().selectByUuid(session, defaultOrganizationProvider.get().getUuid())
        .orElseThrow(() -> new IllegalStateException("Failed to retrieve default organization"));
      List<ActiveRuleChange> changes = new ArrayList<>();
      qualityProfilesByLanguage.entrySet()
        .forEach(entry -> registerProfilesForLanguage(session, organization, entry.getValue(), changes));
      activeRuleIndexer.index(changes);
      profiler.stopDebug();
    }
  }

  /**
   * @return profiles by language
   */
  private ListMultimap<String, RulesProfile> buildRulesProfilesByLanguage() {
    ListMultimap<String, RulesProfile> byLang = ArrayListMultimap.create();
    for (ProfileDefinition definition : definitions) {
      ValidationMessages validation = ValidationMessages.create();
      RulesProfile profile = definition.createProfile(validation);
      validation.log(LOGGER);
      if (profile != null && !validation.hasErrors()) {
        checkArgument(isNotEmpty(profile.getName()), "Profile created by Definition %s can't have a blank name", definition);
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
        return false;
      });
  }

  private Map<String, List<QualityProfile>> toQualityProfilesByLanguage(ListMultimap<String, RulesProfile> rulesProfilesByLanguage) {
    Map<String, List<QualityProfile.Builder>> buildersByLanguage = Multimaps.asMap(rulesProfilesByLanguage)
      .entrySet()
      .stream()
      .collect(Collectors.uniqueIndex(Map.Entry::getKey, RegisterQualityProfiles::toQualityProfileBuilders));
    return buildersByLanguage
      .entrySet()
      .stream()
      .filter(RegisterQualityProfiles::ensureAtMostOneDeclaredDefault)
      .collect(Collectors.uniqueIndex(Map.Entry::getKey, entry -> toQualityProfiles(entry.getValue()), buildersByLanguage.size()));
  }

  /**
   * Creates {@link QualityProfile.Builder} for each unique quality profile name for a given language.
   * Builders will have the following properties populated:
   * <ul>
   *   <li>{@link QualityProfile.Builder#language language}: key of the method's parameter</li>
   *   <li>{@link QualityProfile.Builder#name name}: {@link RulesProfile#getName()}</li>
   *   <li>{@link QualityProfile.Builder#declaredDefault declaredDefault}: {@code true} if at least one RulesProfile
   *       with a given name has {@link RulesProfile#getDefaultProfile()} is {@code true}</li>
   *   <li>{@link QualityProfile.Builder#activeRules activeRules}: the concatenate of the active rules of all
   *       RulesProfile with a given name</li>
   * </ul>
   */
  private static List<QualityProfile.Builder> toQualityProfileBuilders(Map.Entry<String, List<RulesProfile>> rulesProfilesByLanguage) {
    String language = rulesProfilesByLanguage.getKey();
    // use a LinkedHashMap to keep order of insertion of RulesProfiles
    Map<String, QualityProfile.Builder> qualityProfileBuildersByName = new LinkedHashMap<>();
    for (RulesProfile rulesProfile : rulesProfilesByLanguage.getValue()) {
      qualityProfileBuildersByName.compute(
        rulesProfile.getName(),
        (name, existingBuilder) -> updateOrCreateBuilder(language, existingBuilder, rulesProfile, name));
    }
    return ImmutableList.copyOf(qualityProfileBuildersByName.values());
  }

  /**
   * Fails if more than one {@link QualityProfile.Builder#declaredDefault} is {@code true}, otherwise returns {@code true}.
   */
  private static boolean ensureAtMostOneDeclaredDefault(Map.Entry<String, List<QualityProfile.Builder>> entry) {
    Set<String> declaredDefaultProfileNames = entry.getValue().stream()
      .filter(QualityProfile.Builder::isDeclaredDefault)
      .map(QualityProfile.Builder::getName)
      .collect(Collectors.toSet());
    checkState(declaredDefaultProfileNames.size() <= 1, "Several Quality profiles are flagged as default for the language %s: %s", entry.getKey(), declaredDefaultProfileNames);
    return true;
  }

  private static QualityProfile.Builder updateOrCreateBuilder(String language, @Nullable QualityProfile.Builder existingBuilder, RulesProfile rulesProfile, String name) {
    QualityProfile.Builder builder = existingBuilder;
    if (builder == null) {
      builder = new QualityProfile.Builder()
        .setLanguage(language)
        .setName(name);
    }
    Boolean defaultProfile = rulesProfile.getDefaultProfile();
    boolean declaredDefault = defaultProfile != null && defaultProfile;
    return builder
      // if there is multiple RulesProfiles with the same name, if at least one is declared default,
      // then QualityProfile is flagged as declared default
      .setDeclaredDefault(builder.declaredDefault || declaredDefault)
      .addRules(rulesProfile.getActiveRules());
  }

  private static List<QualityProfile> toQualityProfiles(List<QualityProfile.Builder> builders) {
    if (builders.stream().noneMatch(QualityProfile.Builder::isDeclaredDefault)) {
      Optional<QualityProfile.Builder> sonarWayProfile = builders.stream().filter(builder -> builder.getName().equals(DEFAULT_PROFILE_NAME)).findFirst();
      if (sonarWayProfile.isPresent()) {
        sonarWayProfile.get().setComputedDefault(true);
      } else {
        builders.iterator().next().setComputedDefault(true);
      }
    }
    MessageDigest md5Digest = DigestUtils.getMd5Digest();
    return builders.stream()
      .map(builder -> builder.build(md5Digest))
      .collect(Collectors.toList(builders.size()));
  }

  private void registerProfilesForLanguage(DbSession session, OrganizationDto organization, List<QualityProfile> qualityProfiles, List<ActiveRuleChange> changes) {
    qualityProfiles.stream()
      .filter(qp -> shouldRegister(session, qp, organization.getUuid()))
      .forEach(qp -> register(session, organization, qp, changes));
    session.commit();
  }

  private void register(DbSession session, OrganizationDto organization, QualityProfile qualityProfile, List<ActiveRuleChange> changes) {
    LOGGER.info("Register profile " + qualityProfile.getQProfileName());

    QualityProfileDto profileDto = dbClient.qualityProfileDao().selectByNameAndLanguage(organization, qualityProfile.getName(), qualityProfile.getLanguage(), session);
    if (profileDto != null) {
      changes.addAll(profileFactory.delete(session, profileDto.getKey(), true));
    }
    QualityProfileDto newQProfileDto = profileFactory.create(session, organization, qualityProfile.getQProfileName(), qualityProfile.isDefault());
    for (org.sonar.api.rules.ActiveRule activeRule : qualityProfile.getActiveRules()) {
      RuleKey ruleKey = RuleKey.of(activeRule.getRepositoryKey(), activeRule.getRuleKey());
      RuleActivation activation = new RuleActivation(ruleKey);
      activation.setSeverity(activeRule.getSeverity() != null ? activeRule.getSeverity().name() : null);
      for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
        activation.setParameter(param.getKey(), param.getValue());
      }
      changes.addAll(ruleActivator.activate(session, activation, newQProfileDto));
    }

    LoadedTemplateDto template = new LoadedTemplateDto(organization.getUuid(), qualityProfile.getLoadedTemplateType());
    dbClient.loadedTemplateDao().insert(template, session);
    session.commit();
  }

  private boolean shouldRegister(DbSession session, QualityProfile qualityProfile, String organizationUuid) {
    // check if the profile was already registered in the past
    return dbClient.loadedTemplateDao()
      .countByTypeAndKey(qualityProfile.getLoadedTemplateType(), organizationUuid, session) == 0;
  }

  private static final class QualityProfile {
    private final QProfileName qProfileName;
    private final boolean isDefault;
    private final String loadedTemplateType;
    private final List<org.sonar.api.rules.ActiveRule> activeRules;

    public QualityProfile(Builder builder, MessageDigest messageDigest) {
      this.qProfileName = new QProfileName(builder.getLanguage(), builder.getName());
      this.isDefault = builder.declaredDefault || builder.computedDefault;
      this.loadedTemplateType = computeLoadedTemplateType(this.qProfileName, messageDigest);
      this.activeRules = ImmutableList.copyOf(builder.activeRules);
    }

    private static String computeLoadedTemplateType(QProfileName qProfileName, MessageDigest messageDigest) {
      String qpIdentifier = lowerCase(qProfileName.getLanguage(), Locale.ENGLISH) + ":" + qProfileName.getName();
      return format("%s.%s", QUALITY_PROFILE_TYPE, encodeHexString(messageDigest.digest(qpIdentifier.getBytes(UTF_8))));
    }

    public String getName() {
      return qProfileName.getName();
    }

    public String getLanguage() {
      return qProfileName.getLanguage();
    }

    public QProfileName getQProfileName() {
      return qProfileName;
    }

    public boolean isDefault() {
      return isDefault;
    }

    public String getLoadedTemplateType() {
      return loadedTemplateType;
    }

    public List<org.sonar.api.rules.ActiveRule> getActiveRules() {
      return activeRules;
    }

    private static final class Builder {
      private String language;
      private String name;
      private boolean declaredDefault;
      private boolean computedDefault;
      private List<org.sonar.api.rules.ActiveRule> activeRules = new ArrayList<>();

      public String getLanguage() {
        return language;
      }

      public Builder setLanguage(String language) {
        this.language = language;
        return this;
      }

      Builder setName(String name) {
        this.name = name;
        return this;
      }

      String getName() {
        return name;
      }

      Builder setDeclaredDefault(boolean declaredDefault) {
        this.declaredDefault = declaredDefault;
        return this;
      }

      boolean isDeclaredDefault() {
        return declaredDefault;
      }

      Builder setComputedDefault(boolean flag) {
        computedDefault = flag;
        return this;
      }

      Builder addRules(List<org.sonar.api.rules.ActiveRule> rules) {
        this.activeRules.addAll(rules);
        return this;
      }

      QualityProfile build(MessageDigest messageDigest) {
        return new QualityProfile(this, messageDigest);
      }
    }
  }
}
