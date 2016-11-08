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
package org.sonar.server.computation.task.projectanalysis.webhook;

import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newConditionBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newQualityGateBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newScannerContextBuilder;
import static org.sonar.test.JsonAssert.assertJson;

public class WebhookPayloadTest {

  private static final String PROJECT_KEY = "P1";

  @Test
  public void create_payload_for_successful_analysis() {
    CeTask task = newCeTaskBuilder()
      .setStatus(CeTask.Status.SUCCESS)
      .setId("#1")
      .build();
    QualityGate gate = newQualityGateBuilder()
      .setId("G1")
      .setName("Gate One")
      .setStatus(QualityGate.Status.WARN)
      .add(newConditionBuilder()
        .setMetricKey("coverage")
        .setOperator(QualityGate.Operator.GREATER_THAN)
        .setOnLeakPeriod(true)
        .setWarningThreshold("75.0")
        .setErrorThreshold("70.0")
        .build(QualityGate.EvaluationStatus.WARN, "74.0"))
      .build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, gate);

    WebhookPayload payload = WebhookPayload.from(analysis);
    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.toJson()).isSimilarTo(getClass().getResource("WebhookPayloadTest/success.json"));
  }

  @Test
  public void create_payload_for_failed_analysis() {
    CeTask ceTask = newCeTaskBuilder().setStatus(CeTask.Status.FAILED).setId("#1").build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(ceTask, null);

    WebhookPayload payload = WebhookPayload.from(analysis);

    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.toJson()).isSimilarTo(getClass().getResource("WebhookPayloadTest/failed.json"));
  }

  @Test
  public void create_payload_with_gate_conditions_without_value() {
    CeTask task = newCeTaskBuilder()
      .setStatus(CeTask.Status.SUCCESS)
      .setId("#1")
      .build();
    QualityGate gate = newQualityGateBuilder()
      .setId("G1")
      .setName("Gate One")
      .setStatus(QualityGate.Status.WARN)
      .add(newConditionBuilder()
        .setMetricKey("coverage")
        .setOperator(QualityGate.Operator.GREATER_THAN)
        .setWarningThreshold("75.0")
        .setErrorThreshold("70.0")
        .buildNoValue())
      .build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, gate);

    WebhookPayload payload = WebhookPayload.from(analysis);
    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.toJson()).isSimilarTo(getClass().getResource("WebhookPayloadTest/gate_condition_without_value.json"));
  }

  private static PostProjectAnalysisTask.ProjectAnalysis newAnalysis(CeTask task, @Nullable QualityGate gate) {
    return new PostProjectAnalysisTask.ProjectAnalysis() {
      @Override
      public CeTask getCeTask() {
        return task;
      }

      @Override
      public Project getProject() {
        return newProjectBuilder()
          .setUuid("P1_UUID")
          .setKey(PROJECT_KEY)
          .setName("Project One")
          .build();
      }

      @Override
      public QualityGate getQualityGate() {
        return gate;
      }

      @Override
      public Date getDate() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Optional<Date> getAnalysisDate() {
        return Optional.of(new Date(1_500_000_000_000L));
      }

      @Override
      public ScannerContext getScannerContext() {
        return newScannerContextBuilder().build();
      }
    };
  }

}
