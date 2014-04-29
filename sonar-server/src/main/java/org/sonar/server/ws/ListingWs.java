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
package org.sonar.server.ws;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

import java.io.IOException;
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
    NewController controller = context
      .createController("api/webservices")
      .setDescription("List web services");
    defineList(context, controller);
    defineResponseExample(context, controller);
    controller.done();
  }

  private void defineList(final Context context, NewController controller) {
    controller
      .createAction("list")
      .setSince("4.2")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          handleList(context.controllers(), response);
        }
      });
  }

  private void defineResponseExample(final Context context, NewController controller) {
    NewAction action = controller
      .createAction("responseExample")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          Controller controller = context.controller(request.mandatoryParam("controller"));
          Action action = controller.action(request.mandatoryParam("action"));
          handleResponseExample(action, response);
        }
      });
    action.createParam("controller").setRequired(true);
    action.createParam("action").setRequired(true);
  }

  private void handleResponseExample(Action action, Response response) throws IOException {
    if (action.responseExample() != null) {
      response
        .newJsonWriter()
        .beginObject()
        .prop("format", action.responseExampleFormat())
        .prop("example", IOUtils.toString(action.responseExample(), Charsets.UTF_8))
        .endObject()
        .close();
    } else {
      response.noContent();
    }
  }

  void handleList(List<Controller> controllers, Response response) {
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
      writeController(writer, controller);
    }
    writer.endArray();
    writer.endObject();
    writer.close();
  }

  private void writeController(JsonWriter writer, Controller controller) {
    writer.beginObject();
    writer.prop("path", controller.path());
    writer.prop("since", controller.since());
    writer.prop("description", controller.description());
    // sort actions by key
    Ordering<Action> ordering = Ordering.natural().onResultOf(new Function<Action, String>() {
      public String apply(Action action) {
        return action.key();
      }
    });
    writer.name("actions").beginArray();
    for (Action action : ordering.sortedCopy(controller.actions())) {
      writeAction(writer, action);
    }
    writer.endArray();
    writer.endObject();
  }

  private void writeAction(JsonWriter writer, Action action) {
    writer.beginObject();
    writer.prop("key", action.key());
    writer.prop("description", action.description());
    writer.prop("since", action.since());
    writer.prop("internal", action.isInternal());
    writer.prop("post", action.isPost());
    writer.prop("hasResponseExample", action.responseExample()!=null);
    if (!action.params().isEmpty()) {
      // sort parameters by key
      Ordering<Param> ordering = Ordering.natural().onResultOf(new Function<Param, String>() {
        public String apply(Param param) {
          return param.key();
        }
      });
      writer.name("params").beginArray();
      for (Param param : ordering.sortedCopy(action.params())) {
        writeParam(writer, param);
      }
      writer.endArray();
    }
    writer.endObject();
  }

  private void writeParam(JsonWriter writer, Param param) {
    writer.beginObject();
    writer.prop("key", param.key());
    writer.prop("description", param.description());
    writer.prop("required", param.isRequired());
    writer.prop("defaultValue", param.defaultValue());
    writer.prop("exampleValue", param.exampleValue());
    if (param.possibleValues() != null) {
      writer.name("possibleValues").beginArray();
      for (String s : param.possibleValues()) {
        writer.value(s);
      }
      writer.endArray();
    }
    writer.endObject();
  }
}
