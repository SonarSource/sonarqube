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
package org.sonar.server.badge.ws;

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchType.LONG;
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
      .setDescription("Long living branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
  }

  ComponentDto getComponent(DbSession dbSession, Request request) {
    try {
      String projectKey = request.mandatoryParam(PARAM_PROJECT);
      String branchName = request.param(PARAM_BRANCH);
      ComponentDto project = componentFinder.getByKeyAndOptionalBranchOrPullRequest(dbSession, projectKey, branchName, null);
      checkComponentType(dbSession, project);
      userSession.checkComponentPermission(USER, project);
      return project;
    } catch (NotFoundException e) {
      throw new NotFoundException("Project has not been found");
    }
  }

  private void checkComponentType(DbSession dbSession, ComponentDto project) {
    Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbSession, project.uuid());
    if (project.isPrivate()) {
      throw generateInvalidProjectExcpetion();
    }
    if (branch.isPresent() && !branch.get().getBranchType().equals(LONG)) {
      throw generateInvalidProjectExcpetion();
    }
    if (!project.qualifier().equals(PROJECT) && !project.qualifier().equals(APP)) {
      throw generateInvalidProjectExcpetion();
    }
  }

  private static ProjectBadgesException generateInvalidProjectExcpetion() {
    return new ProjectBadgesException("Project is invalid");
  }
}
