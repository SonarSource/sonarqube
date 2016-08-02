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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

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
    NewController controller = context.createController("api/components")
      .setSince("4.2")
      .setDescription("Get information about a component (file, directory, project, ...) and its ancestors or descendants.<br>" +
        "Update a project or module key.");

    for (ComponentsWsAction action : actions) {
      action.define(controller);
    }
    appAction.define(controller);
    searchViewComponentsAction.define(controller);
    defineSuggestionsAction(controller);

    controller.done();
  }

  private void defineSuggestionsAction(NewController controller) {
    NewAction action = controller.createAction("suggestions")
      .setDescription("Internal WS for the top-right search engine")
      .setSince("4.2")
      .setInternal(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "components-example-suggestions.json"));

    action.createParam("s")
      .setRequired(true)
      .setDescription("Substring of project key (minimum 2 characters)")
      .setExampleValue("sonar");

    RailsHandler.addJsonOnlyFormatParam(action);
  }

}
