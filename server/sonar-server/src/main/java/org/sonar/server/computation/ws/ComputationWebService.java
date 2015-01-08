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

package org.sonar.server.computation.ws;

import org.sonar.api.server.ws.WebService;

/**
 * Web service to interact with the "computation" stack :
 * <ul>
 *   <li>queue of analysis reports to be integrated</li>
 *   <li>consolidation and aggregation of analysis measures</li>
 *   <li>persistence in datastores (database/elasticsearch)</li>
 * </ul>
 */
public class ComputationWebService implements WebService {
  public static final String API_ENDPOINT = "api/computation";

  private final ComputationWsAction[] actions;

  public ComputationWebService(ComputationWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context
      .createController(API_ENDPOINT)
      .setDescription("Analysis reports processed");
    for (ComputationWsAction action : actions) {
      action.define(controller);
    }
    controller.done();
  }
}
