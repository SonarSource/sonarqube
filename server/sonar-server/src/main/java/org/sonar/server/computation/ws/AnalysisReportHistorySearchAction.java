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

package org.sonar.server.computation.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.activity.Activity;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.activity.ws.ActivityMapping;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.search.ws.SearchOptions;
import org.sonar.server.user.UserSession;

import java.util.Arrays;
import java.util.Map;

public class AnalysisReportHistorySearchAction implements RequestHandler {

  public static final String PARAM_TYPE = "type";

  public static final String SEARCH_ACTION = "history";

  private final ActivityService logService;
  private final ActivityMapping mapping;

  public AnalysisReportHistorySearchAction(ActivityService logService, ActivityMapping mapping) {
    this.logService = logService;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(SEARCH_ACTION)
      .setDescription("Search for activities")
      .setSince("5.0")
      .setInternal(true)
      .setHandler(this);

    // Generic search parameters
    SearchOptions.defineFieldsParam(action, mapping.supportedFields());
    SearchOptions.definePageParams(action);
  }

  @Override
  public void handle(Request request, Response response) {
    /*
     * TODO should be done in a specific service, not logService but maybe something like AnalysisReportHistory ? A Facade Service could be
     * needed
     */
    checkUserRights();

    ActivityQuery query = logService.newActivityQuery();
    query.setTypes(Arrays.asList(Activity.Type.ANALYSIS_REPORT));

    SearchOptions searchOptions = SearchOptions.create(request);
    QueryContext queryContext = mapping.newQueryOptions(searchOptions);

    Result<Activity> results = logService.search(query, queryContext);

    JsonWriter json = response.newJsonWriter().beginObject();
    searchOptions.writeStatistics(json, results);
    writeReports(results, json, searchOptions);
    json.endObject().close();
  }

  private void checkUserRights() {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
  }

  private void writeReports(Result<Activity> result, JsonWriter json, SearchOptions options) {
    json.name("reports").beginArray();
    for (Activity reportActivity : result.getHits()) {
      json.beginObject();
      for (Map.Entry<String, String> detail : reportActivity.details().entrySet()) {
        json.prop(detail.getKey(), detail.getValue());
      }
      json.endObject();
    }
    json.endArray();
  }
}
