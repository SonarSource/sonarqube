/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.branch.ws.BranchesWs.addProjectParam;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_RENAME;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_NAME;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;

public class RenameAction implements BranchWsAction {
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final DbClient dbClient;

  public RenameAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction(ACTION_RENAME)
      .setSince("6.6")
      .setDescription("Rename the main branch of a project.<br/>"
        + "Requires 'Administer' permission on the specified project.")
      .setPost(true)
      .setHandler(this);

    addProjectParam(action);
    action
      .createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(255)
      .setDescription("New name of the main branch")
      .setExampleValue("branch1");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String newBranchName = request.mandatoryParam(PARAM_NAME);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getRootComponentByUuidOrKey(dbSession, null, projectKey);
      checkPermission(project);

      Optional<BranchDto> existingBranch = dbClient.branchDao().selectByKey(dbSession, project.uuid(), newBranchName);
      checkArgument(!existingBranch.filter(b -> !b.isMain()).isPresent(),
        "Impossible to update branch name: a branch with name \"%s\" already exists in the project.", newBranchName);

      dbClient.branchDao().updateMainBranchName(dbSession, project.uuid(), newBranchName);
      dbSession.commit();
      response.noContent();
    }
  }

  private void checkPermission(ComponentDto project) {
    userSession.checkComponentPermission(UserRole.ADMIN, project);
  }

}
