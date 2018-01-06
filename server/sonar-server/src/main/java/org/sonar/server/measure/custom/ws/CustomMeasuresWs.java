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
package org.sonar.server.measure.custom.ws;

import org.sonar.api.server.ws.WebService;

public class CustomMeasuresWs implements WebService {
  public static final String ENDPOINT = "api/custom_measures";

  private final CustomMeasuresWsAction[] actions;

  public CustomMeasuresWs(CustomMeasuresWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(ENDPOINT)
      .setDescription("Manage custom measures for a project. See also api/metrics.")
      .setSince("5.2");

    for (CustomMeasuresWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }
}
