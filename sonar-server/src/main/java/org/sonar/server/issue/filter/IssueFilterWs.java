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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.permission.GlobalPermissions;
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

    NewAction app = controller.newAction("app");
    app
      .setDescription("Data required for rendering the page 'Issues'")
      .setInternal(true)
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          app(request, response);
        }
      });

    NewAction show = controller.newAction("show");
    show
      .setDescription("Get detail of issue filter")
      .setSince("4.2")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          show(request, response);
        }
      })
      .newParam("id");

    controller.done();
  }

  private void app(Request request, Response response) {
    UserSession session = UserSession.get();

    JsonWriter json = response.newJsonWriter();
    json.beginObject();

    // Current filter (optional)
    int filterId = request.paramAsInt("id", -1);
    DefaultIssueFilter filter = null;
    if (filterId >= 0) {
      filter = service.find((long) filterId, session);
    }

    // Permissions
    json.prop("canManageFilters", session.isLoggedIn());
    json.prop("canBulkChange", session.isLoggedIn());

    // Selected filter
    if (filter != null) {
      json.name("filter");
      writeFilterJson(session, filter, json);
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

  private void show(Request request, Response response) {
    UserSession session = UserSession.get();
    DefaultIssueFilter filter = service.find(Long.parseLong(request.mandatoryParam("id")), session);

    JsonWriter json = response.newJsonWriter();
    json.beginObject();
    json.name("filter");
    writeFilterJson(session, filter, json);
    json.endObject();
    json.close();
  }

  private JsonWriter writeFilterJson(UserSession session, DefaultIssueFilter filter, JsonWriter json) {
    return json.beginObject()
      .prop("id", filter.id())
      .prop("name", filter.name())
      .prop("user", filter.user())
      .prop("shared", filter.shared())
      .prop("query", filter.data())
      .prop("canModify", canModifyFilter(session, filter))
      .endObject();
  }

  private boolean canModifyFilter(UserSession session, DefaultIssueFilter filter) {
    return session.isLoggedIn() &&
      (StringUtils.equals(filter.user(), session.login()) || session.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN));
  }
}
