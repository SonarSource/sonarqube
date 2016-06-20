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

import com.google.common.base.Function;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.server.component.ws.LanguageParamUtils;
import org.sonar.server.qualityprofile.QProfile;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static com.google.common.collect.Maps.uniqueIndex;
import static java.lang.String.format;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements QProfileWsAction {

  static final String PARAM_LANGUAGE = "language";
  static final String PARAM_PROJECT_KEY = "projectKey";
  static final String PARAM_DEFAULTS = "defaults";
  static final String PARAM_PROFILE_NAME = "profileName";

  private final SearchDataLoader dataLoader;
  private final Languages languages;

  public SearchAction(SearchDataLoader dataLoader, Languages languages) {
    this.dataLoader = dataLoader;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("search")
      .setSince("5.2")
      .setDescription("List quality profiles.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-search.json"));

    action
      .createParam(PARAM_LANGUAGE)
      .setDescription(
        format("Language key. If provided, only profiles for the given language are returned. " +
          "It should not be used with '%s', '%s or '%s' at the same time.", PARAM_DEFAULTS, PARAM_PROJECT_KEY, PARAM_PROFILE_NAME))
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages));

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
      .setExampleValue("SonarQube Way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(toSearchWsRequest(request));
    writeProtobuf(searchWsResponse, request, response);
  }

  private static SearchWsRequest toSearchWsRequest(Request request) {
    return new SearchWsRequest()
        .setProjectKey(request.param(PARAM_PROJECT_KEY))
        .setProfileName(request.param(PARAM_PROFILE_NAME))
        .setDefaults(request.paramAsBoolean(PARAM_DEFAULTS))
        .setLanguage(request.param(PARAM_LANGUAGE));
  }

  private SearchWsResponse doHandle(SearchWsRequest request) {
    SearchData data = dataLoader.load(request);
    return buildResponse(data);
  }

  private SearchWsResponse buildResponse(SearchData data) {
    List<QProfile> profiles = data.getProfiles();
    Map<String, QProfile> profilesByKey = uniqueIndex(profiles, QProfileToKey.INSTANCE);

    QualityProfiles.SearchWsResponse.Builder response = QualityProfiles.SearchWsResponse.newBuilder();
    QualityProfile.Builder profileBuilder = QualityProfile.newBuilder();

    for (QProfile profile : profiles) {
      profileBuilder.clear();

      String profileKey = profile.key();
      profileBuilder.setKey(profileKey);
      if (profile.name() != null) {
        profileBuilder.setName(profile.name());
      }
      if (profile.getRulesUpdatedAt() != null) {
        profileBuilder.setRulesUpdatedAt(profile.getRulesUpdatedAt());
      }
      profileBuilder.setActiveRuleCount(data.getActiveRuleCount(profileKey));
      if (!profile.isDefault()) {
        profileBuilder.setProjectCount(data.getProjectCount(profileKey));
      }

      writeLanguageFields(profileBuilder, profile);
      writeParentFields(profileBuilder, profile, profilesByKey);
      profileBuilder.setIsInherited(profile.isInherited());
      profileBuilder.setIsDefault(profile.isDefault());
      response.addProfiles(profileBuilder);
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

  private enum QProfileToKey implements Function<QProfile, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull QProfile input) {
      return input.key();
    }
  }
}
