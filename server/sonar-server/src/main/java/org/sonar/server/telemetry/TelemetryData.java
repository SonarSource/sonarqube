/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.telemetry;

import java.util.Map;
import org.sonar.server.measure.index.ProjectMeasuresStatistics;

import static java.util.Objects.requireNonNull;

public class TelemetryData {
  private final String serverId;
  private final String version;
  private final Map<String, String> plugins;
  private final long lines;
  private final long ncloc;
  private final long userCount;
  private final long projectCount;
  private final Map<String, Long> projectCountByLanguage;
  private final Map<String, Long> nclocByLanguage;

  public TelemetryData(Builder builder) {
    serverId = builder.serverId;
    version = builder.version;
    plugins = builder.plugins;
    lines = builder.projectMeasuresStatistics.getLines();
    ncloc = builder.projectMeasuresStatistics.getNcloc();
    userCount = builder.userCount;
    projectCount = builder.projectMeasuresStatistics.getProjectCount();
    projectCountByLanguage = builder.projectMeasuresStatistics.getProjectCountByLanguage();
    nclocByLanguage = builder.projectMeasuresStatistics.getNclocByLanguage();
  }

  public String getServerId() {
    return serverId;
  }

  public String getVersion() {
    return version;
  }

  public Map<String, String> getPlugins() {
    return plugins;
  }

  public long getLines() {
    return lines;
  }

  public long getNcloc() {
    return ncloc;
  }

  public long getUserCount() {
    return userCount;
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

  static class Builder {
    private String serverId;
    private String version;
    private long userCount;
    private Map<String, String> plugins;
    private ProjectMeasuresStatistics projectMeasuresStatistics;

    private Builder() {
      // enforce static factory method
    }

    Builder setServerId(String serverId) {
      this.serverId = serverId;
      return this;
    }

    Builder setVersion(String version) {
      this.version = version;
      return this;
    }

    void setUserCount(long userCount) {
      this.userCount = userCount;
    }

    void setPlugins(Map<String, String> plugins) {
      this.plugins = plugins;
    }

    void setProjectMeasuresStatistics(ProjectMeasuresStatistics projectMeasuresStatistics) {
      this.projectMeasuresStatistics = projectMeasuresStatistics;
    }

    TelemetryData build() {
      requireNonNull(serverId);
      requireNonNull(version);
      requireNonNull(plugins);
      requireNonNull(projectMeasuresStatistics);

      return new TelemetryData(this);
    }
  }
}
