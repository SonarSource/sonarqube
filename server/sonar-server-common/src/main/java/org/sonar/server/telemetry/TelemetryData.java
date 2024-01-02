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
package org.sonar.server.telemetry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
  private final String defaultQualityGate;
  private final Long installationDate;
  private final String installationVersion;
  private final boolean inDocker;
  private final boolean isScimEnabled;
  private final List<UserTelemetryDto> users;
  private final List<Project> projects;
  private final List<ProjectStatistics> projectStatistics;
  private final List<QualityGate> qualityGates;
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
    defaultQualityGate = builder.defaultQualityGate;
    installationDate = builder.installationDate;
    installationVersion = builder.installationVersion;
    inDocker = builder.inDocker;
    isScimEnabled = builder.isScimEnabled;
    users = builder.users;
    projects = builder.projects;
    projectStatistics = builder.projectStatistics;
    qualityGates = builder.qualityGates;
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

  public String getDefaultQualityGate() {
    return defaultQualityGate;
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

  public List<QualityGate> getQualityGates() {
    return qualityGates;
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
    private String defaultQualityGate;
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
    private List<QualityGate> qualityGates;

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

    Builder setDefaultQualityGate(String defaultQualityGate) {
      this.defaultQualityGate = defaultQualityGate;
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

    Builder setQualityGates(List<QualityGate> qualityGates) {
      this.qualityGates = qualityGates;
      return this;
    }

    private static void requireNonNullValues(Object... values) {
      Arrays.stream(values).forEach(Objects::requireNonNull);
    }

  }

  record Database(String name, String version) {
  }

  record Project(String projectUuid, Long lastAnalysis, String language, Long loc) {
  }

  record QualityGate(String uuid, String caycStatus) {
  }

  public static class ProjectStatistics {
    private final String projectUuid;
    private final Long branchCount;
    private final Long pullRequestCount;
    private final String qualityGate;
    private final String scm;
    private final String ci;
    private final String devopsPlatform;
    private final Long bugs;
    private final Long vulnerabilities;
    private final Long securityHotspots;
    private final Long technicalDebt;
    private final Long developmentCost;

    ProjectStatistics(Builder builder) {
      this.projectUuid = builder.projectUuid;
      this.branchCount = builder.branchCount;
      this.pullRequestCount = builder.pullRequestCount;
      this.qualityGate = builder.qualityGate;
      this.scm = builder.scm;
      this.ci = builder.ci;
      this.devopsPlatform = builder.devopsPlatform;
      this.bugs = builder.bugs;
      this.vulnerabilities = builder.vulnerabilities;
      this.securityHotspots = builder.securityHotspots;
      this.technicalDebt = builder.technicalDebt;
      this.developmentCost = builder.developmentCost;
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

    public String getQualityGate() {
      return qualityGate;
    }

    public String getScm() {
      return scm;
    }

    public String getCi() {
      return ci;
    }

    public String getDevopsPlatform() {
      return devopsPlatform;
    }

    public Optional<Long> getBugs() {
      return Optional.ofNullable(bugs);
    }

    public Optional<Long> getVulnerabilities() {
      return Optional.ofNullable(vulnerabilities);
    }

    public Optional<Long> getSecurityHotspots() {
      return Optional.ofNullable(securityHotspots);
    }

    public Optional<Long> getTechnicalDebt() {
      return Optional.ofNullable(technicalDebt);
    }

    public Optional<Long> getDevelopmentCost() {
      return Optional.ofNullable(developmentCost);
    }

    static class Builder {
      private String projectUuid;
      private Long branchCount;
      private Long pullRequestCount;
      private String qualityGate;
      private String scm;
      private String ci;
      private String devopsPlatform;
      private Long bugs;
      private Long vulnerabilities;
      private Long securityHotspots;
      private Long technicalDebt;
      private Long developmentCost;

      public Builder setProjectUuid(String projectUuid) {
        this.projectUuid = projectUuid;
        return this;
      }

      public Builder setBranchCount(Long branchCount) {
        this.branchCount = branchCount;
        return this;
      }

      public Builder setPRCount(Long pullRequestCount) {
        this.pullRequestCount = pullRequestCount;
        return this;
      }

      public Builder setQG(String qualityGate) {
        this.qualityGate = qualityGate;
        return this;
      }

      public Builder setScm(String scm) {
        this.scm = scm;
        return this;
      }

      public Builder setCi(String ci) {
        this.ci = ci;
        return this;
      }

      public Builder setDevops(String devopsPlatform) {
        this.devopsPlatform = devopsPlatform;
        return this;
      }

      public Builder setBugs(@Nullable Number bugs) {
        this.bugs = bugs != null ? bugs.longValue() : null;
        return this;
      }

      public Builder setVulnerabilities(@Nullable Number vulnerabilities) {
        this.vulnerabilities = vulnerabilities != null ? vulnerabilities.longValue() : null;
        return this;
      }

      public Builder setSecurityHotspots(@Nullable Number securityHotspots) {
        this.securityHotspots = securityHotspots != null ? securityHotspots.longValue() : null;
        return this;
      }

      public Builder setTechnicalDebt(@Nullable Number technicalDebt) {
        this.technicalDebt = technicalDebt != null ? technicalDebt.longValue() : null;
        return this;
      }

      public Builder setDevelopmentCost(@Nullable Number developmentCost) {
        this.developmentCost = developmentCost != null ? developmentCost.longValue() : null;
        return this;
      }

      public ProjectStatistics build() {
        return new ProjectStatistics(this);
      }
    }
  }
}
