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
package org.sonar.server.project.ws;

import com.google.common.io.Resources;
import java.util.Arrays;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class ProjectsWs implements WebService {
  public static final String ENDPOINT = "api/projects";
  private static final String FALSE = "false";
  private static final String TRUE = "true";

  private final ProjectsWsAction[] actions;

  public ProjectsWs(ProjectsWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(ENDPOINT)
      .setSince("2.10")
      .setDescription("Manage project existence.");

    defineIndexAction(controller);
    Arrays.stream(actions).forEach(action -> action.define(controller));
    controller.done();
  }

  private void defineIndexAction(NewController controller) {
    WebService.NewAction action = controller.createAction("index")
      .setDescription("Search for projects")
      .setSince("2.10")
      .setHandler(RailsHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "projects-example-index.json"));

    action.createParam("key")
      .setDescription("id or key of the project")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam("search")
      .setDescription("Substring of project name, case insensitive")
      .setExampleValue("Sonar");

    action.createParam("desc")
      .setDescription("Load project description")
      .setDefaultValue(TRUE)
      .setBooleanPossibleValues();

    action.createParam("subprojects")
      .setDescription("Load sub-projects. Ignored if the parameter key is set")
      .setDefaultValue(FALSE)
      .setBooleanPossibleValues();

    action.createParam("views")
      .setDescription("Load views and sub-views. Ignored if the parameter key is set")
      .setDefaultValue(FALSE)
      .setBooleanPossibleValues();

    action.createParam("libs")
      .setDescription("Load libraries. Ignored if the parameter key is set")
      .setDefaultValue(FALSE)
      .setBooleanPossibleValues();

    action.createParam("versions")
      .setDescription("Load version")
      .setDefaultValue(FALSE)
      .setPossibleValues(TRUE, FALSE, "last");

    RailsHandler.addFormatParam(action);
  }

}
