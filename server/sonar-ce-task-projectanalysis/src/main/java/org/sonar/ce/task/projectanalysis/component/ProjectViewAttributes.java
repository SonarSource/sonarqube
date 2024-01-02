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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class ProjectViewAttributes {
  private final String uuid;
  private final String originalKey;
  @CheckForNull
  private final Long analysisDate;
  private final String branchName;

  public ProjectViewAttributes(String uuid, String originalKey, @Nullable Long analysisDate, @Nullable String branchName) {
    this.uuid = requireNonNull(uuid, "uuid can't be null");
    this.originalKey = requireNonNull(originalKey, "originalKey can't be null");
    this.analysisDate = analysisDate;
    this.branchName = branchName;
  }

  public String getUuid() {
    return uuid;
  }

  @CheckForNull
  public Long getAnalysisDate() {
    return analysisDate;
  }

  public String getBranchName() {
    return branchName;
  }

  public String getOriginalKey() {
    return originalKey;
  }

  @Override
  public String toString() {
    return "ProjectViewAttributes{" +
      ", uuid='" + uuid + '\'' +
      ", branchName='" + branchName + '\'' +
      ", analysisDate=" + analysisDate +
      '}';
  }
}
