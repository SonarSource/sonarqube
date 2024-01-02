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
package org.sonar.ce.task.projectexport.steps;

import java.util.Collections;
import java.util.List;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.requireNonNull;

public class MutableProjectHolderImpl implements MutableProjectHolder {

  private ProjectDto projectDto = null;

  private List<BranchDto> branches = Collections.emptyList();

  @Override
  public void setProjectDto(ProjectDto dto) {
    requireNonNull(dto, "Project must not be null");
    this.projectDto = dto;
  }

  @Override
  public void setBranches(List<BranchDto> branches) {
    requireNonNull(branches, "Branches must not be null");
    this.branches = copyOf(branches);
  }

  @Override
  public ProjectDto projectDto() {
    requireNonNull(projectDto, "Project has not been loaded yet");
    return projectDto;
  }

  @Override
  public List<BranchDto> branches() {
    return branches;
  }
}
