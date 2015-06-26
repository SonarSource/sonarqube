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

package org.sonar.server.measure.custom.ws;

import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.metric.ws.MetricJsonWriter;

import static com.google.common.base.Preconditions.checkArgument;

public class MetricsAction implements CustomMeasuresWsAction {
  public static final String ACTION = "metrics";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";

  private final DbClient dbClient;

  public MetricsAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setSince("5.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-metrics.json"))
      .setDescription("List all custom metrics for which no custom measure already exists on a given project.<br /> " +
        "The project id or project key must be provided.");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue("org.apache.hbas:hbase");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      List<MetricDto> metrics = searchMetrics(dbSession, request);

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      MetricJsonWriter.write(json, metrics, MetricJsonWriter.ALL_FIELDS);
      json.endObject();
      json.close();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private List<MetricDto> searchMetrics(DbSession dbSession, Request request) {
    String projectUuidParam = request.param(PARAM_PROJECT_ID);
    String projectKeyParam = request.param(PARAM_PROJECT_KEY);
    String projectUuid;

    checkArgument(projectUuidParam != null ^ projectKeyParam != null, "The project uuid or the project key must be provided, not both.");

    if (projectUuidParam == null) {
      projectUuid = dbClient.componentDao().selectByKey(dbSession, projectKeyParam).uuid();
    } else {
      projectUuid = projectUuidParam;
    }

    return dbClient.metricDao().selectAvailableCustomMetricsByComponentUuid(dbSession, projectUuid);
  }
}
