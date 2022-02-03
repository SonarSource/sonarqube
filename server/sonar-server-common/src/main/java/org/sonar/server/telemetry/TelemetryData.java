/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.server.measure.index.ProjectMeasuresStatistics;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class TelemetryData {
  private final String serverId;
  private final String version;
  private final Map<String, String> plugins;
  private final long ncloc;
  private final long userCount;
  private final long projectCount;
  private final boolean usingBranches;
  private final Database database;
  private final Map<String, Long> projectCountByLanguage;
  private final Map<String, Long> almIntegrationCountByAlm;
  private final Map<String, Long> nclocByLanguage;
  private final List<String> externalAuthenticationProviders;
  private final Map<String, Long> projectCountByScm;
  private final Map<String, Long> projectCountByCi;
  private final EditionProvider.Edition edition;
  private final String licenseType;
  private final Long installationDate;
  private final String installationVersion;
  private final boolean inDocker;
  private final Boolean hasUnanalyzedC;
  private final Boolean hasUnanalyzedCpp;
  private final List<String> customSecurityConfigs;
  private final long sonarlintWeeklyUsers;
  private final long numberOfConnectedSonarLintClients;

  private TelemetryData(Builder builder) {
    serverId = builder.serverId;
    version = builder.version;
    plugins = builder.plugins;
    ncloc = builder.ncloc;
    userCount = builder.userCount;
    projectCount = builder.projectMeasuresStatistics.getProjectCount();
    usingBranches = builder.usingBranches;
    database = builder.database;
    sonarlintWeeklyUsers = builder.sonarlintWeeklyUsers;
    projectCountByLanguage = builder.projectMeasuresStatistics.getProjectCountByLanguage();
    almIntegrationCountByAlm = builder.almIntegrationCountByAlm;
    nclocByLanguage = builder.projectMeasuresStatistics.getNclocByLanguage();
    edition = builder.edition;
    licenseType = builder.licenseType;
    installationDate = builder.installationDate;
    installationVersion = builder.installationVersion;
    inDocker = builder.inDocker;
    hasUnanalyzedC = builder.hasUnanalyzedC;
    hasUnanalyzedCpp = builder.hasUnanalyzedCpp;
    customSecurityConfigs = builder.customSecurityConfigs == null ? emptyList() : builder.customSecurityConfigs;
    externalAuthenticationProviders = builder.externalAuthenticationProviders;
    projectCountByScm = builder.projectCountByScm;
    projectCountByCi = builder.projectCountByCi;
    numberOfConnectedSonarLintClients = builder.numberOfConnectedSonarLintClients;
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

  public long getNcloc() {
    return ncloc;
  }

  public long sonarlintWeeklyUsers() {
    return sonarlintWeeklyUsers;
  }

  public long sonarLintConnectedClients() {
    return numberOfConnectedSonarLintClients;
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

  public Map<String, Long> getAlmIntegrationCountByAlm() {
    return almIntegrationCountByAlm;
  }

  public Map<String, Long> getNclocByLanguage() {
    return nclocByLanguage;
  }

  public Optional<EditionProvider.Edition> getEdition() {
    return Optional.ofNullable(edition);
  }

  public Optional<String> getLicenseType() {
    return Optional.ofNullable(licenseType);
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

  public Optional<Boolean> hasUnanalyzedC() {
    return Optional.ofNullable(hasUnanalyzedC);
  }

  public Optional<Boolean> hasUnanalyzedCpp() {
    return Optional.ofNullable(hasUnanalyzedCpp);
  }

  public List<String> getCustomSecurityConfigs() {
    return customSecurityConfigs;
  }

  public List<String> getExternalAuthenticationProviders() {
    return externalAuthenticationProviders;
  }

  public Map<String, Long> getProjectCountByScm() {
    return projectCountByScm;
  }

  public Map<String, Long> getProjectCountByCi() {
    return projectCountByCi;
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private String serverId;
    private String version;
    private long userCount;
    private long sonarlintWeeklyUsers;
    private Map<String, String> plugins;
    private Database database;
    private ProjectMeasuresStatistics projectMeasuresStatistics;
    private Map<String, Long> almIntegrationCountByAlm;
    private Long ncloc;
    private Boolean usingBranches;
    private Edition edition;
    private String licenseType;
    private Long installationDate;
    private String installationVersion;
    private boolean inDocker = false;
    private Boolean hasUnanalyzedC;
    private Boolean hasUnanalyzedCpp;
    private List<String> customSecurityConfigs;
    private List<String> externalAuthenticationProviders;
    private Map<String, Long> projectCountByScm;
    private Map<String, Long> projectCountByCi;
    private long numberOfConnectedSonarLintClients;

    private Builder() {
      // enforce static factory method
    }

    Builder setExternalAuthenticationProviders(List<String> providers) {
      this.externalAuthenticationProviders = providers;
      return this;
    }

    Builder setProjectCountByScm(Map<String, Long> projectCountByScm) {
      this.projectCountByScm = projectCountByScm;
      return this;
    }

    Builder setSonarlintWeeklyUsers(long sonarlintWeeklyUsers) {
      this.sonarlintWeeklyUsers = sonarlintWeeklyUsers;
      return this;
    }

    Builder setProjectCountByCi(Map<String, Long> projectCountByCi) {
      this.projectCountByCi = projectCountByCi;
      return this;
    }

    Builder setServerId(String serverId) {
      this.serverId = serverId;
      return this;
    }

    Builder setVersion(String version) {
      this.version = version;
      return this;
    }

    Builder setUserCount(long userCount) {
      this.userCount = userCount;
      return this;
    }

    Builder setPlugins(Map<String, String> plugins) {
      this.plugins = plugins;
      return this;
    }

    Builder setAlmIntegrationCountByAlm(Map<String, Long> almIntegrationCountByAlm) {
      this.almIntegrationCountByAlm = almIntegrationCountByAlm;
      return this;
    }

    Builder setProjectMeasuresStatistics(ProjectMeasuresStatistics projectMeasuresStatistics) {
      this.projectMeasuresStatistics = projectMeasuresStatistics;
      return this;
    }

    Builder setNcloc(long ncloc) {
      this.ncloc = ncloc;
      return this;
    }

    Builder setDatabase(Database database) {
      this.database = database;
      return this;
    }

    Builder setUsingBranches(boolean usingBranches) {
      this.usingBranches = usingBranches;
      return this;
    }

    Builder setEdition(@Nullable Edition edition) {
      this.edition = edition;
      return this;
    }

    Builder setLicenseType(@Nullable String licenseType) {
      this.licenseType = licenseType;
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

    Builder setCustomSecurityConfigs(List<String> customSecurityConfigs) {
      this.customSecurityConfigs = customSecurityConfigs;
      return this;
    }

    Builder setNumberOfConnectedSonarLintClients(long numberOfConnectedSonarLintClients) {
      this.numberOfConnectedSonarLintClients = numberOfConnectedSonarLintClients;
      return this;
    }

    TelemetryData build() {
      requireNonNull(serverId);
      requireNonNull(version);
      requireNonNull(plugins);
      requireNonNull(projectMeasuresStatistics);
      requireNonNull(almIntegrationCountByAlm);
      requireNonNull(ncloc);
      requireNonNull(database);
      requireNonNull(usingBranches);
      requireNonNull(externalAuthenticationProviders);
      requireNonNull(projectCountByScm);
      requireNonNull(projectCountByCi);

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
