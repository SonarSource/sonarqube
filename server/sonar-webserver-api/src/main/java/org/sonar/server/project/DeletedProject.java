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
package org.sonar.server.project;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This record is used to refresh application/portfolio after the deletion of a project / portfolio
 * @param project could refer to a project or a portfolio deleted
 * @param mainBranchUuid refer to the main branch of the project deleted, is null when this record is used for a portfolio
 */
public record DeletedProject(Project project, @Nullable String mainBranchUuid) {
  public DeletedProject(Project project, String mainBranchUuid) {
    this.project = checkNotNull(project, "project can't be null");
    this.mainBranchUuid = mainBranchUuid;
  }
}
