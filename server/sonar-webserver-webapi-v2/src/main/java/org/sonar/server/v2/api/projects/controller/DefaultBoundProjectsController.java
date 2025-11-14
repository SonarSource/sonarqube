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
package org.sonar.server.v2.api.projects.controller;

import org.sonar.server.common.project.ImportProjectRequest;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.project.ImportedProject;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.projects.request.BoundProjectCreateRestRequest;
import org.sonar.server.v2.api.projects.response.BoundProjectCreateRestResponse;

import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class DefaultBoundProjectsController implements BoundProjectsController {

  private final UserSession userSession;

  private final ImportProjectService importProjectService;

  public DefaultBoundProjectsController(UserSession userSession, ImportProjectService importProjectService) {
    this.userSession = userSession;
    this.importProjectService = importProjectService;
  }

  @Override
  public BoundProjectCreateRestResponse createBoundProject(BoundProjectCreateRestRequest request) {
    userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);
    ImportedProject importedProject = importProjectService.importProject(restRequestToServiceRequest(request));
    return toRestResponse(importedProject);

  }

  private static ImportProjectRequest restRequestToServiceRequest(BoundProjectCreateRestRequest request) {
    return new ImportProjectRequest(
      request.projectKey(),
      request.projectName(),
      request.devOpsPlatformSettingId(),
      request.repositoryIdentifier(),
      request.projectIdentifier(),
      request.newCodeDefinitionType(),
      request.newCodeDefinitionValue(),
      request.monorepo());
  }

  private static BoundProjectCreateRestResponse toRestResponse(ImportedProject importedProject) {
    return new BoundProjectCreateRestResponse(importedProject.projectDto().getUuid(), importedProject.projectAlmSettingDto().getUuid());
  }
}
