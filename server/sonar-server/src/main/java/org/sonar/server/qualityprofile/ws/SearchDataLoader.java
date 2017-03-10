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
package org.sonar.server.qualityprofile.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class SearchDataLoader {

  private static final Comparator<QProfile> Q_PROFILE_COMPARATOR = Comparator
          .comparing(QProfile::language)
          .thenComparing(QProfile::name);

  private final Languages languages;
  private final QProfileLookup profileLookup;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final ActiveRuleIndex activeRuleIndex;

  private final QProfileWsSupport qProfileWsSupport;

  public SearchDataLoader(Languages languages, QProfileLookup profileLookup, QProfileFactory profileFactory, DbClient dbClient,
    ComponentFinder componentFinder, ActiveRuleIndex activeRuleIndex, QProfileWsSupport qProfileWsSupport) {
    this.languages = languages;
    this.profileLookup = profileLookup;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.activeRuleIndex = activeRuleIndex;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  SearchData load(SearchWsRequest request) {
    validateRequest(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      return new SearchData()
        .setProfiles(findProfiles(dbSession, request))
        .setActiveRuleCountByProfileKey(activeRuleIndex.countAllByQualityProfileKey())
        .setActiveDeprecatedRuleCountByProfileKey(activeRuleIndex.countAllDeprecatedByQualityProfileKey())
        .setProjectCountByProfileKey(dbClient.qualityProfileDao().countProjectsByProfileKey(dbSession));
    }
  }

  @VisibleForTesting
  List<QProfile> findProfiles(DbSession dbSession, SearchWsRequest request) {
    OrganizationDto organization = qProfileWsSupport.getOrganizationByKey(dbSession, request.getOrganizationKey());
    Collection<QProfile> profiles;
    if (askDefaultProfiles(request)) {
      profiles = findDefaultProfiles(dbSession, request, organization);
    } else if (hasComponentKey(request)) {
      profiles = findProjectProfiles(dbSession, request, organization);
    } else {
      profiles = findAllProfiles(dbSession, request, organization);
    }

    return profiles.stream().sorted(Q_PROFILE_COMPARATOR).collect(Collectors.toList());
  }

  private Collection<QProfile> findDefaultProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization) {
    String profileName = request.getProfileName();

    Set<String> languageKeys = getLanguageKeys();
    Map<String, QProfile> qualityProfiles = new HashMap<>(languageKeys.size());

    Set<String> missingLanguageKeys = lookupByProfileName(dbSession, organization, qualityProfiles, languageKeys, profileName);
    Set<String> noDefaultProfileLanguageKeys = lookupDefaults(dbSession, organization, qualityProfiles, missingLanguageKeys);

    if (!noDefaultProfileLanguageKeys.isEmpty()) {
      throw new IllegalStateException(format("No quality profile can been found on language(s) '%s'", noDefaultProfileLanguageKeys));
    }

    return qualityProfiles.values();
  }

  private Collection<QProfile> findProjectProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization) {
    String componentKey = request.getProjectKey();
    String profileName = request.getProfileName();

    Set<String> languageKeys = getLanguageKeys();
    Map<String, QProfile> qualityProfiles = new HashMap<>(languageKeys.size());

    // look up profiles by profileName (if any) for each language
    Set<String> unresolvedLanguages = lookupByProfileName(dbSession, organization, qualityProfiles, languageKeys, profileName);
    // look up profile by componentKey for each language for which we don't have one yet
    Set<String> stillUnresolvedLanguages = lookupByModuleKey(dbSession, organization, qualityProfiles, unresolvedLanguages, componentKey);
    // look up profile by default for each language for which we don't have one yet
    Set<String> noDefaultProfileLanguages = lookupDefaults(dbSession, organization, qualityProfiles, stillUnresolvedLanguages);

    if (!noDefaultProfileLanguages.isEmpty()) {
      throw new IllegalStateException(format("No quality profile can been found on language(s) '%s' for project '%s'", noDefaultProfileLanguages, componentKey));
    }

    return qualityProfiles.values();
  }

  private List<QProfile> findAllProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization) {
    String language = request.getLanguage();

    if (language == null) {
      return profileLookup.allProfiles(dbSession, organization).stream().filter(qProfile -> languages.get(qProfile.language()) != null).collect(Collectors.toList());
    }
    return profileLookup.profiles(dbSession, language, organization);
  }

  private Set<String> lookupByProfileName(DbSession dbSession, OrganizationDto organization, Map<String, QProfile> qualityProfiles, Set<String> languageKeys,
    @Nullable String profileName) {
    if (languageKeys.isEmpty() || profileName == null) {
      return languageKeys;
    }

    profileFactory.getByNameAndLanguages(dbSession, organization, profileName, languageKeys)
      .forEach(qualityProfile -> qualityProfiles
        .put(qualityProfile.getLanguage(), qualityProfileDtoToQProfile(qualityProfile, organization)));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private Set<String> lookupByModuleKey(DbSession dbSession, OrganizationDto organization, Map<String, QProfile> qualityProfiles, Set<String> languageKeys,
    @Nullable String moduleKey) {
    if (languageKeys.isEmpty() || moduleKey == null) {
      return languageKeys;
    }

    ComponentDto project = getProject(moduleKey, dbSession);
    profileFactory.getByProjectAndLanguages(dbSession, organization, project.getKey(), languageKeys)
            .forEach(qualityProfile -> qualityProfiles.put(qualityProfile.getLanguage(), qualityProfileDtoToQProfile(qualityProfile, organization)));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private ComponentDto getProject(String moduleKey, DbSession session) {
    ComponentDto module = componentFinder.getByKey(session, moduleKey);
    if (module.isRootProject()) {
      return module;
    }
    return dbClient.componentDao().selectOrFailByUuid(session, module.projectUuid());
  }

  private Set<String> lookupDefaults(DbSession dbSession, OrganizationDto organization, Map<String, QProfile> qualityProfiles, Set<String> languageKeys) {
    if (languageKeys.isEmpty()) {
      return languageKeys;
    }

    addAll(qualityProfiles, findDefaultProfiles(dbSession, organization, languageKeys));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private static <T> Set<T> difference(Set<T> languageKeys, Set<T> set2) {
    return Sets.newHashSet(Sets.difference(languageKeys, set2));
  }

  private static void addAll(Map<String, QProfile> qualityProfiles, Collection<QProfile> list) {
    list.forEach(qualityProfile -> qualityProfiles.put(qualityProfile.language(), qualityProfile));
  }

  private Set<String> getLanguageKeys() {
    return Arrays.stream(languages.all()).map(Language::getKey).collect(Collectors.toSet());
  }

  private List<QProfile> findDefaultProfiles(final DbSession dbSession, OrganizationDto organization, Set<String> languageKeys) {
    return profileFactory.getDefaults(dbSession, organization, languageKeys).stream()
      .map(result -> qualityProfileDtoToQProfile(result, organization))
      .collect(Collectors.toList());
  }

  @VisibleForTesting
  static void validateRequest(SearchWsRequest request) {
    boolean hasLanguage = hasLanguage(request);
    boolean isDefault = askDefaultProfiles(request);
    boolean hasComponentKey = hasComponentKey(request);
    boolean hasProfileName = hasProfileName(request);

    checkRequest(!hasLanguage || (!hasComponentKey && !hasProfileName && !isDefault),
      "The language parameter cannot be provided at the same time than the component key or profile name.");
    checkRequest(!isDefault || !hasComponentKey, "The default parameter cannot be provided at the same time than the component key.");
    checkRequest(!hasProfileName || hasComponentKey || isDefault, "The name parameter requires either projectKey or defaults to be set.");
  }

  private static boolean askDefaultProfiles(SearchWsRequest request) {
    return request.getDefaults();
  }

  private static boolean hasProfileName(SearchWsRequest request) {
    return request.getProfileName() != null;
  }

  private static boolean hasComponentKey(SearchWsRequest request) {
    return request.getProjectKey() != null;
  }

  private static boolean hasLanguage(SearchWsRequest request) {
    return request.getLanguage() != null;
  }

  public static QProfile qualityProfileDtoToQProfile(QualityProfileDto input, OrganizationDto organization) {
    return new QProfile()
      .setOrganization(organization)
      .setKey(input.getKey())
      .setName(input.getName())
      .setLanguage(input.getLanguage())
      .setDefault(input.isDefault())
      .setRulesUpdatedAt(input.getRulesUpdatedAt())
      .setLastUsed(input.getLastUsed())
      .setUserUpdatedAt(input.getUserUpdatedAt());
  }
}
