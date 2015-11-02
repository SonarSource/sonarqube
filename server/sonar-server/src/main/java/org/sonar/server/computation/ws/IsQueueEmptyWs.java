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

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

/**
 * Internal WebService with one action
 */
public class IsQueueEmptyWs implements WebService {
  public static final String API_ENDPOINT = "api/analysis_reports";

  private final IsQueueEmptyAction action;

  public IsQueueEmptyWs(DbClient dbClient) {
    this.action = new IsQueueEmptyAction(dbClient);
  }

  @Override
  public void define(Context context) {
    NewController controller = context
      .createController(API_ENDPOINT)
      .setDescription("For internal testing - do not use");
    action.define(controller);
    controller.done();
  }

  static class IsQueueEmptyAction implements RequestHandler {
    private final DbClient dbClient;

    public IsQueueEmptyAction(DbClient dbClient) {
      this.dbClient = dbClient;
    }

    public void define(WebService.NewController controller) {
      controller
        .createAction("is_queue_empty")
        .setDescription("Check if the queue of Compute Engine is empty")
        .setResponseExample(getClass().getResource("is_queue_empty-example.txt"))
        .setSince("5.1")
        .setInternal(true)
        .setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
      DbSession dbSession = dbClient.openSession(false);
      try {
        boolean isQueueEmpty = dbClient.ceQueueDao().selectAllInAscOrder(dbSession).isEmpty();
        IOUtils.write(String.valueOf(isQueueEmpty), response.stream().output());
      } finally {
        dbClient.closeSession(dbSession);
      }
    }
  }
}
