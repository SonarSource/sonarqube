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

package org.sonar.server.qualityprofile.ws;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
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
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class SearchDataLoader {
  private final Languages languages;
  private final QProfileLookup profileLookup;
  private final QProfileLoader profileLoader;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public SearchDataLoader(Languages languages, QProfileLookup profileLookup, QProfileLoader profileLoader, QProfileFactory profileFactory, DbClient dbClient,
    ComponentFinder componentFinder) {
    this.languages = languages;
    this.profileLookup = profileLookup;
    this.profileLoader = profileLoader;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  SearchData load(SearchWsRequest request) {
    validateRequest(request);

    return new SearchData()
      .setProfiles(findProfiles(request))
      .setActiveRuleCountByProfileKey(profileLoader.countAllActiveRules())
      .setProjectCountByProfileKey(dbClient.qualityProfileDao().countProjectsByProfileKey());
  }

  private List<QProfile> findProfiles(SearchWsRequest request) {
    List<QProfile> profiles;
    if (askDefaultProfiles(request)) {
      profiles = findDefaultProfiles(request);
    } else if (hasComponentKey(request)) {
      profiles = findProjectProfiles(request);
    } else {
      profiles = findAllProfiles(request);
    }

    return orderProfiles(profiles);
  }

  private static List<QProfile> orderProfiles(List<QProfile> profiles) {
    return from(profiles)
      .toSortedList(QProfileComparator.INSTANCE);
  }

  private List<QProfile> findAllProfiles(SearchWsRequest request) {
    String language = request.getLanguage();

    List<QProfile> profiles = language != null ? profileLookup.profiles(language)
      : profileLookup.allProfiles();

    return from(profiles)
      .filter(new IsLanguageKnown())
      .toList();
  }

  private List<QProfile> findProjectProfiles(SearchWsRequest request) {
    String moduleKey = request.getProjectKey();
    String profileName = request.getProfileName();

    List<QProfile> profiles = new ArrayList<>();

    DbSession dbSession = dbClient.openSession(false);
    try {
      for (Language language : languages.all()) {
        String languageKey = language.getKey();
        ComponentDto project = getProject(moduleKey, dbSession);
        profiles.add(getProfile(dbSession, languageKey, project.key(), profileName));
      }
    } finally {
      dbClient.closeSession(dbSession);
    }

    return profiles;
  }

  private ComponentDto getProject(String moduleKey, DbSession session) {
    ComponentDto module = componentFinder.getByKey(session, moduleKey);
    if (!module.isRootProject()) {
      return dbClient.componentDao().selectOrFailByUuid(session, module.projectUuid());
    } else {
      return module;
    }
  }

  private List<QProfile> findDefaultProfiles(SearchWsRequest request) {
    String profileName = request.getProfileName();
    List<QProfile> profiles = new ArrayList<>();

    DbSession dbSession = dbClient.openSession(false);
    try {
      for (Language language : languages.all()) {
        profiles.add(getDefaultProfile(dbSession, language.getKey(), profileName));
      }
    } finally {
      dbClient.closeSession(dbSession);
    }

    return profiles;
  }

  private static QProfile profileDtoToQProfile(QualityProfileDto dto) {
    return new QProfile()
      .setKey(dto.getKey())
      .setName(dto.getName())
      .setLanguage(dto.getLanguage())
      .setDefault(dto.isDefault())
      .setRulesUpdatedAt(dto.getRulesUpdatedAt());
  }

  /**
   * First try to find a quality profile matching the given name (if provided) and current language
   * If no profile found, try to find the quality profile set on the project (if provided)
   * If still no profile found, try to find the default profile of the language
   * <p/>
   * Never return null because a default profile should always be set on each language
   */
  private QProfile getProfile(DbSession dbSession, String languageKey, @Nullable String projectKey, @Nullable String profileName) {
    QualityProfileDto profileDto = profileName != null ? profileFactory.getByNameAndLanguage(dbSession, profileName, languageKey) : null;
    if (profileDto == null && projectKey != null) {
      profileDto = profileFactory.getByProjectAndLanguage(dbSession, projectKey, languageKey);
    }
    profileDto = profileDto != null ? profileDto : profileFactory.getDefault(dbSession, languageKey);
    checkState(profileDto != null, format("No quality profile can been found on language '%s' for project '%s'", languageKey, projectKey));

    return profileDtoToQProfile(profileDto);
  }

  private QProfile getDefaultProfile(DbSession dbSession, String languageKey, @Nullable String profileName) {
    QualityProfileDto profile = profileName != null ? profileFactory.getByNameAndLanguage(dbSession, profileName, languageKey) : null;
    if (profile == null) {
      profile = profileFactory.getDefault(dbSession, languageKey);
    }
    checkState(profile != null, format("No quality profile can been found on language '%s'", languageKey));

    return profileDtoToQProfile(profile);
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

  private static boolean hasProfileName(SearchWsRequest request) {
    return request.getProfileName() != null;
  }

  private static boolean hasComponentKey(SearchWsRequest request) {
    return request.getProjectKey() != null;
  }

  private static Boolean askDefaultProfiles(SearchWsRequest request) {
    return request.getDefaults();
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

  private class IsLanguageKnown implements Predicate<QProfile> {
    @Override
    public boolean apply(@Nonnull QProfile profile) {
      return languages.get(profile.language()) != null;
    }
  }
}
