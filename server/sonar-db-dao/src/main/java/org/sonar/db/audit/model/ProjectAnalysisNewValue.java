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
package org.sonar.db.audit.model;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class ProjectAnalysisNewValue extends NewValue{
  private final String jobId;
  private final String projectKey;
  private final String projectName;
  private final int ncloc;

  public ProjectAnalysisNewValue(@NotNull String projectKey, @NotNull String projectName, @NotNull int ncloc, String jobId) {
    this.projectKey = projectKey;
    this.projectName = projectName;
    this.jobId = jobId;
    this.ncloc = ncloc;
  }

  public String getJobId() {
    return jobId;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public String getProjectName() {
    return projectName;
  }

  public int getNcloc() {
    return ncloc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProjectAnalysisNewValue that = (ProjectAnalysisNewValue) o;
    return Objects.equals(jobId, that.jobId) && Objects.equals(projectKey, that.projectKey)
            && Objects.equals(projectName, that.projectName) && ncloc == that.ncloc;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"projectKey\": ", this.projectKey, true);
    addField(sb, "\"projectName\": ", this.projectName, true);
    addField(sb, "\"jobId\": ", this.jobId, true);
    addField(sb, "\"ncloc\": ", String.valueOf(this.ncloc), true);
    endString(sb);
    return sb.toString();
  }
}