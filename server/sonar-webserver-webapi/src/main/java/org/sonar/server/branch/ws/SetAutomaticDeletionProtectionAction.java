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
package org.sonar.server.branch.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.branch.ws.BranchesWs.addBranchParam;
import static org.sonar.server.branch.ws.BranchesWs.addProjectParam;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_SET_AUTOMATIC_DELETION_PROTECTION;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_VALUE;

public class SetAutomaticDeletionProtectionAction implements BranchWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public SetAutomaticDeletionProtectionAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SET_AUTOMATIC_DELETION_PROTECTION)
      .setSince("8.1")
      .setDescription("Protect a specific branch from automatic deletion. Protection can't be disabled for the main branch.<br/>"
        + "Requires 'Administer' permission on the specified project or application.")
      .setPost(true)
      .setHandler(this);

    addProjectParam(action);
    addBranchParam(action);
    action.createParam(PARAM_VALUE)
      .setRequired(true)
      .setBooleanPossibleValues()
      .setDescription("Sets whether the branch should be protected from automatic deletion when it hasn't been analyzed for a set period of time.")
      .setExampleValue("true");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String branchKey = request.mandatoryParam(PARAM_BRANCH);
    boolean excludeFromPurge = request.mandatoryParamAsBoolean(PARAM_VALUE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
      checkPermission(project);

      BranchDto branch = dbClient.branchDao().selectByBranchKey(dbSession, project.getUuid(), branchKey)
        .orElseThrow(() -> new NotFoundException(format("Branch '%s' not found for project '%s'", branchKey, projectKey)));
      checkArgument(!branch.isMain() || excludeFromPurge, "Main branch of the project is always excluded from automatic deletion.");

      dbClient.branchDao().updateExcludeFromPurge(dbSession, branch.getUuid(), excludeFromPurge);
      dbSession.commit();
      response.noContent();
    }

  }

  private void checkPermission(ProjectDto project) {
    userSession.checkProjectPermission(UserRole.ADMIN, project);
  }
}
