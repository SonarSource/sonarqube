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
package org.sonar.ce.task.projectanalysis.api.posttask;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.CeTask;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.analysis.Organization;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateHolderRule;
import org.sonar.ce.task.projectanalysis.qualitygate.MutableQualityGateStatusHolderRule;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.ce.task.projectanalysis.qualitygate.QualityGateStatus;
import org.sonar.db.component.BranchType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class PostProjectAnalysisTasksExecutorTest {
  private static final long QUALITY_GATE_ID = 98451;
  private static final String QUALITY_GATE_NAME = "qualityGate name";
  private static final Condition CONDITION_1 = createCondition("metric key 1");
  private static final Condition CONDITION_2 = createCondition("metric key 2");

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public MutableQualityGateHolderRule qualityGateHolder = new MutableQualityGateHolderRule();
  @Rule
  public MutableQualityGateStatusHolderRule qualityGateStatusHolder = new MutableQualityGateStatusHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public LogTester logTester = new LogTester();

  private String organizationUuid = "org1";
  private String organizationKey = organizationUuid + "_key";
  private String organizationName = organizationUuid + "_name";
  private System2 system2 = mock(System2.class);
  private ArgumentCaptor<PostProjectAnalysisTask.Context> taskContextCaptor = ArgumentCaptor.forClass(PostProjectAnalysisTask.Context.class);
  private CeTask.Component component = new CeTask.Component("component uuid", "component key", "component name");
  private CeTask ceTask = new CeTask.Builder()
    .setOrganizationUuid(organizationUuid)
    .setType("type")
    .setUuid("uuid")
    .setComponent(component)
    .setMainComponent(component)
    .build();
  private PostProjectAnalysisTask postProjectAnalysisTask = newPostProjectAnalysisTask("PT1");
  private PostProjectAnalysisTasksExecutor underTest = new PostProjectAnalysisTasksExecutor(
    ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder,
    reportReader, system2,
    new PostProjectAnalysisTask[] {postProjectAnalysisTask});

  @Before
  public void setUp() throws Exception {
    qualityGateHolder.setQualityGate(new QualityGate(QUALITY_GATE_ID, QUALITY_GATE_NAME, of(CONDITION_1, CONDITION_2)));
    qualityGateStatusHolder.setStatus(QualityGateStatus.OK, ImmutableMap.of(
      CONDITION_1, ConditionStatus.create(ConditionStatus.EvaluationStatus.OK, "value"),
      CONDITION_2, ConditionStatus.NO_VALUE_STATUS));
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.LONG);
    analysisMetadataHolder
      .setBranch(branch)
      .setOrganizationsEnabled(new Random().nextBoolean())
      .setOrganization(Organization.from(
        new OrganizationDto().setKey(organizationKey).setName(organizationName).setUuid(organizationUuid).setDefaultQualityGateUuid("foo")));

    reportReader.setMetadata(ScannerReport.Metadata.newBuilder().build());
  }

  @Test
  @UseDataProvider("booleanValues")
  public void does_not_fail_when_there_is_no_PostProjectAnalysisTasksExecutor(boolean allStepsExecuted) {
    new PostProjectAnalysisTasksExecutor(ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader, system2)
      .finished(allStepsExecuted);
  }

  @Test
  @UseDataProvider("booleanValues")
  public void finished_calls_all_PostProjectAnalysisTask_in_order_of_the_array_and_passes_the_same_object_to_all(boolean allStepsExecuted) {
    PostProjectAnalysisTask postProjectAnalysisTask1 = newPostProjectAnalysisTask("PT1");
    PostProjectAnalysisTask postProjectAnalysisTask2 = newPostProjectAnalysisTask("PT2");
    InOrder inOrder = inOrder(postProjectAnalysisTask1, postProjectAnalysisTask2);

    new PostProjectAnalysisTasksExecutor(
      ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader,
      system2, new PostProjectAnalysisTask[] {postProjectAnalysisTask1, postProjectAnalysisTask2})
        .finished(allStepsExecuted);

    inOrder.verify(postProjectAnalysisTask1).finished(taskContextCaptor.capture());
    inOrder.verify(postProjectAnalysisTask1).getDescription();
    inOrder.verify(postProjectAnalysisTask2).finished(taskContextCaptor.capture());
    inOrder.verify(postProjectAnalysisTask2).getDescription();
    inOrder.verifyNoMoreInteractions();

    ArgumentCaptor<PostProjectAnalysisTask.Context> taskContextCaptor = this.taskContextCaptor;
    List<PostProjectAnalysisTask.ProjectAnalysis> allValues = getAllProjectAnalyses(taskContextCaptor);
    assertThat(allValues).hasSize(2);
    assertThat(allValues.get(0)).isSameAs(allValues.get(1));

    assertThat(logTester.logs()).hasSize(2);
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0)).matches("^PT1 \\| status=SUCCESS \\| time=\\d+ms$");
    assertThat(logs.get(1)).matches("^PT2 \\| status=SUCCESS \\| time=\\d+ms$");
  }

  @Test
  @UseDataProvider("booleanValues")
  public void organization_is_null_when_organization_are_disabled(boolean allStepsExecuted) {
    analysisMetadataHolder
      .setOrganizationsEnabled(false)
      .setOrganization(Organization.from(
        new OrganizationDto().setKey(organizationKey).setName(organizationName).setUuid(organizationUuid).setDefaultQualityGateUuid("foo")));
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getOrganization()).isEmpty();
  }

  @Test
  @UseDataProvider("booleanValues")
  public void organization_is_not_null_when_organization_are_enabled(boolean allStepsExecuted) {
    analysisMetadataHolder
      .setOrganizationsEnabled(true)
      .setOrganization(Organization.from(
        new OrganizationDto().setKey(organizationKey).setName(organizationName).setUuid(organizationUuid).setDefaultQualityGateUuid("foo")));
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    org.sonar.api.ce.posttask.Organization organization = taskContextCaptor.getValue().getProjectAnalysis().getOrganization().get();
    assertThat(organization.getKey()).isEqualTo(organizationKey);
    assertThat(organization.getName()).isEqualTo(organizationName);
  }

  @Test
  @UseDataProvider("booleanValues")
  public void CeTask_status_depends_on_finished_method_argument_is_true_or_false(boolean allStepsExecuted) {
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getCeTask().getStatus())
      .isEqualTo(
        allStepsExecuted ? org.sonar.api.ce.posttask.CeTask.Status.SUCCESS : org.sonar.api.ce.posttask.CeTask.Status.FAILED);
  }

  @Test
  public void ceTask_uuid_is_UUID_of_CeTask() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getCeTask().getId())
      .isEqualTo(ceTask.getUuid());
  }

  @Test
  public void project_uuid_key_and_name_come_from_CeTask() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    Project project = taskContextCaptor.getValue().getProjectAnalysis().getProject();
    assertThat(project.getUuid()).isEqualTo(ceTask.getComponent().get().getUuid());
    assertThat(project.getKey()).isEqualTo(ceTask.getComponent().get().getKey().get());
    assertThat(project.getName()).isEqualTo(ceTask.getComponent().get().getName().get());
  }

  @Test
  public void date_comes_from_AnalysisMetadataHolder() {
    analysisMetadataHolder.setAnalysisDate(8_465_132_498L);
    analysisMetadataHolder.setUuid(RandomStringUtils.randomAlphanumeric(40));

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getDate())
      .isEqualTo(new Date(analysisMetadataHolder.getAnalysisDate()));
  }

  @Test
  public void date_comes_from_system2_if_not_set_in_AnalysisMetadataHolder() {
    long now = 1_999_663L;
    when(system2.now()).thenReturn(now);

    underTest.finished(false);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getDate()).isEqualTo(new Date(now));
  }

  @Test
  public void analysisDate_and_analysisUuid_comes_from_AnalysisMetadataHolder_when_set() {
    analysisMetadataHolder.setAnalysisDate(8465132498L);
    analysisMetadataHolder.setUuid(RandomStringUtils.randomAlphanumeric(40));

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getAnalysis().get().getDate())
      .isEqualTo(new Date(analysisMetadataHolder.getAnalysisDate()));
    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getAnalysis().get().getAnalysisUuid())
      .isEqualTo(analysisMetadataHolder.getUuid());
  }

  @Test
  public void analysis_is_empty_when_not_set_in_AnalysisMetadataHolder() {
    underTest.finished(false);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getAnalysis()).isEmpty();
  }

  @Test
  public void branch_comes_from_AnalysisMetadataHolder_when_set() {
    analysisMetadataHolder.setBranch(new Branch() {
      @Override
      public BranchType getType() {
        return BranchType.SHORT;
      }

      @Override
      public boolean isMain() {
        return false;
      }

      @Override
      public String getMergeBranchUuid() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getName() {
        return "feature/foo";
      }

      @Override
      public boolean supportsCrossProjectCpd() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getPullRequestKey() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getTargetBranchName() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String generateKey(String projectKey, @Nullable String fileOrDirPath) {
        throw new UnsupportedOperationException();
      }
    });

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    org.sonar.api.ce.posttask.Branch branch = taskContextCaptor.getValue().getProjectAnalysis().getBranch().get();
    assertThat(branch.isMain()).isFalse();
    assertThat(branch.getName()).hasValue("feature/foo");
    assertThat(branch.getType()).isEqualTo(BranchImpl.Type.SHORT);
  }

  @Test
  public void qualityGate_is_null_when_finished_method_argument_is_false() {
    underTest.finished(false);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    assertThat(taskContextCaptor.getValue().getProjectAnalysis().getQualityGate()).isNull();
  }

  @Test
  public void qualityGate_is_populated_when_finished_method_argument_is_true() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    org.sonar.api.ce.posttask.QualityGate qualityGate = taskContextCaptor.getValue().getProjectAnalysis().getQualityGate();
    assertThat(qualityGate.getStatus()).isEqualTo(org.sonar.api.ce.posttask.QualityGate.Status.OK);
    assertThat(qualityGate.getId()).isEqualTo(String.valueOf(QUALITY_GATE_ID));
    assertThat(qualityGate.getName()).isEqualTo(QUALITY_GATE_NAME);
    assertThat(qualityGate.getConditions()).hasSize(2);
  }

  @Test
  public void scannerContext_loads_properties_from_scanner_report() {
    reportReader.putContextProperties(asList(ScannerReport.ContextProperty.newBuilder().setKey("foo").setValue("bar").build()));
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());

    org.sonar.api.ce.posttask.ScannerContext scannerContext = taskContextCaptor.getValue().getProjectAnalysis().getScannerContext();
    assertThat(scannerContext.getProperties()).containsExactly(entry("foo", "bar"));
  }

  @Test
  @UseDataProvider("booleanValues")
  public void logStatistics_add_fails_when_NPE_if_key_or_value_is_null(boolean allStepsExecuted) {
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());
    PostProjectAnalysisTask.LogStatistics logStatistics = taskContextCaptor.getValue().getLogStatistics();

    assertThat(catchThrowable(() -> logStatistics.add(null, "foo")))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Statistic has null key");
    assertThat(catchThrowable(() -> logStatistics.add(null, null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Statistic has null key");
    assertThat(catchThrowable(() -> logStatistics.add("bar", null)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Statistic with key [bar] has null value");
  }

  @Test
  @UseDataProvider("booleanValues")
  public void logStatistics_add_fails_with_IAE_if_key_is_time_or_status_ignoring_case(boolean allStepsExecuted) {
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());
    PostProjectAnalysisTask.LogStatistics logStatistics = taskContextCaptor.getValue().getLogStatistics();

    for (String reservedKey : asList("time", "TIME", "TImE", "status", "STATUS", "STaTuS")) {
      assertThat(catchThrowable(() -> logStatistics.add(reservedKey, "foo")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Statistic with key [" + reservedKey + "] is not accepted");
    }
  }

  @Test
  @UseDataProvider("booleanValues")
  public void logStatistics_add_fails_with_IAE_if_same_key_with_exact_case_added_twice(boolean allStepsExecuted) {
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(taskContextCaptor.capture());
    PostProjectAnalysisTask.LogStatistics logStatistics = taskContextCaptor.getValue().getLogStatistics();

    String key = RandomStringUtils.randomAlphabetic(10);
    logStatistics.add(key, new Object());
    assertThat(catchThrowable(() -> logStatistics.add(key, "bar")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Statistic with key [" + key + "] is already present");
  }

  @Test
  @UseDataProvider("booleanValues")
  public void logStatistics_adds_statistics_to_end_of_task_log(boolean allStepsExecuted) {
    Map<String, Object> stats = new HashMap<>();
    for (int i = 0; i <= new Random().nextInt(10); i++) {
      stats.put("statKey_" + i, "statVal_" + i);
    }
    PostProjectAnalysisTask logStatisticsTask = mock(PostProjectAnalysisTask.class);
    when(logStatisticsTask.getDescription()).thenReturn("PT1");
    doAnswer(i -> {
      PostProjectAnalysisTask.Context context = i.getArgument(0);
      stats.forEach((k, v) -> context.getLogStatistics().add(k, v));
      return null;
    }).when(logStatisticsTask)
      .finished(any(PostProjectAnalysisTask.Context.class));

    new PostProjectAnalysisTasksExecutor(
      ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader,
      system2, new PostProjectAnalysisTask[] {logStatisticsTask})
        .finished(allStepsExecuted);

    verify(logStatisticsTask).finished(taskContextCaptor.capture());

    assertThat(logTester.logs()).hasSize(1);
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(1);
    StringBuilder expectedLog = new StringBuilder("^PT1 ");
    stats.forEach((k, v) -> expectedLog.append("\\| " + k + "=" + v + " "));
    expectedLog.append("\\| status=SUCCESS \\| time=\\d+ms$");
    assertThat(logs.get(0)).matches(expectedLog.toString());
  }

  @Test
  @UseDataProvider("booleanValues")
  public void finished_does_not_fail_if_listener_throws_exception_and_execute_subsequent_listeners(boolean allStepsExecuted) {
    PostProjectAnalysisTask postProjectAnalysisTask1 = newPostProjectAnalysisTask("PT1");
    PostProjectAnalysisTask postProjectAnalysisTask2 = newPostProjectAnalysisTask("PT2");
    PostProjectAnalysisTask postProjectAnalysisTask3 = newPostProjectAnalysisTask("PT3");
    InOrder inOrder = inOrder(postProjectAnalysisTask1, postProjectAnalysisTask2, postProjectAnalysisTask3);

    doThrow(new RuntimeException("Faking a listener throws an exception"))
      .when(postProjectAnalysisTask2)
      .finished(any(PostProjectAnalysisTask.Context.class));

    new PostProjectAnalysisTasksExecutor(
      ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader,
      system2, new PostProjectAnalysisTask[] {postProjectAnalysisTask1, postProjectAnalysisTask2, postProjectAnalysisTask3})
        .finished(allStepsExecuted);

    inOrder.verify(postProjectAnalysisTask1).finished(taskContextCaptor.capture());
    inOrder.verify(postProjectAnalysisTask1).getDescription();
    inOrder.verify(postProjectAnalysisTask2).finished(taskContextCaptor.capture());
    inOrder.verify(postProjectAnalysisTask2).getDescription();
    inOrder.verify(postProjectAnalysisTask3).finished(taskContextCaptor.capture());
    inOrder.verify(postProjectAnalysisTask3).getDescription();
    inOrder.verifyNoMoreInteractions();

    assertThat(logTester.logs()).hasSize(4);
    List<String> logs = logTester.logs(LoggerLevel.INFO);
    assertThat(logs).hasSize(3);
    assertThat(logs.get(0)).matches("^PT1 \\| status=SUCCESS \\| time=\\d+ms$");
    assertThat(logs.get(1)).matches("^PT2 \\| status=FAILED \\| time=\\d+ms$");
    assertThat(logs.get(2)).matches("^PT3 \\| status=SUCCESS \\| time=\\d+ms$");
  }

  @DataProvider
  public static Object[][] booleanValues() {
    return new Object[][] {
      {true},
      {false}
    };
  }

  private static Condition createCondition(String metricKey) {
    Metric metric = mock(Metric.class);
    when(metric.getKey()).thenReturn(metricKey);
    return new Condition(metric, Condition.Operator.LESS_THAN.getDbValue(), "error threshold");
  }

  private static PostProjectAnalysisTask newPostProjectAnalysisTask(String description) {
    PostProjectAnalysisTask res = mock(PostProjectAnalysisTask.class);
    when(res.getDescription()).thenReturn(description);
    doAnswer(i -> null).when(res).finished(any(PostProjectAnalysisTask.Context.class));
    return res;
  }

  private static List<PostProjectAnalysisTask.ProjectAnalysis> getAllProjectAnalyses(ArgumentCaptor<PostProjectAnalysisTask.Context> taskContextCaptor) {
    return taskContextCaptor.getAllValues()
      .stream()
      .map(PostProjectAnalysisTask.Context::getProjectAnalysis)
      .collect(toList());
  }

}
