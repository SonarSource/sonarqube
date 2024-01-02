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

import com.google.common.annotations.VisibleForTesting;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.ProjectAlmKeyAndProject;
import org.sonar.db.component.AnalysisPropertyValuePerProject;
import org.sonar.db.component.PrBranchAnalyzedLanguageCountByProjectDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.ProjectLocDistributionDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.platform.DockerSupport;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.telemetry.TelemetryData.Database;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.internal.apachecommons.lang.StringUtils.startsWithIgnoreCase;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDCI;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDSCM;
import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;
import static org.sonar.core.platform.EditionProvider.Edition.DATACENTER;
import static org.sonar.core.platform.EditionProvider.Edition.ENTERPRISE;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP_KEY;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C_KEY;
import static org.sonar.server.telemetry.TelemetryDaemon.I_PROP_MESSAGE_SEQUENCE;

@ServerSide
public class TelemetryDataLoaderImpl implements TelemetryDataLoader {
  @VisibleForTesting
  static final String SCIM_PROPERTY_ENABLED = "sonar.scim.enabled";
  private static final String UNDETECTED = "undetected";

  private static final Map<String, String> LANGUAGES_BY_SECURITY_JSON_PROPERTY_MAP = Map.of(
    "sonar.security.config.javasecurity", "java",
    "sonar.security.config.phpsecurity", "php",
    "sonar.security.config.pythonsecurity", "python",
    "sonar.security.config.roslyn.sonaranalyzer.security.cs", "csharp");

  private final Server server;
  private final DbClient dbClient;
  private final PluginRepository pluginRepository;
  private final PlatformEditionProvider editionProvider;
  private final Configuration configuration;
  private final InternalProperties internalProperties;
  private final DockerSupport dockerSupport;
  private final QualityGateCaycChecker qualityGateCaycChecker;
  private final QualityGateFinder qualityGateFinder;

  @Inject
  public TelemetryDataLoaderImpl(Server server, DbClient dbClient, PluginRepository pluginRepository,
    PlatformEditionProvider editionProvider, InternalProperties internalProperties, Configuration configuration,
    DockerSupport dockerSupport, QualityGateCaycChecker qualityGateCaycChecker, QualityGateFinder qualityGateFinder) {
    this.server = server;
    this.dbClient = dbClient;
    this.pluginRepository = pluginRepository;
    this.editionProvider = editionProvider;
    this.internalProperties = internalProperties;
    this.configuration = configuration;
    this.dockerSupport = dockerSupport;
    this.qualityGateCaycChecker = qualityGateCaycChecker;
    this.qualityGateFinder = qualityGateFinder;
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

    data.setMessageSequenceNumber(retrieveCurrentMessageSequenceNumber() + 1);
    data.setServerId(server.getId());
    data.setVersion(server.getVersion());
    data.setEdition(editionProvider.get().orElse(null));
    Function<PluginInfo, String> getVersion = plugin -> plugin.getVersion() == null ? "undefined" : plugin.getVersion().getName();
    Map<String, String> plugins = pluginRepository.getPluginInfos().stream().collect(MoreCollectors.uniqueIndex(PluginInfo::getKey,
      getVersion));
    data.setPlugins(plugins);
    try (DbSession dbSession = dbClient.openSession(false)) {
      data.setDatabase(loadDatabaseMetadata(dbSession));

      String defaultQualityGateUuid = qualityGateFinder.getDefault(dbSession).getUuid();

      data.setDefaultQualityGate(defaultQualityGateUuid);
      resolveUnanalyzedLanguageCode(data, dbSession);
      resolveProjectStatistics(data, dbSession, defaultQualityGateUuid);
      resolveProjects(data, dbSession);
      resolveQualityGates(data, dbSession);
      resolveUsers(data, dbSession);
    }

    setSecurityCustomConfigIfPresent(data);

    Optional<String> installationDateProperty = internalProperties.read(InternalProperties.INSTALLATION_DATE);
    installationDateProperty.ifPresent(s -> data.setInstallationDate(Long.valueOf(s)));
    Optional<String> installationVersionProperty = internalProperties.read(InternalProperties.INSTALLATION_VERSION);

    return data
      .setInstallationVersion(installationVersionProperty.orElse(null))
      .setInDocker(dockerSupport.isRunningInDocker())
      .setIsScimEnabled(isScimEnabled())
      .build();
  }

  private void resolveUnanalyzedLanguageCode(TelemetryData.Builder data, DbSession dbSession) {
    long numberOfUnanalyzedCMeasures = dbClient.liveMeasureDao().countProjectsHavingMeasure(dbSession, UNANALYZED_C_KEY);
    long numberOfUnanalyzedCppMeasures = dbClient.liveMeasureDao().countProjectsHavingMeasure(dbSession, UNANALYZED_CPP_KEY);
    editionProvider.get()
      .filter(edition -> edition.equals(COMMUNITY))
      .ifPresent(edition -> {
        data.setHasUnanalyzedC(numberOfUnanalyzedCMeasures > 0);
        data.setHasUnanalyzedCpp(numberOfUnanalyzedCppMeasures > 0);
      });
  }

  private Long retrieveCurrentMessageSequenceNumber() {
    return internalProperties.read(I_PROP_MESSAGE_SEQUENCE).map(Long::parseLong).orElse(0L);
  }

  private void resolveProjectStatistics(TelemetryData.Builder data, DbSession dbSession, String defaultQualityGateUuid) {
    List<String> projectUuids = dbClient.projectDao().selectAllProjectUuids(dbSession);
    Map<String, String> scmByProject = getAnalysisPropertyByProject(dbSession, SONAR_ANALYSIS_DETECTEDSCM);
    Map<String, String> ciByProject = getAnalysisPropertyByProject(dbSession, SONAR_ANALYSIS_DETECTEDCI);
    Map<String, ProjectAlmKeyAndProject> almAndUrlByProject = getAlmAndUrlByProject(dbSession);
    Map<String, PrBranchAnalyzedLanguageCountByProjectDto> prAndBranchCountByProject =
      dbClient.branchDao().countPrBranchAnalyzedLanguageByProjectUuid(dbSession)
        .stream().collect(toMap(PrBranchAnalyzedLanguageCountByProjectDto::getProjectUuid, Function.identity()));
    Map<String, String> qgatesByProject = getProjectQgatesMap(dbSession);
    Map<String, Map<String, Number>> metricsByProject =
      getProjectMetricsByMetricKeys(dbSession, TECHNICAL_DEBT_KEY, DEVELOPMENT_COST_KEY, SECURITY_HOTSPOTS_KEY, VULNERABILITIES_KEY,
        BUGS_KEY);

    List<TelemetryData.ProjectStatistics> projectStatistics = new ArrayList<>();
    for (String projectUuid : projectUuids) {
      Map<String, Number> metrics = metricsByProject.getOrDefault(projectUuid, Collections.emptyMap());
      Optional<PrBranchAnalyzedLanguageCountByProjectDto> counts = ofNullable(prAndBranchCountByProject.get(projectUuid));

      TelemetryData.ProjectStatistics stats = new TelemetryData.ProjectStatistics.Builder()
        .setProjectUuid(projectUuid)
        .setBranchCount(counts.map(PrBranchAnalyzedLanguageCountByProjectDto::getBranch).orElse(0L))
        .setPRCount(counts.map(PrBranchAnalyzedLanguageCountByProjectDto::getPullRequest).orElse(0L))
        .setQG(qgatesByProject.getOrDefault(projectUuid, defaultQualityGateUuid))
        .setScm(Optional.ofNullable(scmByProject.get(projectUuid)).orElse(UNDETECTED))
        .setCi(Optional.ofNullable(ciByProject.get(projectUuid)).orElse(UNDETECTED))
        .setDevops(resolveDevopsPlatform(almAndUrlByProject, projectUuid))
        .setBugs(metrics.getOrDefault("bugs", null))
        .setDevelopmentCost(metrics.getOrDefault("development_cost", null))
        .setVulnerabilities(metrics.getOrDefault("vulnerabilities", null))
        .setSecurityHotspots(metrics.getOrDefault("security_hotspots", null))
        .setTechnicalDebt(metrics.getOrDefault("sqale_index", null))
        .build();
      projectStatistics.add(stats);
    }
    data.setProjectStatistics(projectStatistics);
  }

  private static String resolveDevopsPlatform(Map<String, ProjectAlmKeyAndProject> almAndUrlByProject, String projectUuid) {
    if (almAndUrlByProject.containsKey(projectUuid)) {
      ProjectAlmKeyAndProject projectAlmKeyAndProject = almAndUrlByProject.get(projectUuid);
      return getAlmName(projectAlmKeyAndProject.getAlmId(), projectAlmKeyAndProject.getUrl());
    }
    return UNDETECTED;
  }

  private void resolveProjects(TelemetryData.Builder data, DbSession dbSession) {
    Map<String, String> metricUuidMap = getNclocMetricUuidMap(dbSession);
    String nclocUuid = metricUuidMap.get(NCLOC_KEY);
    String nclocDistributionUuid = metricUuidMap.get(NCLOC_LANGUAGE_DISTRIBUTION_KEY);
    List<ProjectLocDistributionDto> branchesWithLargestNcloc = dbClient.liveMeasureDao().selectLargestBranchesLocDistribution(dbSession, nclocUuid, nclocDistributionUuid);
    List<String> branchUuids = branchesWithLargestNcloc.stream().map(ProjectLocDistributionDto::branchUuid).toList();
    Map<String, Long> latestSnapshotMap = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, branchUuids)
      .stream()
      .collect(toMap(SnapshotDto::getComponentUuid, SnapshotDto::getBuildDate));
    data.setProjects(buildProjectsList(branchesWithLargestNcloc, latestSnapshotMap));
  }

  private static List<TelemetryData.Project> buildProjectsList(List<ProjectLocDistributionDto> branchesWithLargestNcloc,
    Map<String, Long> latestSnapshotMap) {
    return branchesWithLargestNcloc.stream()
      .flatMap(measure -> Arrays.stream(measure.locDistribution().split(";"))
        .map(languageAndLoc -> languageAndLoc.split("="))
        .map(languageAndLoc -> new TelemetryData.Project(
          measure.projectUuid(),
          latestSnapshotMap.get(measure.branchUuid()),
          languageAndLoc[0],
          Long.parseLong(languageAndLoc[1])
        ))
      ).toList();
  }

  private Map<String, String> getNclocMetricUuidMap(DbSession dbSession) {
    return dbClient.metricDao().selectByKeys(dbSession, asList(NCLOC_KEY, NCLOC_LANGUAGE_DISTRIBUTION_KEY))
      .stream()
      .collect(toMap(MetricDto::getKey, MetricDto::getUuid));
  }

  private void resolveQualityGates(TelemetryData.Builder data, DbSession dbSession) {
    List<TelemetryData.QualityGate> qualityGates = new ArrayList<>();
    Collection<QualityGateDto> qualityGateDtos = dbClient.qualityGateDao().selectAll(dbSession);
    for (QualityGateDto qualityGateDto : qualityGateDtos) {
      qualityGates.add(
        new TelemetryData.QualityGate(qualityGateDto.getUuid(), qualityGateCaycChecker.checkCaycCompliant(dbSession,
          qualityGateDto.getUuid()).toString())
      );
    }

    data.setQualityGates(qualityGates);
  }

  private void resolveUsers(TelemetryData.Builder data, DbSession dbSession) {
    data.setUsers(dbClient.userDao().selectUsersForTelemetry(dbSession));
  }

  private void setSecurityCustomConfigIfPresent(TelemetryData.Builder data) {
    editionProvider.get()
      .filter(edition -> asList(ENTERPRISE, DATACENTER).contains(edition))
      .ifPresent(edition -> data.setCustomSecurityConfigs(getCustomerSecurityConfigurations()));
  }

  private Map<String, String> getAnalysisPropertyByProject(DbSession dbSession, String analysisPropertyKey) {
    return dbClient.analysisPropertiesDao()
      .selectAnalysisPropertyValueInLastAnalysisPerProject(dbSession, analysisPropertyKey)
      .stream()
      .collect(toMap(AnalysisPropertyValuePerProject::getProjectUuid, AnalysisPropertyValuePerProject::getPropertyValue));
  }

  private Map<String, ProjectAlmKeyAndProject> getAlmAndUrlByProject(DbSession dbSession) {
    List<ProjectAlmKeyAndProject> projectAlmKeyAndProjects = dbClient.projectAlmSettingDao().selectAlmTypeAndUrlByProject(dbSession);
    return projectAlmKeyAndProjects.stream().collect(toMap(ProjectAlmKeyAndProject::getProjectUuid, Function.identity()));
  }

  private static String getAlmName(String alm, String url) {
    if (checkIfCloudAlm(alm, ALM.GITHUB.getId(), url, "https://api.github.com")) {
      return "github_cloud";
    }

    if (checkIfCloudAlm(alm, ALM.GITLAB.getId(), url, "https://gitlab.com/api/v4")) {
      return "gitlab_cloud";
    }

    if (checkIfCloudAlm(alm, ALM.AZURE_DEVOPS.getId(), url, "https://dev.azure.com")) {
      return "azure_devops_cloud";
    }

    if (ALM.BITBUCKET_CLOUD.getId().equals(alm)) {
      return alm;
    }

    return alm + "_server";
  }

  private Map<String, String> getProjectQgatesMap(DbSession dbSession) {
    return dbClient.projectQgateAssociationDao().selectAll(dbSession)
      .stream()
      .collect(toMap(ProjectQgateAssociationDto::getUuid, p -> Optional.ofNullable(p.getGateUuid()).orElse("")));
  }

  private Map<String, Map<String, Number>> getProjectMetricsByMetricKeys(DbSession dbSession, String... metricKeys) {
    Map<String, String> metricNamesByUuid = dbClient.metricDao().selectByKeys(dbSession, asList(metricKeys))
      .stream()
      .collect(toMap(MetricDto::getUuid, MetricDto::getKey));

    // metrics can be empty for un-analyzed projects
    if (metricNamesByUuid.isEmpty()) {
      return Collections.emptyMap();
    }

    return dbClient.liveMeasureDao().selectForProjectsByMetricUuids(dbSession, metricNamesByUuid.keySet())
      .stream()
      .collect(groupingBy(LiveMeasureDto::getProjectUuid,
        toMap(lmDto -> metricNamesByUuid.get(lmDto.getMetricUuid()),
          lmDto -> Optional.ofNullable(lmDto.getValue()).orElseGet(() -> Double.valueOf(lmDto.getTextValue())),
          (oldValue, newValue) -> newValue, HashMap::new)));
  }

  private static boolean checkIfCloudAlm(String almRaw, String alm, String url, String cloudUrl) {
    return alm.equals(almRaw) && startsWithIgnoreCase(url, cloudUrl);
  }

  @Override
  public String loadServerId() {
    return server.getId();
  }

  private Set<String> getCustomerSecurityConfigurations() {
    return LANGUAGES_BY_SECURITY_JSON_PROPERTY_MAP.keySet().stream()
      .filter(this::isPropertyPresentInConfiguration)
      .map(LANGUAGES_BY_SECURITY_JSON_PROPERTY_MAP::get)
      .collect(Collectors.toSet());
  }

  private boolean isPropertyPresentInConfiguration(String property) {
    return configuration.get(property).isPresent();
  }

  private boolean isScimEnabled() {
    return this.configuration.getBoolean(SCIM_PROPERTY_ENABLED).orElse(false);
  }
}
