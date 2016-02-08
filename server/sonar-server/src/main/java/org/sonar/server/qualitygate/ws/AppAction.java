/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.qualitygate.ws;

import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualitygate.QualityGates;

public class AppAction implements QGateWsAction {

  private final QualityGates qualityGates;

  public AppAction(QualityGates qualityGates) {
    this.qualityGates = qualityGates;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("app")
      .setInternal(true)
      .setDescription("Get initialization items for the admin UI. For internal use")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.3")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    JsonWriter writer = response.newJsonWriter().beginObject();
    addPermissions(writer);
    addPeriods(writer);
    addMetrics(writer);
    writer.endObject().close();
  }

  private void addPermissions(JsonWriter writer) {
    writer.prop("edit", qualityGates.currentUserHasWritePermission());
  }

  private static void addPeriods(JsonWriter writer) {
    writer.name("periods").beginArray();
    writer.beginObject()
      .prop("key", 1L)
      .prop("text", "Leak")
      .endObject();
    writer.endArray();
  }

  private void addMetrics(JsonWriter writer) {
    writer.name("metrics").beginArray();
    for (Metric metric : qualityGates.gateMetrics()) {
      writer.beginObject()
        .prop("id", metric.getId())
        .prop("key", metric.getKey())
        .prop("name", metric.getName())
        .prop("type", metric.getType().toString())
        .prop("domain", metric.getDomain())
        .prop("hidden", BooleanUtils.isNotFalse(metric.isHidden()))
        .endObject();
    }
    writer.endArray();
  }

}
