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
package org.sonar.server.ce.ws;

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.nio.charset.StandardCharsets.UTF_8;

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
      .setDescription("Get details about Compute Engine tasks.");
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
      boolean isQueueEmpty = false;
      try (DbSession dbSession = dbClient.openSession(false)) {
        isQueueEmpty = dbClient.ceQueueDao().selectAllInAscOrder(dbSession).isEmpty();
      } catch (Exception e) {
        // ignore this FP : https://gist.github.com/simonbrandhof/3d98f854d427519ef5b858a73b59585b
        Loggers.get(getClass()).error("Cannot select rows from ce_queue", e);
      }
      IOUtils.write(String.valueOf(isQueueEmpty), response.stream().output(), UTF_8);
    }
  }
}
