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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleCountQuery;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.util.LanguageParamUtils;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.component.ComponentsWsParameters;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT;

public class SearchAction implements QProfileWsAction {
  private static final Comparator<QProfileDto> Q_PROFILE_COMPARATOR = Comparator
    .comparing(QProfileDto::getLanguage)
    .thenComparing(QProfileDto::getName);

  private final Languages languages;
  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;
  private final ComponentFinder componentFinder;

  public SearchAction(Languages languages, DbClient dbClient, QProfileWsSupport wsSupport, ComponentFinder componentFinder) {
    this.languages = languages;
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_SEARCH)
      .setSince("5.2")
      .setDescription("Search quality profiles")
      .setHandler(this)
      .setChangelog(new Change("6.5", format("The parameters '%s', '%s' and '%s' can be combined without any constraint", PARAM_DEFAULTS, PARAM_PROJECT, PARAM_LANGUAGE)))
      .setResponseExample(getClass().getResource("search-example.json"));

    action
      .createParam(ComponentsWsParameters.PARAM_ORGANIZATION)
      .setDescription("Organization key. If no organization key is provided, this defaults to the organization of the specified project. If neither organization nor project are" +
        "specified, the default organization will be used.")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");

    action
      .createParam(PARAM_DEFAULTS)
      .setDescription("If set to true, return only the quality profiles marked as default for each language")
      .setDefaultValue(false)
      .setBooleanPossibleValues();

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setDeprecatedKey("projectKey", "6.5")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action
      .createParam(PARAM_LANGUAGE)
      .setDescription("Language key. If provided, only profiles for the given language are returned.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages));

    action.createParam(PARAM_PROFILE_NAME)
      .setDescription("Profile name")
      .setExampleValue("SonarQube Way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(request));
    writeProtobuf(searchWsResponse, request, response);
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
      .setOrganizationKey(request.param(PARAM_ORGANIZATION))
      .setProjectKey(request.param(PARAM_PROJECT))
      .setProfileName(request.param(PARAM_PROFILE_NAME))
      .setDefaults(request.paramAsBoolean(PARAM_DEFAULTS))
      .setLanguage(request.param(PARAM_LANGUAGE));
  }

  @VisibleForTesting
  SearchWsResponse doHandle(SearchWsRequest request) {
    SearchData data = load(request);
    return buildResponse(data);
  }

  private SearchData load(SearchWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      OrganizationDto organization = wsSupport.getOrganizationByKey(dbSession, request.getOrganizationKey());
      ComponentDto project = findProject(dbSession, organization, request);

      List<QProfileDto> defaultProfiles = dbClient.qualityProfileDao().selectDefaultProfiles(dbSession, organization, getLanguageKeys());
      List<QProfileDto> profiles = searchProfiles(dbSession, request, organization, defaultProfiles, project);

      ActiveRuleCountQuery.Builder builder = ActiveRuleCountQuery.builder().setOrganization(organization);
      return new SearchData()
        .setOrganization(organization)
        .setProfiles(profiles)
        .setActiveRuleCountByProfileKey(
          dbClient.activeRuleDao().countActiveRulesByQuery(dbSession, builder.setProfiles(profiles).build()))
        .setActiveDeprecatedRuleCountByProfileKey(
          dbClient.activeRuleDao().countActiveRulesByQuery(dbSession, builder.setProfiles(profiles).setRuleStatus(DEPRECATED).build()))
        .setProjectCountByProfileKey(dbClient.qualityProfileDao().countProjectsByOrganizationAndProfiles(dbSession, organization, profiles))
        .setDefaultProfileKeys(defaultProfiles);
    }
  }

  @CheckForNull
  private ComponentDto findProject(DbSession dbSession, OrganizationDto organization, SearchWsRequest request) {
    if (request.getProjectKey() == null) {
      return null;
    }

    ComponentDto project = componentFinder.getByKey(dbSession, request.getProjectKey());
    if (!project.getOrganizationUuid().equals(organization.getUuid())) {
      throw new NotFoundException(format("Component key '%s' not found", project.getKey()));
    }
    if (project.isRoot()) {
      return project;
    }
    ComponentDto component = dbClient.componentDao().selectByUuid(dbSession, project.projectUuid()).orNull();
    checkState(component != null, "Project uuid of component uuid '%s' does not exist", project.uuid());
    return component;
  }

  private List<QProfileDto> searchProfiles(DbSession dbSession, SearchWsRequest request, OrganizationDto organization, List<QProfileDto> defaultProfiles,
    @Nullable ComponentDto project) {
    Collection<QProfileDto> profiles = selectAllProfiles(dbSession, organization);

    return profiles.stream()
      .filter(hasLanguagePlugin())
      .filter(byLanguage(request))
      .filter(byName(request))
      .filter(byDefault(request, defaultProfiles))
      .filter(byProject(dbSession, project, defaultProfiles))
      .sorted(Q_PROFILE_COMPARATOR)
      .collect(Collectors.toList());
  }

  private Predicate<QProfileDto> hasLanguagePlugin() {
    return p -> languages.get(p.getLanguage()) != null;
  }

  private static Predicate<QProfileDto> byName(SearchWsRequest request) {
    return p -> request.getProfileName() == null || Objects.equals(p.getName(), request.getProfileName());
  }

  private static Predicate<QProfileDto> byLanguage(SearchWsRequest request) {
    return p -> request.getLanguage() == null || Objects.equals(p.getLanguage(), request.getLanguage());
  }

  private static Predicate<QProfileDto> byDefault(SearchWsRequest request, List<QProfileDto> defaultProfiles) {
    Set<String> defaultProfileUuids = defaultProfiles.stream().map(QProfileDto::getKee).collect(Collectors.toSet());
    return p -> !request.getDefaults() || defaultProfileUuids.contains(p.getKee());
  }

  private Predicate<QProfileDto> byProject(DbSession dbSession, @Nullable ComponentDto project, List<QProfileDto> defaultProfiles) {
    if (project == null) {
      return p -> true;
    }
    Map<String, QProfileDto> effectiveProfiles = defaultProfiles.stream().collect(Collectors.toMap(QProfileDto::getLanguage, identity()));
    effectiveProfiles.putAll(dbClient.qualityProfileDao().selectAssociatedToProjectUuidAndLanguages(dbSession, project, getLanguageKeys()).stream()
      .collect(MoreCollectors.uniqueIndex(QProfileDto::getLanguage)));
    return p -> Objects.equals(p.getKee(), effectiveProfiles.get(p.getLanguage()).getKee());
  }

  private Collection<QProfileDto> selectAllProfiles(DbSession dbSession, OrganizationDto organization) {
    return dbClient.qualityProfileDao().selectOrderedByOrganizationUuid(dbSession, organization);
  }

  private Set<String> getLanguageKeys() {
    return Arrays.stream(languages.all()).map(Language::getKey).collect(MoreCollectors.toSet());
  }

  private SearchWsResponse buildResponse(SearchData data) {
    List<QProfileDto> profiles = data.getProfiles();
    Map<String, QProfileDto> profilesByKey = profiles.stream().collect(Collectors.toMap(QProfileDto::getKee, identity()));

    SearchWsResponse.Builder response = SearchWsResponse.newBuilder();

    for (QProfileDto profile : profiles) {
      QualityProfile.Builder profileBuilder = response.addProfilesBuilder();

      String profileKey = profile.getKee();
      setNullable(profile.getOrganizationUuid(), o -> profileBuilder.setOrganization(data.getOrganization().getKey()));
      profileBuilder.setKey(profileKey);
      setNullable(profile.getName(), profileBuilder::setName);
      setNullable(profile.getRulesUpdatedAt(), profileBuilder::setRulesUpdatedAt);
      setNullable(profile.getLastUsed(), last -> profileBuilder.setLastUsed(formatDateTime(last)));
      setNullable(profile.getUserUpdatedAt(), userUpdatedAt -> profileBuilder.setUserUpdatedAt(formatDateTime(userUpdatedAt)));
      profileBuilder.setActiveRuleCount(data.getActiveRuleCount(profileKey));
      profileBuilder.setActiveDeprecatedRuleCount(data.getActiveDeprecatedRuleCount(profileKey));
      boolean isDefault = data.isDefault(profile);
      profileBuilder.setIsDefault(isDefault);
      if (!isDefault) {
        profileBuilder.setProjectCount(data.getProjectCount(profileKey));
      }

      writeLanguageFields(profileBuilder, profile);
      writeParentFields(profileBuilder, profile, profilesByKey);
      profileBuilder.setIsInherited(profile.getParentKee() != null);
      profileBuilder.setIsBuiltIn(profile.isBuiltIn());
    }

    return response.build();
  }

  private void writeLanguageFields(QualityProfile.Builder profileBuilder, QProfileDto profile) {
    String languageKey = profile.getLanguage();
    if (languageKey == null) {
      return;
    }

    profileBuilder.setLanguage(languageKey);
    String languageName = languages.get(languageKey).getName();
    if (languageName != null) {
      profileBuilder.setLanguageName(languageName);
    }
  }

  private static void writeParentFields(QualityProfile.Builder profileBuilder, QProfileDto profile, Map<String, QProfileDto> profilesByKey) {
    String parentKey = profile.getParentKee();
    if (parentKey == null) {
      return;
    }

    profileBuilder.setParentKey(parentKey);
    QProfileDto parent = profilesByKey.get(parentKey);
    if (parent != null && parent.getName() != null) {
      profileBuilder.setParentName(parent.getName());
    }
  }

}
