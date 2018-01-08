/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.api.posttask;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
import org.sonar.ce.queue.CeTask;
import org.sonar.db.component.BranchType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.qualitygate.Condition;
import org.sonar.server.computation.task.projectanalysis.qualitygate.ConditionStatus;
import org.sonar.server.computation.task.projectanalysis.qualitygate.MutableQualityGateHolderRule;
import org.sonar.server.computation.task.projectanalysis.qualitygate.MutableQualityGateStatusHolderRule;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGate;
import org.sonar.server.computation.task.projectanalysis.qualitygate.QualityGateStatus;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Matchers.any;
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

  private String organizationUuid = "org1";
  private String organizationKey = organizationUuid + "_key";
  private String organizationName = organizationUuid + "_name";
  private System2 system2 = mock(System2.class);
  private ArgumentCaptor<PostProjectAnalysisTask.ProjectAnalysis> projectAnalysisArgumentCaptor = ArgumentCaptor.forClass(PostProjectAnalysisTask.ProjectAnalysis.class);
  private CeTask ceTask = new CeTask.Builder()
    .setOrganizationUuid(organizationUuid)
    .setType("type")
    .setUuid("uuid")
    .setComponentKey("component key")
    .setComponentName("component name")
    .setComponentUuid("component uuid")
    .build();
  private PostProjectAnalysisTask postProjectAnalysisTask = mock(PostProjectAnalysisTask.class);
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
    PostProjectAnalysisTask postProjectAnalysisTask1 = mock(PostProjectAnalysisTask.class);
    PostProjectAnalysisTask postProjectAnalysisTask2 = mock(PostProjectAnalysisTask.class);
    InOrder inOrder = inOrder(postProjectAnalysisTask1, postProjectAnalysisTask2);

    new PostProjectAnalysisTasksExecutor(
      ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader,
      system2, new PostProjectAnalysisTask[] {postProjectAnalysisTask1, postProjectAnalysisTask2})
      .finished(allStepsExecuted);

    inOrder.verify(postProjectAnalysisTask1).finished(projectAnalysisArgumentCaptor.capture());
    inOrder.verify(postProjectAnalysisTask2).finished(projectAnalysisArgumentCaptor.capture());
    inOrder.verifyNoMoreInteractions();

    List<PostProjectAnalysisTask.ProjectAnalysis> allValues = projectAnalysisArgumentCaptor.getAllValues();
    assertThat(allValues).hasSize(2);
    assertThat(allValues.get(0)).isSameAs(allValues.get(1));
  }

  @Test
  @UseDataProvider("booleanValues")
  public void organization_is_null_when_organization_are_disabled(boolean allStepsExecuted) {
    analysisMetadataHolder
      .setOrganizationsEnabled(false)
      .setOrganization(Organization.from(
      new OrganizationDto().setKey(organizationKey).setName(organizationName).setUuid(organizationUuid).setDefaultQualityGateUuid("foo")));
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getOrganization()).isEmpty();
  }

  @Test
  @UseDataProvider("booleanValues")
  public void organization_is_not_null_when_organization_are_enabled(boolean allStepsExecuted) {
    analysisMetadataHolder
      .setOrganizationsEnabled(true)
      .setOrganization(Organization.from(
      new OrganizationDto().setKey(organizationKey).setName(organizationName).setUuid(organizationUuid).setDefaultQualityGateUuid("foo")));
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    org.sonar.api.ce.posttask.Organization organization = projectAnalysisArgumentCaptor.getValue().getOrganization().get();
    assertThat(organization.getKey()).isEqualTo(organizationKey);
    assertThat(organization.getName()).isEqualTo(organizationName);
  }

  @Test
  @UseDataProvider("booleanValues")
  public void CeTask_status_depends_on_finished_method_argument_is_true_or_false(boolean allStepsExecuted) {
    underTest.finished(allStepsExecuted);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getCeTask().getStatus())
      .isEqualTo(
        allStepsExecuted ? org.sonar.api.ce.posttask.CeTask.Status.SUCCESS : org.sonar.api.ce.posttask.CeTask.Status.FAILED);
  }

  @Test
  public void ceTask_uuid_is_UUID_of_CeTask() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getCeTask().getId())
      .isEqualTo(ceTask.getUuid());
  }

  @Test
  public void project_uuid_key_and_name_come_from_CeTask() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    Project project = projectAnalysisArgumentCaptor.getValue().getProject();
    assertThat(project.getUuid()).isEqualTo(ceTask.getComponentUuid());
    assertThat(project.getKey()).isEqualTo(ceTask.getComponentKey());
    assertThat(project.getName()).isEqualTo(ceTask.getComponentName());
  }

  @Test
  public void date_comes_from_AnalysisMetadataHolder() {
    analysisMetadataHolder.setAnalysisDate(8_465_132_498L);
    analysisMetadataHolder.setUuid(RandomStringUtils.randomAlphanumeric(40));

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getDate())
      .isEqualTo(new Date(analysisMetadataHolder.getAnalysisDate()));
  }

  @Test
  public void date_comes_from_system2_if_not_set_in_AnalysisMetadataHolder() {
    long now = 1_999_663L;
    when(system2.now()).thenReturn(now);

    underTest.finished(false);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getDate()).isEqualTo(new Date(now));
  }

  @Test
  public void analysisDate_and_analysisUuid_comes_from_AnalysisMetadataHolder_when_set() {
    analysisMetadataHolder.setAnalysisDate(8465132498L);
    analysisMetadataHolder.setUuid(RandomStringUtils.randomAlphanumeric(40));

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getAnalysis().get().getDate())
      .isEqualTo(new Date(analysisMetadataHolder.getAnalysisDate()));
    assertThat(projectAnalysisArgumentCaptor.getValue().getAnalysis().get().getAnalysisUuid())
      .isEqualTo(analysisMetadataHolder.getUuid());
  }

  @Test
  public void analysis_is_empty_when_not_set_in_AnalysisMetadataHolder() {
    underTest.finished(false);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getAnalysis()).isEmpty();
  }

  @Test
  public void branch_is_empty_when_legacy_branch_implementation_is_used() {
    analysisMetadataHolder.setBranch(new DefaultBranchImpl("feature/foo"));

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getBranch()).isEmpty();
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
      public boolean isLegacyFeature() {
        return false;
      }

      @Override
      public Optional<String> getMergeBranchUuid() {
        return Optional.empty();
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
      public String generateKey(ScannerReport.Component module, @Nullable ScannerReport.Component fileOrDir) {
        throw new UnsupportedOperationException();
      }
    });

    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    org.sonar.api.ce.posttask.Branch branch = projectAnalysisArgumentCaptor.getValue().getBranch().get();
    assertThat(branch.isMain()).isFalse();
    assertThat(branch.getName()).hasValue("feature/foo");
    assertThat(branch.getType()).isEqualTo(BranchImpl.Type.SHORT);
  }

  @Test
  public void qualityGate_is_null_when_finished_method_argument_is_false() {
    underTest.finished(false);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getQualityGate()).isNull();
  }

  @Test
  public void qualityGate_is_populated_when_finished_method_argument_is_true() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    org.sonar.api.ce.posttask.QualityGate qualityGate = projectAnalysisArgumentCaptor.getValue().getQualityGate();
    assertThat(qualityGate.getStatus()).isEqualTo(org.sonar.api.ce.posttask.QualityGate.Status.OK);
    assertThat(qualityGate.getId()).isEqualTo(String.valueOf(QUALITY_GATE_ID));
    assertThat(qualityGate.getName()).isEqualTo(QUALITY_GATE_NAME);
    assertThat(qualityGate.getConditions()).hasSize(2);
  }

  @Test
  public void scannerContext_loads_properties_from_scanner_report() {
    reportReader.putContextProperties(asList(ScannerReport.ContextProperty.newBuilder().setKey("foo").setValue("bar").build()));
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    org.sonar.api.ce.posttask.ScannerContext scannerContext = projectAnalysisArgumentCaptor.getValue().getScannerContext();
    assertThat(scannerContext.getProperties()).containsExactly(entry("foo", "bar"));
  }

  @Test
  @UseDataProvider("booleanValues")
  public void finished_does_not_fail_if_listener_throws_exception_and_execute_subsequent_listeners(boolean allStepsExecuted) {
    PostProjectAnalysisTask postProjectAnalysisTask1 = mock(PostProjectAnalysisTask.class);
    PostProjectAnalysisTask postProjectAnalysisTask2 = mock(PostProjectAnalysisTask.class);
    PostProjectAnalysisTask postProjectAnalysisTask3 = mock(PostProjectAnalysisTask.class);
    InOrder inOrder = inOrder(postProjectAnalysisTask1, postProjectAnalysisTask2, postProjectAnalysisTask3);

    doThrow(new RuntimeException("Faking a listener throws an exception"))
      .when(postProjectAnalysisTask2)
      .finished(any(PostProjectAnalysisTask.ProjectAnalysis.class));

    new PostProjectAnalysisTasksExecutor(
      ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder, reportReader,
      system2, new PostProjectAnalysisTask[] {postProjectAnalysisTask1, postProjectAnalysisTask2, postProjectAnalysisTask3})
      .finished(allStepsExecuted);

    inOrder.verify(postProjectAnalysisTask1).finished(projectAnalysisArgumentCaptor.capture());
    inOrder.verify(postProjectAnalysisTask2).finished(projectAnalysisArgumentCaptor.capture());
    inOrder.verify(postProjectAnalysisTask3).finished(projectAnalysisArgumentCaptor.capture());
    inOrder.verifyNoMoreInteractions();
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
    return new Condition(metric, Condition.Operator.EQUALS.getDbValue(), "error threshold", "warn threshold", false);
  }

}
