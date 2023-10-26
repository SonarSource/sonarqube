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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTelemetryDto;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.platform.ContainerSupport;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;
import org.sonar.server.qualitygate.QualityGateCaycChecker;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.telemetry.TelemetryData.Branch;
import org.sonar.server.telemetry.TelemetryData.CloudUsage;
import org.sonar.server.telemetry.TelemetryData.NewCodeDefinition;
import org.sonar.server.telemetry.TelemetryData.ProjectStatistics;
import org.sonar.updatecenter.common.Version;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DEVELOPMENT_COST_KEY;
import static org.sonar.api.measures.CoreMetrics.LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDCI;
import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDSCM;
import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;
import static org.sonar.core.platform.EditionProvider.Edition.DEVELOPER;
import static org.sonar.core.platform.EditionProvider.Edition.ENTERPRISE;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_CPP_KEY;
import static org.sonar.server.metric.UnanalyzedLanguageMetrics.UNANALYZED_C_KEY;
import static org.sonar.server.qualitygate.QualityGateCaycStatus.NON_COMPLIANT;
import static org.sonar.server.telemetry.TelemetryDataLoaderImpl.EXTERNAL_SECURITY_REPORT_EXPORTED_AT;

@RunWith(DataProviderRunner.class)
public class TelemetryDataLoaderImplIT {
  private final static Long NOW = 100_000_000L;
  public static final String SERVER_ID = "AU-TpxcB-iU5OvuD2FL7";
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final FakeServer server = new FakeServer();
  private final PluginRepository pluginRepository = mock(PluginRepository.class);
  private final Configuration configuration = mock(Configuration.class);
  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final ContainerSupport containerSupport = mock(ContainerSupport.class);
  private final QualityGateCaycChecker qualityGateCaycChecker = mock(QualityGateCaycChecker.class);
  private final QualityGateFinder qualityGateFinder = new QualityGateFinder(db.getDbClient());

  private final QualityProfileDataProvider qualityProfileDataProvider = new QualityProfileDataProvider(db.getDbClient(), new QProfileComparison(db.getDbClient()));
  private final InternalProperties internalProperties = spy(new MapInternalProperties());
  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);
  private final CloudUsageDataProvider cloudUsageDataProvider = mock(CloudUsageDataProvider.class);

  private final TelemetryDataLoader communityUnderTest = new TelemetryDataLoaderImpl(server, db.getDbClient(), pluginRepository, editionProvider,
    internalProperties, configuration, containerSupport, qualityGateCaycChecker, qualityGateFinder, managedInstanceService, cloudUsageDataProvider, qualityProfileDataProvider);
  private final TelemetryDataLoader commercialUnderTest = new TelemetryDataLoaderImpl(server, db.getDbClient(), pluginRepository, editionProvider,
    internalProperties, configuration, containerSupport, qualityGateCaycChecker, qualityGateFinder, managedInstanceService, cloudUsageDataProvider, qualityProfileDataProvider);

  private QualityGateDto builtInDefaultQualityGate;
  private MetricDto bugsDto;
  private MetricDto vulnerabilitiesDto;
  private MetricDto securityHotspotsDto;
  private MetricDto technicalDebtDto;
  private MetricDto developmentCostDto;

  @Before
  public void setUpBuiltInQualityGate() {
    String builtInQgName = "Sonar way";
    builtInDefaultQualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName(builtInQgName).setBuiltIn(true));
    when(qualityGateCaycChecker.checkCaycCompliant(any(), any())).thenReturn(NON_COMPLIANT);
    db.qualityGates().setDefaultQualityGate(builtInDefaultQualityGate);

    bugsDto = db.measures().insertMetric(m -> m.setKey(BUGS_KEY));
    vulnerabilitiesDto = db.measures().insertMetric(m -> m.setKey(VULNERABILITIES_KEY));
    securityHotspotsDto = db.measures().insertMetric(m -> m.setKey(SECURITY_HOTSPOTS_KEY));
    technicalDebtDto = db.measures().insertMetric(m -> m.setKey(TECHNICAL_DEBT_KEY));
    developmentCostDto = db.measures().insertMetric(m -> m.setKey(DEVELOPMENT_COST_KEY));
  }

  @Test
  public void send_telemetry_data() {
    String version = "7.5.4";
    Long analysisDate = 1L;
    Long lastConnectionDate = 5L;

    server.setId(SERVER_ID);
    server.setVersion(version);
    List<PluginInfo> plugins = asList(newPlugin("java", "4.12.0.11033"), newPlugin("scmgit", "1.2"), new PluginInfo("other"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);
    when(editionProvider.get()).thenReturn(Optional.of(DEVELOPER));

    List<UserDto> activeUsers = composeActiveUsers(3);

    // update last connection
    activeUsers.forEach(u -> db.users().updateLastConnectionDate(u, 5L));

    UserDto inactiveUser = db.users().insertUser(u -> u.setActive(false).setExternalIdentityProvider("provider0"));

    MetricDto lines = db.measures().insertMetric(m -> m.setKey(LINES_KEY));
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey(NCLOC_KEY));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    MetricDto nclocDistrib = db.measures().insertMetric(m -> m.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY));

    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    var branch1 = db.components().insertProjectBranch(mainBranch1, branchDto -> branchDto.setKey("reference"));
    var branch2 = db.components().insertProjectBranch(mainBranch1, branchDto -> branchDto.setKey("custom"));
    db.measures().insertLiveMeasure(mainBranch1, lines, m -> m.setValue(110d));
    db.measures().insertLiveMeasure(mainBranch1, ncloc, m -> m.setValue(110d));
    db.measures().insertLiveMeasure(mainBranch1, coverage, m -> m.setValue(80d));
    db.measures().insertLiveMeasure(mainBranch1, nclocDistrib, m -> m.setValue(null).setData("java=70;js=30;kotlin=10"));
    db.measures().insertLiveMeasure(mainBranch1, bugsDto, m -> m.setValue(1d));
    db.measures().insertLiveMeasure(mainBranch1, vulnerabilitiesDto, m -> m.setValue(1d).setData((String) null));
    db.measures().insertLiveMeasure(mainBranch1, securityHotspotsDto, m -> m.setValue(1d).setData((String) null));
    db.measures().insertLiveMeasure(mainBranch1, developmentCostDto, m -> m.setData("50").setValue(null));
    db.measures().insertLiveMeasure(mainBranch1, technicalDebtDto, m -> m.setValue(5d).setData((String) null));
    // Measures on other branches
    db.measures().insertLiveMeasure(branch1, technicalDebtDto, m -> m.setValue(6d).setData((String) null));
    db.measures().insertLiveMeasure(branch2, technicalDebtDto, m -> m.setValue(7d).setData((String) null));

    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto mainBranch2 = projectData2.getMainBranchComponent();
    db.measures().insertLiveMeasure(mainBranch2, lines, m -> m.setValue(200d));
    db.measures().insertLiveMeasure(mainBranch2, ncloc, m -> m.setValue(200d));
    db.measures().insertLiveMeasure(mainBranch2, coverage, m -> m.setValue(80d));
    db.measures().insertLiveMeasure(mainBranch2, nclocDistrib, m -> m.setValue(null).setData("java=180;js=20"));

    SnapshotDto project1Analysis = db.components().insertSnapshot(mainBranch1, t -> t.setLast(true).setAnalysisDate(analysisDate));
    SnapshotDto project2Analysis = db.components().insertSnapshot(mainBranch2, t -> t.setLast(true).setAnalysisDate(analysisDate));
    db.measures().insertMeasure(mainBranch1, project1Analysis, nclocDistrib, m -> m.setData("java=70;js=30;kotlin=10"));
    db.measures().insertMeasure(mainBranch2, project2Analysis, nclocDistrib, m -> m.setData("java=180;js=20"));

    insertAnalysisProperty(project1Analysis, "prop-uuid-1", SONAR_ANALYSIS_DETECTEDCI, "ci-1");
    insertAnalysisProperty(project2Analysis, "prop-uuid-2", SONAR_ANALYSIS_DETECTEDCI, "ci-2");
    insertAnalysisProperty(project1Analysis, "prop-uuid-3", SONAR_ANALYSIS_DETECTEDSCM, "scm-1");
    insertAnalysisProperty(project2Analysis, "prop-uuid-4", SONAR_ANALYSIS_DETECTEDSCM, "scm-2");

    // alm
    db.almSettings().insertAzureAlmSetting();
    db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto almSettingDto = db.almSettings().insertAzureAlmSetting(a -> a.setUrl("https://dev.azure.com"));
    AlmSettingDto gitHubAlmSetting = db.almSettings().insertGitHubAlmSetting(a -> a.setUrl("https://api.github.com"));
    db.almSettings().insertAzureProjectAlmSetting(almSettingDto, projectData1.getProjectDto());
    db.almSettings().insertGitlabProjectAlmSetting(gitHubAlmSetting, projectData2.getProjectDto());

    // quality gates
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate(qg -> qg.setName("QG1").setBuiltIn(true));
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate(qg -> qg.setName("QG2"));

    QualityGateConditionDto condition1 = db.qualityGates().addCondition(qualityGate1, vulnerabilitiesDto, c -> c.setOperator("GT").setErrorThreshold("80"));
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(qualityGate2, securityHotspotsDto, c -> c.setOperator("LT").setErrorThreshold("2"));

    // quality profiles
    QProfileDto javaQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("java"));
    QProfileDto kotlinQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("kotlin"));
    QProfileDto jsQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("js"));
    db.qualityProfiles().associateWithProject(projectData1.getProjectDto(), javaQP, kotlinQP, jsQP);
    db.qualityProfiles().associateWithProject(projectData2.getProjectDto(), javaQP, jsQP);

    QProfileDto qualityProfile1 = db.qualityProfiles().insert(qp -> qp.setIsBuiltIn(true));
    QProfileDto qualityProfile2 = db.qualityProfiles().insert();
    db.qualityProfiles().setAsDefault(qualityProfile1, qualityProfile2);

    // link one project to a non-default QG
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDtoByMainBranch(mainBranch1), qualityGate1);

    var ncd1 = db.newCodePeriods().insert(projectData1.projectUuid(), NewCodePeriodType.NUMBER_OF_DAYS, "30");
    var ncd2 = db.newCodePeriods().insert(projectData1.projectUuid(), branch2.branchUuid(), NewCodePeriodType.REFERENCE_BRANCH, "reference");

    var instanceNcdId = NewCodeDefinition.getInstanceDefault().hashCode();
    var projectNcdId = new NewCodeDefinition(NewCodePeriodType.NUMBER_OF_DAYS.name(), "30", "project").hashCode();
    var branchNcdId = new NewCodeDefinition(NewCodePeriodType.REFERENCE_BRANCH.name(), branch1.uuid(), "branch").hashCode();

    TelemetryData data = communityUnderTest.load();
    assertThat(data.getServerId()).isEqualTo(SERVER_ID);
    assertThat(data.getVersion()).isEqualTo(version);
    assertThat(data.getEdition()).contains(DEVELOPER);
    assertThat(data.getDefaultQualityGate()).isEqualTo(builtInDefaultQualityGate.getUuid());
    assertThat(data.getSonarWayQualityGate()).isEqualTo(builtInDefaultQualityGate.getUuid());
    assertThat(data.getNcdId()).isEqualTo(NewCodeDefinition.getInstanceDefault().hashCode());
    assertThat(data.getMessageSequenceNumber()).isOne();
    assertDatabaseMetadata(data.getDatabase());
    assertThat(data.getPlugins()).containsOnly(
      entry("java", "4.12.0.11033"), entry("scmgit", "1.2"), entry("other", "undefined"));
    assertThat(data.isInContainer()).isFalse();

    assertThat(data.getUserTelemetries())
      .extracting(UserTelemetryDto::getUuid, UserTelemetryDto::getLastConnectionDate, UserTelemetryDto::getLastSonarlintConnectionDate, UserTelemetryDto::isActive)
      .containsExactlyInAnyOrder(
        tuple(activeUsers.get(0).getUuid(), lastConnectionDate, activeUsers.get(0).getLastSonarlintConnectionDate(), true),
        tuple(activeUsers.get(1).getUuid(), lastConnectionDate, activeUsers.get(1).getLastSonarlintConnectionDate(), true),
        tuple(activeUsers.get(2).getUuid(), lastConnectionDate, activeUsers.get(2).getLastSonarlintConnectionDate(), true),
        tuple(inactiveUser.getUuid(), null, inactiveUser.getLastSonarlintConnectionDate(), false));
    assertThat(data.getProjects())
      .extracting(TelemetryData.Project::projectUuid, TelemetryData.Project::language, TelemetryData.Project::loc, TelemetryData.Project::lastAnalysis)
      .containsExactlyInAnyOrder(
        tuple(projectData1.projectUuid(), "java", 70L, analysisDate),
        tuple(projectData1.projectUuid(), "js", 30L, analysisDate),
        tuple(projectData1.projectUuid(), "kotlin", 10L, analysisDate),
        tuple(projectData2.projectUuid(), "java", 180L, analysisDate),
        tuple(projectData2.projectUuid(), "js", 20L, analysisDate));
    assertThat(data.getProjectStatistics())
      .extracting(ProjectStatistics::getBranchCount, ProjectStatistics::getPullRequestCount, ProjectStatistics::getQualityGate,
        ProjectStatistics::getScm, ProjectStatistics::getCi, ProjectStatistics::getDevopsPlatform,
        ProjectStatistics::getBugs, ProjectStatistics::getVulnerabilities, ProjectStatistics::getSecurityHotspots,
        ProjectStatistics::getDevelopmentCost, ProjectStatistics::getTechnicalDebt, ProjectStatistics::getNcdId)
      .containsExactlyInAnyOrder(
        tuple(3L, 0L, qualityGate1.getUuid(), "scm-1", "ci-1", "azure_devops_cloud", Optional.of(1L), Optional.of(1L), Optional.of(1L), Optional.of(50L), Optional.of(5L),
          projectNcdId),
        tuple(1L, 0L, builtInDefaultQualityGate.getUuid(), "scm-2", "ci-2", "github_cloud", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
          Optional.empty(), instanceNcdId));

    assertThat(data.getBranches())
      .extracting(Branch::branchUuid, Branch::ncdId)
      .containsExactlyInAnyOrder(
        tuple(branch1.uuid(), projectNcdId),
        tuple(branch2.uuid(), branchNcdId),
        tuple(mainBranch1.uuid(), projectNcdId),
        tuple(mainBranch2.uuid(), instanceNcdId));

    assertThat(data.getNewCodeDefinitions())
      .extracting(NewCodeDefinition::scope, NewCodeDefinition::type, NewCodeDefinition::value)
      .containsExactlyInAnyOrder(
        tuple("instance", NewCodePeriodType.PREVIOUS_VERSION.name(), ""),
        tuple("project", NewCodePeriodType.NUMBER_OF_DAYS.name(), "30"),
        tuple("branch", NewCodePeriodType.REFERENCE_BRANCH.name(), branch1.uuid()));

    assertThat(data.getQualityGates())
      .extracting(TelemetryData.QualityGate::uuid, TelemetryData.QualityGate::caycStatus,
        qg -> qg.conditions().stream()
          .map(condition -> tuple(condition.getMetricKey(), condition.getOperator().getDbValue(), condition.getErrorThreshold(), condition.isOnLeakPeriod()))
          .collect(Collectors.toList()))
      .containsExactlyInAnyOrder(
        tuple(builtInDefaultQualityGate.getUuid(), "non-compliant", Collections.emptyList()),
        tuple(qualityGate1.getUuid(), "non-compliant", List.of(tuple(vulnerabilitiesDto.getKey(), condition1.getOperator(), condition1.getErrorThreshold(), false))),
        tuple(qualityGate2.getUuid(), "non-compliant", List.of(tuple(securityHotspotsDto.getKey(), condition2.getOperator(), condition2.getErrorThreshold(), false))));

    assertThat(data.getQualityProfiles())
      .extracting(TelemetryData.QualityProfile::uuid, TelemetryData.QualityProfile::isBuiltIn)
      .containsExactlyInAnyOrder(
        tuple(qualityProfile1.getKee(), qualityProfile1.isBuiltIn()),
        tuple(qualityProfile2.getKee(), qualityProfile2.isBuiltIn()),
        tuple(jsQP.getKee(), jsQP.isBuiltIn()),
        tuple(javaQP.getKee(), javaQP.isBuiltIn()),
        tuple(kotlinQP.getKee(), kotlinQP.isBuiltIn()));

  }

  @Test
  public void send_branch_measures_data() {
    Long analysisDate = ZonedDateTime.now(ZoneId.systemDefault()).toInstant().toEpochMilli();

    MetricDto qg = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));

    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();

    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto mainBranch2 = projectData2.getMainBranchComponent();

    SnapshotDto project1Analysis1 = db.components().insertSnapshot(mainBranch1, t -> t.setLast(true).setAnalysisDate(analysisDate));
    SnapshotDto project1Analysis2 = db.components().insertSnapshot(mainBranch1, t -> t.setLast(true).setAnalysisDate(analysisDate));
    SnapshotDto project2Analysis = db.components().insertSnapshot(mainBranch2, t -> t.setLast(true).setAnalysisDate(analysisDate));
    db.measures().insertMeasure(mainBranch1, project1Analysis1, qg, pm -> pm.setData("OK"));
    db.measures().insertMeasure(mainBranch1, project1Analysis2, qg, pm -> pm.setData("ERROR"));
    db.measures().insertMeasure(mainBranch2, project2Analysis, qg, pm -> pm.setData("ERROR"));

    var branch1 = db.components().insertProjectBranch(mainBranch1, branchDto -> branchDto.setKey("reference"));
    var branch2 = db.components().insertProjectBranch(mainBranch1, branchDto -> branchDto.setKey("custom"));

    db.newCodePeriods().insert(projectData1.projectUuid(), NewCodePeriodType.NUMBER_OF_DAYS, "30");
    db.newCodePeriods().insert(projectData1.projectUuid(), branch2.branchUuid(), NewCodePeriodType.REFERENCE_BRANCH, "reference");

    var instanceNcdId = NewCodeDefinition.getInstanceDefault().hashCode();
    var projectNcdId = new NewCodeDefinition(NewCodePeriodType.NUMBER_OF_DAYS.name(), "30", "project").hashCode();
    var branchNcdId = new NewCodeDefinition(NewCodePeriodType.REFERENCE_BRANCH.name(), branch1.uuid(), "branch").hashCode();

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getBranches())
      .extracting(Branch::branchUuid, Branch::ncdId, Branch::greenQualityGateCount, Branch::analysisCount)
      .containsExactlyInAnyOrder(
        tuple(branch1.uuid(), projectNcdId, 0, 0),
        tuple(branch2.uuid(), branchNcdId, 0, 0),
        tuple(mainBranch1.uuid(), projectNcdId, 1, 2),
        tuple(mainBranch2.uuid(), instanceNcdId, 0, 1));

  }

  private List<UserDto> composeActiveUsers(int count) {
    UserDbTester userDbTester = db.users();
    Function<Integer, Consumer<UserDto>> userConfigurator = index -> user -> user.setExternalIdentityProvider("provider" + index).setLastSonarlintConnectionDate(index * 2L);

    return IntStream
      .rangeClosed(1, count)
      .mapToObj(userConfigurator::apply)
      .map(userDbTester::insertUser)
      .toList();
  }

  private void assertDatabaseMetadata(TelemetryData.Database database) {
    try (DbSession dbSession = db.getDbClient().openSession(false)) {
      DatabaseMetaData metadata = dbSession.getConnection().getMetaData();
      assertThat(database.name()).isEqualTo("H2");
      assertThat(database.version()).isEqualTo(metadata.getDatabaseProductVersion());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void take_largest_branch_snapshot_project_data() {
    server.setId(SERVER_ID).setVersion("7.5.4");

    MetricDto lines = db.measures().insertMetric(m -> m.setKey(LINES_KEY));
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey(NCLOC_KEY));
    MetricDto coverage = db.measures().insertMetric(m -> m.setKey(COVERAGE_KEY));
    MetricDto nclocDistrib = db.measures().insertMetric(m -> m.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY));

    ProjectData projectData = db.components().insertPublicProject();

    QProfileDto javaQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("java"));
    QProfileDto kotlinQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("kotlin"));
    QProfileDto jsQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("js"));
    db.qualityProfiles().associateWithProject(projectData.getProjectDto(), javaQP, kotlinQP, jsQP);

    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.measures().insertLiveMeasure(mainBranch, lines, m -> m.setValue(110d));
    db.measures().insertLiveMeasure(mainBranch, ncloc, m -> m.setValue(110d));
    db.measures().insertLiveMeasure(mainBranch, coverage, m -> m.setValue(80d));
    db.measures().insertLiveMeasure(mainBranch, nclocDistrib, m -> m.setValue(null).setData("java=70;js=30;kotlin=10"));

    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BRANCH));
    db.measures().insertLiveMeasure(branch, lines, m -> m.setValue(180d));
    db.measures().insertLiveMeasure(branch, ncloc, m -> m.setValue(180d));
    db.measures().insertLiveMeasure(branch, coverage, m -> m.setValue(80d));
    db.measures().insertLiveMeasure(branch, nclocDistrib, m -> m.setValue(null).setData("java=100;js=50;kotlin=30"));

    SnapshotDto project1Analysis = db.components().insertSnapshot(mainBranch, t -> t.setLast(true));
    SnapshotDto project2Analysis = db.components().insertSnapshot(branch, t -> t.setLast(true));
    db.measures().insertMeasure(mainBranch, project1Analysis, nclocDistrib, m -> m.setData("java=70;js=30;kotlin=10"));
    db.measures().insertMeasure(branch, project2Analysis, nclocDistrib, m -> m.setData("java=100;js=50;kotlin=30"));

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getProjects()).extracting(TelemetryData.Project::projectUuid, TelemetryData.Project::language, TelemetryData.Project::loc)
      .containsExactlyInAnyOrder(
        tuple(projectData.projectUuid(), "java", 100L),
        tuple(projectData.projectUuid(), "js", 50L),
        tuple(projectData.projectUuid(), "kotlin", 30L));
    assertThat(data.getProjectStatistics())
      .extracting(ProjectStatistics::getBranchCount, ProjectStatistics::getPullRequestCount,
        ProjectStatistics::getScm, ProjectStatistics::getCi)
      .containsExactlyInAnyOrder(
        tuple(2L, 0L, "undetected", "undetected"));
  }

  @Test
  public void load_shouldProvideQualityProfileInProjectSection() {
    server.setId(SERVER_ID).setVersion("7.5.4");
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey(NCLOC_KEY));
    MetricDto nclocDistrib = db.measures().insertMetric(m -> m.setKey(NCLOC_LANGUAGE_DISTRIBUTION_KEY));

    ProjectData projectData = db.components().insertPublicProject();

    // default quality profile
    QProfileDto javaQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("java"));
    QProfileDto kotlinQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("kotlin"));
    db.qualityProfiles().setAsDefault(javaQP, kotlinQP);
    // selected quality profile
    QProfileDto jsQP = db.qualityProfiles().insert(qProfileDto -> qProfileDto.setLanguage("js"));
    db.qualityProfiles().associateWithProject(projectData.getProjectDto(), jsQP);

    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.measures().insertLiveMeasure(mainBranch, ncloc, m -> m.setValue(110d));
    db.measures().insertLiveMeasure(mainBranch, nclocDistrib, m -> m.setValue(null).setData("java=70;js=30;kotlin=10"));

    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setBranchType(BRANCH));
    db.measures().insertLiveMeasure(branch, ncloc, m -> m.setValue(180d));
    db.measures().insertLiveMeasure(branch, nclocDistrib, m -> m.setValue(null).setData("java=100;js=50;kotlin=30"));

    SnapshotDto project1Analysis = db.components().insertSnapshot(mainBranch, t -> t.setLast(true));
    SnapshotDto project2Analysis = db.components().insertSnapshot(branch, t -> t.setLast(true));
    db.measures().insertMeasure(mainBranch, project1Analysis, nclocDistrib, m -> m.setData("java=70;js=30;kotlin=10"));
    db.measures().insertMeasure(branch, project2Analysis, nclocDistrib, m -> m.setData("java=100;js=50;kotlin=30"));

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getProjects()).extracting(TelemetryData.Project::projectUuid, TelemetryData.Project::language, TelemetryData.Project::qualityProfile)
      .containsExactlyInAnyOrder(
        tuple(projectData.projectUuid(), "java", javaQP.getKee()),
        tuple(projectData.projectUuid(), "js", jsQP.getKee()),
        tuple(projectData.projectUuid(), "kotlin", kotlinQP.getKee()));
  }

  @Test
  public void load_shouldProvideCreationMethodInProjectStatisticsSection() {
    server.setId(SERVER_ID).setVersion("7.5.4");

    ProjectData projectData1 = db.components().insertPrivateProjectWithCreationMethod(CreationMethod.LOCAL_API);
    ProjectData projectData2 = db.components().insertPrivateProjectWithCreationMethod(CreationMethod.LOCAL_BROWSER);
    ProjectData projectData3 = db.components().insertPrivateProjectWithCreationMethod(CreationMethod.UNKNOWN);
    ProjectData projectData4 = db.components().insertPrivateProjectWithCreationMethod(CreationMethod.SCANNER_API);
    ProjectData projectData5 = db.components().insertPrivateProjectWithCreationMethod(CreationMethod.ALM_IMPORT_BROWSER);
    ProjectData projectData6 = db.components().insertPrivateProjectWithCreationMethod(CreationMethod.ALM_IMPORT_API);

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getProjectStatistics()).extracting(TelemetryData.ProjectStatistics::getProjectUuid, TelemetryData.ProjectStatistics::getCreationMethod)
      .containsExactlyInAnyOrder(
        tuple(projectData1.projectUuid(), CreationMethod.LOCAL_API),
        tuple(projectData2.projectUuid(), CreationMethod.LOCAL_BROWSER),
        tuple(projectData3.projectUuid(), CreationMethod.UNKNOWN),
        tuple(projectData4.projectUuid(), CreationMethod.SCANNER_API),
        tuple(projectData5.projectUuid(), CreationMethod.ALM_IMPORT_BROWSER),
        tuple(projectData6.projectUuid(), CreationMethod.ALM_IMPORT_API));
  }

  @Test
  public void test_ncd_on_community_edition() {
    server.setId(SERVER_ID).setVersion("7.5.4");
    when(editionProvider.get()).thenReturn(Optional.of(COMMUNITY));

    ProjectData project = db.components().insertPublicProject();

    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setBranchType(BRANCH));

    db.newCodePeriods().insert(project.projectUuid(), branch.branchUuid(), NewCodePeriodType.NUMBER_OF_DAYS, "30");

    var projectNcdId = new NewCodeDefinition(NewCodePeriodType.NUMBER_OF_DAYS.name(), "30", "project").hashCode();

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getProjectStatistics())
      .extracting(ProjectStatistics::getBranchCount, ProjectStatistics::getNcdId)
      .containsExactlyInAnyOrder(tuple(2L, projectNcdId));

    assertThat(data.getBranches())
      .extracting(Branch::branchUuid, Branch::ncdId)
      .contains(tuple(branch.uuid(), projectNcdId));
  }

  @Test
  public void data_contains_weekly_count_sonarlint_users() {
    db.users().insertUser(c -> c.setLastSonarlintConnectionDate(NOW - 100_000L));
    db.users().insertUser(c -> c.setLastSonarlintConnectionDate(NOW));
    // these don't count
    db.users().insertUser(c -> c.setLastSonarlintConnectionDate(NOW - 1_000_000_000L));
    db.users().insertUser();

    TelemetryData data = communityUnderTest.load();
    assertThat(data.getUserTelemetries())
      .hasSize(4);
  }

  @Test
  public void send_server_id_and_version() {
    String id = randomAlphanumeric(40);
    String version = randomAlphanumeric(10);
    server.setId(id);
    server.setVersion(version);

    TelemetryData data = communityUnderTest.load();
    assertThat(data.getServerId()).isEqualTo(id);
    assertThat(data.getVersion()).isEqualTo(version);

    data = commercialUnderTest.load();
    assertThat(data.getServerId()).isEqualTo(id);
    assertThat(data.getVersion()).isEqualTo(version);
  }

  @Test
  public void send_server_installation_date_and_installation_version() {
    String installationVersion = "7.9.BEST.LTS.EVER";
    Long installationDate = 1546300800000L; // 2019/01/01
    internalProperties.write(InternalProperties.INSTALLATION_DATE, String.valueOf(installationDate));
    internalProperties.write(InternalProperties.INSTALLATION_VERSION, installationVersion);

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getInstallationDate()).isEqualTo(installationDate);
    assertThat(data.getInstallationVersion()).isEqualTo(installationVersion);
  }

  @Test
  public void send_correct_sequence_number() {
    internalProperties.write(TelemetryDaemon.I_PROP_MESSAGE_SEQUENCE, "10");
    TelemetryData data = communityUnderTest.load();
    assertThat(data.getMessageSequenceNumber()).isEqualTo(11L);
  }

  @Test
  public void do_not_send_server_installation_details_if_missing_property() {
    TelemetryData data = communityUnderTest.load();
    assertThat(data.getInstallationDate()).isNull();
    assertThat(data.getInstallationVersion()).isNull();

    data = commercialUnderTest.load();
    assertThat(data.getInstallationDate()).isNull();
    assertThat(data.getInstallationVersion()).isNull();
  }

  @Test
  public void send_unanalyzed_languages_flags_when_edition_is_community() {
    when(editionProvider.get()).thenReturn(Optional.of(COMMUNITY));
    MetricDto unanalyzedC = db.measures().insertMetric(m -> m.setKey(UNANALYZED_C_KEY));
    MetricDto unanalyzedCpp = db.measures().insertMetric(m -> m.setKey(UNANALYZED_CPP_KEY));
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    db.measures().insertLiveMeasure(project1, unanalyzedC);
    db.measures().insertLiveMeasure(project2, unanalyzedC);
    db.measures().insertLiveMeasure(project2, unanalyzedCpp);

    TelemetryData data = communityUnderTest.load();

    assertThat(data.hasUnanalyzedC().get()).isTrue();
    assertThat(data.hasUnanalyzedCpp().get()).isTrue();
  }

  @Test
  public void do_not_send_unanalyzed_languages_flags_when_edition_is_not_community() {
    when(editionProvider.get()).thenReturn(Optional.of(DEVELOPER));
    MetricDto unanalyzedC = db.measures().insertMetric(m -> m.setKey(UNANALYZED_C_KEY));
    MetricDto unanalyzedCpp = db.measures().insertMetric(m -> m.setKey(UNANALYZED_CPP_KEY));
    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    db.measures().insertLiveMeasure(project1, unanalyzedC);
    db.measures().insertLiveMeasure(project2, unanalyzedCpp);

    TelemetryData data = communityUnderTest.load();

    assertThat(data.hasUnanalyzedC()).isEmpty();
    assertThat(data.hasUnanalyzedCpp()).isEmpty();
  }

  @Test
  public void unanalyzed_languages_flags_are_set_to_false_when_no_unanalyzed_languages_and_edition_is_community() {
    when(editionProvider.get()).thenReturn(Optional.of(COMMUNITY));

    TelemetryData data = communityUnderTest.load();

    assertThat(data.hasUnanalyzedC().get()).isFalse();
    assertThat(data.hasUnanalyzedCpp().get()).isFalse();
  }

  @Test
  public void populate_security_custom_config_for_languages_on_enterprise() {
    when(editionProvider.get()).thenReturn(Optional.of(ENTERPRISE));

    when(configuration.get("sonar.security.config.javasecurity")).thenReturn(Optional.of("{}"));
    when(configuration.get("sonar.security.config.phpsecurity")).thenReturn(Optional.of("{}"));
    when(configuration.get("sonar.security.config.pythonsecurity")).thenReturn(Optional.of("{}"));
    when(configuration.get("sonar.security.config.roslyn.sonaranalyzer.security.cs")).thenReturn(Optional.of("{}"));

    TelemetryData data = commercialUnderTest.load();

    assertThat(data.getCustomSecurityConfigs())
      .containsExactlyInAnyOrder("java", "php", "python", "csharp");
  }

  @Test
  public void skip_security_custom_config_on_community() {
    when(editionProvider.get()).thenReturn(Optional.of(COMMUNITY));

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getCustomSecurityConfigs()).isEmpty();
  }

  @Test
  public void undetected_alm_ci_slm_data() {
    server.setId(SERVER_ID).setVersion("7.5.4");
    db.components().insertPublicProject().getMainBranchComponent();
    TelemetryData data = communityUnderTest.load();
    assertThat(data.getProjectStatistics())
      .extracting(ProjectStatistics::getDevopsPlatform, ProjectStatistics::getScm, ProjectStatistics::getCi)
      .containsExactlyInAnyOrder(tuple("undetected", "undetected", "undetected"));
  }

  @Test
  public void givenExistingExternalSecurityReport_whenTelemetryIsGenerated_payloadShouldContainLastUsageDate() {
    server.setId(SERVER_ID).setVersion("7.5.4");
    ProjectData projectData = db.components().insertPublicProject();
    db.getDbClient().propertiesDao().saveProperty(new PropertyDto().setKey(EXTERNAL_SECURITY_REPORT_EXPORTED_AT).setEntityUuid(projectData.projectUuid()).setValue("1"));

    TelemetryData data = communityUnderTest.load();

    assertThat(data.getProjectStatistics()).isNotEmpty();
    assertThat(data.getProjectStatistics().get(0).getExternalSecurityReportExportedAt()).isPresent()
      .get().isEqualTo(1L);
  }

  @Test
  @UseDataProvider("getManagedInstanceData")
  public void managedInstanceData_containsCorrectInformation(boolean isManaged, String provider) {
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(isManaged);
    when(managedInstanceService.getProviderName()).thenReturn(provider);

    TelemetryData data = commercialUnderTest.load();

    TelemetryData.ManagedInstanceInformation managedInstance = data.getManagedInstanceInformation();
    assertThat(managedInstance.isManaged()).isEqualTo(isManaged);
    assertThat(managedInstance.provider()).isEqualTo(provider);
  }

  @Test
  public void load_shouldContainCloudUsage() {
    CloudUsage cloudUsage = new CloudUsage(true, "1.27", "linux/amd64", "5.4.181-99.354.amzn2.x86_64", "10.1.0", "docker", false);
    when(cloudUsageDataProvider.getCloudUsage()).thenReturn(cloudUsage);

    TelemetryData data = commercialUnderTest.load();
    assertThat(data.getCloudUsage()).isEqualTo(cloudUsage);
  }

  @Test
  public void default_quality_gate_sent_with_project() {
    db.components().insertPublicProject().getMainBranchComponent();
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate(qg -> qg.setName("anything").setBuiltIn(true));
    db.qualityGates().setDefaultQualityGate(qualityGate);
    TelemetryData data = communityUnderTest.load();
    assertThat(data.getProjectStatistics())
      .extracting(ProjectStatistics::getQualityGate)
      .containsOnly(qualityGate.getUuid());
  }

  private PluginInfo newPlugin(String key, String version) {
    return new PluginInfo(key)
      .setVersion(Version.create(version));
  }

  private void insertAnalysisProperty(SnapshotDto snapshotDto, String uuid, String key, String value) {
    db.getDbClient().analysisPropertiesDao().insert(db.getSession(), new AnalysisPropertyDto()
      .setUuid(uuid)
      .setAnalysisUuid(snapshotDto.getUuid())
      .setKey(key)
      .setValue(value)
      .setCreatedAt(1L));
  }



  @DataProvider
  public static Set<String> getScimFeatureStatues() {
    HashSet<String> result = new HashSet<>();
    result.add("true");
    result.add("false");
    result.add(null);
    return result;
  }

  @DataProvider
  public static Object[][] getManagedInstanceData() {
    return new Object[][] {
      {true, "scim"},
      {true, "github"},
      {true, "gitlab"},
      {false, null},
    };
  }
}
