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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.index.ActivityDoc;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.activity.index.ActivityQuery;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.search.QueryContext;

public class SearchAction implements RequestHandler {

  public static final String PARAM_TYPE = "type";
  public static final String SEARCH_ACTION = "search";

  private final ActivityIndex activityIndex;
  private final ActivityMapping docToJsonMapping;

  public SearchAction(ActivityIndex activityIndex, ActivityMapping docToJsonMapping) {
    this.activityIndex = activityIndex;
    this.docToJsonMapping = docToJsonMapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction(SEARCH_ACTION)
      .setDescription("Search for activities")
      .setSince("4.4")
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_TYPE)
      .setDescription("Activity type")
      .setPossibleValues(Activity.Type.values());

    action.addPagingParams(10);
    action.addFieldsParam(docToJsonMapping.supportedFields());
  }

  @Override
  public void handle(Request request, Response response) {
    ActivityQuery query = new ActivityQuery();
    query.setTypes(request.paramAsStrings(PARAM_TYPE));

    SearchOptions options = new SearchOptions();
    options.setPage(request.mandatoryParamAsInt(WebService.Param.PAGE), request.mandatoryParamAsInt(WebService.Param.PAGE_SIZE));

    SearchResult<ActivityDoc> results = activityIndex.search(query, options);

    JsonWriter json = response.newJsonWriter().beginObject();
    options.writeJson(json, results.getTotal());
    writeActivities(results, json, new QueryContext().setFieldsToReturn(request.paramAsStrings(WebService.Param.FIELDS)));
    json.endObject().close();
  }

  private void writeActivities(SearchResult<ActivityDoc> docs, JsonWriter json, QueryContext context) {
    json.name("logs").beginArray();
    for (ActivityDoc doc : docs.getDocs()) {
      docToJsonMapping.write(doc, json, context);
    }
    json.endArray();
  }
}
