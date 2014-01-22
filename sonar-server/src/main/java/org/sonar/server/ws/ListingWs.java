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

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

import java.util.List;

/**
 * This web service lists all the existing web services, including itself,
 * for documentation usage.
 *
 * @since 4.2
 */
public class ListingWs implements WebService {

  @Override
  public void define(final Context context) {
    NewController controller = context.newController("api/webservices")
      .setDescription("List web services")
      .setSince("4.2");
    controller.newAction("index")
      .setHandler(new RequestHandler() {
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

    // sort controllers by path
    Ordering<Controller> ordering = Ordering.natural().onResultOf(new Function<Controller, String>() {
      public String apply(Controller controller) {
        return controller.path();
      }
    });
    for (Controller controller : ordering.sortedCopy(controllers)) {
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
      // sort actions by key
      Ordering<Action> ordering = Ordering.natural().onResultOf(new Function<Action, String>() {
        public String apply(Action action) {
          return action.key();
        }
      });
      writer.name("actions").beginArray();
      for (Action action : ordering.sortedCopy(controller.actions())) {
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
    if (!action.params().isEmpty()) {
      // sort parameters by key
      Ordering<Param> ordering = Ordering.natural().onResultOf(new Function<Param, String>() {
        public String apply(Param param) {
          return param.key();
        }
      });
      writer.name("params").beginArray();
      for (Param param : ordering.sortedCopy(action.params())) {
        write(writer, param);
      }
      writer.endArray();
    }
    writer.endObject();
  }

  private void write(JsonWriter writer, Param param) {
    writer.beginObject();
    writer.prop("key", param.key());
    writer.prop("description", param.description());
    writer.endObject();
  }
}
