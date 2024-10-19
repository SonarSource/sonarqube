/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.project.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;

import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_UPDATE_KEY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_TO;

public class UpdateKeyAction implements ProjectsWsAction {
  private final DbClient dbClient;
  private final ComponentService componentService;
  private final ComponentFinder componentFinder;

  public UpdateKeyAction(DbClient dbClient, ComponentService componentService, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.componentService = componentService;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    doDefine(context);
  }

  public WebService.NewAction doDefine(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_UPDATE_KEY)
      .setDescription("Update a project all its sub-components keys.<br>" +
        "Requires 'Administer' permission on the project.")
      .setSince("6.1")
      .setPost(true)
      .setHandler(this);

    action.setChangelog(
      new Change("7.1", "Ability to update key of a disabled module"));

    action.createParam(PARAM_FROM)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue("my_old_project");

    action.createParam(PARAM_TO)
      .setDescription("New project key")
      .setRequired(true)
      .setExampleValue("my_new_project");

    return action;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam(PARAM_FROM);
    String newKey = request.mandatoryParam(PARAM_TO);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, key);
      componentService.updateKey(dbSession, project, newKey);
    }
    response.noContent();
  }
}
