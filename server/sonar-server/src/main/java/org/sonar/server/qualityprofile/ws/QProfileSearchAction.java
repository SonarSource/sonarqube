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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QProfileSearchAction implements BaseQProfileWsAction {

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

  private static final Set<String> ALL_FIELDS = ImmutableSet.of(
    FIELD_KEY, FIELD_NAME, FIELD_LANGUAGE, FIELD_LANGUAGE_NAME, FIELD_IS_INHERITED, FIELD_PARENT_KEY, FIELD_PARENT_NAME, FIELD_IS_DEFAULT, FIELD_ACTIVE_RULE_COUNT,
    FIELD_PROJECT_COUNT);

  private static final String PARAM_LANGUAGE = FIELD_LANGUAGE;
  private static final String PARAM_FIELDS = "f";


  private final Languages languages;

  private final QProfileLookup profileLookup;

  private final QProfileLoader profileLoader;

  private final QualityProfileDao qualityProfileDao;

  public QProfileSearchAction(Languages languages, QProfileLookup profileLookup, QProfileLoader profileLoader, QualityProfileDao qualityProfileDao) {
    this.languages = languages;
    this.profileLookup = profileLookup;
    this.profileLoader = profileLoader;
    this.qualityProfileDao = qualityProfileDao;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction search = controller.createAction("search")
      .setSince("5.2")
      .setDescription("List quality profiles.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-search.json"));

    search.createParam(PARAM_LANGUAGE)
      .setDescription("The key of a language supported by the platform. If specified, only profiles for the given language are returned.")
      .setExampleValue("js")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages));

    search.createParam(PARAM_FIELDS)
      .setDescription("Use to restrict returned fields.")
      .setExampleValue("key,language")
      .setPossibleValues(ALL_FIELDS);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    List<String> fields = request.paramAsStrings(PARAM_FIELDS);

    String language = request.param(PARAM_LANGUAGE);

    List<QProfile> profiles = null;
    if (language == null) {
      profiles = profileLookup.allProfiles();
    } else {
      profiles = profileLookup.profiles(language);
    }

    Collections.sort(profiles, new Comparator<QProfile>() {
      @Override
      public int compare(QProfile o1, QProfile o2) {
        return new CompareToBuilder()
          .append(o1.language(), o2.language())
          .append(o1.name(), o2.name())
          .toComparison();
      }
    });

    JsonWriter json = response.newJsonWriter().beginObject();
    writeProfiles(json, profiles, fields);
    json.endObject().close();
  }

  private void writeProfiles(JsonWriter json, List<QProfile> profiles, List<String> fields) {
    Map<String, QProfile> profilesByKey = Maps.uniqueIndex(profiles, new NonNullInputFunction<QProfile, String>() {
      @Override
      protected String doApply(QProfile input) {
        return input.key();
      }
    });
    Map<String, Long> activeRuleCountByKey = profileLoader.countAllActiveRules();
    Map<String, Long> projectCountByKey = qualityProfileDao.countProjectsByProfileKey();


    json.name("profiles")
      .beginArray();
    for (QProfile profile : profiles) {
      if (languages.get(profile.language()) == null) {
        // Hide profiles on an unsupported language
        continue;
      }

      String key = profile.key();
      Long activeRuleCount = activeRuleCountByKey.containsKey(key) ? activeRuleCountByKey.get(key) : 0L;
      Long projectCount = projectCountByKey.containsKey(key) ? projectCountByKey.get(key) : 0L;
      json.beginObject()
        .prop(FIELD_KEY, nullUnlessNeeded(FIELD_KEY, key, fields))
        .prop(FIELD_NAME, nullUnlessNeeded(FIELD_NAME, profile.name(), fields))
        .prop(FIELD_ACTIVE_RULE_COUNT, nullUnlessNeeded(FIELD_ACTIVE_RULE_COUNT, activeRuleCount, fields));

      if (!profile.isDefault()) {
        json.prop(FIELD_PROJECT_COUNT, nullUnlessNeeded(FIELD_PROJECT_COUNT, projectCount, fields));
      }
      writeLanguageFields(json, profile, fields);
      writeParentFields(json, profile, fields, profilesByKey);
      // Special case for booleans
      if (fieldIsNeeded(FIELD_IS_INHERITED, fields)) {
        json.prop(FIELD_IS_INHERITED, profile.isInherited());
      }
      if (fieldIsNeeded(FIELD_IS_DEFAULT, fields)) {
        json.prop(FIELD_IS_DEFAULT, profile.isDefault());
      }
      json.endObject();
    }
    json.endArray();
  }

  private void writeLanguageFields(JsonWriter json, QProfile profile, List<String> fields) {
    String languageKey = profile.language();
    json.prop(FIELD_LANGUAGE, nullUnlessNeeded(FIELD_LANGUAGE, languageKey, fields))
      .prop(FIELD_LANGUAGE_NAME, nullUnlessNeeded(FIELD_LANGUAGE_NAME, languages.get(languageKey).getName(), fields));
  }

  private void writeParentFields(JsonWriter json, QProfile profile, List<String> fields, Map<String, QProfile> profilesByKey) {
    String parentKey = profile.parent();
    QProfile parent = parentKey == null ? null : profilesByKey.get(parentKey);
    json.prop(FIELD_PARENT_KEY, nullUnlessNeeded(FIELD_PARENT_KEY, parentKey, fields))
      .prop(FIELD_PARENT_NAME, nullUnlessNeeded(FIELD_PARENT_NAME, parent == null ? parentKey : parent.name(), fields));
  }

  @CheckForNull
  private <T> T nullUnlessNeeded(String field, T value, @Nullable List<String> fields) {
    return fieldIsNeeded(field, fields) ? value : null;
  }

  private boolean fieldIsNeeded(String field, @Nullable List<String> fields) {
    return fields == null || fields.contains(field);
  }
}
