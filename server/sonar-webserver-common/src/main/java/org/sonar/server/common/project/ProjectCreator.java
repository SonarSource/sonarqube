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
package org.sonar.server.common.project;

import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.component.ComponentCreationParameters;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.component.NewComponent;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static org.sonar.db.component.ComponentQualifiers.PROJECT;

@ServerSide
public class ProjectCreator {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectCreator.class);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ComponentUpdater componentUpdater;

  public ProjectCreator(DbClient dbClient, UserSession userSession, ProjectDefaultVisibility projectDefaultVisibility, ComponentUpdater componentUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.componentUpdater = componentUpdater;
  }

  public ComponentCreationData createProject(DbSession dbSession, String projectKey, String projectName, @Nullable String mainBranchName, CreationMethod creationMethod,
    @Nullable Boolean isPrivate, boolean isManaged) {
    boolean visibility = isPrivate != null ? isPrivate : projectDefaultVisibility.get(dbSession).isPrivate();
    NewComponent projectComponent = NewComponent.newComponentBuilder()
      .setKey(projectKey)
      .setName(projectName)
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(projectComponent)
      .userLogin(userSession.getLogin())
      .userUuid(userSession.getUuid())
      .mainBranchName(mainBranchName)
      .isManaged(isManaged)
      .creationMethod(creationMethod)
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  public ComponentCreationData createProject(DbSession dbSession, String projectKey, String projectName, @Nullable String mainBranchName, CreationMethod creationMethod) {
    return createProject(dbSession, projectKey, projectName, mainBranchName, creationMethod, projectDefaultVisibility.get(dbSession).isPrivate(), false);
  }

  public ComponentCreationData getOrCreateProject(DbSession dbSession, ProjectCreationRequest request) {
    // Check if project already exists
    Optional<ProjectDto> existingProject = dbClient.projectDao().selectProjectByKey(dbSession, request.projectKey());
    
    if (existingProject.isPresent()) {
      if (!request.allowExisting()) {
        throw BadRequestException.create("Could not create Project with key: \"" + request.projectKey() + "\". A similar key already exists: \"" + request.projectKey() + "\"");
      }
      
      // Validate name matches
      if (!existingProject.get().getName().equals(request.projectName())) {
        // Log detailed info for debugging/auditing
        LOG.warn("Project binding failed: key '{}' exists with name '{}', expected '{}'", 
            request.projectKey(), existingProject.get().getName(), request.projectName());
        // Return vague error to prevent information disclosure
        throw BadRequestException.create("Project with key '" + request.projectKey() + "' cannot be bound - configuration mismatch");
      }
      
      // Return existing project data (not created)
      ComponentDto componentDto = dbClient.componentDao().selectByKey(dbSession, request.projectKey())
        .orElseThrow(() -> new IllegalStateException("Component not found for existing project"));
      BranchDto mainBranch = dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, existingProject.get().getUuid())
        .orElseThrow(() -> new IllegalStateException("Main branch not found"));
      
      return new ComponentCreationData(componentDto, null, mainBranch, existingProject.get(), false);
    }
    
    // Create new project
    ComponentCreationData creationData = createProject(dbSession, request.projectKey(), request.projectName(), request.mainBranchName(), 
        request.creationMethod(), request.isPrivate(), request.isManaged());
    
    // Explicitly mark as newly created
    return new ComponentCreationData(creationData.mainBranchComponent(), creationData.portfolioDto(), 
        creationData.mainBranchDto(), creationData.projectDto(), true);
  }
}
