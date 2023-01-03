/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.metric.ws;

import java.util.Collection;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;

public class UserMetricsAction implements MetricsWsAction {

  private final MetricFinder metricFinder;

  public UserMetricsAction(MetricFinder metricFinder) {
    this.metricFinder = metricFinder;
  }

  @Override
  public void handle(Request request, Response response) {
    Collection<Metric> metrics = metricFinder.findAll();
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject()
        .name("manual_metrics")
        .beginObject();
      metrics.stream()
        .filter(metric -> metric.getUserManaged() && ValueType.STRING == metric.getType())
        .forEach(metric -> json.prop(metric.getKey(), metric.getName()));
      json.endObject();
      json.name("remote_servers").beginArray().endArray().endObject();
    }
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("user_metrics")
      .setDescription("Metrics managed by users")
      .setSince("1.0")
      .setDeprecatedSince("7.4")
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-user-metrics.json"))
      .setHandler(this);
  }
}
