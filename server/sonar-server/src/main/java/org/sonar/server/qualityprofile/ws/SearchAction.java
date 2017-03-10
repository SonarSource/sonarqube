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
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.util.LanguageParamUtils;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.*;

public class SearchAction implements QProfileWsAction {

  private final SearchDataLoader dataLoader;
  private final Languages languages;

  public SearchAction(SearchDataLoader dataLoader, Languages languages) {
    this.dataLoader = dataLoader;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_SEARCH)
      .setSince("5.2")
      .setDescription("List quality profiles.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"));

    QProfileWsSupport.createOrganizationParam(action);

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
      .setDescription(format("Return the quality profile marked as default for each language. " +
          "If provided, then the parameters '%s', '%s' must not be set.",
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
    return  new SearchWsRequest()
      .setOrganizationKey(request.param(PARAM_ORGANIZATION))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setProfileName(request.param(PARAM_PROFILE_NAME))
      .setDefaults(request.paramAsBoolean(PARAM_DEFAULTS))
      .setLanguage(request.param(PARAM_LANGUAGE));
  }

  @VisibleForTesting
  SearchWsResponse doHandle(SearchWsRequest request) {
    SearchData data = dataLoader.load(request);
    return buildResponse(data);
  }

  private SearchWsResponse buildResponse(SearchData data) {
    List<QProfile> profiles = data.getProfiles();
    Map<String, QProfile> profilesByKey = profiles.stream().collect(Collectors.toMap(QProfile::key, identity()));

    SearchWsResponse.Builder response = SearchWsResponse.newBuilder();

    for (QProfile profile : profiles) {
      QualityProfile.Builder profileBuilder = response.addProfilesBuilder();

      String profileKey = profile.key();
      if (profile.organization() != null) {
        profileBuilder.setOrganization(profile.organization().getKey());
      }
      profileBuilder.setKey(profileKey);
      if (profile.name() != null) {
        profileBuilder.setName(profile.name());
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
      profileBuilder.setIsInherited(profile.isInherited());
      profileBuilder.setIsDefault(profile.isDefault());
    }

    return response.build();
  }

  private void writeLanguageFields(QualityProfile.Builder profileBuilder, QProfile profile) {
    String languageKey = profile.language();
    if (languageKey == null) {
      return;
    }

    profileBuilder.setLanguage(languageKey);
    String languageName = languages.get(languageKey).getName();
    if (languageName != null) {
      profileBuilder.setLanguageName(languageName);
    }
  }

  private static void writeParentFields(QualityProfile.Builder profileBuilder, QProfile profile, Map<String, QProfile> profilesByKey) {
    String parentKey = profile.parent();
    if (parentKey == null) {
      return;
    }

    profileBuilder.setParentKey(parentKey);
    QProfile parent = profilesByKey.get(parentKey);
    if (parent != null && parent.name() != null) {
      profileBuilder.setParentName(parent.name());
    }
  }
}
