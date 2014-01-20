/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.server.user.UserSession;

import java.util.List;

public class IssueFilterWs implements WebService {

  private final IssueFilterService service;

  public IssueFilterWs(IssueFilterService service) {
    this.service = service;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.newController("api/issue_filters")
      .setSince("4.2")
      .setDescription("Issue Filters");

    NewAction search = controller.newAction("page");
    search
      .setDescription("Data required for rendering page 'Issues'. Internal use only.")
      .setPrivate(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          page(request, response);
        }
      });

    controller.done();
  }

  private void page(Request request, Response response) {
    UserSession session = UserSession.get();

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    // Permissions
    json.prop("canManageFilter", session.isLoggedIn());
    json.prop("canBulkChange", session.isLoggedIn());

    // Current filter (optional)
    int filterId = request.intParam("id", -1);
    if (filterId >= 0) {
      DefaultIssueFilter filter = service.find((long)filterId, session);
      json.name("filter")
        .beginObject()
        .prop("id", filter.id())
        .prop("name", filter.name())
        .prop("user", filter.user())
        .prop("shared", filter.shared())
        .prop("query", filter.data())
        .endObject();
    }

    // Favorite filters, if logged in
    if (session.isLoggedIn()) {
      List<DefaultIssueFilter> favorites = service.findFavoriteFilters(session);
      json.name("favorites").beginArray();
      for (DefaultIssueFilter favorite : favorites) {
        json
          .beginObject()
          .prop("id", favorite.id())
          .prop("name", favorite.name())
          .endObject();
      }
      json.endArray();
    }

    json.endObject();
    json.close();
  }
}
