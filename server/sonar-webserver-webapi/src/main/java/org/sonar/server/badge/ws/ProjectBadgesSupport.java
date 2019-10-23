/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class ProjectBadgesSupport {

  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public ProjectBadgesSupport(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
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
  }

  BranchDto getBranch(DbSession dbSession, Request request) {
    try {
      String projectKey = request.mandatoryParam(PARAM_PROJECT);
      String branchName = request.param(PARAM_BRANCH);
      ProjectDto project = componentFinder.getProjectOrApplicationByKey(dbSession, projectKey);
      userSession.checkProjectPermission(USER, project);
      if (project.isPrivate()) {
        throw generateInvalidProjectException();
      }

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
}
