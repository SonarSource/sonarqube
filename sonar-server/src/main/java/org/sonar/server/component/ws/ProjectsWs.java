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

package org.sonar.server.component.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

public class ProjectsWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/projects")
      .setSince("2.10")
      .setDescription("Projects management");

    defineIndexAction(controller);
    defineCreateAction(controller);
    defineDestroyAction(controller);

    controller.done();
  }

  private void defineIndexAction(NewController controller) {
    WebService.NewAction action = controller.createAction("index")
      .setDescription("Search for projects")
      .setSince("2.10")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-index.json"));

    action.createParam("key")
      .setDescription("id or key of the project")
      .setExampleValue("org.codehaus.sonar:sonar");

    action.createParam("search")
      .setDescription("substring of project name, case insensitive")
      .setExampleValue("Sonar");

    action.createParam("desc")
      .setDescription("Load project description")
      .setDefaultValue("true")
      .setBooleanPossibleValues();

    action.createParam("subprojects")
      .setDescription("Load sub-projects. Ignored if the parameter key is set")
      .setDefaultValue("false")
      .setBooleanPossibleValues();

    action.createParam("views")
      .setDescription("Load views and sub-views. Ignored if the parameter key is set")
      .setDefaultValue("false")
      .setBooleanPossibleValues();

    action.createParam("libs")
      .setDescription("Load libraries. Ignored if the parameter key is set")
      .setDefaultValue("false")
      .setBooleanPossibleValues();

    action.createParam("versions")
      .setDescription("Load version")
      .setDefaultValue("false")
      .setPossibleValues("true", "false", "last");
  }

  private void defineCreateAction(NewController controller) {
    WebService.NewAction action = controller.createAction("create")
      .setDescription("Provision a project. Requires Provision Projects permission")
      .setSince("4.0")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("key")
      .setDescription("Key of the project")
      .setRequired(true)
      .setExampleValue("org.codehaus.sonar:sonar");

    action.createParam("name")
      .setDescription("Name of the project")
      .setRequired(true)
      .setExampleValue("SonarQube");
  }

  private void defineDestroyAction(NewController controller) {
    WebService.NewAction action = controller.createAction("destroy")
      .setDescription("Delete a project. Requires Administer System permission")
      .setSince("2.11")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("id")
      .setDescription("id or key of the resource (ie: component)")
      .setRequired(true)
      .setExampleValue("org.codehaus.sonar:sonar");
  }

}
