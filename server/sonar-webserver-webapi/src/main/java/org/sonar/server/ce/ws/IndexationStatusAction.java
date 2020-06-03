/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.ce.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonarqube.ws.Ce.IndexationStatusWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class IndexationStatusAction implements CeWsAction {
  private static final int PERCENT_100 = 100;
  private final DbClient dbClient;

  public IndexationStatusAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("indexation_status")
      .setDescription("Returns percentage of completed issue synchronization.")
      .setResponseExample(getClass().getResource("indexation_status-example.json"))
      .setHandler(this)
      .setInternal(true)
      .setSince("8.4");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    IndexationStatusWsResponse activityResponse = doHandle();
    writeProtobuf(activityResponse, wsRequest, wsResponse);
  }

  private IndexationStatusWsResponse doHandle() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      int branchesToProcess = dbClient.branchDao().countByNeedIssueSync(dbSession, false);
      int total = dbClient.branchDao().countAll(dbSession);

      int percentCompleted = PERCENT_100;
      if (total != 0) {
        percentCompleted = (int) Math.floor(PERCENT_100 * (double) branchesToProcess / total);
      }
      return IndexationStatusWsResponse.newBuilder()
        .setIsCompleted(percentCompleted == PERCENT_100)
        .setPercentCompleted(percentCompleted)
        .build();
    }
  }

}
