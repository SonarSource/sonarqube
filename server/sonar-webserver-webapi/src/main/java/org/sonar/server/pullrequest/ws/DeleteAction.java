/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.pullrequest.ws;

import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.server.pullrequest.ws.PullRequestsWs.addProjectParam;
import static org.sonar.server.pullrequest.ws.PullRequestsWs.addPullRequestParam;
import static org.sonar.server.pullrequest.ws.PullRequestsWsParameters.PARAM_PROJECT;
import static org.sonar.server.pullrequest.ws.PullRequestsWsParameters.PARAM_PULL_REQUEST;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_DELETE;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

public class DeleteAction implements PullRequestWsAction {

    private final DbClient dbClient;
    private final UserSession userSession;
    private final ComponentCleanerService componentCleanerService;
    private final ComponentFinder componentFinder;

    public DeleteAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession,
            ComponentCleanerService componentCleanerService) {
        this.dbClient = dbClient;
        this.componentFinder = componentFinder;
        this.userSession = userSession;
        this.componentCleanerService = componentCleanerService;
    }

    @Override
    public void define(NewController context) {
        WebService.NewAction action = context.createAction(ACTION_DELETE)
                .setSince("7.1")
                .setDescription("Delete a pull request.<br/>" +
                        "Requires 'Administer' rights on the specified project.")
                .setPost(true)
                .setHandler(this);

        addProjectParam(action);
        addPullRequestParam(action);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        userSession.checkLoggedIn();
        String projectKey = request.mandatoryParam(PARAM_PROJECT);
        String pullRequestId = request.mandatoryParam(PARAM_PULL_REQUEST);

        try (DbSession dbSession = dbClient.openSession(false)) {
            ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
            checkPermission(project);

            BranchDto pullRequest = dbClient.branchDao()
                    .selectByPullRequestKey(dbSession, project.getUuid(), pullRequestId)
                    .filter(branch -> branch.getBranchType() == PULL_REQUEST)
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Pull request '%s' is not found for project '%s'", pullRequestId,
                                    projectKey)));

            componentCleanerService.deleteBranch(dbSession, pullRequest);
            response.noContent();
        }
    }

    private void checkPermission(ProjectDto project) {
        userSession.checkProjectPermission(UserRole.ADMIN, project);
    }

}
