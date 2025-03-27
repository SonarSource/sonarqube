/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.pushapi.sonarlint;

import java.util.List;
import java.util.Set;
import org.sonar.api.server.ServerSide;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;

@ServerSide
public class SonarLintClientPermissionsValidator {

  private final DbClient dbClient;
  private final UserSessionFactory userSessionFactory;

  public SonarLintClientPermissionsValidator(DbClient dbClient, UserSessionFactory userSessionFactory) {
    this.dbClient = dbClient;
    this.userSessionFactory = userSessionFactory;
  }

  public List<ProjectDto> validateUserCanReceivePushEventForProjects(UserSession userSession, Set<String> projectKeys) {
    List<ProjectDto> projectDtos;
    try (DbSession dbSession = dbClient.openSession(false)) {
      projectDtos = dbClient.projectDao().selectProjectsByKeys(dbSession, projectKeys);
    }
    validateProjectPermissions(userSession, projectDtos);
    return projectDtos;
  }

  public void validateUserCanReceivePushEventForProjectUuids(String userUuid, Set<String> projectUuids) {
    UserDto userDto;
    try (DbSession dbSession = dbClient.openSession(false)) {
      userDto = dbClient.userDao().selectByUuid(dbSession, userUuid);
    }
    if (userDto == null) {
      throw new ForbiddenException("User does not exist");
    }
    UserSession userSession = userSessionFactory.create(userDto, false);
    List<ProjectDto> projectDtos;
    try (DbSession dbSession = dbClient.openSession(false)) {
      projectDtos = dbClient.projectDao().selectByUuids(dbSession, projectUuids);
    }
    validateProjectPermissions(userSession, projectDtos);
  }

  private static void validateProjectPermissions(UserSession userSession, List<ProjectDto> projectDtos) {
    validateUsersDeactivationStatus(userSession);
    for (ProjectDto projectDto : projectDtos) {
      userSession.checkEntityPermission(ProjectPermission.USER, projectDto);
    }
  }

  private static void validateUsersDeactivationStatus(UserSession userSession) {
    if (!userSession.isActive()) {
      throw new ForbiddenException("User doesn't have rights to requested resource anymore.");
    }
  }
}
