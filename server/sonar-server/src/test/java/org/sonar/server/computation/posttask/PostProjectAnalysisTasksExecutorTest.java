/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.posttask;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.ce.queue.CeTask;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.qualitygate.Condition;
import org.sonar.server.computation.qualitygate.ConditionStatus;
import org.sonar.server.computation.qualitygate.MutableQualityGateHolderRule;
import org.sonar.server.computation.qualitygate.MutableQualityGateStatusHolderRule;
import org.sonar.server.computation.qualitygate.QualityGate;
import org.sonar.server.computation.qualitygate.QualityGateStatus;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
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
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setAnalysisDate(8465132498L);
  @Rule
  public MutableQualityGateHolderRule qualityGateHolder = new MutableQualityGateHolderRule();
  @Rule
  public MutableQualityGateStatusHolderRule qualityGateStatusHolder = new MutableQualityGateStatusHolderRule();

  private ArgumentCaptor<PostProjectAnalysisTask.ProjectAnalysis> projectAnalysisArgumentCaptor = ArgumentCaptor.forClass(PostProjectAnalysisTask.ProjectAnalysis.class);
  private CeTask ceTask = new CeTask.Builder()
    .setType("type")
    .setUuid("uuid")
    .setComponentKey("component key")
    .setComponentName("component name")
    .setComponentUuid("component uuid")
    .build();
  private PostProjectAnalysisTask postProjectAnalysisTask = mock(PostProjectAnalysisTask.class);
  private PostProjectAnalysisTasksExecutor underTest = new PostProjectAnalysisTasksExecutor(
    ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder,
    new PostProjectAnalysisTask[] {postProjectAnalysisTask});

  @Before
  public void setUp() throws Exception {
    qualityGateHolder.setQualityGate(new QualityGate(QUALITY_GATE_ID, QUALITY_GATE_NAME, of(CONDITION_1, CONDITION_2)));
    qualityGateStatusHolder.setStatus(QualityGateStatus.OK, ImmutableMap.of(
      CONDITION_1, ConditionStatus.create(ConditionStatus.EvaluationStatus.OK, "value"),
      CONDITION_2, ConditionStatus.NO_VALUE_STATUS));
  }

  @Test
  @UseDataProvider("booleanValues")
  public void does_not_fail_when_there_is_no_PostProjectAnalysisTasksExecutor(boolean allStepsExecuted) {
    new PostProjectAnalysisTasksExecutor(ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder)
      .finished(allStepsExecuted);
  }

  @Test
  @UseDataProvider("booleanValues")
  public void finished_calls_all_PostProjectAnalysisTask_in_order_of_the_array_and_passes_the_same_object_to_all(boolean allStepsExecuted) {
    PostProjectAnalysisTask postProjectAnalysisTask1 = mock(PostProjectAnalysisTask.class);
    PostProjectAnalysisTask postProjectAnalysisTask2 = mock(PostProjectAnalysisTask.class);
    InOrder inOrder = inOrder(postProjectAnalysisTask1, postProjectAnalysisTask2);

    new PostProjectAnalysisTasksExecutor(
      ceTask, analysisMetadataHolder, qualityGateHolder, qualityGateStatusHolder,
      new PostProjectAnalysisTask[] {postProjectAnalysisTask1, postProjectAnalysisTask2})
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
  public void analysisDate_comes_from_AnalysisMetadataHolder() {
    underTest.finished(true);

    verify(postProjectAnalysisTask).finished(projectAnalysisArgumentCaptor.capture());

    assertThat(projectAnalysisArgumentCaptor.getValue().getDate())
      .isEqualTo(new Date(analysisMetadataHolder.getAnalysisDate()));
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
    return new Condition(metric, Condition.Operator.EQUALS.getDbValue(), "error threshold", "warn threshold", null);
  }

}
