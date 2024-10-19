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
package org.sonar.ce.task.projectanalysis.component;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;

/**
 * Creates or updates the data in table {@code PROJECTS} for the current root.
 */
public class ProjectPersister {
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final System2 system2;

  public ProjectPersister(DbClient dbClient, TreeRootHolder treeRootHolder, System2 system2) {
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.system2 = system2;
  }

  public void persist(DbSession dbSession) {
    if (shouldSkip(treeRootHolder.getRoot())) {
      return;
    }

    ProjectDto dbProjectDto = dbClient.projectDao().selectProjectByKey(dbSession, treeRootHolder.getRoot().getKey())
      .orElseThrow(() -> new IllegalStateException("Project has been deleted by end-user during analysis"));
    ProjectDto projectDto = toProjectDto(treeRootHolder.getRoot(), dbProjectDto);

    if (hasChanged(dbProjectDto, projectDto)) {
      // insert or update in projects table
      dbClient.projectDao().update(dbSession, projectDto);
    }
  }

  private static boolean shouldSkip(Component rootComponent) {
    return !rootComponent.getType().equals(Component.Type.PROJECT) && !rootComponent.getType().equals(Component.Type.PROJECT_VIEW);
  }

  private static boolean hasChanged(ProjectDto dbProject, ProjectDto newProject) {
    return !StringUtils.equals(dbProject.getName(), newProject.getName()) ||
      !StringUtils.equals(dbProject.getDescription(), newProject.getDescription());
  }

  private ProjectDto toProjectDto(Component root, ProjectDto projectDtoFromDatabase) {
    ProjectDto projectDto = new ProjectDto();
    // Component has different uuid from project
    projectDto.setUuid(projectDtoFromDatabase.getUuid());
    projectDto.setName(root.getName());
    projectDto.setDescription(root.getDescription());
    projectDto.setUpdatedAt(system2.now());
    projectDto.setKey(root.getKey());
    projectDto.setQualifier(root.getType().equals(Component.Type.PROJECT) ? Qualifiers.PROJECT : Qualifiers.APP);
    return projectDto;
  }
}
