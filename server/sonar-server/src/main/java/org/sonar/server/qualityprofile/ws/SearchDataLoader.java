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
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static java.lang.String.format;

public class SearchDataLoader {

  private static final Comparator<QualityProfileDto> Q_PROFILE_COMPARATOR = Comparator
    .comparing(QualityProfileDto::getLanguage)
    .thenComparing(QualityProfileDto::getName);

  private final Languages languages;
  private final QProfileLookup profileLookup;
  private final DbClient dbClient;

  public SearchDataLoader(Languages languages, QProfileLookup profileLookup, DbClient dbClient) {
    this.languages = languages;
    this.profileLookup = profileLookup;
    this.dbClient = dbClient;
  }

  @VisibleForTesting
  List<QualityProfileDto> findProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization, @Nullable ComponentDto project) {
    Collection<QualityProfileDto> profiles;
    if (askDefaultProfiles(request)) {
      profiles = findDefaultProfiles(dbSession, request, organization);
    } else if (project != null) {
      profiles = findProjectProfiles(dbSession, request, organization, project);
    } else {
      profiles = findAllProfiles(dbSession, request, organization);
    }

    return profiles.stream().sorted(Q_PROFILE_COMPARATOR).collect(Collectors.toList());
  }

  private Collection<QualityProfileDto> findDefaultProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization) {
    String profileName = request.getProfileName();

    Set<String> languageKeys = getLanguageKeys();
    Map<String, QualityProfileDto> qualityProfiles = new HashMap<>(languageKeys.size());

    Set<String> missingLanguageKeys = lookupByProfileName(dbSession, organization, qualityProfiles, languageKeys, profileName);
    Set<String> noDefaultProfileLanguageKeys = lookupDefaults(dbSession, organization, qualityProfiles, missingLanguageKeys);

    if (!noDefaultProfileLanguageKeys.isEmpty()) {
      throw new IllegalStateException(format("No quality profile can been found on language(s) '%s'", noDefaultProfileLanguageKeys));
    }

    return qualityProfiles.values();
  }

  private Collection<QualityProfileDto> findProjectProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization, ComponentDto project) {
    String profileName = request.getProfileName();

    Set<String> languageKeys = getLanguageKeys();
    Map<String, QualityProfileDto> qualityProfiles = new HashMap<>(languageKeys.size());

    // look up profiles by profileName (if any) for each language
    Set<String> unresolvedLanguages = lookupByProfileName(dbSession, organization, qualityProfiles, languageKeys, profileName);
    // look up profile by componentKey for each language for which we don't have one yet
    Set<String> stillUnresolvedLanguages = lookupByModule(dbSession, organization, qualityProfiles, unresolvedLanguages, project);
    // look up profile by default for each language for which we don't have one yet
    Set<String> noDefaultProfileLanguages = lookupDefaults(dbSession, organization, qualityProfiles, stillUnresolvedLanguages);

    if (!noDefaultProfileLanguages.isEmpty()) {
      throw new IllegalStateException(format("No quality profile can been found on language(s) '%s' for project '%s'", noDefaultProfileLanguages, project.getKey()));
    }

    return qualityProfiles.values();
  }

  private List<QualityProfileDto> findAllProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization) {
    String language = request.getLanguage();

    if (language == null) {
      return profileLookup.allProfiles(dbSession, organization).stream().filter(qProfile -> languages.get(qProfile.getLanguage()) != null).collect(Collectors.toList());
    }
    return profileLookup.profiles(dbSession, language, organization);
  }

  private Set<String> lookupByProfileName(DbSession dbSession, OrganizationDto organization, Map<String, QualityProfileDto> qualityProfiles, Set<String> languageKeys,
    @Nullable String profileName) {
    if (languageKeys.isEmpty() || profileName == null) {
      return languageKeys;
    }

    dbClient.qualityProfileDao().selectByNameAndLanguages(organization, profileName, languageKeys, dbSession)
      .forEach(qualityProfile -> qualityProfiles
        .put(qualityProfile.getLanguage(), qualityProfile));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private Set<String> lookupByModule(DbSession dbSession, OrganizationDto organization, Map<String, QualityProfileDto> qualityProfiles, Set<String> languageKeys,
    ComponentDto project) {
    if (languageKeys.isEmpty()) {
      return languageKeys;
    }

    dbClient.qualityProfileDao().selectByProjectAndLanguages(dbSession, organization, project, languageKeys)
      .forEach(qualityProfile -> qualityProfiles.put(qualityProfile.getLanguage(), qualityProfile));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private Set<String> lookupDefaults(DbSession dbSession, OrganizationDto organization, Map<String, QualityProfileDto> qualityProfiles, Set<String> languageKeys) {
    if (languageKeys.isEmpty()) {
      return languageKeys;
    }

    addAll(qualityProfiles, findDefaultProfiles(dbSession, organization, languageKeys));
    return difference(languageKeys, qualityProfiles.keySet());
  }

  private static <T> Set<T> difference(Set<T> languageKeys, Set<T> set2) {
    return Sets.newHashSet(Sets.difference(languageKeys, set2));
  }

  private static void addAll(Map<String, QualityProfileDto> qualityProfiles, Collection<QualityProfileDto> list) {
    list.forEach(qualityProfile -> qualityProfiles.put(qualityProfile.getLanguage(), qualityProfile));
  }

  private Set<String> getLanguageKeys() {
    return Arrays.stream(languages.all()).map(Language::getKey).collect(Collectors.toSet());
  }

  private List<QualityProfileDto> findDefaultProfiles(final DbSession dbSession, OrganizationDto organization, Set<String> languageKeys) {
    return dbClient.qualityProfileDao().selectDefaultProfiles(dbSession, organization, languageKeys);
  }

  private static boolean askDefaultProfiles(SearchWsRequest request) {
    return request.getDefaults();
  }
}
