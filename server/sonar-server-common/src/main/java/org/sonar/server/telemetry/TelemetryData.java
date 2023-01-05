/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.db.user.UserTelemetryDto;

import static java.util.Objects.requireNonNullElse;

public class TelemetryData {
  private final String serverId;
  private final String version;
  private final Long messageSequenceNumber;
  private final Map<String, String> plugins;
  private final Database database;
  private final EditionProvider.Edition edition;
  private final Long installationDate;
  private final String installationVersion;
  private final boolean inDocker;
  private final boolean isScimEnabled;
  private final List<UserTelemetryDto> users;
  private final List<Project> projects;
  private final List<ProjectStatistics> projectStatistics;
  private final Boolean hasUnanalyzedC;
  private final Boolean hasUnanalyzedCpp;
  private final Set<String> customSecurityConfigs;

  private TelemetryData(Builder builder) {
    serverId = builder.serverId;
    version = builder.version;
    messageSequenceNumber = builder.messageSequenceNumber;
    plugins = builder.plugins;
    database = builder.database;
    edition = builder.edition;
    installationDate = builder.installationDate;
    installationVersion = builder.installationVersion;
    inDocker = builder.inDocker;
    isScimEnabled = builder.isScimEnabled;
    users = builder.users;
    projects = builder.projects;
    projectStatistics = builder.projectStatistics;
    hasUnanalyzedC = builder.hasUnanalyzedC;
    hasUnanalyzedCpp = builder.hasUnanalyzedCpp;
    customSecurityConfigs = requireNonNullElse(builder.customSecurityConfigs, Set.of());
  }

  public String getServerId() {
    return serverId;
  }

  public String getVersion() {
    return version;
  }

  public Long getMessageSequenceNumber() {
    return messageSequenceNumber;
  }

  public Map<String, String> getPlugins() {
    return plugins;
  }

  public Database getDatabase() {
    return database;
  }

  public Optional<EditionProvider.Edition> getEdition() {
    return Optional.ofNullable(edition);
  }

  public Long getInstallationDate() {
    return installationDate;
  }

  public String getInstallationVersion() {
    return installationVersion;
  }

  public boolean isInDocker() {
    return inDocker;
  }

  public boolean isScimEnabled() {
    return isScimEnabled;
  }

  public Optional<Boolean> hasUnanalyzedC() {
    return Optional.ofNullable(hasUnanalyzedC);
  }

  public Optional<Boolean> hasUnanalyzedCpp() {
    return Optional.ofNullable(hasUnanalyzedCpp);
  }

  public Set<String> getCustomSecurityConfigs() {
    return customSecurityConfigs;
  }

  public List<UserTelemetryDto> getUserTelemetries() {
    return users;
  }

  public List<Project> getProjects() {
    return projects;
  }

  public List<ProjectStatistics> getProjectStatistics() {
    return projectStatistics;
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private String serverId;
    private String version;
    private Long messageSequenceNumber;
    private Map<String, String> plugins;
    private Database database;
    private Edition edition;
    private Long installationDate;
    private String installationVersion;
    private boolean inDocker = false;
    private boolean isScimEnabled;
    private Boolean hasUnanalyzedC;
    private Boolean hasUnanalyzedCpp;
    private Set<String> customSecurityConfigs;
    private List<UserTelemetryDto> users;
    private List<Project> projects;
    private List<ProjectStatistics> projectStatistics;

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

    Builder setMessageSequenceNumber(@Nullable Long messageSequenceNumber) {
      this.messageSequenceNumber = messageSequenceNumber;
      return this;
    }

    Builder setPlugins(Map<String, String> plugins) {
      this.plugins = plugins;
      return this;
    }

    Builder setDatabase(Database database) {
      this.database = database;
      return this;
    }

    Builder setEdition(@Nullable Edition edition) {
      this.edition = edition;
      return this;
    }

    Builder setInstallationDate(@Nullable Long installationDate) {
      this.installationDate = installationDate;
      return this;
    }

    Builder setInstallationVersion(@Nullable String installationVersion) {
      this.installationVersion = installationVersion;
      return this;
    }

    Builder setInDocker(boolean inDocker) {
      this.inDocker = inDocker;
      return this;
    }

    Builder setHasUnanalyzedC(@Nullable Boolean hasUnanalyzedC) {
      this.hasUnanalyzedC = hasUnanalyzedC;
      return this;
    }

    Builder setHasUnanalyzedCpp(@Nullable Boolean hasUnanalyzedCpp) {
      this.hasUnanalyzedCpp = hasUnanalyzedCpp;
      return this;
    }

    Builder setCustomSecurityConfigs(Set<String> customSecurityConfigs) {
      this.customSecurityConfigs = customSecurityConfigs;
      return this;
    }

    Builder setUsers(List<UserTelemetryDto> users) {
      this.users = users;
      return this;
    }

    Builder setProjects(List<Project> projects) {
      this.projects = projects;
      return this;
    }

    public Builder setIsScimEnabled(boolean isEnabled) {
      this.isScimEnabled = isEnabled;
      return this;
    }

    TelemetryData build() {
      requireNonNullValues(serverId, version, plugins, database, messageSequenceNumber);
      return new TelemetryData(this);
    }

    Builder setProjectStatistics(List<ProjectStatistics> projectStatistics) {
      this.projectStatistics = projectStatistics;
      return this;
    }

    private static void requireNonNullValues(Object... values) {
      Arrays.stream(values).forEach(Objects::requireNonNull);
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

  static class Project {
    private final String projectUuid;
    private final String language;
    private final Long loc;
    private final Long lastAnalysis;

    public Project(String projectUuid, Long lastAnalysis, String language, Long loc) {
      this.projectUuid = projectUuid;
      this.lastAnalysis = lastAnalysis;
      this.language = language;
      this.loc = loc;
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    public String getLanguage() {
      return language;
    }

    public Long getLoc() {
      return loc;
    }

    public Long getLastAnalysis() {
      return lastAnalysis;
    }
  }

  static class ProjectStatistics {
    private final String projectUuid;
    private final Long branchCount;
    private final Long pullRequestCount;
    private final String scm;
    private final String ci;
    private final String devopsPlatform;

    ProjectStatistics(String projectUuid, Long branchCount, Long pullRequestCount,
      @Nullable String scm, @Nullable String ci, @Nullable String devopsPlatform) {
      this.projectUuid = projectUuid;
      this.branchCount = branchCount;
      this.pullRequestCount = pullRequestCount;
      this.scm = scm;
      this.ci = ci;
      this.devopsPlatform = devopsPlatform;
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    public Long getBranchCount() {
      return branchCount;
    }

    public Long getPullRequestCount() {
      return pullRequestCount;
    }

    @CheckForNull
    public String getScm() {
      return scm;
    }

    @CheckForNull
    public String getCi() {
      return ci;
    }

    @CheckForNull
    public String getDevopsPlatform() {
      return devopsPlatform;
    }

  }
}
