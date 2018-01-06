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
package org.sonar.server.ws.ws;

import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;

public class ListAction implements WebServicesWsAction {
  private WebService.Context context;

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("list")
      .setSince("4.2")
      .setDescription("List web services")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(this);

    action
      .createParam("include_internals")
      .setDescription("Include web services that are implemented for internal use only. Their forward-compatibility is not assured")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    checkState(context != null && !context.controllers().isEmpty(), "Web service controllers must be loaded before calling the action");

    boolean includeInternals = request.mandatoryParamAsBoolean("include_internals");
    JsonWriter writer = response.newJsonWriter();
    writer.beginObject();
    writer.name("webServices").beginArray();

    // sort controllers by path
    Ordering<WebService.Controller> ordering = Ordering.natural().onResultOf(WebService.Controller::path);
    for (WebService.Controller controller : ordering.sortedCopy(context.controllers())) {
      writeController(writer, controller, includeInternals);
    }
    writer.endArray();
    writer.endObject();
    writer.close();
  }

  @Override
  public void setContext(WebService.Context context) {
    this.context = context;
  }

  private static void writeController(JsonWriter writer, WebService.Controller controller, boolean includeInternals) {
    if (includeInternals || !controller.isInternal()) {
      writer.beginObject();
      writer.prop("path", controller.path());
      writer.prop("since", controller.since());
      writer.prop("description", controller.description());
      // sort actions by key
      Ordering<WebService.Action> ordering = Ordering.natural().onResultOf(WebService.Action::key);
      writer.name("actions").beginArray();
      for (WebService.Action action : ordering.sortedCopy(controller.actions())) {
        writeAction(writer, action, includeInternals);
      }
      writer.endArray();
      writer.endObject();
    }
  }

  private static void writeAction(JsonWriter writer, WebService.Action action, boolean includeInternals) {
    if (includeInternals || !action.isInternal()) {
      writer.beginObject();
      writer.prop("key", action.key());
      writer.prop("description", action.description());
      writer.prop("since", action.since());
      writer.prop("deprecatedSince", action.deprecatedSince());
      writer.prop("internal", action.isInternal());
      writer.prop("post", action.isPost());
      writer.prop("hasResponseExample", action.responseExample() != null);
      writeChangelog(writer, action);
      writeParameters(writer, action, includeInternals);
      writer.endObject();
    }
  }

  private static void writeParameters(JsonWriter writer, WebService.Action action, boolean includeInternals) {
    List<WebService.Param> params = action.params().stream().filter(p -> includeInternals || !p.isInternal()).collect(MoreCollectors.toList());
    if (!params.isEmpty()) {
      // sort parameters by key
      Ordering<WebService.Param> ordering = Ordering.natural().onResultOf(WebService.Param::key);
      writer.name("params").beginArray();
      for (WebService.Param param : ordering.sortedCopy(params)) {
        writeParam(writer, param);
      }
      writer.endArray();
    }
  }

  private static void writeParam(JsonWriter writer, WebService.Param param) {
    writer.beginObject();
    writer.prop("key", param.key());
    writer.prop("description", param.description());
    writer.prop("since", param.since());
    writer.prop("required", param.isRequired());
    writer.prop("internal", param.isInternal());
    writer.prop("defaultValue", param.defaultValue());
    writer.prop("exampleValue", param.exampleValue());
    writer.prop("deprecatedSince", param.deprecatedSince());
    writer.prop("deprecatedKey", param.deprecatedKey());
    writer.prop("deprecatedKeySince", param.deprecatedKeySince());
    writer.prop("maxValuesAllowed", param.maxValuesAllowed());
    ofNullable(param.possibleValues()).ifPresent(possibleValues -> writer.name("possibleValues").beginArray().values(possibleValues).endArray());
    ofNullable(param.maximumLength()).ifPresent(maximumLength -> writer.prop("maximumLength", maximumLength));
    ofNullable(param.minimumLength()).ifPresent(minimumLength -> writer.prop("minimumLength", minimumLength));
    ofNullable(param.maximumValue()).ifPresent(maximumValue -> writer.prop("maximumValue", maximumValue));
    writer.endObject();
  }

  private static void writeChangelog(JsonWriter writer, WebService.Action action) {
    writer.name("changelog").beginArray();
    action.changelog().stream()
      .sorted(Comparator.comparing(Change::getVersion).reversed())
      .forEach(changelog -> {
        writer.beginObject();
        writer.prop("description", changelog.getDescription());
        writer.prop("version", changelog.getVersion());
        writer.endObject();
      });
    writer.endArray();
  }

}
