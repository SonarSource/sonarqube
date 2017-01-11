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
package org.sonar.server.component.ws;

import org.sonar.api.server.ws.WebService;

import static org.sonarqube.ws.client.component.ComponentsWsParameters.CONTROLLER_COMPONENTS;

public class ComponentsWs implements WebService {

  private final AppAction appAction;
  private final SearchViewComponentsAction searchViewComponentsAction;
  private final ComponentsWsAction[] actions;

  public ComponentsWs(AppAction appAction, SearchViewComponentsAction searchViewComponentsAction, ComponentsWsAction... actions) {
    this.appAction = appAction;
    this.searchViewComponentsAction = searchViewComponentsAction;
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(CONTROLLER_COMPONENTS)
      .setSince("4.2")
      .setDescription("Get information about a component (file, directory, project, ...) and its ancestors or descendants. " +
        "Update a project or module key.");

    for (ComponentsWsAction action : actions) {
      action.define(controller);
    }
    appAction.define(controller);
    searchViewComponentsAction.define(controller);

    controller.done();
  }

}
