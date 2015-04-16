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
package org.sonar.server.qualitygate.ws;

import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.i18n.I18n;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.timemachine.Periods;
import org.sonar.server.qualitygate.QualityGates;

import java.util.Locale;

public class QGatesAppAction implements BaseQGateWsAction {

  private final QualityGates qualityGates;

  private final Periods periods;

  private final I18n i18n;

  public QGatesAppAction(QualityGates qualityGates, Periods periods, I18n i18n) {
    this.qualityGates = qualityGates;
    this.periods = periods;
    this.i18n = i18n;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("app")
      .setInternal(true)
      .setDescription("Get initialization items for the admin UI. For internal use")
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

  private void addPeriods(JsonWriter writer) {
    writer.name("periods").beginArray();
    for (int i = 0; i < 3; i++) {
      writer.beginObject().prop("key", (long) i + 1).prop("text", periods.label(i + 1)).endObject();
    }
    addProjectPeriod(4, writer);
    addProjectPeriod(5, writer);
    writer.endArray();
  }

  private void addProjectPeriod(int periodIndex, JsonWriter writer) {
    writer.beginObject().prop("key", periodIndex).prop("text",
      i18n.message(Locale.getDefault(), "quality_gates.project_period", "Period " + periodIndex, periodIndex)
      ).endObject();
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
