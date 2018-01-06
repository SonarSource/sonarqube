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
package org.sonar.server.metric.ws;

import org.sonar.api.server.ws.WebService;

public class MetricsWs implements WebService {

  public static final String ENDPOINT = "api/metrics";

  private final MetricsWsAction[] actions;

  public MetricsWs(MetricsWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(ENDPOINT);
    controller.setDescription("Get information on automatic metrics, and manage custom metrics. See also api/custom_measures.");
    controller.setSince("2.6");

    for (MetricsWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }
}
