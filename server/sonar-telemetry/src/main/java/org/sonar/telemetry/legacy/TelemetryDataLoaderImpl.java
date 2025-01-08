/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.telemetry.legacy;

import jakarta.inject.Inject;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.ProjectAlmKeyAndProject;
import org.sonar.db.component.AnalysisPropertyValuePerProject;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchMeasuresDto;
import org.sonar.db.component.PrBranchAnalyzedLanguageCountByProjectDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceVerifier;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.telemetry.legacy.TelemetryData.Database;
import org.sonar.telemetry.legacy.TelemetryData.NewCodeDefinition;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDCI;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDSCM;
import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;
import static org.sonar.core.platform.EditionProvider.Edition.DATACENTER;
import static org.sonar.core.platform.EditionProvider.Edition.ENTERPRISE;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP_KEY;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C_KEY;
import static org.sonar.server.qualitygate.Condition.Operator.fromDbValue;
import static org.sonar.telemetry.TelemetryDaemon.I_PROP_MESSAGE_SEQUENCE;

@ServerSide
public class TelemetryDataLoaderImpl implements TelemetryDataLoader {
  private static final String UNDETECTED = "undetected";
  public static final String EXTERNAL_SECURITY_REPORT_EXPORTED_AT = "project.externalSecurityReportExportedAt";

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
  private final ContainerSupport containerSupport;
  private final QualityGateCaycChecker qualityGateCaycChecker;
  private final QualityGateFinder qualityGateFinder;
  private final ManagedInstanceService managedInstanceService;
  private final CloudUsageDataProvider cloudUsageDataProvider;
  private final QualityProfileDataProvider qualityProfileDataProvider;
  private final AiCodeAssuranceVerifier aiCodeAssuranceVerifier;
  private final ProjectLocDistributionDataProvider projectLocDistributionDataProvider;
  private final Set<NewCodeDefinition> newCodeDefinitions = new HashSet<>();
  private final Map<String, NewCodeDefinition> ncdByProject = new HashMap<>();
  private final Map<String, NewCodeDefinition> ncdByBranch = new HashMap<>();
  private final Map<String, String> defaultQualityProfileByLanguage = new HashMap<>();
  private final Map<ProjectLanguageKey, String> qualityProfileByProjectAndLanguage = new HashMap<>();
  private NewCodeDefinition instanceNcd = NewCodeDefinition.getInstanceDefault();

  @Inject
  public TelemetryDataLoaderImpl(Server server, DbClient dbClient, PluginRepository pluginRepository,
    PlatformEditionProvider editionProvider, InternalProperties internalProperties, Configuration configuration,
    ContainerSupport containerSupport, QualityGateCaycChecker qualityGateCaycChecker, QualityGateFinder qualityGateFinder,
    ManagedInstanceService managedInstanceService, CloudUsageDataProvider cloudUsageDataProvider, QualityProfileDataProvider qualityProfileDataProvider,
    AiCodeAssuranceVerifier aiCodeAssuranceVerifier, ProjectLocDistributionDataProvider projectLocDistributionDataProvider) {
    this.server = server;
    this.dbClient = dbClient;
    this.pluginRepository = pluginRepository;
    this.editionProvider = editionProvider;
    this.internalProperties = internalProperties;
    this.configuration = configuration;
    this.containerSupport = containerSupport;
    this.qualityGateCaycChecker = qualityGateCaycChecker;
    this.qualityGateFinder = qualityGateFinder;
    this.managedInstanceService = managedInstanceService;
    this.cloudUsageDataProvider = cloudUsageDataProvider;
    this.qualityProfileDataProvider = qualityProfileDataProvider;
    this.aiCodeAssuranceVerifier = aiCodeAssuranceVerifier;
    this.projectLocDistributionDataProvider = projectLocDistributionDataProvider;
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
    Map<String, String> plugins = pluginRepository.getPluginInfos().stream().collect(toMap(PluginInfo::getKey, getVersion));
    data.setPlugins(plugins);
    try (DbSession dbSession = dbClient.openSession(false)) {
      var branchMeasuresDtos = dbClient.branchDao().selectBranchMeasuresWithCaycMetric(dbSession);
      loadNewCodeDefinitions(dbSession, branchMeasuresDtos);
      loadQualityProfiles(dbSession);

      data.setDatabase(loadDatabaseMetadata(dbSession));
      data.setNcdId(instanceNcd.hashCode());
      data.setNewCodeDefinitions(newCodeDefinitions);

      String defaultQualityGateUuid = qualityGateFinder.getDefault(dbSession).getUuid();
      String sonarWayQualityGateUuid = qualityGateFinder.getSonarWay(dbSession).getUuid();
      List<ProjectDto> projects = dbClient.projectDao().selectProjects(dbSession);

      data.setDefaultQualityGate(defaultQualityGateUuid);
      data.setSonarWayQualityGate(sonarWayQualityGateUuid);
      resolveUnanalyzedLanguageCode(data, dbSession);
      resolveProjectStatistics(data, dbSession, defaultQualityGateUuid, projects);
      resolveProjects(data, dbSession);
      resolveBranches(data, branchMeasuresDtos);
      resolveQualityGates(data, dbSession);
      resolveUsers(data, dbSession);
    }

    data.setQualityProfiles(qualityProfileDataProvider.retrieveQualityProfilesData());

    setSecurityCustomConfigIfPresent(data);

    Optional<String> installationDateProperty = internalProperties.read(InternalProperties.INSTALLATION_DATE);
    installationDateProperty.ifPresent(s -> data.setInstallationDate(Long.valueOf(s)));
    Optional<String> installationVersionProperty = internalProperties.read(InternalProperties.INSTALLATION_VERSION);

    return data
      .setInstallationVersion(installationVersionProperty.orElse(null))
      .setInContainer(containerSupport.isRunningInContainer())
      .setManagedInstanceInformation(buildManagedInstanceInformation())
      .setCloudUsage(buildCloudUsage())
      .build();
  }

  private void resolveBranches(TelemetryData.Builder data, List<BranchMeasuresDto> branchMeasuresDtos) {
    var branches = branchMeasuresDtos.stream()
      .map(dto -> {
        var projectNcd = ncdByProject.getOrDefault(dto.getProjectUuid(), instanceNcd);
        var ncdId = ncdByBranch.getOrDefault(dto.getBranchUuid(), projectNcd).hashCode();
        return new TelemetryData.Branch(
          dto.getProjectUuid(), dto.getBranchUuid(), ncdId,
          dto.getGreenQualityGateCount(), dto.getAnalysisCount(), dto.getExcludeFromPurge());
      })
      .toList();
    data.setBranches(branches);
  }

  @Override
  public void reset() {
    this.newCodeDefinitions.clear();
    this.ncdByBranch.clear();
    this.ncdByProject.clear();
    this.instanceNcd = NewCodeDefinition.getInstanceDefault();
    this.defaultQualityProfileByLanguage.clear();
    this.qualityProfileByProjectAndLanguage.clear();
  }

  private void loadNewCodeDefinitions(DbSession dbSession, List<BranchMeasuresDto> branchMeasuresDtos) {
    var branchUuidByKey = branchMeasuresDtos.stream()
      .collect(Collectors.toMap(dto -> createBranchUniqueKey(dto.getProjectUuid(), dto.getBranchKey()), BranchMeasuresDto::getBranchUuid));
    List<NewCodePeriodDto> newCodePeriodDtos = dbClient.newCodePeriodDao().selectAll(dbSession);
    NewCodeDefinition ncd;
    boolean hasInstance = false;
    for (var dto : newCodePeriodDtos) {
      String projectUuid = dto.getProjectUuid();
      String branchUuid = dto.getBranchUuid();
      if (branchUuid == null && projectUuid == null) {
        ncd = new NewCodeDefinition(dto.getType().name(), dto.getValue(), "instance");
        this.instanceNcd = ncd;
        hasInstance = true;
      } else if (projectUuid != null) {
        var value = dto.getType() == REFERENCE_BRANCH ? branchUuidByKey.get(createBranchUniqueKey(projectUuid, dto.getValue())) : dto.getValue();
        if (branchUuid == null || isCommunityEdition()) {
          ncd = new NewCodeDefinition(dto.getType().name(), value, "project");
          this.ncdByProject.put(projectUuid, ncd);
        } else {
          ncd = new NewCodeDefinition(dto.getType().name(), value, "branch");
          this.ncdByBranch.put(branchUuid, ncd);
        }
      } else {
        throw new IllegalStateException(String.format("Error in loading telemetry data. New code definition for branch %s doesn't have a projectUuid", branchUuid));
      }
      this.newCodeDefinitions.add(ncd);
    }
    if (!hasInstance) {
      this.newCodeDefinitions.add(NewCodeDefinition.getInstanceDefault());
    }
  }

  private void loadQualityProfiles(DbSession dbSession) {
    dbClient.qualityProfileDao().selectAllDefaultProfiles(dbSession)
      .forEach(defaultQualityProfile -> this.defaultQualityProfileByLanguage.put(defaultQualityProfile.getLanguage(), defaultQualityProfile.getKee()));

    dbClient.qualityProfileDao().selectAllProjectAssociations(dbSession)
      .forEach(projectAssociation -> qualityProfileByProjectAndLanguage.put(
        new ProjectLanguageKey(projectAssociation.projectUuid(), projectAssociation.language()),
        projectAssociation.profileKey()));
  }

  private boolean isCommunityEdition() {
    var edition = editionProvider.get();
    return edition.isPresent() && edition.get() == COMMUNITY;
  }

  private static String createBranchUniqueKey(String projectUuid, @Nullable String branchKey) {
    return projectUuid + "-" + branchKey;
  }

  private void resolveUnanalyzedLanguageCode(TelemetryData.Builder data, DbSession dbSession) {
    editionProvider.get()
      .filter(edition -> edition.equals(COMMUNITY))
      .ifPresent(edition -> {
        List<BranchDto> mainBranches = dbClient.branchDao().selectMainBranches(dbSession);
        List<MeasureDto> measureDtos = dbClient.measureDao().selectByComponentUuidsAndMetricKeys(dbSession,
          mainBranches.stream().map(BranchDto::getUuid).toList(), List.of(UNANALYZED_C_KEY, UNANALYZED_CPP_KEY));

        long numberOfUnanalyzedCMeasures = countProjectsHavingMeasure(measureDtos, UNANALYZED_C_KEY);
        long numberOfUnanalyzedCppMeasures = countProjectsHavingMeasure(measureDtos, UNANALYZED_CPP_KEY);

        data.setHasUnanalyzedC(numberOfUnanalyzedCMeasures > 0);
        data.setHasUnanalyzedCpp(numberOfUnanalyzedCppMeasures > 0);
      });
  }

  private static long countProjectsHavingMeasure(List<MeasureDto> measureDtos, String metricKey) {
    return measureDtos.stream()
      .filter(m -> m.getMetricValues().containsKey(metricKey))
      .count();
  }

  private Long retrieveCurrentMessageSequenceNumber() {
    return internalProperties.read(I_PROP_MESSAGE_SEQUENCE).map(Long::parseLong).orElse(0L);
  }

  private void resolveProjectStatistics(TelemetryData.Builder data, DbSession dbSession, String defaultQualityGateUuid, List<ProjectDto> projects) {
    Map<String, String> scmByProject = getAnalysisPropertyByProject(dbSession, SONAR_ANALYSIS_DETECTEDSCM);
    Map<String, String> ciByProject = getAnalysisPropertyByProject(dbSession, SONAR_ANALYSIS_DETECTEDCI);
    Map<String, ProjectAlmKeyAndProject> almAndUrlAndMonorepoByProject = getAlmAndUrlByProject(dbSession);
    Map<String, PrBranchAnalyzedLanguageCountByProjectDto> prAndBranchCountByProject = dbClient.branchDao().countPrBranchAnalyzedLanguageByProjectUuid(dbSession)
      .stream().collect(toMap(PrBranchAnalyzedLanguageCountByProjectDto::getProjectUuid, Function.identity()));
    Map<String, String> qgatesByProject = getProjectQgatesMap(dbSession);
    Map<String, Map<String, Number>> metricsByProject = getProjectMetricsByMetricKeys(dbSession, List.of(TECHNICAL_DEBT_KEY,
      DEVELOPMENT_COST_KEY, SECURITY_HOTSPOTS_KEY, VULNERABILITIES_KEY, BUGS_KEY));
    Map<String, Long> securityReportExportedAtByProjectUuid = getSecurityReportExportedAtDateByProjectUuid(dbSession);

    List<TelemetryData.ProjectStatistics> projectStatistics = new ArrayList<>();
    for (ProjectDto project : projects) {
      String projectUuid = project.getUuid();
      Map<String, Number> metrics = metricsByProject.getOrDefault(projectUuid, Collections.emptyMap());
      Optional<PrBranchAnalyzedLanguageCountByProjectDto> counts = ofNullable(prAndBranchCountByProject.get(projectUuid));

      TelemetryData.ProjectStatistics stats = new TelemetryData.ProjectStatistics.Builder()
        .setProjectUuid(projectUuid)
        .setBranchCount(counts.map(PrBranchAnalyzedLanguageCountByProjectDto::getBranch).orElse(0L))
        .setPRCount(counts.map(PrBranchAnalyzedLanguageCountByProjectDto::getPullRequest).orElse(0L))
        .setQG(qgatesByProject.getOrDefault(projectUuid, defaultQualityGateUuid))
        .setScm(Optional.ofNullable(scmByProject.get(projectUuid)).orElse(UNDETECTED))
        .setCi(Optional.ofNullable(ciByProject.get(projectUuid)).orElse(UNDETECTED))
        .setDevops(resolveDevopsPlatform(almAndUrlAndMonorepoByProject, projectUuid))
        .setBugs(metrics.getOrDefault("bugs", null))
        .setDevelopmentCost(metrics.getOrDefault("development_cost", null))
        .setVulnerabilities(metrics.getOrDefault("vulnerabilities", null))
        .setSecurityHotspots(metrics.getOrDefault("security_hotspots", null))
        .setTechnicalDebt(metrics.getOrDefault("sqale_index", null))
        .setNcdId(ncdByProject.getOrDefault(projectUuid, instanceNcd).hashCode())
        .setExternalSecurityReportExportedAt(securityReportExportedAtByProjectUuid.get(projectUuid))
        .setCreationMethod(project.getCreationMethod())
        .setMonorepo(resolveMonorepo(almAndUrlAndMonorepoByProject, projectUuid))
        .setIsAiCodeAssured(aiCodeAssuranceVerifier.isAiCodeAssured(project))
        .build();
      projectStatistics.add(stats);
    }
    data.setProjectStatistics(projectStatistics);
  }

  private Map<String, Long> getSecurityReportExportedAtDateByProjectUuid(DbSession dbSession) {
    PropertyQuery propertyQuery = PropertyQuery.builder().setKey(EXTERNAL_SECURITY_REPORT_EXPORTED_AT).build();
    List<PropertyDto> properties = dbClient.propertiesDao().selectByQuery(propertyQuery, dbSession);
    return properties.stream()
      .collect(toMap(PropertyDto::getEntityUuid, propertyDto -> Long.parseLong(propertyDto.getValue())));
  }

  private static String resolveDevopsPlatform(Map<String, ProjectAlmKeyAndProject> almAndUrlByProject, String projectUuid) {
    if (almAndUrlByProject.containsKey(projectUuid)) {
      ProjectAlmKeyAndProject projectAlmKeyAndProject = almAndUrlByProject.get(projectUuid);
      return getAlmName(projectAlmKeyAndProject.getAlmId(), projectAlmKeyAndProject.getUrl());
    }
    return UNDETECTED;
  }

  private static Boolean resolveMonorepo(Map<String, ProjectAlmKeyAndProject> almAndUrlByProject, String projectUuid) {
    return Optional.ofNullable(almAndUrlByProject.get(projectUuid))
      .map(ProjectAlmKeyAndProject::getMonorepo)
      .orElse(false);
  }

  private void resolveProjects(TelemetryData.Builder data, DbSession dbSession) {
    List<ProjectLocDistributionDto> branchesWithLargestNcloc = projectLocDistributionDataProvider.getProjectLocDistribution(dbSession);
    List<String> branchUuids = branchesWithLargestNcloc.stream().map(ProjectLocDistributionDto::branchUuid).toList();
    Map<String, Long> latestSnapshotMap = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, branchUuids)
      .stream()
      .collect(toMap(SnapshotDto::getRootComponentUuid, SnapshotDto::getAnalysisDate));
    data.setProjects(buildProjectsList(branchesWithLargestNcloc, latestSnapshotMap));
  }

  private List<TelemetryData.Project> buildProjectsList(List<ProjectLocDistributionDto> branchesWithLargestNcloc, Map<String, Long> latestSnapshotMap) {
    return branchesWithLargestNcloc.stream()
      .flatMap(measure -> Arrays.stream(measure.locDistribution().split(";"))
        .map(languageAndLoc -> languageAndLoc.split("="))
        .map(languageAndLoc -> new TelemetryData.Project(
          measure.projectUuid(),
          latestSnapshotMap.get(measure.branchUuid()),
          languageAndLoc[0],
          getQualityProfile(measure.projectUuid(), languageAndLoc[0]),
          Long.parseLong(languageAndLoc[1]))))
      .toList();
  }

  private String getQualityProfile(String projectUuid, String language) {
    String qualityProfile = this.qualityProfileByProjectAndLanguage.get(new ProjectLanguageKey(projectUuid, language));
    if (qualityProfile != null) {
      return qualityProfile;
    }
    return this.defaultQualityProfileByLanguage.get(language);
  }

  private void resolveQualityGates(TelemetryData.Builder data, DbSession dbSession) {
    List<TelemetryData.QualityGate> qualityGates = new ArrayList<>();
    Collection<QualityGateDto> qualityGateDtos = dbClient.qualityGateDao().selectAll(dbSession);
    Collection<QualityGateConditionDto> qualityGateConditions = dbClient.gateConditionDao().selectAll(dbSession);
    Map<String, MetricDto> metricsByUuid = getMetricsByUuid(dbSession, qualityGateConditions);

    Map<String, List<Condition>> conditionsMap = mapQualityGateConditions(qualityGateConditions, metricsByUuid);

    for (QualityGateDto qualityGateDto : qualityGateDtos) {
      String qualityGateUuid = qualityGateDto.getUuid();
      List<Condition> conditions = conditionsMap.getOrDefault(qualityGateUuid, Collections.emptyList());
      qualityGates.add(
        new TelemetryData.QualityGate(qualityGateDto.getUuid(), qualityGateCaycChecker.checkCaycCompliant(dbSession,
          qualityGateDto.getUuid()).toString(), qualityGateDto.isAiCodeSupported(), conditions));
    }

    data.setQualityGates(qualityGates);
  }

  private static Map<String, List<Condition>> mapQualityGateConditions(Collection<QualityGateConditionDto> qualityGateConditions, Map<String, MetricDto> metricsByUuid) {
    Map<String, List<Condition>> conditionsMap = new HashMap<>();

    for (QualityGateConditionDto condition : qualityGateConditions) {
      String qualityGateUuid = condition.getQualityGateUuid();

      MetricDto metricDto = metricsByUuid.get(condition.getMetricUuid());
      String metricKey = metricDto != null ? metricDto.getKey() : "Unknown Metric";

      Condition telemetryCondition = new Condition(
        metricKey,
        fromDbValue(condition.getOperator()),
        condition.getErrorThreshold());

      conditionsMap
        .computeIfAbsent(qualityGateUuid, k -> new ArrayList<>())
        .add(telemetryCondition);
    }

    return conditionsMap;
  }

  private Map<String, MetricDto> getMetricsByUuid(DbSession dbSession, Collection<QualityGateConditionDto> conditions) {
    Set<String> metricUuids = conditions.stream().map(QualityGateConditionDto::getMetricUuid).collect(Collectors.toSet());
    return dbClient.metricDao().selectByUuids(dbSession, metricUuids).stream().filter(MetricDto::isEnabled).collect(Collectors.toMap(MetricDto::getUuid, Function.identity()));
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

  private Map<String, Map<String, Number>> getProjectMetricsByMetricKeys(DbSession dbSession, List<String> metricKeys) {
    Map<String, Map<String, Number>> measuresByProject = new HashMap<>();

    List<BranchDto> mainBranches = dbClient.branchDao().selectMainBranches(dbSession);
    Map<String, String> branchUuidToProjectUuid = mainBranches.stream().collect(Collectors.toMap(BranchDto::getUuid,
      BranchDto::getProjectUuid));
    List<MeasureDto> measureDtos = dbClient.measureDao().selectByComponentUuidsAndMetricKeys(dbSession, branchUuidToProjectUuid.keySet(),
      metricKeys);

    for (MeasureDto measureDto : measureDtos) {
      Map<String, Number> measures = measureDto.getMetricValues().entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> Double.parseDouble(e.getValue().toString())));
      measuresByProject.put(branchUuidToProjectUuid.get(measureDto.getComponentUuid()), measures);
    }

    return measuresByProject;
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

  private TelemetryData.ManagedInstanceInformation buildManagedInstanceInformation() {
    String provider = managedInstanceService.isInstanceExternallyManaged() ? managedInstanceService.getProviderName() : null;
    return new TelemetryData.ManagedInstanceInformation(managedInstanceService.isInstanceExternallyManaged(), provider);
  }

  private TelemetryData.CloudUsage buildCloudUsage() {
    return cloudUsageDataProvider.getCloudUsage();
  }

  private record ProjectLanguageKey(String projectKey, String language) {
  }
}
