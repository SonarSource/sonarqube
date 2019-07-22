/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Optional;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.exceptions.NotFoundException;

import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_UPDATE_KEY;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_FROM;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_TO;

public class UpdateKeyAction implements ProjectsWsAction {
  private final DbClient dbClient;
  private final ComponentService componentService;

  public UpdateKeyAction(DbClient dbClient, ComponentService componentService) {
    this.dbClient = dbClient;
    this.componentService = componentService;
  }

  @Override
  public void define(WebService.NewController context) {
    doDefine(context);
  }

  public WebService.NewAction doDefine(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_UPDATE_KEY)
      .setDescription("Update a project or module key and all its sub-components keys.<br>" +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("6.1")
      .setPost(true)
      .setHandler(this);

    action.setChangelog(
      new Change("7.1", "Ability to update key of a disabled module"));

    action.createParam(PARAM_FROM)
      .setDescription("Project or module key")
      .setRequired(true)
      .setExampleValue("my_old_project");

    action.createParam(PARAM_TO)
      .setDescription("New component key")
      .setRequired(true)
      .setExampleValue("my_new_project");

    return action;
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String key = request.mandatoryParam(PARAM_FROM);
    String newKey = request.mandatoryParam(PARAM_TO);

    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> component;
      component = dbClient.componentDao().selectByKey(dbSession, key);
      if (!component.isPresent() || component.get().getMainBranchProjectUuid() != null) {
        throw new NotFoundException("Component not found");
      }

      componentService.updateKey(dbSession, component.get(), newKey);
    }
    response.noContent();
  }
}
