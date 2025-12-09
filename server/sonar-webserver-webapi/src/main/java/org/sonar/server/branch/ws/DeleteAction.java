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
package org.sonar.server.branch.ws;

import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singleton;
import static org.sonar.server.branch.ws.BranchesWs.addBranchParam;
import static org.sonar.server.branch.ws.BranchesWs.addProjectParam;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_DELETE;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class DeleteAction implements BranchWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentCleanerService componentCleanerService;
  private final ComponentFinder componentFinder;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;

  public DeleteAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, ComponentCleanerService componentCleanerService,
    ProjectLifeCycleListeners projectLifeCycleListeners) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.componentCleanerService = componentCleanerService;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction(ACTION_DELETE)
      .setSince("6.6")
      .setDescription("Delete a non-main branch of a project or application.<br/>" +
        "Requires 'Administer' rights on the specified project or application.")
      .setPost(true)
      .setHandler(this);

    addProjectParam(action);
    addBranchParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String branchKey = request.mandatoryParam(PARAM_BRANCH);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
      checkPermission(project);

      BranchDto branch = checkFoundWithOptional(
        dbClient.branchDao().selectByBranchKey(dbSession, project.getUuid(), branchKey),
        "Branch '%s' not found for project '%s'", branchKey, projectKey);

      componentCleanerService.deleteBranch(dbSession, branch);
      projectLifeCycleListeners.onProjectBranchesChanged(singleton(Project.fromProjectDtoWithTags(project)), Set.of(branch.getUuid()));
      response.noContent();
    }
  }

  private void checkPermission(ProjectDto project) {
    userSession.checkEntityPermission(ProjectPermission.ADMIN, project);
  }

}
