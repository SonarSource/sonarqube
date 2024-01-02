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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.Indexers;
import org.sonar.server.project.Project;
import org.sonar.server.project.ProjectLifeCycleListeners;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singleton;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.ACTION_SET_MAIN_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_BRANCH;
import static org.sonar.server.branch.ws.ProjectBranchesParameters.PARAM_PROJECT;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class SetMainBranchAction implements BranchWsAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetMainBranchAction.class);
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectLifeCycleListeners projectLifeCycleListeners;
  private final Indexers indexers;

  public SetMainBranchAction(DbClient dbClient, UserSession userSession, ProjectLifeCycleListeners projectLifeCycleListeners, Indexers indexers) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectLifeCycleListeners = projectLifeCycleListeners;
    this.indexers = indexers;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SET_MAIN_BRANCH)
      .setSince("10.2")
      .setDescription("Allow to set a new main branch.<br/>. Caution, only applicable on projects.<br>" +
        "Requires 'Administer' rights on the specified project or application.")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue("my_project")
      .setRequired(true);
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue("new_master")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String newMainBranchKey = request.mandatoryParam(PARAM_BRANCH);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto projectDto = checkFoundWithOptional(dbClient.projectDao().selectProjectByKey(dbSession, projectKey),
        "Project '%s' not found.", projectKey);
      checkPermission(projectDto);

      BranchDto oldMainBranch = dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, projectDto.getUuid())
        .orElseThrow(() -> new IllegalStateException("No main branch for existing project '%s'".formatted(projectDto.getKey())));
      BranchDto newMainBranch = checkFoundWithOptional(dbClient.branchDao().selectByBranchKey(dbSession, projectDto.getUuid(), newMainBranchKey),
        "Branch '%s' not found for project '%s'.", newMainBranchKey, projectDto.getKey());

      if (checkAndLogIfNewBranchIsAlreadyMainBranch(oldMainBranch, newMainBranch)) {
        response.noContent();
        return;
      }
      configureProjectWithNewMainBranch(dbSession, projectDto.getKey(), oldMainBranch, newMainBranch);
      refreshApplicationsAndPortfoliosComputedByProject(projectDto, Set.of(oldMainBranch.getUuid()));
      indexers.commitAndIndexBranches(dbSession, List.of(oldMainBranch, newMainBranch), Indexers.BranchEvent.SWITCH_OF_MAIN_BRANCH);

      dbSession.commit();
      response.noContent();
    }
  }

  private void configureProjectWithNewMainBranch(DbSession dbSession, String projectKey, BranchDto oldMainBranch, BranchDto newMainBranch) {
    updatePreviousMainBranch(dbSession, oldMainBranch);
    updateNewMainBranch(dbSession, newMainBranch);

    LOGGER.info("The new main branch of project '{}' is '{}' (Previous one : '{}')",
      projectKey, newMainBranch.getKey(), oldMainBranch.getKey());
  }

  private static boolean checkAndLogIfNewBranchIsAlreadyMainBranch(BranchDto oldMainBranch, BranchDto newMainBranch) {
    if (Objects.equals(oldMainBranch.getKey(), newMainBranch.getKey())) {
      LOGGER.info("Branch '{}' is already the main branch.", newMainBranch.getKey());
      return true;
    }
    return false;
  }

  private void refreshApplicationsAndPortfoliosComputedByProject(ProjectDto projectDto, Set<String> impactedBranchesUuids) {
    projectLifeCycleListeners.onProjectBranchesChanged(singleton(Project.from(projectDto)), impactedBranchesUuids);
  }

  private void updateNewMainBranch(DbSession dbSession, BranchDto newMainBranch) {
    if (!newMainBranch.isExcludeFromPurge()) {
      dbClient.branchDao().updateExcludeFromPurge(dbSession, newMainBranch.getUuid(), true);
    }
    dbClient.branchDao().updateIsMain(dbSession, newMainBranch.getUuid(), true);
  }

  private void updatePreviousMainBranch(DbSession dbSession, BranchDto oldMainBranch) {
    dbClient.branchDao().updateIsMain(dbSession, oldMainBranch.getUuid(), false);
  }

  private void checkPermission(ProjectDto project) {
    userSession.checkEntityPermission(UserRole.ADMIN, project);
  }
}
