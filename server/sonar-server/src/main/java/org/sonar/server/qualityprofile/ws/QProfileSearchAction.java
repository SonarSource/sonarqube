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
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileLookup;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class QProfileSearchAction implements BaseQProfileWsAction {

  private static final String FIELD_KEY = "key";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_LANGUAGE = "language";
  private static final String FIELD_IS_INHERITED = "isInherited";
  private static final String FIELD_IS_DEFAULT = "isDefault";
  private static final String FIELD_PARENT_KEY = "parentKey";
  private static final Set<String> ALL_FIELDS = ImmutableSet.of(FIELD_KEY, FIELD_NAME, FIELD_LANGUAGE, FIELD_IS_INHERITED, FIELD_PARENT_KEY, FIELD_IS_DEFAULT);

  private static final String PARAM_LANGUAGE = FIELD_LANGUAGE;
  private static final String PARAM_FIELDS = "f";


  private final Languages languages;

  private final QProfileLookup profileLookup;

  public QProfileSearchAction(Languages languages, QProfileLookup profileLookup) {
    this.languages = languages;
    this.profileLookup = profileLookup;
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
    writeLanguages(json);
    json.endObject().close();
  }

  private void writeProfiles(JsonWriter json, List<QProfile> profiles, List<String> fields) {
    json.name("profiles")
      .beginArray();
    for (QProfile profile : profiles) {
      json.beginObject()
        .prop(FIELD_KEY, nullUnlessNeeded(FIELD_KEY, profile.key(), fields))
        .prop(FIELD_NAME, nullUnlessNeeded(FIELD_NAME, profile.name(), fields))
        .prop(FIELD_LANGUAGE, nullUnlessNeeded(FIELD_LANGUAGE, profile.language(), fields))
        .prop(FIELD_PARENT_KEY, nullUnlessNeeded(FIELD_PARENT_KEY, profile.parent(), fields));
      // Special case for booleans
      if (fieldIsNeeded(FIELD_IS_INHERITED, fields)) {
        json.prop(FIELD_IS_INHERITED, profile.isInherited());
      }
      if (fieldIsNeeded(FIELD_IS_DEFAULT, fields) && profile.isDefault()) {
        json.prop(FIELD_IS_DEFAULT, true);
      }
      json.endObject();
    }
    json.endArray();
  }

  @CheckForNull
  private <T> T nullUnlessNeeded(String field, T value, @Nullable List<String> fields) {
    return fieldIsNeeded(field, fields) ? value : null;
  }

  private boolean fieldIsNeeded(String field, @Nullable List<String> fields) {
    return fields == null || fields.contains(field);
  }

  private void writeLanguages(JsonWriter json) {
    json.name("languages")
      .beginArray();
    for (Language language : languages.all()) {
      json.beginObject()
        .prop(FIELD_KEY, language.getKey())
        .prop(FIELD_NAME, language.getName())
        .endObject();
    }
    json.endArray();
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction search = controller.createAction("search")
      .setSince("5.2")
      .setDescription("")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-search.json"));

    search.createParam(PARAM_LANGUAGE)
      .setDescription("The key of a language supported by the platform. If specified, only profiles for the given language are returned")
      .setExampleValue("js")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages));

    search.createParam(PARAM_FIELDS)
      .setDescription("Use to restrict returned fields")
      .setExampleValue("key,language")
      .setPossibleValues(ALL_FIELDS);
  }

}
