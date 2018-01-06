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
  private final boolean usingBranches;
  private final Database database;
  private final Map<String, Long> projectCountByLanguage;
  private final Map<String, Long> nclocByLanguage;

  private TelemetryData(Builder builder) {
    serverId = builder.serverId;
    version = builder.version;
    plugins = builder.plugins;
    lines = builder.projectMeasuresStatistics.getLines();
    ncloc = builder.projectMeasuresStatistics.getNcloc();
    userCount = builder.userCount;
    projectCount = builder.projectMeasuresStatistics.getProjectCount();
    usingBranches = builder.usingBranches;
    database = builder.database;
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

  public boolean isUsingBranches() {
    return usingBranches;
  }

  public Database getDatabase() {
    return database;
  }

  public Map<String, Long> getProjectCountByLanguage() {
    return projectCountByLanguage;
  }

  public Map<String, Long> getNclocByLanguage() {
    return nclocByLanguage;
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private String serverId;
    private String version;
    private long userCount;
    private Map<String, String> plugins;
    private Database database;
    private ProjectMeasuresStatistics projectMeasuresStatistics;
    private Boolean usingBranches;

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

    Builder setDatabase(Database database) {
      this.database = database;
      return this;
    }

    Builder setUsingBranches(boolean usingBranches) {
      this.usingBranches = usingBranches;
      return this;
    }

    TelemetryData build() {
      requireNonNull(serverId);
      requireNonNull(version);
      requireNonNull(plugins);
      requireNonNull(projectMeasuresStatistics);
      requireNonNull(database);
      requireNonNull(usingBranches);

      return new TelemetryData(this);
    }
  }

  static class Database {
    private final String name;
    private final String version;

    Database(String name, String version) {
      this.name = name;
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }
  }
}
