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
package org.sonar.server.common.management;

import org.sonar.db.DbSession;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.management.ManagedProjectService;

public class ManagedInstanceChecker {

  private static final String INSTANCE_EXCEPTION_MESSAGE = "Operation not allowed when the instance is externally managed.";
  private static final String PROJECT_EXCEPTION_MESSAGE = "Operation not allowed when the project is externally managed.";

  private final ManagedInstanceService managedInstanceService;
  private final ManagedProjectService managedProjectService;

  public ManagedInstanceChecker(ManagedInstanceService managedInstanceService, ManagedProjectService managedProjectService) {
    this.managedInstanceService = managedInstanceService;
    this.managedProjectService = managedProjectService;
  }

  public void throwIfInstanceIsManaged() {
    BadRequestException.checkRequest(!managedInstanceService.isInstanceExternallyManaged(), INSTANCE_EXCEPTION_MESSAGE);
  }

  public void throwIfProjectIsManaged(DbSession dbSession, String projectUuid) {
    BadRequestException.checkRequest(!managedProjectService.isProjectManaged(dbSession, projectUuid), PROJECT_EXCEPTION_MESSAGE);
  }

  public void throwIfUserIsManaged(DbSession dbSession, String userUuid) {
    BadRequestException.checkRequest(!managedInstanceService.isUserManaged(dbSession, userUuid), INSTANCE_EXCEPTION_MESSAGE);
  }

  public void throwIfUserAndProjectAreManaged(DbSession dbSession, String userUuid, String projectUuid) {
    boolean isUserManaged = managedInstanceService.isUserManaged(dbSession, userUuid);
    boolean isProjectManaged = managedProjectService.isProjectManaged(dbSession, projectUuid);
    BadRequestException.checkRequest(!(isUserManaged && isProjectManaged), PROJECT_EXCEPTION_MESSAGE);
  }

  public void throwIfGroupAndProjectAreManaged(DbSession dbSession, String groupUuid, String projectUuid) {
    boolean isGroupManaged = managedInstanceService.isGroupManaged(dbSession, groupUuid);
    boolean isProjectManaged = managedProjectService.isProjectManaged(dbSession, projectUuid);
    BadRequestException.checkRequest(!(isGroupManaged && isProjectManaged), PROJECT_EXCEPTION_MESSAGE);
  }

}
