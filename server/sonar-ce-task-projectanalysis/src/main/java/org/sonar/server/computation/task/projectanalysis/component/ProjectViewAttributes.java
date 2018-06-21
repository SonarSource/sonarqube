/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class ProjectViewAttributes {
  private final String projectUuid;
  @CheckForNull
  private final Long analysisDate;

  public ProjectViewAttributes(String projectUuid, @Nullable Long analysisDate) {
    this.projectUuid = requireNonNull(projectUuid, "projectUuid can't be null");
    this.analysisDate = analysisDate;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  @CheckForNull
  public Long getAnalysisDate() {
    return analysisDate;
  }

  @Override
  public String toString() {
    return "ProjectViewAttributes{" +
      ", projectUuid='" + projectUuid + '\'' +
      ", analysisDate=" + analysisDate +
      '}';
  }
}
