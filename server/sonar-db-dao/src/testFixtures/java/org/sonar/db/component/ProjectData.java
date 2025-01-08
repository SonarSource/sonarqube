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
package org.sonar.db.component;

import org.sonar.db.project.ProjectDto;

public class ProjectData {
  private final BranchDto mainBranchDto;
  private final ComponentDto mainBranchComponent;
  private final ProjectDto projectDto;

  public ProjectData(ProjectDto projectDto, BranchDto mainBranchDto, ComponentDto mainBranchComponent) {
    this.mainBranchDto = mainBranchDto;
    this.mainBranchComponent = mainBranchComponent;
    this.projectDto = projectDto;
  }

  public ComponentDto getMainBranchComponent() {
    return mainBranchComponent;
  }

  public ProjectDto getProjectDto() {
    return projectDto;
  }

  public BranchDto getMainBranchDto() {
    return mainBranchDto;
  }

  public String projectUuid() {
    return projectDto.getUuid();
  }

  public String projectKey() {
    return projectDto.getKey();
  }

  public String mainBranchUuid() {
    return mainBranchDto.getUuid();
  }

}
