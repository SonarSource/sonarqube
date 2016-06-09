/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.component;

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class ProjectViewAttributes {
  private final long projectId;
  private final String projectUuid;

  public ProjectViewAttributes(long projectId, String projectUuid) {
    this.projectId = projectId;
    this.projectUuid = requireNonNull(projectUuid, "projectUuid can't be null");
  }

  public long getProjectId() {
    return projectId;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  @Override
  public String toString() {
    return "ProjectViewAttributes{" +
        "projectId=" + projectId +
        ", projectUuid='" + projectUuid + '\'' +
        '}';
  }
}
