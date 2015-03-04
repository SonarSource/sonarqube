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
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.ws.IssuesWs;
import org.sonar.server.user.UserSession;

import java.util.Arrays;
import java.util.Map;

// FIXME replace by api/activities/search
public class HistoryWsAction implements ComputationWsAction, RequestHandler {

  public static final String PARAM_TYPE = "type";

  private final ActivityIndex activityIndex;

  public HistoryWsAction(ActivityIndex activityIndex) {
    this.activityIndex = activityIndex;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("history")
      .setDescription("Past integrations of analysis reports")
      .setSince("5.0")
      .setInternal(true)
      .setHandler(this);

    action.addPagingParams(10);
  }

  @Override
  public void handle(Request request, Response response) {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    ActivityQuery query = new ActivityQuery();
    query.setTypes(Arrays.asList(Activity.Type.ANALYSIS_REPORT.name()));

    SearchOptions options = new SearchOptions();
    options.setPage(request.mandatoryParamAsInt(IssuesWs.Param.PAGE), request.mandatoryParamAsInt(IssuesWs.Param.PAGE_SIZE));
    SearchResult<ActivityDoc> results = activityIndex.search(query, options);

    JsonWriter json = response.newJsonWriter().beginObject();
    options.writeJson(json, results.getTotal());
    writeReports(results, json);
    json.endObject().close();
  }

  private void writeReports(SearchResult<ActivityDoc> result, JsonWriter json) {
    json.name("reports").beginArray();
    for (ActivityDoc doc : result.getDocs()) {
      json.beginObject();
      for (Map.Entry<String, String> detail : doc.getDetails().entrySet()) {
        json.prop(detail.getKey(), detail.getValue());
      }
      json.endObject();
    }
    json.endArray();
  }
}
