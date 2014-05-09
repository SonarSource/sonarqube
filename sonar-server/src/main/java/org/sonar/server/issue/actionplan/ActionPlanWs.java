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

package org.sonar.server.issue.actionplan;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class ActionPlanWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/action_plans");
    controller.setDescription("Action plans management");

    defineSearchAction(controller);
    defineCreateAction(controller);
    defineUpdateAction(controller);
    defineDeleteAction(controller);
    defineOpenAction(controller);
    defineCloseAction(controller);

    controller.done();
  }

  private void defineSearchAction(NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Get a list of action plans. Requires Browse permission on project")
      .setSince("3.6")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-search.json"));
    addProjectParam(action);
    addFormatParam(action);
  }

  private void defineCreateAction(NewController controller) {
    WebService.NewAction action = controller.createAction("create")
      .setDescription("Create an action plan. Requires Administer permission on project")
      .setSince("3.6")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);
    addNameParam(action);
    addDescriptionParam(action);
    addDeadLineParam(action);
    addProjectParam(action);
    addFormatParam(action);
  }

  private void defineUpdateAction(NewController controller) {
    WebService.NewAction action = controller.createAction("update")
      .setDescription("Update an action plan. Requires Administer permission on project")
      .setSince("3.6")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);
    addKeyParam(action);
    addNameParam(action);
    addDescriptionParam(action);
    addDeadLineParam(action);
    addFormatParam(action);
  }

  private void defineDeleteAction(NewController controller) {
    WebService.NewAction action = controller.createAction("delete")
      .setDescription("Delete an action plan. Requires Administer permission on project")
      .setSince("3.6")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);
    addKeyParam(action);
    addFormatParam(action);
  }

  private void defineOpenAction(NewController controller) {
    WebService.NewAction action = controller.createAction("open")
      .setDescription("Open an action plan. Requires Administer permission on project")
      .setSince("3.6")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);
    addKeyParam(action);
    addFormatParam(action);
  }

  private void defineCloseAction(NewController controller) {
    WebService.NewAction action = controller.createAction("close")
      .setDescription("Close an action plan. Requires Administer permission on project")
      .setSince("3.6")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);
    addKeyParam(action);
    addFormatParam(action);
  }

  private static NewParam addKeyParam(WebService.NewAction action) {
    return action.createParam("key")
      .setDescription("Key of the action plan")
      .setExampleValue("3f19de90-1521-4482-a737-a311758ff513")
      .setRequired(true);
  }

  private static NewParam addNameParam(WebService.NewAction action) {
    return action.createParam("name")
      .setDescription("Name of the action plan")
      .setExampleValue("Version 3.6")
      .setRequired(true);
  }

  private static NewParam addDescriptionParam(WebService.NewAction action) {
    return action.createParam("description")
      .setDescription("Description of the action plan")
      .setExampleValue("Version 3.6");
  }

  private static NewParam addDeadLineParam(WebService.NewAction action) {
    return action.createParam("deadLine")
      .setDescription("Due date of the action plan. Format: YYYY-MM-DD")
      .setExampleValue("2013-12-31");
  }

  private static NewParam addProjectParam(WebService.NewAction action) {
    return action.createParam("project")
      .setDescription("Project key")
      .setExampleValue("org.codehaus.sonar:sonar")
      .setRequired(true);
  }

  private static NewParam addFormatParam(WebService.NewAction action) {
    return RailsHandler.addFormatParam(action);
  }
}
