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
package org.sonar.server.dashboard.ws;

import com.google.common.collect.ListMultimap;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.dashboard.WidgetDto;
import org.sonar.core.dashboard.WidgetPropertyDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import java.util.Collection;

public class DashboardsShowAction implements DashboardsAction {

  private static final String PARAM_KEY = "key";

  private final DbClient dbClient;

  public DashboardsShowAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController newController) {
    WebService.NewAction action = newController.createAction("show");
    action.setDescription("Detail of a dashboard (name, description, layout and widgets)");
    action.setInternal(true);
    action.setHandler(this);
    action.createParam(PARAM_KEY)
      .setDescription("Dashboard key")
      .setExampleValue("12345")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Integer userId = UserSession.get().userId();
      DashboardDto dashboard = dbClient.dashboardDao().getAllowedByKey(dbSession, request.mandatoryParamAsLong(PARAM_KEY),
        userId != null ? userId.longValue() : null);
      if (dashboard == null) {
        throw new NotFoundException();
      }

      JsonWriter json = response.newJsonWriter();
      json.beginObject();
      json.prop("key", dashboard.getKey());
      json.prop("name", dashboard.getName());
      json.prop("layout", dashboard.getColumnLayout());
      json.prop("desc", dashboard.getDescription());
      json.prop("global", dashboard.getGlobal());
      json.prop("shared", dashboard.getShared());
      if (dashboard.getUserId() != null) {
        UserDto user = dbClient.userDao().getUser(dashboard.getUserId());
        if (user != null) {
          json.name("owner").beginObject();
          // TODO to be shared and extracted from here
          json.prop("login", user.getLogin());
          json.prop("name", user.getName());
          json.endObject();
        }
      }
      // load widgets and related properties
      json.name("widgets").beginArray();
      Collection<WidgetDto> widgets = dbClient.widgetDao().findByDashboard(dbSession, dashboard.getKey());
      ListMultimap<Long, WidgetPropertyDto> propertiesByWidget = WidgetPropertyDto.groupByWidgetId(
        dbClient.widgetPropertyDao().findByDashboard(dbSession, dashboard.getKey()));
      for (WidgetDto widget : widgets) {
        json.beginObject();
        json.prop("id", widget.getId());
        json.prop("key", widget.getWidgetKey());
        json.prop("name", widget.getName());
        json.prop("desc", widget.getDescription());
        json.prop("col", widget.getColumnIndex());
        json.prop("row", widget.getRowIndex());
        json.prop("configured", widget.getConfigured());
        json.prop("componentId", widget.getResourceId());
        json.name("props").beginArray();
        for (WidgetPropertyDto prop : propertiesByWidget.get(widget.getId())) {
          json.beginObject();
          json.prop("key", prop.getPropertyKey());
          json.prop("val", prop.getTextValue());
          json.endObject();
        }
        json.endArray().endObject();
      }

      json.endArray();
      json.endObject();
      json.close();
    } finally {
      dbSession.close();
    }
  }
}
