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

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * This web service lists all the existing web services, including itself,
 * for documentation usage.
 *
 * @since 4.2
 */
public class WebServicesWs implements WebService {

  @Override
  public void define(final Context context) {
    NewController controller = context
      .createController("api/webservices")
      .setSince("4.2")
      .setDescription("List web services");
    defineList(context, controller);
    defineResponseExample(context, controller);
    controller.done();
  }

  private void defineList(final Context context, NewController controller) {
    NewAction action = controller
      .createAction("list")
      .setSince("4.2")
      .setDescription("List web services")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) {
          handleList(context.controllers(), request, response);
        }
      });
    action
      .createParam("include_internals")
      .setDescription("Include web services that are implemented for internal use only. Their forward-compatibility is " +
        "not assured")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  private void defineResponseExample(final Context context, NewController controller) {
    NewAction action = controller
      .createAction("response_example")
      .setDescription("Display web service response example")
      .setResponseExample(getClass().getResource("response_example-example.json"))
      .setSince("4.4")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          String controllerKey = request.mandatoryParam("controller");
          Controller controller = context.controller(controllerKey);
          if (controller == null) {
            throw new IllegalArgumentException("Controller does not exist: " + controllerKey);
          }
          String actionKey = request.mandatoryParam("action");
          Action action = controller.action(actionKey);
          if (action == null) {
            throw new IllegalArgumentException("Action does not exist: " + actionKey);
          }
          handleResponseExample(action, response);
        }
      });
    action.createParam("controller")
      .setRequired(true)
      .setDescription("Controller of the web service")
      .setExampleValue("api/issues");
    action.createParam("action")
      .setRequired(true)
      .setDescription("Action of the web service")
      .setExampleValue("search");
  }

  private void handleResponseExample(Action action, Response response) throws IOException {
    if (action.responseExample() != null) {
      response
        .newJsonWriter()
        .beginObject()
        .prop("format", action.responseExampleFormat())
        .prop("example", action.responseExampleAsString())
        .endObject()
        .close();
    } else {
      response.noContent();
    }
  }

  void handleList(List<Controller> controllers, Request request, Response response) {
    boolean includeInternals = request.mandatoryParamAsBoolean("include_internals");
    JsonWriter writer = response.newJsonWriter();
    writer.beginObject();
    writer.name("webServices").beginArray();

    // sort controllers by path
    Ordering<Controller> ordering = Ordering.natural().onResultOf(new Function<Controller, String>() {
      @Override
      public String apply(Controller controller) {
        return controller.path();
      }
    });
    for (Controller controller : ordering.sortedCopy(controllers)) {
      writeController(writer, controller, includeInternals);
    }
    writer.endArray();
    writer.endObject();
    writer.close();
  }

  private void writeController(JsonWriter writer, Controller controller, boolean includeInternals) {
    if (includeInternals || !controller.isInternal()) {
      writer.beginObject();
      writer.prop("path", controller.path());
      writer.prop("since", controller.since());
      writer.prop("description", controller.description());
      // sort actions by key
      Ordering<Action> ordering = Ordering.natural().onResultOf(new Function<Action, String>() {
        @Override
        public String apply(Action action) {
          return action.key();
        }
      });
      writer.name("actions").beginArray();
      for (Action action : ordering.sortedCopy(controller.actions())) {
        writeAction(writer, action, includeInternals);
      }
      writer.endArray();
      writer.endObject();
    }
  }

  private void writeAction(JsonWriter writer, Action action, boolean includeInternals) {
    if (includeInternals || !action.isInternal()) {
      writer.beginObject();
      writer.prop("key", action.key());
      writer.prop("description", action.description());
      writer.prop("since", action.since());
      writer.prop("deprecatedSince", action.deprecatedSince());
      writer.prop("internal", action.isInternal());
      writer.prop("post", action.isPost());
      writer.prop("hasResponseExample", action.responseExample() != null);
      if (!action.params().isEmpty()) {
        // sort parameters by key
        Ordering<Param> ordering = Ordering.natural().onResultOf(new Function<Param, String>() {
          @Override
          public String apply(@Nullable Param param) {
            return param != null ? param.key() : null;
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
  }

  private void writeParam(JsonWriter writer, Param param) {
    writer.beginObject();
    writer.prop("key", param.key());
    writer.prop("description", param.description());
    writer.prop("since", param.since());
    writer.prop("required", param.isRequired());
    writer.prop("defaultValue", param.defaultValue());
    writer.prop("exampleValue", param.exampleValue());
    writer.prop("deprecatedSince", param.deprecatedSince());
    Set<String> possibleValues = param.possibleValues();
    if (possibleValues != null) {
      writer.name("possibleValues").beginArray().values(possibleValues).endArray();
    }
    writer.endObject();
  }
}
