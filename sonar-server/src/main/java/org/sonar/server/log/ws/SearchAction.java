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
package org.sonar.server.log.ws;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.log.Log;
import org.sonar.server.log.LogService;
import org.sonar.server.log.index.LogDoc;
import org.sonar.server.log.index.LogQuery;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.search.ws.SearchOptions;

/**
 * @since 4.4
 */
public class SearchAction implements RequestHandler {

  public static final String PARAM_TYPE = "type";

  public static final String SEARCH_ACTION = "search";

  private final LogService logService;
  private final LogMapping mapping;

  public SearchAction(LogService logService, LogMapping mapping) {
    this.logService = logService;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(SEARCH_ACTION)
      .setDescription("Search for a logs")
      .setSince("4.4")
      .setHandler(this);

    // Other parameters
    action.createParam(PARAM_TYPE)
      .setDescription("Select types of log to search")
      .setPossibleValues(Log.Type.values())
      .setDefaultValue(StringUtils.join(Log.Type.values(), ","));

    // Generic search parameters
    SearchOptions.defineFieldsParam(action, mapping.supportedFields());

    SearchOptions.definePageParams(action);
  }

  @Override
  public void handle(Request request, Response response) {
    LogQuery query = createLogQuery(logService.newLogQuery(), request);
    SearchOptions searchOptions = SearchOptions.create(request);
    QueryOptions queryOptions = mapping.newQueryOptions(searchOptions);

    Result<Log> results = logService.search(query, queryOptions);

    JsonWriter json = response.newJsonWriter().beginObject();
    searchOptions.writeStatistics(json, results);
    writeLogs(results, json, searchOptions);
    json.endObject().close();
  }

  public static LogQuery createLogQuery(LogQuery query, Request request) {
    // query.setTypes(request.param(SearchOptions.PARAM_TEXT_QUERY));
    return query;
  }

  private void writeLogs(Result<Log> result, JsonWriter json, SearchOptions options) {
    json.name("logs").beginArray();
    for (Log log : result.getHits()) {
      mapping.write((LogDoc) log, json, options);
    }
    json.endArray();
  }
}
