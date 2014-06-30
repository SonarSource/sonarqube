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
package org.sonar.server.activity.ws;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.search.ws.SearchOptions;

/**
 * @since 4.4
 */
public class SearchAction implements RequestHandler {

  public static final String PARAM_TYPE = "type";

  public static final String SEARCH_ACTION = "search";

  private final ActivityService logService;
  private final ActivityMapping mapping;

  public SearchAction(ActivityService logService, ActivityMapping mapping) {
    this.logService = logService;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(SEARCH_ACTION)
      .setDescription("Search for a logs")
      .setSince("4.4")
      .setInternal(true)
      .setHandler(this);

    // Other parameters
    action.createParam(PARAM_TYPE)
      .setDescription("Select types of log to search")
      .setPossibleValues(Activity.Type.values())
      .setDefaultValue(StringUtils.join(Activity.Type.values(), ","));

    // Generic search parameters
    SearchOptions.defineFieldsParam(action, mapping.supportedFields());

    SearchOptions.definePageParams(action);
  }

  @Override
  public void handle(Request request, Response response) {
    ActivityQuery query = logService.newActivityQuery();
    SearchOptions searchOptions = SearchOptions.create(request);
    QueryOptions queryOptions = mapping.newQueryOptions(searchOptions);

    Result<Activity> results = logService.search(query, queryOptions);

    JsonWriter json = response.newJsonWriter().beginObject();
    searchOptions.writeStatistics(json, results);
    writeLogs(results, json, searchOptions);
    json.endObject().close();
  }

  private void writeLogs(Result<Activity> result, JsonWriter json, SearchOptions options) {
    json.name("logs").beginArray();
    for (Activity log : result.getHits()) {
      mapping.write((ActivityDoc) log, json, options);
    }
    json.endArray();
  }
}
