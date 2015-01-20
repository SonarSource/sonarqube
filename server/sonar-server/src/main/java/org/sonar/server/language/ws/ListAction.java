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
package org.sonar.server.language.ws;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.regex.Pattern;

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
    String query = request.param("q");
    int pageSize = request.mandatoryParamAsInt("ps");

    JsonWriter json = response.newJsonWriter().beginObject().name("languages").beginArray();
    for (Language language : listMatchingLanguages(query, pageSize)) {
      json.beginObject().prop("key", language.getKey()).prop("name", language.getName()).endObject();
    }
    json.endArray().endObject().close();
  }

  private Collection<Language> listMatchingLanguages(@Nullable String query, int pageSize) {
    Pattern pattern = Pattern.compile(query == null ? MATCH_ALL : MATCH_ALL + query + MATCH_ALL, Pattern.CASE_INSENSITIVE);

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

  void define(WebService.NewController controller) {
    NewAction action = controller.createAction("list")
      .setDescription("List supported programming languages")
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-list.json"));

    action.createParam("q")
      .setDescription("A pattern to match language keys/names against")
      .setExampleValue("java");
    action.createParam("ps")
      .setDescription("The size of the list to return, 0 for all languages")
      .setExampleValue("25")
      .setDefaultValue("0");
  }

}
