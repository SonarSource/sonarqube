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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.period.NewCodePeriodResolver;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventTesting;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.project.Project;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.event.EventDto.CATEGORY_VERSION;
import static org.sonar.db.event.EventTesting.newEvent;

@RunWith(DataProviderRunner.class)
public class LoadPeriodsStepTest extends BaseStepTest {
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
  private System2 system2Mock = mock(System2.class);
  private NewCodePeriodDao dao = new NewCodePeriodDao(system2Mock, new SequenceUuidFactory());
  private NewCodePeriodResolver newCodePeriodResolver = new NewCodePeriodResolver(dbTester.getDbClient());
  private ZonedDateTime analysisDate = ZonedDateTime.of(2019, 3, 20, 5, 30, 40, 0, ZoneId.systemDefault());

  private LoadPeriodsStep underTest = new LoadPeriodsStep(analysisMetadataHolder, dao, treeRootHolder, periodsHolder, dbTester.getDbClient(), newCodePeriodResolver);

  private OrganizationDto organization;
  private ComponentDto project;

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setUp() {
    organization = dbTester.organizations().insert();
    project = dbTester.components().insertMainBranch(organization);

    when(analysisMetadataHolder.isBranch()).thenReturn(true);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(analysisDate.toInstant().toEpochMilli());
  }

  @Test
  public void no_period_on_first_analysis() {
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    underTest.execute(new TestComputationStepContext());

    verify(analysisMetadataHolder).isFirstAnalysis();
    assertThat(periodsHolder.hasPeriod()).isFalse();
    verifyNoMoreInteractions(analysisMetadataHolder);
  }

  @Test
  public void no_period_if_not_LLB() {
    when(analysisMetadataHolder.isBranch()).thenReturn(false);
    underTest.execute(new TestComputationStepContext());

    verify(analysisMetadataHolder).isFirstAnalysis();
    verify(analysisMetadataHolder).isBranch();
    assertThat(periodsHolder.hasPeriod()).isFalse();
    verifyNoMoreInteractions(analysisMetadataHolder);
  }

  @Test
  public void load_default_if_nothing_defined() {
    setupRoot(project);

    SnapshotDto analysis = dbTester.components().insertSnapshot(project,
      snapshot -> snapshot.setCreatedAt(milisSinceEpoch(2019, 3, 15, 0)));

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.PREVIOUS_VERSION, null, analysis.getCreatedAt());
    verifyDebugLogs("Resolving first analysis as new code period as there is only one existing version");
  }

  @Test
  public void load_number_of_days_global() {
    setGlobalPeriod(NewCodePeriodType.NUMBER_OF_DAYS, "10");

    testNumberOfDays(project);
  }

  @Test
  public void load_number_of_days_on_project() {
    setGlobalPeriod(NewCodePeriodType.PREVIOUS_VERSION, null);
    setProjectPeriod(project.uuid(), NewCodePeriodType.NUMBER_OF_DAYS, "10");

    testNumberOfDays(project);
  }

  @Test
  public void load_number_of_days_on_branch() {
    ComponentDto branch = dbTester.components().insertProjectBranch(project);

    setGlobalPeriod(NewCodePeriodType.PREVIOUS_VERSION, null);
    setProjectPeriod(project.uuid(), NewCodePeriodType.PREVIOUS_VERSION, null);
    setBranchPeriod(project.uuid(), branch.uuid(), NewCodePeriodType.NUMBER_OF_DAYS, "10");

    testNumberOfDays(branch);
  }

  private void testNumberOfDays(ComponentDto projectOrBranch) {
    setupRoot(projectOrBranch);

    SnapshotDto analysis = dbTester.components().insertSnapshot(projectOrBranch,
      snapshot -> snapshot.setCreatedAt(milisSinceEpoch(2019, 3, 15, 0)));

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.NUMBER_OF_DAYS, "10", analysis.getCreatedAt());
    verifyDebugLogs("Resolving new code period by 10 days: 2019-03-10");
  }

  @Test
  public void load_specific_analysis() {
    ComponentDto branch = dbTester.components().insertProjectBranch(project);
    SnapshotDto selectedAnalysis = dbTester.components().insertSnapshot(branch);
    SnapshotDto aVersionAnalysis = dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(milisSinceEpoch(2019, 3, 12, 0)).setLast(false));
    dbTester.events().insertEvent(EventTesting.newEvent(aVersionAnalysis).setName("a_version").setCategory(CATEGORY_VERSION));
    dbTester.components().insertSnapshot(branch, snapshot -> snapshot.setCreatedAt(milisSinceEpoch(2019, 3, 15, 0)).setLast(true));

    setBranchPeriod(project.uuid(), branch.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, selectedAnalysis.getUuid());
    setupRoot(branch);

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.SPECIFIC_ANALYSIS, selectedAnalysis.getUuid(), selectedAnalysis.getCreatedAt());
    verifyDebugLogs("Resolving new code period with a specific analysis");
  }

  @Test
  public void throw_ISE_if_no_analysis_found_for_number_of_days() {
    setProjectPeriod(project.uuid(), NewCodePeriodType.NUMBER_OF_DAYS, "10");

    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Attempting to resolve period while no analysis exist");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void throw_ISE_if_no_analysis_found_with_default() {
    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Attempting to resolve period while no analysis exist");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void ignore_unprocessed_snapshots() {
    SnapshotDto analysis1 = dbTester.components()
      .insertSnapshot(project, snapshot -> snapshot.setStatus(STATUS_UNPROCESSED).setCreatedAt(milisSinceEpoch(2019, 3, 12, 0)).setLast(false));
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project,
      snapshot -> snapshot.setStatus(STATUS_PROCESSED).setProjectVersion("not provided").setCreatedAt(milisSinceEpoch(2019, 3, 15, 0)).setLast(false));
    dbTester.events().insertEvent(newEvent(analysis1).setName("not provided").setCategory(CATEGORY_VERSION).setDate(analysis1.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis2).setName("not provided").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    setupRoot(project);
    setProjectPeriod(project.uuid(), NewCodePeriodType.NUMBER_OF_DAYS, "10");

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.NUMBER_OF_DAYS, "10", analysis2.getCreatedAt());
    verifyDebugLogs("Resolving new code period by 10 days: 2019-03-10");
  }

  @Test
  public void throw_ISE_when_specific_analysis_is_set_but_does_not_exist_in_DB() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertMainBranch(organization);
    setProjectPeriod(project.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, "nonexistent");
    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis 'nonexistent' of project '" + project.uuid() + "' defined as the baseline does not exist");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void throw_ISE_when_specific_analysis_is_set_but_does_not_belong_to_current_project() {
    ComponentDto otherProject = dbTester.components().insertMainBranch(organization);
    SnapshotDto otherProjectAnalysis = dbTester.components().insertSnapshot(otherProject);
    setBranchPeriod(project.uuid(), project.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, otherProjectAnalysis.getUuid());
    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis '" + otherProjectAnalysis.getUuid() + "' of project '" + project.uuid()
      + "' defined as the baseline does not exist");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void throw_ISE_when_specific_analysis_is_set_but_does_not_belong_to_current_branch() {
    ComponentDto otherBranch = dbTester.components().insertProjectBranch(project);
    SnapshotDto otherBranchAnalysis = dbTester.components().insertSnapshot(otherBranch);
    setBranchPeriod(project.uuid(), project.uuid(), NewCodePeriodType.SPECIFIC_ANALYSIS, otherBranchAnalysis.getUuid());
    setupRoot(project);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis '" + otherBranchAnalysis.getUuid() + "' of project '" + project.uuid()
      + "' defined as the baseline does not exist");

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void load_previous_version() {
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setProjectVersion("1.1").setLast(false)); // 2008-11-20
    SnapshotDto analysis4 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setProjectVersion("1.1").setLast(false)); // 2008-11-22
    SnapshotDto analysis5 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setProjectVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION).setDate(analysis1.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.0").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    dbTester.events().insertEvent(newEvent(analysis5).setName("1.1").setCategory(CATEGORY_VERSION).setDate(analysis5.getCreatedAt()));
    setupRoot(project, "1.1");
    setProjectPeriod(project.uuid(), NewCodePeriodType.PREVIOUS_VERSION, null);

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.PREVIOUS_VERSION, "1.0", analysis2.getCreatedAt());

    verifyDebugLogs("Resolving new code period by previous version: 1.0");
  }

  @Test
  public void load_previous_version_when_version_is_changing() {
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("0.9").setLast(true)); // 2008-11-12

    dbTester.events().insertEvent(newEvent(analysis2).setName("0.9").setCategory(CATEGORY_VERSION).setDate(analysis2.getCreatedAt()));
    setupRoot(project, "1.0");
    setProjectPeriod(project.uuid(), NewCodePeriodType.PREVIOUS_VERSION, null);

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.PREVIOUS_VERSION, "0.9", analysis2.getCreatedAt());

    verifyDebugLogs("Resolving new code period by previous version: 0.9");
  }

  @Test
  @UseDataProvider("zeroOrLess")
  public void fail_with_MessageException_if_period_is_0_or_less(int zeroOrLess) {
    setupRoot(project);
    setProjectPeriod(project.uuid(), NewCodePeriodType.NUMBER_OF_DAYS, String.valueOf(zeroOrLess));

    verifyFailWithInvalidValueMessageException(String.valueOf(zeroOrLess),
      "Invalid code period '" + zeroOrLess + "': number of days is <= 0");
  }

  @Test
  public void load_previous_version_with_previous_version_deleted() {
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setProjectVersion("1.0").setLast(false)); // 2008-11-12
    SnapshotDto analysis3 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setProjectVersion("1.1").setLast(true)); // 2008-11-20
    dbTester.events().insertEvent(newEvent(analysis1).setName("0.9").setCategory(CATEGORY_VERSION));
    // The "1.0" version was deleted from the history
    setupRoot(project, "1.1");

    underTest.execute(new TestComputationStepContext());

    // Analysis form 2008-11-11
    assertPeriod(NewCodePeriodType.PREVIOUS_VERSION, "0.9", analysis1.getCreatedAt());
  }

  @Test
  public void load_previous_version_with_first_analysis_when_no_previous_version_found() {
    SnapshotDto analysis1 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("1.1").setLast(false)); // 2008-11-11
    SnapshotDto analysis2 = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setProjectVersion("1.1").setLast(true)); // 2008-11-29
    dbTester.events().insertEvent(newEvent(analysis2).setName("1.1").setCategory(CATEGORY_VERSION));
    setupRoot(project, "1.1");

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.PREVIOUS_VERSION, null, analysis1.getCreatedAt());

    verifyDebugLogs("Resolving first analysis as new code period as there is only one existing version");
  }

  @Test
  public void load_previous_version_with_first_analysis_when_previous_snapshot_is_the_last_one() {
    SnapshotDto analysis = dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setProjectVersion("0.9").setLast(true)); // 2008-11-11
    dbTester.events().insertEvent(newEvent(analysis).setName("0.9").setCategory(CATEGORY_VERSION));
    setupRoot(project, "1.1");

    dbTester.newCodePeriods().insert(NewCodePeriodType.PREVIOUS_VERSION, null);

    underTest.execute(new TestComputationStepContext());

    assertPeriod(NewCodePeriodType.PREVIOUS_VERSION, "0.9", analysis.getCreatedAt());
    verifyDebugLogs("Resolving new code period by previous version: 0.9");
  }

  @Test
  @UseDataProvider("anyValidLeakPeriodSettingValue")
  public void leak_period_setting_is_ignored_for_PR(NewCodePeriodType type, @Nullable String value) {
    when(analysisMetadataHolder.isBranch()).thenReturn(false);

    dbTester.newCodePeriods().insert(type, value);

    underTest.execute(new TestComputationStepContext());

    assertThat(periodsHolder.hasPeriod()).isFalse();
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

  @DataProvider
  public static Object[][] zeroOrLess() {
    return new Object[][] {
      {0},
      {-1 - new Random().nextInt(30)}
    };
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

  @DataProvider
  public static Object[][] projectVersionNullOrNot() {
    return new Object[][] {
      {null},
      {randomAlphabetic(15)},
    };
  }

  @DataProvider
  public static Object[][] anyValidLeakPeriodSettingValue() {
    return new Object[][] {
      // days
      {NewCodePeriodType.NUMBER_OF_DAYS, "100"},
      // previous_version
      {NewCodePeriodType.PREVIOUS_VERSION, null}
    };
  }

  private List<SnapshotDto> createSnapshots(ComponentDto project) {
    ArrayList<SnapshotDto> list = new ArrayList<>();
    list.add(dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226379600000L).setLast(false))); // 2008-11-11
    list.add(dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1226494680000L).setLast(false))); // 2008-11-12
    list.add(dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227157200000L).setLast(false))); // 2008-11-20
    list.add(dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227358680000L).setLast(false))); // 2008-11-22
    list.add(dbTester.components().insertSnapshot(project, snapshot -> snapshot.setCreatedAt(1227934800000L).setLast(true))); // 2008-11-29
    return list;
  }

  private long milisSinceEpoch(int year, int month, int day, int hour) {
    return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneId.systemDefault())
      .toInstant().toEpochMilli();
  }

  private void setProjectPeriod(String projectUuid, NewCodePeriodType type, @Nullable String value) {
    dbTester.newCodePeriods().insert(projectUuid, type, value);
  }

  private void setBranchPeriod(String projectUuid, String branchUuid, NewCodePeriodType type, @Nullable String value) {
    dbTester.newCodePeriods().insert(projectUuid, branchUuid, type, value);
  }

  private void setGlobalPeriod(NewCodePeriodType type, @Nullable String value) {
    dbTester.newCodePeriods().insert(type, value);
  }

  private void assertPeriod(NewCodePeriodType type, @Nullable String value, long snapshotDate) {
    Period period = periodsHolder.getPeriod();
    assertThat(period).isNotNull();
    assertThat(period.getMode()).isEqualTo(type.name());
    assertThat(period.getModeParameter()).isEqualTo(value);
    assertThat(period.getSnapshotDate()).isEqualTo(snapshotDate);
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

  private void setupRoot(ComponentDto projectDto, String version) {
    treeRootHolder.setRoot(ReportComponent
      .builder(Component.Type.PROJECT, 1)
      .setUuid(projectDto.uuid())
      .setKey(projectDto.getKey())
      .setProjectVersion(version)
      .build());

    Project project = mock(Project.class);
    when(project.getUuid()).thenReturn(projectDto.getMainBranchProjectUuid() != null ? projectDto.getMainBranchProjectUuid() : projectDto.uuid());
    when(analysisMetadataHolder.getProject()).thenReturn(project);
  }

  private static void verifyInvalidValueMessage(MessageException e, String propertyValue) {
    assertThat(e).hasMessage("Invalid new code period. '" + propertyValue
      + "' is not one of: integer > 0, date before current analysis j, \"previous_version\", or version string that exists in the project' \n" +
      "Please contact a project administrator to correct this setting");
  }

}
