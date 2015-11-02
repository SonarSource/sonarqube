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

package org.sonar.server.issue.filter;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.user.UserSession;

import static org.sonar.server.issue.filter.IssueFilterJsonWriter.writeWithName;

public class AppAction implements IssueFilterWsAction {

  private final IssueFilterService service;
  private final UserSession userSession;

  public AppAction(IssueFilterService service, UserSession userSession) {
    this.service = service;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app");
    action
      .setDescription("Data required for rendering the page 'Issues'")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-app.json"));
    action
      .createParam("id")
      .setDescription("Optionally, the ID of the current filter");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    // Current filter (optional)
    Integer filterId = request.paramAsInt("id");
    IssueFilterDto filter = null;
    if (filterId != null && filterId >= 0) {
      filter = service.find((long) filterId, userSession);
    }

    // Permissions
    json.prop("canManageFilters", userSession.isLoggedIn());
    json.prop("canBulkChange", userSession.isLoggedIn());

    // Selected filter
    if (filter != null) {
      writeWithName(json, filter, userSession);
    }

    // Favorite filters, if logged in
    if (userSession.isLoggedIn()) {
      List<IssueFilterDto> favorites = service.findFavoriteFilters(userSession);
      json.name("favorites").beginArray();
      for (IssueFilterDto favorite : favorites) {
        json
          .beginObject()
          .prop("id", favorite.getId())
          .prop("name", favorite.getName())
          .endObject();
      }
      json.endArray();
    }

    json.endObject();
    json.close();
  }

}
