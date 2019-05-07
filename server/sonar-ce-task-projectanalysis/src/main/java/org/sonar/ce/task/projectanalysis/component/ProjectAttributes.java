/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Optional;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class ProjectAttributes {
  private final String projectVersion;

  @Nullable
  private final String buildString;

  @Nullable
  private final String scmRevisionId;

  public ProjectAttributes(String projectVersion, @Nullable String buildString, @Nullable String scmRevisionId) {
    this.projectVersion = requireNonNull(projectVersion, "project version can't be null");
    this.buildString = buildString;
    this.scmRevisionId = scmRevisionId;
  }

  public ProjectAttributes(ProjectAttributes projectAttributes) {
    this.projectVersion = projectAttributes.projectVersion;
    this.buildString = projectAttributes.buildString;
    this.scmRevisionId = projectAttributes.scmRevisionId;
  }

  public String getProjectVersion() {
    return projectVersion;
  }

  public Optional<String> getBuildString() {
    return Optional.ofNullable(buildString);
  }

  public Optional<String> getScmRevisionId() {
    return Optional.ofNullable(scmRevisionId);
  }

  @Override
  public String toString() {
    return "ProjectAttributes{" +
      "projectVersion='" + projectVersion + '\'' +
      "buildString='" + buildString + '\'' +
      "scmRevisionId='" + scmRevisionId + '\'' +
      '}';
  }
}
