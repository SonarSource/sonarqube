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
package org.sonar.server.ws;

import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.ws.Request;
import org.sonar.api.web.ws.RequestHandler;
import org.sonar.api.web.ws.Response;
import org.sonar.api.web.ws.WebService;

import java.util.List;

/**
 * This web service lists all the existing web services, including itself,
 * for documentation usage.
 *
 * @since 4.2
 */
public class ListingWebService implements WebService {

  @Override
  public void define(final Context context) {
    NewController controller = context.newController("api/webservices")
      .setDescription("List web services")
      .setSince("4.2");
    controller.newAction("index").setHandler(new RequestHandler() {
      @Override
      public void handle(Request request, Response response) {
        list(context.controllers(), response);
      }
    });
    controller.done();
  }

  void list(List<Controller> controllers, Response response) {
    JsonWriter writer = response.newJsonWriter();
    writer.beginObject();
    writer.name("webServices").beginArray();
    for (Controller controller : controllers) {
      write(writer, controller);
    }
    writer.endArray();
    writer.endObject();
    writer.close();
  }

  private void write(JsonWriter writer, Controller controller) {
    writer.beginObject();
    writer.prop("path", controller.path());
    writer.prop("since", controller.since());
    writer.prop("description", controller.description());
    if (!controller.actions().isEmpty()) {
      writer.name("actions").beginArray();
      for (Action action : controller.actions()) {
        write(writer, action);
      }
      writer.endArray();
    }
    writer.endObject();
  }

  private void write(JsonWriter writer, Action action) {
    writer.beginObject();
    writer.prop("key", action.key());
    writer.prop("description", action.description());
    writer.prop("since", action.since());
    writer.prop("post", action.isPost());
    writer.endObject();
  }
}
