/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.rule.ws;

import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

public class TagsAction implements RulesWsAction {

  private final DbClient dbClient;

  public TagsAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller
      .createAction("tags")
      .setDescription("List rule tags")
      .setSince("4.4")
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "tags-example.json"))
        .setChangelog(new Change("9.4", "Max page size increased to 500"));

    action.createSearchQuery("misra", "tags");
    action.createPageSize(10, 500);
  }

  @Override
  public void handle(Request request, Response response) {
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt("ps");

    try (DbSession dbSession = dbClient.openSession(false)) {
      Pagination pagination = Pagination.forPage(1).andSize(pageSize == 0 ? 500 : pageSize);
      List<String> tags = dbClient.ruleDao().selectTags(dbSession, query, pagination);
      writeResponse(response, tags);
    }
  }

  private static void writeResponse(Response response, List<String> tags) {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("tags").beginArray();
      tags.forEach(json::value);
      json.endArray().endObject();
    }
  }
}
