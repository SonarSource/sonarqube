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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ProjectCountPerAnalysisPropertyValue;
import org.sonar.db.measure.SumNclocDbQuery;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.measure.index.ProjectMeasuresStatistics;
import org.sonar.server.platform.DockerSupport;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.telemetry.TelemetryData.Database;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserQuery;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;
import static org.sonar.core.platform.EditionProvider.Edition.DATACENTER;
import static org.sonar.core.platform.EditionProvider.Edition.ENTERPRISE;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP_KEY;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C_KEY;

@ServerSide
public class TelemetryDataLoaderImpl implements TelemetryDataLoader {
  private final Server server;
  private final DbClient dbClient;
  private final PluginRepository pluginRepository;
  private final UserIndex userIndex;
  private final ProjectMeasuresIndex projectMeasuresIndex;
  private final PlatformEditionProvider editionProvider;
  private final Configuration configuration;
  private final InternalProperties internalProperties;
  private final DockerSupport dockerSupport;
  @CheckForNull
  private final LicenseReader licenseReader;

  @Inject
  public TelemetryDataLoaderImpl(Server server, DbClient dbClient, PluginRepository pluginRepository, UserIndex userIndex, ProjectMeasuresIndex projectMeasuresIndex,
    PlatformEditionProvider editionProvider, InternalProperties internalProperties, Configuration configuration,
    DockerSupport dockerSupport, @Nullable LicenseReader licenseReader) {
    this.server = server;
    this.dbClient = dbClient;
    this.pluginRepository = pluginRepository;
    this.userIndex = userIndex;
    this.projectMeasuresIndex = projectMeasuresIndex;
    this.editionProvider = editionProvider;
    this.internalProperties = internalProperties;
    this.configuration = configuration;
    this.dockerSupport = dockerSupport;
    this.licenseReader = licenseReader;
  }

  private static Database loadDatabaseMetadata(DbSession dbSession) {
    try {
      DatabaseMetaData metadata = dbSession.getConnection().getMetaData();
      return new Database(metadata.getDatabaseProductName(), metadata.getDatabaseProductVersion());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to get DB metadata", e);
    }
  }

  @Override
  public TelemetryData load() {
    TelemetryData.Builder data = TelemetryData.builder();

    data.setServerId(server.getId());
    data.setVersion(server.getVersion());
    data.setEdition(editionProvider.get().orElse(null));
    ofNullable(licenseReader)
      .flatMap(reader -> licenseReader.read())
      .ifPresent(license -> data.setLicenseType(license.getType()));
    Function<PluginInfo, String> getVersion = plugin -> plugin.getVersion() == null ? "undefined" : plugin.getVersion().getName();
    Map<String, String> plugins = pluginRepository.getPluginInfos().stream().collect(MoreCollectors.uniqueIndex(PluginInfo::getKey, getVersion));
    data.setPlugins(plugins);
    long userCount = userIndex.search(UserQuery.builder().build(), new SearchOptions().setLimit(1)).getTotal();
    data.setUserCount(userCount);
    ProjectMeasuresStatistics projectMeasuresStatistics = projectMeasuresIndex.searchTelemetryStatistics();
    data.setProjectMeasuresStatistics(projectMeasuresStatistics);
    try (DbSession dbSession = dbClient.openSession(false)) {
      data.setDatabase(loadDatabaseMetadata(dbSession));
      data.setUsingBranches(dbClient.branchDao().hasNonMainBranches(dbSession));
      SumNclocDbQuery query = SumNclocDbQuery.builder()
        .setOnlyPrivateProjects(false)
        .build();
      data.setNcloc(dbClient.liveMeasureDao().sumNclocOfBiggestBranch(dbSession, query));
      long numberOfUnanalyzedCMeasures = dbClient.liveMeasureDao().countProjectsHavingMeasure(dbSession, UNANALYZED_C_KEY);
      long numberOfUnanalyzedCppMeasures = dbClient.liveMeasureDao().countProjectsHavingMeasure(dbSession, UNANALYZED_CPP_KEY);
      editionProvider.get()
        .filter(edition -> edition.equals(COMMUNITY))
        .ifPresent(edition -> {
          data.setHasUnanalyzedC(numberOfUnanalyzedCMeasures > 0);
          data.setHasUnanalyzedCpp(numberOfUnanalyzedCppMeasures > 0);
        });

      data.setAlmIntegrationCountByAlm(countAlmUsage(dbSession));
      data.setExternalAuthenticationProviders(dbClient.userDao().selectExternalIdentityProviders(dbSession));
      data.setSonarlintWeeklyUsers(dbClient.userDao().countSonarlintWeeklyUsers(dbSession));
      addScmInformationToTelemetry(dbSession, data);
      addCiInformationToTelemetry(dbSession, data);
    }

    setSecurityCustomConfigIfPresent(data);

    Optional<String> installationDateProperty = internalProperties.read(InternalProperties.INSTALLATION_DATE);
    installationDateProperty.ifPresent(s -> data.setInstallationDate(Long.valueOf(s)));
    Optional<String> installationVersionProperty = internalProperties.read(InternalProperties.INSTALLATION_VERSION);
    data.setInstallationVersion(installationVersionProperty.orElse(null));
    data.setInDocker(dockerSupport.isRunningInDocker());
    return data.build();
  }

  private void setSecurityCustomConfigIfPresent(TelemetryData.Builder data) {
    editionProvider.get()
      .filter(edition -> asList(ENTERPRISE, DATACENTER).contains(edition))
      .ifPresent(edition -> {
        List<String> customSecurityConfigs = new LinkedList<>();
        configuration.get("sonar.security.config.javasecurity")
          .ifPresent(s -> customSecurityConfigs.add("java"));
        configuration.get("sonar.security.config.phpsecurity")
          .ifPresent(s -> customSecurityConfigs.add("php"));
        configuration.get("sonar.security.config.pythonsecurity")
          .ifPresent(s -> customSecurityConfigs.add("python"));
        configuration.get("sonar.security.config.roslyn.sonaranalyzer.security.cs")
          .ifPresent(s -> customSecurityConfigs.add("csharp"));
        data.setCustomSecurityConfigs(customSecurityConfigs);
      });
  }

  private void addScmInformationToTelemetry(DbSession dbSession, TelemetryData.Builder data) {
    Map<String, Long> projectCountPerScmDetected = dbClient.analysisPropertiesDao()
      .selectProjectCountPerAnalysisPropertyValueInLastAnalysis(dbSession, CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDSCM)
      .stream()
      .collect(Collectors.toMap(ProjectCountPerAnalysisPropertyValue::getPropertyValue, ProjectCountPerAnalysisPropertyValue::getCount));
    data.setProjectCountByScm(projectCountPerScmDetected);
  }

  private void addCiInformationToTelemetry(DbSession dbSession, TelemetryData.Builder data) {
    Map<String, Long> projectCountPerCiDetected = dbClient.analysisPropertiesDao()
      .selectProjectCountPerAnalysisPropertyValueInLastAnalysis(dbSession, CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDCI)
      .stream()
      .collect(Collectors.toMap(ProjectCountPerAnalysisPropertyValue::getPropertyValue, ProjectCountPerAnalysisPropertyValue::getCount));
    data.setProjectCountByCi(projectCountPerCiDetected);
  }

  private Map<String, Long> countAlmUsage(DbSession dbSession) {
    return dbClient.almSettingDao().selectAll(dbSession).stream()
      .collect(Collectors.groupingBy(almSettingDto -> {
        if (checkIfCloudAlm(almSettingDto, ALM.GITHUB, "https://api.github.com")) {
          return "github_cloud";
        } else if (checkIfCloudAlm(almSettingDto, ALM.GITLAB, "https://gitlab.com/api/v4")) {
          return "gitlab_cloud";
        } else if (checkIfCloudAlm(almSettingDto, ALM.AZURE_DEVOPS, "https://dev.azure.com")) {
          return "azure_devops_cloud";
        } else if (ALM.BITBUCKET_CLOUD.equals(almSettingDto.getAlm())) {
          return almSettingDto.getRawAlm();
        }
        return almSettingDto.getRawAlm() + "_server";
      }, Collectors.counting()));
  }

  private static boolean checkIfCloudAlm(AlmSettingDto almSettingDto, ALM alm, String url) {
    return alm.equals(almSettingDto.getAlm()) && startsWith(almSettingDto.getUrl(), url);
  }

  @Override
  public String loadServerId() {
    return server.getId();
  }
}
