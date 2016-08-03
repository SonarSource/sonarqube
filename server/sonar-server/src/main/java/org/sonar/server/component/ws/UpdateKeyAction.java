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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.component.ComponentService;
import org.sonarqube.ws.client.component.UpdateWsRequest;

import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_NEW_KEY;

public class UpdateKeyAction implements ComponentsWsAction {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final ComponentService componentService;

  public UpdateKeyAction(DbClient dbClient, ComponentFinder componentFinder, ComponentService componentService) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.componentService = componentService;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("update_key")
      .setDescription("Update a project or module key and all its sub-components keys.<br>" +
        "Either '%s' or '%s' must be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "<li>'Browse' on the specified project</li>" +
        "</ul>", PARAM_ID, PARAM_KEY)
      .setSince("6.1")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("Project or module id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_KEY)
      .setDescription("Project or module key")
      .setExampleValue("my_old_project");

    action.createParam(PARAM_NEW_KEY)
      .setDescription("New component key")
      .setRequired(true)
      .setExampleValue("my_new_project");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toWsRequest(request));
    response.noContent();
  }

  private void doHandle(UpdateWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto projectOrModule = componentFinder.getByUuidOrKey(dbSession, request.getId(), request.getKey(), ParamNames.ID_AND_KEY);
      componentService.updateKey(dbSession, projectOrModule.key(), request.getNewKey());
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static UpdateWsRequest toWsRequest(Request request) {
    return UpdateWsRequest.builder()
      .setId(request.param(PARAM_ID))
      .setKey(request.param(PARAM_KEY))
      .setNewKey(request.mandatoryParam(PARAM_NEW_KEY))
      .build();
  }
}
