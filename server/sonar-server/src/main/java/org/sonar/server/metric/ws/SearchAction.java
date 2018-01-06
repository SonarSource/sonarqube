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
package org.sonar.server.metric.ws;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.es.SearchOptions;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_ID;
import static org.sonar.server.metric.ws.MetricJsonWriter.FIELD_KEY;

public class SearchAction implements MetricsWsAction {

  private static final String ACTION = "search";

  public static final String PARAM_IS_CUSTOM = "isCustom";

  private final Set<String> allPossibleFields;

  private final DbClient dbClient;

  public SearchAction(DbClient dbClient) {
    this.dbClient = dbClient;
    Set<String> possibleFields = newHashSet(FIELD_ID, FIELD_KEY);
    possibleFields.addAll(MetricJsonWriter.OPTIONAL_FIELDS);
    allPossibleFields = possibleFields;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setSince("5.2")
      .setDescription("Search for metrics")
      .setResponseExample(getClass().getResource("example-search.json"))
      .addPagingParams(100, MAX_LIMIT)
      .addFieldsParam(MetricJsonWriter.OPTIONAL_FIELDS)
      .setHandler(this);

    action.createParam(PARAM_IS_CUSTOM)
      .setExampleValue("true")
      .setDescription("Choose custom metrics following 3 cases:" +
        "<ul>" +
        "<li>true: only custom metrics are returned</li>" +
        "<li>false: only non custom metrics are returned</li>" +
        "<li>not specified: all metrics are returned</li>" +
        "</ul>");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchOptions searchOptions = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(Param.PAGE),
        request.mandatoryParamAsInt(Param.PAGE_SIZE));
    Boolean isCustom = request.paramAsBoolean(PARAM_IS_CUSTOM);
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<MetricDto> metrics = dbClient.metricDao().selectEnabled(dbSession, isCustom, searchOptions.getOffset(), searchOptions.getLimit());
      int nbMetrics = dbClient.metricDao().countEnabled(dbSession, isCustom);
      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      Set<String> desiredFields = desiredFields(request.paramAsStrings(Param.FIELDS));
      writeMetrics(json, metrics, desiredFields);
      searchOptions.writeJson(json, nbMetrics);
      json.endObject();
      json.close();
    }
  }

  private Set<String> desiredFields(@Nullable List<String> fields) {
    if (fields == null || fields.isEmpty()) {
      return allPossibleFields;
    }

    return newHashSet(fields);
  }

  public static void writeMetrics(JsonWriter json, List<MetricDto> metrics, Set<String> desiredFields) {
    MetricJsonWriter.write(json, metrics, desiredFields);
  }
}
