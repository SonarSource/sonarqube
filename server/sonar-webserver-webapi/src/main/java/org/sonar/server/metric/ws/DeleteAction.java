/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.join;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;

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
      .setDescription("Delete metrics and associated measures. Delete only custom metrics.<br/>" +
        "Ids or keys must be provided.<br/>" +
        "Requires 'Administer System' permission.")
      .setHandler(this)
      .setSince("5.2")
      .setDeprecatedSince("7.7")
      .setPost(true)
      .setChangelog(
        new Change("8.4", "Parameter 'ids' format changes from integer to string."));

    action.createParam(PARAM_IDS)
      .setDescription("Metrics uuids to delete.")
      .setExampleValue(join(", ", UUID_EXAMPLE_01, UUID_EXAMPLE_02, UUID_EXAMPLE_03));

    action.createParam(PARAM_KEYS)
      .setDescription("Metrics keys to delete")
      .setExampleValue("team_size, business_value");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<String> uuids = loadUuids(dbSession, request);
      dbClient.metricDao().disableCustomByUuids(dbSession, uuids);
      dbClient.gateConditionDao().deleteConditionsWithInvalidMetrics(dbSession);
      dbSession.commit();
    }

    response.noContent();
  }

  private List<String> loadUuids(DbSession dbSession, Request request) {
    List<String> uuids = request.paramAsStrings(PARAM_IDS);
    List<String> keys = request.paramAsStrings(PARAM_KEYS);
    checkArgument(uuids != null || keys != null, "Uuids or keys must be provided.");
    if (uuids == null) {
      uuids = Lists.transform(dbClient.metricDao().selectByKeys(dbSession, keys), MetricDto::getUuid);
    }

    return uuids;
  }
}
