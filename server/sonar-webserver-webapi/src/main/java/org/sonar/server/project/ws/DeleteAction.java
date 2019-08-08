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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singleton;
import static org.sonar.server.project.Project.from;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;

public class DeleteAction implements ProjectsWsAction {
  private static final String ACTION = "delete";

  private final ComponentCleanerService componentCleanerService;
  private final ComponentFinder componentFinder;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;

  public DeleteAction(ComponentCleanerService componentCleanerService, ComponentFinder componentFinder, DbClient dbClient,
    UserSession userSession, ProjectLifeCycleListeners projectLifeCycleListeners) {
    this.componentCleanerService = componentCleanerService;
    this.componentFinder = componentFinder;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION)
      .setPost(true)
      .setDescription("Delete a project.<br> " +
        "Requires 'Administer System' permission or 'Administer' permission on the project.")
      .setSince("5.2")
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    // fail-fast if not logged in
    userSession.checkLoggedIn();
    String key = request.mandatoryParam(PARAM_PROJECT);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKey(dbSession, key);
      checkPermission(project);
      componentCleanerService.delete(dbSession, project);
      projectLifeCycleListeners.onProjectsDeleted(singleton(from(project)));
    }

    response.noContent();
  }

  private void checkPermission(ComponentDto project) {
    if (!userSession.hasComponentPermission(UserRole.ADMIN, project)) {
      userSession.checkPermission(OrganizationPermission.ADMINISTER, project.getOrganizationUuid());
    }
  }
}
