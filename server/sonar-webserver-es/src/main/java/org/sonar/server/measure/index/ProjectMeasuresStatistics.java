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
package org.sonar.server.measure.index;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ProjectMeasuresStatistics {
  private final long projectCount;
  private final Map<String, Long> projectCountByLanguage;
  private final Map<String, Long> nclocByLanguage;

  private ProjectMeasuresStatistics(Builder builder) {
    projectCount = builder.projectCount;
    projectCountByLanguage = builder.projectCountByLanguage;
    nclocByLanguage = builder.nclocByLanguage;
  }

  public long getProjectCount() {
    return projectCount;
  }

  public Map<String, Long> getProjectCountByLanguage() {
    return projectCountByLanguage;
  }

  public Map<String, Long> getNclocByLanguage() {
    return nclocByLanguage;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long projectCount;
    private Map<String, Long> projectCountByLanguage;
    private Map<String, Long> nclocByLanguage;

    private Builder() {
      // enforce static factory method
    }

    public Builder setProjectCount(long projectCount) {
      this.projectCount = projectCount;
      return this;
    }

    public Builder setProjectCountByLanguage(Map<String, Long> projectCountByLanguage) {
      this.projectCountByLanguage = projectCountByLanguage;
      return this;
    }

    public Builder setNclocByLanguage(Map<String, Long> nclocByLanguage) {
      this.nclocByLanguage = nclocByLanguage;
      return this;
    }

    public ProjectMeasuresStatistics build() {
      requireNonNull(projectCount);
      requireNonNull(projectCountByLanguage);
      requireNonNull(nclocByLanguage);
      return new ProjectMeasuresStatistics(this);
    }
  }
}
