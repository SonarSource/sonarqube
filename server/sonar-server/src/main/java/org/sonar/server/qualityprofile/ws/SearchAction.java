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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.server.qualityprofile.QProfile;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse;
import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;

import static com.google.common.collect.Maps.uniqueIndex;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements QProfileWsAction {

  private static final String FIELD_KEY = "key";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_LANGUAGE = "language";
  private static final String FIELD_LANGUAGE_NAME = "languageName";
  private static final String FIELD_IS_INHERITED = "isInherited";
  private static final String FIELD_IS_DEFAULT = "isDefault";
  private static final String FIELD_PARENT_KEY = "parentKey";
  private static final String FIELD_PARENT_NAME = "parentName";
  private static final String FIELD_ACTIVE_RULE_COUNT = "activeRuleCount";
  private static final String FIELD_PROJECT_COUNT = "projectCount";
  private static final String FIELD_RULES_UPDATED_AT = "rulesUpdatedAt";

  private static final Set<String> ALL_FIELDS = ImmutableSet.of(
    FIELD_KEY, FIELD_NAME, FIELD_LANGUAGE, FIELD_LANGUAGE_NAME, FIELD_IS_INHERITED, FIELD_PARENT_KEY, FIELD_PARENT_NAME, FIELD_IS_DEFAULT, FIELD_ACTIVE_RULE_COUNT,
    FIELD_PROJECT_COUNT, FIELD_RULES_UPDATED_AT);

  static final String PARAM_LANGUAGE = FIELD_LANGUAGE;
  static final String PARAM_PROJECT_KEY = "projectKey";
  static final String PARAM_DEFAULT = "defaults";
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
      .addFieldsParam(ALL_FIELDS)
      .setResponseExample(getClass().getResource("example-search.json"));

    action
      .createParam(PARAM_LANGUAGE)
      .setDescription(
        "The key of a language supported by the platform. If specified, only profiles for the given language are returned. " +
          "It should not be used with project key, profile name or default at the same time.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages));

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project or module key. If provided, language key and default parameters should not be provided.")
      .setExampleValue("org.codehaus.sonar:sonar");

    action.createParam(PARAM_DEFAULT)
      .setDescription("Return default quality profiles. If provided, the language, project key and profile name should not be provided")
      .setDefaultValue(false)
      .setBooleanPossibleValues();

    action.createParam(PARAM_PROFILE_NAME)
      .setDescription("Profile name. It should be always used with the project key parameter.")
      .setExampleValue("SonarQube Way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchData data = dataLoader.load(request);
    WsSearchResponse protobufResponse = buildResponse(data);
    writeProtobuf(protobufResponse, request, response);
  }

  private WsSearchResponse buildResponse(SearchData data) {
    List<QProfile> profiles = data.getProfiles();
    List<String> fields = data.getFieldsToReturn();
    Map<String, QProfile> profilesByKey = uniqueIndex(profiles, new QProfileToKey());

    WsSearchResponse.Builder response = WsSearchResponse.newBuilder();
    QualityProfile.Builder profileBuilder = QualityProfile.newBuilder();

    for (QProfile profile : profiles) {
      String profileKey = profile.key();
      profileBuilder.clear();

      if (shouldSetValue(FIELD_KEY, profile.key(), fields)) {
        profileBuilder.setKey(profile.key());
      }
      if (shouldSetValue(FIELD_NAME, profile.name(), fields)) {
        profileBuilder.setName(profile.name());
      }
      if (shouldSetValue(FIELD_RULES_UPDATED_AT, profile.getRulesUpdatedAt(), fields)) {
        profileBuilder.setRulesUpdatedAt(profile.getRulesUpdatedAt());
      }
      if (shouldSetValue(FIELD_ACTIVE_RULE_COUNT, data.getActiveRuleCount(profileKey), fields)) {
        profileBuilder.setActiveRuleCount(data.getActiveRuleCount(profileKey));
      }
      if (!profile.isDefault() && shouldSetValue(FIELD_PROJECT_COUNT, data.getProjectCount(profileKey), fields)) {
        profileBuilder.setProjectCount(data.getProjectCount(profileKey));
      }

      writeLanguageFields(profileBuilder, profile, fields);
      writeParentFields(profileBuilder, profile, fields, profilesByKey);
      // Special case for booleans
      if (fieldIsNeeded(FIELD_IS_INHERITED, fields)) {
        profileBuilder.setIsInherited(profile.isInherited());
      }
      if (fieldIsNeeded(FIELD_IS_DEFAULT, fields)) {
        profileBuilder.setIsDefault(profile.isDefault());
      }
      response.addProfiles(profileBuilder);
    }

    return response.build();
  }

  private void writeLanguageFields(QualityProfile.Builder profileBuilder, QProfile profile, List<String> fields) {
    String languageKey = profile.language();
    if (shouldSetValue(FIELD_LANGUAGE, languageKey, fields)) {
      profileBuilder.setLanguage(languageKey);
    }
    String languageName = languages.get(languageKey).getName();
    if (shouldSetValue(FIELD_LANGUAGE_NAME, languageName, fields)) {
      profileBuilder.setLanguageName(languageName);
    }
  }

  private static void writeParentFields(QualityProfile.Builder profileBuilder, QProfile profile, List<String> fields, Map<String, QProfile> profilesByKey) {
    String parentKey = profile.parent();
    QProfile parent = parentKey == null ? null : profilesByKey.get(parentKey);
    if (shouldSetValue(FIELD_PARENT_KEY, parentKey, fields)) {
      profileBuilder.setParentKey(parentKey);
    }
    if (parent != null && shouldSetValue(FIELD_PARENT_NAME, parent.name(), fields)) {
      profileBuilder.setParentName(parent.name());
    }
  }

  @CheckForNull
  private static <T> T valueIfFieldNeeded(String field, T value, @Nullable List<String> fields) {
    return fieldIsNeeded(field, fields) ? value : null;
  }

  private static <T> boolean shouldSetValue(String field, T value, List<String> fields) {
    return valueIfFieldNeeded(field, value, fields) != null;
  }

  private static boolean fieldIsNeeded(String field, @Nullable List<String> fields) {
    return fields == null || fields.contains(field);
  }

  private static class QProfileToKey implements Function<QProfile, String> {
    @Override
    public String apply(@Nonnull QProfile input) {
      return input.key();
    }
  }
}
