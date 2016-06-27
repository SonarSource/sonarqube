/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
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
  private final Languages languages;
  private final QProfileLookup profileLookup;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final ActiveRuleIndex activeRuleIndex;

  public SearchDataLoader(Languages languages, QProfileLookup profileLookup, QProfileFactory profileFactory, DbClient dbClient,
    ComponentFinder componentFinder, ActiveRuleIndex activeRuleIndex) {
    this.languages = languages;
    this.profileLookup = profileLookup;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.activeRuleIndex = activeRuleIndex;
  }

  SearchData load(SearchWsRequest request) {
    validateRequest(request);

    return new SearchData()
      .setProfiles(findProfiles(request))
      .setActiveRuleCountByProfileKey(activeRuleIndex.countAllByQualityProfileKey())
      .setActiveDeprecatedRuleCountByProfileKey(activeRuleIndex.countAllDeprecatedByQualityProfileKey())
      .setProjectCountByProfileKey(dbClient.qualityProfileDao().countProjectsByProfileKey());
  }

  private List<QProfile> findProfiles(SearchWsRequest request) {
    Collection<QProfile> profiles;
    if (askDefaultProfiles(request)) {
      profiles = findDefaultProfiles(request);
    } else if (hasComponentKey(request)) {
      profiles = findProjectProfiles(request);
    } else {
      profiles = findAllProfiles(request);
    }

    return profiles.stream().sorted(QProfileComparator.INSTANCE).collect(Collectors.toList());
  }

  private Collection<QProfile> findDefaultProfiles(SearchWsRequest request) {
    String profileName = request.getProfileName();

    Set<String> languageKeys = getLanguageKeys();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, QProfile> qualityProfiles = new HashMap<>(languageKeys.size());

      Set<String> missingLanguageKeys = lookupByProfileName(dbSession, qualityProfiles, languageKeys, profileName);
      Set<String> noDefaultProfileLanguageKeys = lookupDefaults(dbSession, qualityProfiles, missingLanguageKeys);

      if (!noDefaultProfileLanguageKeys.isEmpty()) {
        throw new IllegalStateException(format("No quality profile can been found on language(s) '%s'", noDefaultProfileLanguageKeys));
      }

      return qualityProfiles.values();
    }
  }

  private Collection<QProfile> findProjectProfiles(SearchWsRequest request) {
    String componentKey = request.getProjectKey();
    String profileName = request.getProfileName();

    Set<String> languageKeys = getLanguageKeys();
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, QProfile> qualityProfiles = new HashMap<>(languageKeys.size());

      // look up profiles by profileName (if any) for each language
      Set<String> unresolvedLanguages = lookupByProfileName(dbSession, qualityProfiles, languageKeys, profileName);
      // look up profile by componentKey for each language for which we don't have one yet
      Set<String> stillUnresolvedLanguages = lookupByModuleKey(dbSession, qualityProfiles, unresolvedLanguages, componentKey);
      // look up profile by default for each language for which we don't have one yet
      Set<String> noDefaultProfileLanguages = lookupDefaults(dbSession, qualityProfiles, stillUnresolvedLanguages);

      if (!noDefaultProfileLanguages.isEmpty()) {
        throw new IllegalStateException(format("No quality profile can been found on language(s) '%s' for project '%s'", noDefaultProfileLanguages, componentKey));
      }

      return qualityProfiles.values();
    }
  }

  private List<QProfile> findAllProfiles(SearchWsRequest request) {
    String language = request.getLanguage();

    if (language == null) {
      return profileLookup.allProfiles().stream().filter(qProfile -> languages.get(qProfile.language()) != null).collect(Collectors.toList());
    }
    return profileLookup.profiles(language);
  }

  private Set<String> lookupByProfileName(DbSession dbSession, Map<String, QProfile> qualityProfiles, Set<String> languageKeys, @Nullable String profileName) {
    if (languageKeys.isEmpty() || profileName == null) {
      return languageKeys;
    }

    addAllFromDto(qualityProfiles, profileFactory.getByNameAndLanguages(dbSession, profileName, languageKeys));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private Set<String> lookupByModuleKey(DbSession dbSession, Map<String, QProfile> qualityProfiles, Set<String> languageKeys, @Nullable String moduleKey) {
    if (languageKeys.isEmpty() || moduleKey == null) {
      return languageKeys;
    }

    ComponentDto project = getProject(moduleKey, dbSession);
    addAllFromDto(qualityProfiles, profileFactory.getByProjectAndLanguages(dbSession, project.getKey(), languageKeys));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private ComponentDto getProject(String moduleKey, DbSession session) {
    ComponentDto module = componentFinder.getByKey(session, moduleKey);
    if (module.isRootProject()) {
      return module;
    }
    return dbClient.componentDao().selectOrFailByUuid(session, module.projectUuid());
  }

  private Set<String> lookupDefaults(DbSession dbSession, Map<String, QProfile> qualityProfiles, Set<String> languageKeys) {
    if (languageKeys.isEmpty()) {
      return languageKeys;
    }

    addAll(qualityProfiles, findDefaultProfiles(dbSession, languageKeys));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private static <T> Set<T> difference(Set<T> languageKeys, Set<T> set2) {
    return Sets.newHashSet(Sets.difference(languageKeys, set2));
  }

  private static void addAllFromDto(Map<String, QProfile> qualityProfiles, Collection<QualityProfileDto> list) {
    list.forEach(qualityProfile -> qualityProfiles.put(qualityProfile.getLanguage(), QualityProfileDtoToQProfile.INSTANCE.apply(qualityProfile)));
  }

  private static void addAll(Map<String, QProfile> qualityProfiles, Collection<QProfile> list) {
    list.forEach(qualityProfile -> qualityProfiles.put(qualityProfile.language(), qualityProfile));
  }

  private Set<String> getLanguageKeys() {
    return Arrays.stream(languages.all()).map(Language::getKey).collect(Collectors.toSet());
  }

  private List<QProfile> findDefaultProfiles(final DbSession dbSession, Set<String> languageKeys) {
    return profileFactory.getDefaults(dbSession, languageKeys).stream()
      .map(QualityProfileDtoToQProfile.INSTANCE)
      .collect(Collectors.toList());
  }

  private static void validateRequest(SearchWsRequest request) {
    boolean hasLanguage = hasLanguage(request);
    boolean isDefault = askDefaultProfiles(request);
    boolean hasComponentKey = hasComponentKey(request);
    boolean hasProfileName = hasProfileName(request);

    checkRequest(!hasLanguage || (!hasComponentKey && !hasProfileName && !isDefault),
      "The language parameter cannot be provided at the same time than the component key or profile name.");
    checkRequest(!isDefault || !hasComponentKey, "The default parameter cannot be provided at the same time than the component key");
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

  private enum QProfileComparator implements Comparator<QProfile> {
    INSTANCE;
    @Override
    public int compare(QProfile o1, QProfile o2) {
      return new CompareToBuilder()
        .append(o1.language(), o2.language())
        .append(o1.name(), o2.name())
        .toComparison();
    }
  }

  private enum QualityProfileDtoToQProfile implements Function<QualityProfileDto, QProfile> {
    INSTANCE;

    @Override
    public QProfile apply(QualityProfileDto input) {
      return new QProfile()
        .setKey(input.getKey())
        .setName(input.getName())
        .setLanguage(input.getLanguage())
        .setDefault(input.isDefault())
        .setRulesUpdatedAt(input.getRulesUpdatedAt())
        .setLastUsed(input.getLastUsed())
        .setUserUpdatedAt(input.getUserUpdatedAt());
    }
  }
}
