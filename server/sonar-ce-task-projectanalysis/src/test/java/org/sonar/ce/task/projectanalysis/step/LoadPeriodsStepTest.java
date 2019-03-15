/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventTesting;
import org.sonar.db.organization.OrganizationDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DAYS;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_MANUAL_BASELINE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_VERSION;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_VERSION;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.event.EventDto.CATEGORY_VERSION;
import static org.sonar.db.event.EventTesting.newEvent;

@RunWith(DataProviderRunner.class)
public class LoadPeriodsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private PeriodHolderImpl periodsHolder = new PeriodHolderImpl();
  private MapSettings settings = new MapSettings();
  private ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
  private System2 system2Mock = mock(System2.class);

  private LoadPeriodsStep underTest = new LoadPeriodsStep(analysisMetadataHolder, treeRootHolder, periodsHolder, system2Mock,
    dbTester.getDbClient(), configurationRepository);

  private Date november30th2008;

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setUp() throws Exception {
    november30th2008 = DATE_FORMAT.parse("2008-11-30");
    when(analysisMetadataHolder.isLongLivingBranch()).thenReturn(true);
  }

  @Test
  public void no_period_on_first_analysis_and_execute_has_no_effect() {
    TreeRootHolder treeRootHolderMock = mock(TreeRootHolder.class);
    PeriodHolderImpl periodHolderMock = mock(PeriodHolderImpl.class);
    DbClient dbClientMock = mock(DbClient.class);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);

    LoadPeriodsStep underTest = new LoadPeriodsStep(analysisMetadataHolder, treeRootHolderMock, periodHolderMock, system2Mock, dbClientMock, configurationRepository);

    underTest.execute(new TestComputationStepContext());

    verify(analysisMetadataHolder).isFirstAnalysis();
    verify(periodHolderMock).setPeriod(null);
    verifyNoMoreInteractions(analysisMetadataHolder, periodHolderMock);
    verifyZeroInteractions(treeRootHolderMock, system2Mock, dbClientMock, configurationRepository);
  }

  @Test
  public void feed_one_period() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    String textDate = "2008-11-22";

    settings.setProperty("sonar.leak.period", textDate);
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_DATE, textDate, analysis.getCreatedAt(), analysis.getUuid());
  }

  @Test
  public void ignore_unprocessed_snapshots() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components()
      .insertSnapshot(project, snapshot -> snapshot.setStatus(STATUS_UNPROCESSED).setCreatedAt(1226379600000L).setLast(false));// 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project,
      snapshot -> snapshot.setStatus(STATUS_PROCESSED).setProjectVersion("not provided").setCreatedAt(1226379600000L).setLast(false));// 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("not provided").setCategory(CATEGORY_VERSION).setDate(analysis1.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis2).setName("not provided").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "100");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_DAYS, "100", analysis2.getCreatedAt(), analysis2.getUuid());
    verifyDebugLogs("Resolving new code period by 100 days: 2008-08-22");
  }

  @Test
  public void feed_period_by_date() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    String textDate = "2008-11-22";
    settings.setProperty("sonar.leak.period", textDate);
    underTest.execute(new TestComputationStepContext());

    // Return analysis from given date 2008-11-22
    assertPeriod(LEAK_PERIOD_MODE_DATE, textDate, analysis4.getCreatedAt(), analysis4.getUuid());

    verifyDebugLogs("Resolving new code period by date: 2008-11-22");
  }

  @Test
  @UseDataProvider("branchTypesNotAllowedToHaveManualBaseline")
  public void feed_period_by_date_and_ignore_baseline_when_not_eligible_for_manual(BranchType branchType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    ComponentDto branch = dbTester.components().insertProjectBranch(project, t -> t.setBranchType(branchType));
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    dbTester.getDbClient().branchDao().updateManualBaseline(dbTester.getSession(), branch.uuid(), analysis1.getUuid());
    dbTester.commit();
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(branch));
    setupRoot(branch);

    String textDate = "2008-11-22";
    settings.setProperty("sonar.leak.period", textDate);
    underTest.execute(new TestComputationStepContext());

    // Return analysis from given date 2008-11-22
    assertPeriod(LEAK_PERIOD_MODE_DATE, textDate, analysis4.getCreatedAt(), analysis4.getUuid());

    verifyDebugLogs("Resolving new code period by date: 2008-11-22");
  }

  @DataProvider
  public static Object[][] branchTypesNotAllowedToHaveManualBaseline() {
    return new Object[][] {
      {BranchType.SHORT},
      {BranchType.PULL_REQUEST}
    };
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void do_not_fail_when_project_has_no_BranchDto(String leakPeriodSettingValue) {
    OrganizationDto organization = dbTester.organizations().insert();
    // creates row in projects but not in project_branches
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    dbTester.components().insertSnapshot(project);
    SnapshotDto aVersionAnalysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false));// 2008-11-22
    dbTester.events().insertEvent(EventTesting.newEvent(aVersionAnalysis).setName("a_version").setCategory(CATEGORY_VERSION));
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    Branch branch = mockBranch(BranchType.LONG);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    setupRoot(project);

    settings.setProperty("sonar.leak.period", leakPeriodSettingValue);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void fail_with_ISE_when_manual_baseline_is_set_but_does_not_exist_in_DB(String leakPeriodSettingValue) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    dbTester.components().setManualBaseline(project, new SnapshotDto().setUuid("nonexistent"));
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis 'nonexistent' of project '" + project.uuid() + "' defined as manual baseline does not exist");

    settings.setProperty("sonar.leak.period", leakPeriodSettingValue);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void fail_with_ISE_when_manual_baseline_is_set_but_does_not_belong_to_current_project(String leakPeriodSettingValue) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    ComponentDto otherProject = dbTester.components().insertMainBranch(organization);
    SnapshotDto otherProjectAnalysis = dbTester.components().insertSnapshot(otherProject);
    dbTester.components().setManualBaseline(project, otherProjectAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis '" + otherProjectAnalysis.getUuid() + "' of project '" + project.uuid()
      + "' defined as manual baseline does not exist");

    settings.setProperty("sonar.leak.period", leakPeriodSettingValue);
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void feed_period_by_manual_baseline_ignores_leak_period_setting(String leakPeriodSettingValue) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto manualBaselineAnalysis = dbTester.components().insertSnapshot(project);
    SnapshotDto aVersionAnalysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false));// 2008-11-22
    dbTester.events().insertEvent(EventTesting.newEvent(aVersionAnalysis).setName("a_version").setCategory(CATEGORY_VERSION));
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    dbTester.components().setManualBaseline(project, manualBaselineAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    settings.setProperty("sonar.leak.period", leakPeriodSettingValue);
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_MANUAL_BASELINE, null, manualBaselineAnalysis.getCreatedAt(), manualBaselineAnalysis.getUuid());

    verifyDebugLogs("Resolving new code period by manual baseline");
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void feed_period_by_manual_baseline_on_long_living_branch(String leakPeriodSettingValue) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    SnapshotDto manualBaselineAnalysis = dbTester.components().insertSnapshot(branch);
    SnapshotDto aVersionAnalysis = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false));// 2008-11-22
    dbTester.events().insertEvent(EventTesting.newEvent(aVersionAnalysis).setName("a_version").setCategory(CATEGORY_VERSION));
    dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    dbTester.components().setManualBaseline(branch, manualBaselineAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(branch));
    setupRoot(branch);

    settings.setProperty("sonar.leak.period", leakPeriodSettingValue);
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_MANUAL_BASELINE, null, manualBaselineAnalysis.getCreatedAt(), manualBaselineAnalysis.getUuid());

    verifyDebugLogs("Resolving new code period by manual baseline");
  }

  @DataProvider
  public static Object[][] anyValidLeakPeriodSettingValue() {
    return new Object[][] {
      // days
      {"100"},
      // date
      {"2008-11-22"},
      // previous_version keyword
      {"previous_version"},
      // an existing version event value
      {"a_version"},
    };
  }

  @Test
  public void feed_period_parameter_as_null_when_manual_baseline_has_no_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto manualBaselineAnalysis = dbTester.components().insertSnapshot(project);
    dbTester.components().setManualBaseline(project, manualBaselineAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "ignored");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_MANUAL_BASELINE, null, manualBaselineAnalysis.getCreatedAt(), manualBaselineAnalysis.getUuid());
  }

  @Test
  public void feed_period_parameter_as_null_when_manual_baseline_has_same_project_version() {
    String version = randomAlphabetic(12);
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto manualBaselineAnalysis = dbTester.components().insertSnapshot(project, t -> t.setProjectVersion(version).setProjectVersion(version));
    dbTester.components().setManualBaseline(project, manualBaselineAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "ignored");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_MANUAL_BASELINE, null, manualBaselineAnalysis.getCreatedAt(), manualBaselineAnalysis.getUuid());
  }

  @Test
  public void feed_no_period_parameter_as_projectVersion_when_manual_baseline_has_project_version() {
    String projectVersion = randomAlphabetic(15);
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto manualBaselineAnalysis = dbTester.components().insertSnapshot(project, t -> t.setProjectVersion(projectVersion));
    dbTester.components().setManualBaseline(project, manualBaselineAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "ignored");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_MANUAL_BASELINE, null, manualBaselineAnalysis.getCreatedAt(), manualBaselineAnalysis.getUuid());
  }

  @Test
  @UseDataProvider("projectVersionNullOrNot")
  public void feed_no_period_parameter_as_version_event_version_when_manual_baseline_has_one(@Nullable String projectVersion) {
    String eventVersion = randomAlphabetic(15);
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto manualBaselineAnalysis = dbTester.components().insertSnapshot(project, t -> t.setProjectVersion(projectVersion));
    dbTester.events().insertEvent(EventTesting.newEvent(manualBaselineAnalysis).setCategory(CATEGORY_VERSION).setName(eventVersion));
    dbTester.components().setManualBaseline(project, manualBaselineAnalysis);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(project));
    setupRoot(project);

    settings.setProperty("sonar.leak.period", "ignored");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_MANUAL_BASELINE, null, manualBaselineAnalysis.getCreatedAt(), manualBaselineAnalysis.getUuid());
  }

  @DataProvider
  public static Object[][] projectVersionNullOrNot() {
    return new Object[][] {
      {null},
      {randomAlphabetic(15)},
    };
  }

  private Branch branchOf(ComponentDto project) {
    BranchDto branchDto = dbTester.getDbClient().branchDao().selectByUuid(dbTester.getSession(), project.uuid()).get();
    return new Branch() {
      @Override
      public BranchType getType() {
        return branchDto.getBranchType();
      }

      @Override
      public boolean isMain() {
        throw new UnsupportedOperationException("isMain not implemented");
      }

      @Override
      public boolean isLegacyFeature() {
        throw new UnsupportedOperationException("isLegacyFeature not implemented");
      }

      @Override
      public String getName() {
        throw new UnsupportedOperationException("getName not implemented");
      }

      @Override
      public Optional<String> getMergeBranchUuid() {
        throw new UnsupportedOperationException("getMergeBranchUuid not implemented");
      }

      @Override
      public boolean supportsCrossProjectCpd() {
        throw new UnsupportedOperationException("supportsCrossProjectCpd not implemented");
      }

      @Override
      public String getPullRequestKey() {
        throw new UnsupportedOperationException("getPullRequestKey not implemented");
      }

      @Override
      public String generateKey(String projectKey, @Nullable String fileOrDirPath) {
        throw new UnsupportedOperationException("generateKey not implemented");
      }
    };
  }

  private Branch mockBranch(BranchType branchType) {
    Branch mock = mock(Branch.class);
    when(mock.getType()).thenReturn(branchType);
    return mock;
  }

  @Test
  public void search_by_date_return_nearest_later_analysis() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    String date = "2008-11-13";
    settings.setProperty("sonar.leak.period", date);
    underTest.execute(new TestComputationStepContext());

    // Analysis form 2008-11-20
    assertPeriod(LEAK_PERIOD_MODE_DATE, date, analysis3.getCreatedAt(), analysis3.getUuid());
    verifyDebugLogs("Resolving new code period by date: 2008-11-13");
  }

  @Test
  public void fail_with_MessageException_if_period_is_date_after_today() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    String propertyValue = "2008-12-01";
    settings.setProperty("sonar.leak.period", propertyValue);

    verifyFailWithInvalidValueMessageException(propertyValue,
      "Invalid code period '2008-12-01': date is in the future (now: '2008-11-30')");
  }

  @Test
  public void fail_with_MessageException_if_date_does_not_exist() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    String propertyValue = "2008-11-31";
    settings.setProperty("sonar.leak.period", propertyValue);

    verifyFailWithInvalidValueMessageException(propertyValue,
      "Invalid code period '2008-11-31': Invalid date");
  }

  @Test
  public void fail_with_MessageException_if_period_is_today_but_no_analysis_today() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L)); // 2008-11-29
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    String propertyValue = "2008-11-30";
    settings.setProperty("sonar.leak.period", propertyValue);

    verifyFailWithInvalidValueMessageException(propertyValue,
      "Resolving new code period by date: 2008-11-30",
      "Invalid code period '2008-11-30': No analysis found created after date '2008-11-30'");
  }

  @Test
  @UseDataProvider("zeroOrLess")
  public void fail_with_MessageException_if_period_is_0_or_less(int zeroOrLess) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    String propertyValue = String.valueOf(zeroOrLess);
    settings.setProperty("sonar.leak.period", propertyValue);

    verifyFailWithInvalidValueMessageException(propertyValue,
      "Invalid code period '" + zeroOrLess + "': number of days is <= 0");
  }

  @DataProvider
  public static Object[][] zeroOrLess() {
    return new Object[][] {
      {0},
      {-1 - new Random().nextInt(30)}
    };
  }

  @Test
  public void fail_with_ISE_if_not_firstAnalysis_but_no_snapshot_in_DB() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    settings.setProperty("sonar.leak.period", "previous_version");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Attempting to resolve period while no analysis exist");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  @UseDataProvider("stringConsideredAsVersions")
  public void fail_with_MessageException_if_string_is_not_an_existing_version_event(String propertyValue) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setProjectVersion("1.1").setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setProjectVersion("1.1").setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setProjectVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION).setDate(analysis1.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.0").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION).setDate(analysis4.getCreatedAt()));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);
    settings.setProperty("sonar.leak.period", propertyValue);

    try {
      underTest.execute(new TestComputationStepContext());
      fail("a Message Exception should have been thrown");
    } catch (MessageException e) {
      verifyInvalidValueMessage(e, propertyValue);
      assertThat(logTester.getLogs()).hasSize(2);
      assertThat(logTester.getLogs(LoggerLevel.DEBUG))
        .hasSize(2)
        .extracting(LogAndArguments::getFormattedMsg)
        .contains("Invalid code period '" + propertyValue + "': version is none of the existing ones: [1.1, 1.0, 0.9]");
    }
  }

  @DataProvider
  public static Object[][] stringConsideredAsVersions() {
    return new Object[][] {
      {randomAlphabetic(5)},
      {"1,3"},
      {"1.3"},
      {"0 1"},
      {"1-SNAPSHOT"},
      {"01-12-2018"}, // unsupported date format
    };
  }

  @Test
  public void fail_with_MessageException_if_property_does_not_exist() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);

    verifyFailWithInvalidValueMessageException("", "Invalid code period '': property is undefined or value is empty");
  }

  @Test
  public void fail_with_MessageException_if_property_is_empty() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    String propertyValue = "";
    settings.setProperty("sonar.leak.period", propertyValue);

    verifyFailWithInvalidValueMessageException(propertyValue, "Invalid code period '': property is undefined or value is empty");
  }

  private void verifyFailWithInvalidValueMessageException(String propertyValue, String debugLog, String... otherDebugLogs) {
    try {
      underTest.execute(new TestComputationStepContext());
      fail("a Message Exception should have been thrown");
    } catch (MessageException e) {
      verifyInvalidValueMessage(e, propertyValue);
      verifyDebugLogs(debugLog, otherDebugLogs);
    }
  }

  @Test
  public void feed_period_by_days() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true)); // 2008-11-29
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project);
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "10");
    underTest.execute(new TestComputationStepContext());

    // return analysis from 2008-11-20
    assertPeriod(LEAK_PERIOD_MODE_DAYS, "10", analysis3.getCreatedAt(), analysis3.getUuid());

    assertThat(logTester.getLogs()).hasSize(1);
    assertThat(logTester.getLogs(LoggerLevel.DEBUG))
      .extracting(LogAndArguments::getFormattedMsg)
      .containsOnly("Resolving new code period by 10 days: 2008-11-20");
  }

  @Test
  public void feed_period_by_previous_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setProjectVersion("1.1").setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setProjectVersion("1.1").setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setProjectVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION).setDate(analysis1.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.0").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION).setDate(analysis4.getCreatedAt()));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project, "1.1");
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute(new TestComputationStepContext());

    // Analysis form 2008-11-12
    assertPeriod(LEAK_PERIOD_MODE_PREVIOUS_VERSION, "1.0", analysis2.getCreatedAt(), analysis2.getUuid());

    verifyDebugLogs("Resolving new code period by previous version: 1.0");
  }

  @Test
  public void feed_period_by_previous_version_with_previous_version_deleted() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setProjectVersion("1.1").setLast(false)); // 2008-11-20
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION));
    // The "1.0" version was deleted from the history
    dbTester.events().insertEvent(newEvent(analysis3).setName("1.1").setCategory(CATEGORY_VERSION));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project, "1.1");
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute(new TestComputationStepContext());

    // Analysis form 2008-11-11
    assertPeriod(LEAK_PERIOD_MODE_PREVIOUS_VERSION, "0.9", analysis1.getCreatedAt(), analysis1.getUuid());
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_no_previous_version_found() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("1.1").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setProjectVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.1").setCategory(CATEGORY_VERSION));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project, "1.1");
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, analysis1.getCreatedAt(), analysis1.getUuid());

    verifyDebugLogs("Resolving first analysis as new code period as there is only one existing version");
  }

  @Test
  public void feed_period_by_previous_version_with_first_analysis_when_previous_snapshot_is_the_last_one() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(true)); // 2008-11-11
    dbTester.events().insertEvent(newEvent(analysis).setName("0.9").setCategory(CATEGORY_VERSION));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project, "1.1");
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "previous_version");
    underTest.execute(new TestComputationStepContext());

    assertPeriod(LEAK_PERIOD_MODE_PREVIOUS_VERSION, null, analysis.getCreatedAt(), analysis.getUuid());
    verifyDebugLogs("Resolving first analysis as new code period as there is only one existing version");
  }

  @Test
  public void feed_period_by_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setProjectVersion("1.1").setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setProjectVersion("1.1").setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setProjectVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION));
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.0").setCategory(CATEGORY_VERSION));
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project, "1.1");
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "1.0");
    underTest.execute(new TestComputationStepContext());

    // Analysis form 2008-11-11
    assertPeriod(LEAK_PERIOD_MODE_VERSION, "1.0", analysis2.getCreatedAt(), analysis2.getUuid());
    verifyDebugLogs("Resolving new code period by version: 1.0");
  }

  /**
   * SONAR-11492
   */
  @Test
  public void feed_period_by_version_with_only_one_existing_version() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(true)); // 2008-11-11
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION));
    when(system2Mock.now()).thenReturn(november30th2008.getTime());
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    setupRoot(project, "0.9");
    setupBranchWithNoManualBaseline(analysisMetadataHolder, project);

    settings.setProperty("sonar.leak.period", "0.9");
    underTest.execute(new TestComputationStepContext());

    // Analysis form 2008-11-11
    assertPeriod(LEAK_PERIOD_MODE_VERSION, "0.9", analysis1.getCreatedAt(), analysis1.getUuid());
    verifyDebugLogs("Resolving new code period by version: 0.9");
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void leak_period_setting_is_ignored_for_SLB_or_PR(String leakPeriodSettingValue) {
    when(analysisMetadataHolder.isLongLivingBranch()).thenReturn(false);

    settings.setProperty("sonar.leak.period", leakPeriodSettingValue);
    underTest.execute(new TestComputationStepContext());

    assertThat(periodsHolder.hasPeriod()).isFalse();
  }

  private void assertPeriod(String mode, @Nullable String modeParameter, long snapshotDate, String analysisUuid) {
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(mode);
    assertThat(period.getModeParameter()).isEqualTo(modeParameter);
    assertThat(period.getSnapshotDate()).isEqualTo(snapshotDate);
    assertThat(period.getAnalysisUuid()).isEqualTo(analysisUuid);
  }

  private void verifyDebugLogs(String log, String... otherLogs) {
    assertThat(logTester.getLogs()).hasSize(1 + otherLogs.length);
    assertThat(logTester.getLogs(LoggerLevel.DEBUG))
      .extracting(LogAndArguments::getFormattedMsg)
      .containsOnly(Stream.concat(Stream.of(log), Arrays.stream(otherLogs)).toArray(String[]::new));
  }

  private void setupRoot(ComponentDto project) {
    setupRoot(project, randomAlphanumeric(3));
  }

  private void setupRoot(ComponentDto project, String version) {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey()).setProjectVersion(version).build());
    when(configurationRepository.getConfiguration()).thenReturn(settings.asConfig());
  }

  private void setupBranchWithNoManualBaseline(AnalysisMetadataHolder analysisMetadataHolder, ComponentDto projectOrLongBranch) {
    dbTester.components().unsetManualBaseline(projectOrLongBranch);
    when(analysisMetadataHolder.getBranch()).thenReturn(branchOf(projectOrLongBranch));
  }

  private static void verifyInvalidValueMessage(MessageException e, String propertyValue) {
    assertThat(e).hasMessage("Invalid new code period. '" + propertyValue
      + "' is not one of: integer > 0, date before current analysis j, \"previous_version\", or version string that exists in the project' \n" +
      "Please contact a project administrator to correct this setting");
  }

}
