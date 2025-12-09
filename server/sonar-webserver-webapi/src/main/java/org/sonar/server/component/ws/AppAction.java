/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.component.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_BRANCH;
import static org.sonar.server.component.ws.MeasuresWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PULL_REQUEST_EXAMPLE_001;

public class AppAction implements ComponentsWsAction {
  static final String PARAM_COMPONENT = "component";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final ComponentViewerJsonWriter componentViewerJsonWriter;

  public AppAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder, ComponentViewerJsonWriter componentViewerJsonWriter) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.componentViewerJsonWriter = componentViewerJsonWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("app")
      .setDescription("Coverage data required for rendering the component viewer.<br>" +
        "Either branch or pull request can be provided, not both<br>" +
        "Requires the following permission: 'Browse'.")
      .setResponseExample(getClass().getResource("app-example.json"))
      .setSince("4.4")
      .setChangelog(
        new Change("10.1", String.format("The use of module keys in parameter '%s' is removed", PARAM_COMPONENT)),
        new Change("9.6", "The fields 'subProject', 'subProjectName' were removed from the response."),
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true)
      .setSince("6.4");

    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key. Not available in the community edition.")
      .setSince("6.6")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);

    action.createParam(PARAM_PULL_REQUEST)
      .setDescription("Pull request id. Not available in the community edition.")
      .setSince("7.1")
      .setExampleValue(KEY_PULL_REQUEST_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession session = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(session, request);
      userSession.checkComponentPermission(ProjectPermission.USER, component);

      EntityDto entity = dbClient.entityDao().selectByComponentUuid(session, component.uuid())
        .orElseThrow(() -> new IllegalStateException("Couldn't find entity for component " + component.uuid()));
      writeJsonResponse(response, session, entity, component, request);
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request request) {
    String branch = request.param(PARAM_BRANCH);
    String pullRequest = request.param(PARAM_PULL_REQUEST);
    String componentKey = request.mandatoryParam(PARAM_COMPONENT);
    return componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, componentKey, branch, pullRequest);
  }

  private void writeJsonResponse(Response response, DbSession session, EntityDto entity, ComponentDto component, Request request) {
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject();
      componentViewerJsonWriter.writeComponent(json, entity, component, userSession, session, request.param(PARAM_BRANCH),
        request.param(PARAM_PULL_REQUEST));
      appendPermissions(json, userSession);
      componentViewerJsonWriter.writeMeasures(json, component, session);
      json.endObject();
    }
  }

  private static void appendPermissions(JsonWriter json, UserSession userSession) {
    json.prop("canMarkAsFavorite", userSession.isLoggedIn());
  }

}
