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

import com.google.common.collect.Lists;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;

public class DeleteAction implements MetricsWsAction {
  private static final String PARAM_IDS = "ids";
  private static final String PARAM_KEYS = "keys";

  private final DbClient dbClient;
  private final UserSession userSession;

  public DeleteAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("delete")
      .setHandler(this)
      .setSince("5.2")
      .setPost(true)
      .setDescription("Delete metrics and associated measures. Delete only custom metrics.<br />Ids or keys must be provided. <br />Requires 'Administer System' permission.");

    action.createParam(PARAM_IDS)
      .setDescription("Metrics ids to delete.")
      .setExampleValue("5, 23, 42");

    action.createParam(PARAM_KEYS)
      .setDescription("Metrics keys to delete")
      .setExampleValue("team_size, business_value");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<Integer> ids = loadIds(dbSession, request);
      dbClient.metricDao().disableCustomByIds(dbSession, ids);
      dbClient.customMeasureDao().deleteByMetricIds(dbSession, ids);
      dbClient.gateConditionDao().deleteConditionsWithInvalidMetrics(dbSession);
      dbSession.commit();
    }

    response.noContent();
  }

  private List<Integer> loadIds(DbSession dbSession, Request request) {
    List<String> idsAsStrings = request.paramAsStrings(PARAM_IDS);
    List<String> keys = request.paramAsStrings(PARAM_KEYS);
    checkArgument(idsAsStrings != null || keys != null, "Ids or keys must be provided.");
    List<Integer> ids;
    if (idsAsStrings != null) {
      ids = Lists.transform(idsAsStrings, Integer::valueOf);
    } else {
      ids = Lists.transform(dbClient.metricDao().selectByKeys(dbSession, keys), MetricDto::getId);
    }

    return ids;
  }
}
