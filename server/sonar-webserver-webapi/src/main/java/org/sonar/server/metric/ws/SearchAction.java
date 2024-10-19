/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.metric.ws;

import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.es.SearchOptions;

import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;

public class SearchAction implements MetricsWsAction {

  private static final String ACTION = "search";

  private final DbClient dbClient;

  public SearchAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setSince("5.2")
      .setDescription("Search for metrics")
      .setResponseExample(getClass().getResource("example-search.json"))
      .addPagingParams(100, MAX_PAGE_SIZE)
      .setChangelog(
        new Change("8.4", "Field 'id' in the response is deprecated"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchOptions searchOptions = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(Param.PAGE),
        request.mandatoryParamAsInt(Param.PAGE_SIZE));
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<MetricDto> metrics = dbClient.metricDao().selectEnabled(dbSession, searchOptions.getPage(), searchOptions.getLimit());
      int nbMetrics = dbClient.metricDao().countEnabled(dbSession);
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        MetricJsonWriter.write(json, metrics);
        searchOptions.writeJson(json, nbMetrics);
        json.endObject();
      }
    }
  }
}
