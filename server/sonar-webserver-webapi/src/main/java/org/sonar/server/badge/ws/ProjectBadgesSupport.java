/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.badge.ws;

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectBadgeTokenDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;

import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.PROJECT_BADGE_TOKEN_EXAMPLE;

public class ProjectBadgesSupport {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_TOKEN = "token";

  private final ComponentFinder componentFinder;
  private final DbClient dbClient;

  public ProjectBadgesSupport(ComponentFinder componentFinder, DbClient dbClient) {
    this.componentFinder = componentFinder;
    this.dbClient = dbClient;
  }

  void addProjectAndBranchParams(WebService.NewAction action) {
    action.createParam(PARAM_PROJECT)
      .setDescription("Project or application key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action
      .createParam(PARAM_TOKEN)
      .setDescription("Project badge token")
      .setExampleValue(PROJECT_BADGE_TOKEN_EXAMPLE);
  }

  BranchDto getBranch(DbSession dbSession, Request request) {
    try {
      String projectKey = request.mandatoryParam(PARAM_PROJECT);
      String branchName = request.param(PARAM_BRANCH);
      ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);

      BranchDto branch;
      if (branchName == null) {
        branch = componentFinder.getMainBranch(dbSession, project);
      } else {
        branch = componentFinder.getBranchOrPullRequest(dbSession, project, branchName, null);
      }

      if (!branch.getBranchType().equals(BRANCH)) {
        throw generateInvalidProjectException();
      }

      return branch;
    } catch (NotFoundException e) {
      throw new NotFoundException("Project has not been found");
    }
  }

  private static ProjectBadgesException generateInvalidProjectException() {
    return new ProjectBadgesException("Project is invalid");
  }

  public void validateToken(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = request.mandatoryParam(PARAM_PROJECT);
      ProjectDto projectDto;
      try {
        projectDto = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
      } catch (NotFoundException e) {
        throw new NotFoundException("Project has not been found");
      }
      String token = request.param(PARAM_TOKEN);
      if (projectDto.isPrivate() && !isTokenValid(dbSession, projectDto, token)) {
        throw generateInvalidProjectException();
      }
    }
  }

  private boolean isTokenValid(DbSession dbSession, ProjectDto projectDto, @Nullable String token) {
    ProjectBadgeTokenDto projectBadgeTokenDto = dbClient.projectBadgeTokenDao().selectTokenByProject(dbSession, projectDto);
    return token != null && projectBadgeTokenDto != null && token.equals(projectBadgeTokenDto.getToken());
  }
}
