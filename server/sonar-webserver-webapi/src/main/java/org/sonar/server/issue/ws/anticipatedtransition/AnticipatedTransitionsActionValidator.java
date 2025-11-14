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
package org.sonar.server.issue.ws.anticipatedtransition;

import java.util.Objects;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class AnticipatedTransitionsActionValidator {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;


  public AnticipatedTransitionsActionValidator(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  public ProjectDto validateProjectKey(String projectKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return componentFinder.getProjectByKey(dbSession, projectKey);
    } catch (NotFoundException e) {
      // To hide information about the existence or not of the project.
      throw insufficientPrivilegesException();
    }
  }

  public String validateUserLoggedIn() {
    userSession.checkLoggedIn();
    return userSession.getUuid();
  }

  public void validateUserHasAdministerIssuesPermission(String projectUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String userUuid = Objects.requireNonNull(userSession.getUuid());
      if (!dbClient.authorizationDao().selectEntityPermissions(dbSession, projectUuid, userUuid).contains(ISSUE_ADMIN.getKey())){
        throw insufficientPrivilegesException();
      }
    }
  }

}
