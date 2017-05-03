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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.util.LanguageParamUtils;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.component.ComponentsWsParameters;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;

public class SearchAction implements QProfileWsAction {

  private final SearchDataLoader dataLoader;
  private final Languages languages;
  private final DbClient dbClient;
  private final QProfileWsSupport wsSupport;

  public SearchAction(SearchDataLoader dataLoader, Languages languages, DbClient dbClient, QProfileWsSupport wsSupport) {
    this.dataLoader = dataLoader;
    this.languages = languages;
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_SEARCH)
      .setSince("5.2")
      .setDescription("List quality profiles.")
      .setHandler(this)
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
      .createParam(PARAM_LANGUAGE)
      .setDescription(
        format("Language key. If provided, only profiles for the given language are returned. " +
          "It should not be used with '%s', '%s or '%s' at the same time.", PARAM_DEFAULTS, PARAM_PROJECT_KEY, PARAM_PROFILE_NAME))
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setDeprecatedSince("6.4");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription(format("Project or module key. If provided, '%s' and '%s' parameters should not be provided.",
        PARAM_LANGUAGE, PARAM_DEFAULTS))
      .setExampleValue("my-project-key");

    action
      .createParam(PARAM_DEFAULTS)
      .setDescription(format("If set to true, return only the quality profile marked as default for each language, '%s' and '%s' parameters must not be set.",
        PARAM_LANGUAGE, PARAM_PROJECT_KEY))
      .setDefaultValue(false)
      .setBooleanPossibleValues();

    action.createParam(PARAM_PROFILE_NAME)
      .setDescription(format("Profile name. It should be always used with the '%s' or '%s' parameter.", PARAM_PROJECT_KEY, PARAM_DEFAULTS))
      .setExampleValue("SonarQube Way")
      .setDeprecatedSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(request));
    writeProtobuf(searchWsResponse, request, response);
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
      .setOrganizationKey(request.param(PARAM_ORGANIZATION))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setProfileName(request.param(PARAM_PROFILE_NAME))
      .setDefaults(request.paramAsBoolean(PARAM_DEFAULTS))
      .setLanguage(request.param(PARAM_LANGUAGE));
  }

  @VisibleForTesting
  SearchWsResponse doHandle(SearchWsRequest request) {
    validateRequest(request);
    SearchData data = load(request);
    return buildResponse(data);
  }

  private SearchData load(SearchWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      @Nullable ComponentDto project;
      OrganizationDto organization;
      if (request.getProjectKey() == null) {
        project = null;
        organization = wsSupport.getOrganizationByKey(dbSession, request.getOrganizationKey());
      } else {
        project = getProject(request.getProjectKey(), dbSession);
        organization = dbClient.organizationDao().selectByUuid(dbSession, project.getOrganizationUuid())
          .orElseThrow(() -> new IllegalStateException(
            String.format("Organization with uuid '%s' is referenced by project '%s' but could not be found", project.getOrganizationUuid(), project.getKey())));
        if (request.getOrganizationKey() != null && !request.getOrganizationKey().equals(organization.getKey())) {
          throw new IllegalArgumentException(String.format("The provided organization key '%s' does not match the organization key '%s' of the component '%s'",
            request.getOrganizationKey(),
            organization.getKey(),
            project.getKey()
            ));
        }
      }

      return new SearchData()
        .setOrganization(organization)
        .setProfiles(dataLoader.findProfiles(dbSession, request, organization, project))
        .setActiveRuleCountByProfileKey(dbClient.activeRuleDao().countActiveRulesByProfileKey(dbSession, organization))
        .setActiveDeprecatedRuleCountByProfileKey(dbClient.activeRuleDao().countActiveRulesForRuleStatusByProfileKey(dbSession, organization, RuleStatus.DEPRECATED))
        .setProjectCountByProfileKey(dbClient.qualityProfileDao().countProjectsByProfileKey(dbSession, organization));
    }
  }

  private ComponentDto getProject(String moduleKey, DbSession dbSession) {
    ComponentDto module = checkFoundWithOptional(dbClient.componentDao().selectByKey(dbSession, moduleKey), "Component key '%s' not found", moduleKey);
    if (module.isRootProject()) {
      return module;
    }
    return dbClient.componentDao().selectOrFailByUuid(dbSession, module.projectUuid());
  }

  private static void validateRequest(SearchWsRequest request) {
    boolean hasLanguage = request.getLanguage() != null;
    boolean isDefault = request.getDefaults();
    boolean hasComponentKey = request.getProjectKey() != null;
    boolean hasProfileName = request.getProfileName() != null;

    checkRequest(!hasLanguage || (!hasComponentKey && !hasProfileName && !isDefault),
      "The language parameter cannot be provided at the same time than the component key or profile name.");
    checkRequest(!isDefault || !hasComponentKey, "The default parameter cannot be provided at the same time than the component key.");
    checkRequest(!hasProfileName || hasComponentKey || isDefault, "The name parameter requires either projectKey or defaults to be set.");
  }

  private SearchWsResponse buildResponse(SearchData data) {
    List<QualityProfileDto> profiles = data.getProfiles();
    Map<String, QualityProfileDto> profilesByKey = profiles.stream().collect(Collectors.toMap(QualityProfileDto::getKey, identity()));

    SearchWsResponse.Builder response = SearchWsResponse.newBuilder();

    for (QualityProfileDto profile : profiles) {
      QualityProfile.Builder profileBuilder = response.addProfilesBuilder();

      String profileKey = profile.getKey();
      if (profile.getOrganizationUuid() != null) {
        profileBuilder.setOrganization(data.getOrganization().getKey());
      }
      profileBuilder.setKey(profileKey);
      if (profile.getName() != null) {
        profileBuilder.setName(profile.getName());
      }
      if (profile.getRulesUpdatedAt() != null) {
        profileBuilder.setRulesUpdatedAt(profile.getRulesUpdatedAt());
      }
      if (profile.getLastUsed() != null) {
        profileBuilder.setLastUsed(formatDateTime(profile.getLastUsed()));
      }
      if (profile.getUserUpdatedAt() != null) {
        profileBuilder.setUserUpdatedAt(formatDateTime(profile.getUserUpdatedAt()));
      }
      profileBuilder.setActiveRuleCount(data.getActiveRuleCount(profileKey));
      profileBuilder.setActiveDeprecatedRuleCount(data.getActiveDeprecatedRuleCount(profileKey));
      if (!profile.isDefault()) {
        profileBuilder.setProjectCount(data.getProjectCount(profileKey));
      }

      writeLanguageFields(profileBuilder, profile);
      writeParentFields(profileBuilder, profile, profilesByKey);
      profileBuilder.setIsInherited(profile.getParentKee() != null);
      profileBuilder.setIsDefault(profile.isDefault());
    }

    return response.build();
  }

  private void writeLanguageFields(QualityProfile.Builder profileBuilder, QualityProfileDto profile) {
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

  private static void writeParentFields(QualityProfile.Builder profileBuilder, QualityProfileDto profile, Map<String, QualityProfileDto> profilesByKey) {
    String parentKey = profile.getParentKee();
    if (parentKey == null) {
      return;
    }

    profileBuilder.setParentKey(parentKey);
    QualityProfileDto parent = profilesByKey.get(parentKey);
    if (parent != null && parent.getName() != null) {
      profileBuilder.setParentName(parent.getName());
    }
  }
}
