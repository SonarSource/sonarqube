/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.language.ws;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;

/**
 * @since 5.1
 */
public class ListAction implements RequestHandler {

  private static final String MATCH_ALL = ".*";
  private final Languages languages;

  public ListAction(Languages languages) {
    this.languages = languages;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt("ps");

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("languages").beginArray();
      for (Language language : listMatchingLanguages(query, pageSize)) {
        json.beginObject().prop("key", language.getKey()).prop("name", language.getName()).endObject();
      }
      json.endArray().endObject();
    }
  }

  void define(WebService.NewController controller) {
    NewAction action = controller.createAction("list")
      .setDescription("List supported programming languages")
      .setSince("5.1")
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-list.json"));

    action.createParam(Param.TEXT_QUERY)
      .setDescription("A pattern to match language keys/names against")
      .setExampleValue("java");
    action.createParam("ps")
      .setDescription("The size of the list to return, 0 for all languages")
      .setExampleValue("25")
      .setDefaultValue("0");
  }

  private Collection<Language> listMatchingLanguages(@Nullable String query, int pageSize) {
    Pattern pattern = Pattern.compile(query == null ? MATCH_ALL : MATCH_ALL + Pattern.quote(query) + MATCH_ALL, Pattern.CASE_INSENSITIVE);

    SortedMap<String, Language> languagesByName = Maps.newTreeMap();
    for (Language lang : languages.all()) {
      if (pattern.matcher(lang.getKey()).matches() || pattern.matcher(lang.getName()).matches()) {
        languagesByName.put(lang.getName(), lang);
      }
    }
    List<Language> result = Lists.newArrayList(languagesByName.values());
    if (pageSize > 0 && pageSize < result.size()) {
      result = result.subList(0, pageSize);
    }
    return result;
  }

}
