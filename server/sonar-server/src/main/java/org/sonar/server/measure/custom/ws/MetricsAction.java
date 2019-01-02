/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.measure.custom.ws;

import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.metric.ws.MetricJsonWriter;
import org.sonar.server.user.UserSession;

import static org.sonar.server.component.ComponentFinder.ParamNames.PROJECT_ID_AND_KEY;
import static org.sonar.server.measure.custom.ws.CustomMeasureValidator.checkPermissions;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class MetricsAction implements CustomMeasuresWsAction {
  public static final String ACTION = "metrics";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public MetricsAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setSince("5.2")
      .setDeprecatedSince("7.4")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-metrics.json"))
      .setDescription("List all custom metrics for which no custom measure already exists on a given project.<br /> " +
        "The project id or project key must be provided.<br />" +
        "Requires 'Administer System' permission or 'Administer' permission on the project.");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByUuidOrKey(dbSession, request.param(CreateAction.PARAM_PROJECT_ID), request.param(CreateAction.PARAM_PROJECT_KEY),
        PROJECT_ID_AND_KEY);
      checkPermissions(userSession, project);
      List<MetricDto> metrics = searchMetrics(dbSession, project);

      writeResponse(response, metrics);
    }
  }

  private static void writeResponse(Response response, List<MetricDto> metrics) {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    MetricJsonWriter.write(json, metrics, MetricJsonWriter.ALL_FIELDS);
    json.endObject();
    json.close();
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, ComponentDto project) {
    return dbClient.metricDao().selectAvailableCustomMetricsByComponentUuid(dbSession, project.uuid());
  }
}
