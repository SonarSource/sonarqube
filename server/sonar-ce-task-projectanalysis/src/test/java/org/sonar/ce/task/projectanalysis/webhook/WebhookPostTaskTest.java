/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.webhook;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.LogStatistics;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.server.qualitygate.Condition;
import org.sonar.server.qualitygate.EvaluatedCondition;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.webhook.Analysis;
import org.sonar.server.webhook.ProjectAnalysis;
import org.sonar.server.webhook.WebHooks;
import org.sonar.server.webhook.WebhookPayload;
import org.sonar.server.webhook.WebhookPayloadFactory;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester.newBranchBuilder;
import static org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
import static org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester.newConditionBuilder;
import static org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
import static org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester.newQualityGateBuilder;
import static org.sonar.api.testfixtures.posttask.PostProjectAnalysisTaskTester.newScannerContextBuilder;

public class WebhookPostTaskTest {

  private final Random random = new Random();
  private final Configuration configuration = mock(Configuration.class);
  private final WebhookPayload webhookPayload = mock(WebhookPayload.class);
  private final WebhookPayloadFactory payloadFactory = mock(WebhookPayloadFactory.class);
  private final WebHooks webHooks = mock(WebHooks.class);
  private final ConfigurationRepository configurationRepository = mock(ConfigurationRepository.class);
  private WebhookPostTask underTest = new WebhookPostTask(payloadFactory, webHooks);

  @Before
  public void wireMocks() {
    when(payloadFactory.create(any(ProjectAnalysis.class))).thenReturn(webhookPayload);
    when(configurationRepository.getConfiguration()).thenReturn(configuration);
  }

  @Test
  public void has_description() {
    assertThat(underTest.getDescription()).isNotEmpty();
  }

  @Test
  public void call_webhooks_when_no_analysis_not_qualitygate() {
    callWebHooks(null, null);
  }

  @Test
  public void call_webhooks_with_analysis_and_qualitygate() {
    QualityGate.Condition condition = newConditionBuilder()
      .setMetricKey(secure().nextAlphanumeric(96))
      .setOperator(QualityGate.Operator.LESS_THAN)
      .setErrorThreshold(secure().nextAlphanumeric(22))
      .build(QualityGate.EvaluationStatus.OK, secure().nextAlphanumeric(33));
    QualityGate qualityGate = newQualityGateBuilder()
      .setId(secure().nextAlphanumeric(23))
      .setName(secure().nextAlphanumeric(66))
      .setStatus(QualityGate.Status.values()[random.nextInt(QualityGate.Status.values().length)])
      .add(condition)
      .build();

    callWebHooks(secure().nextAlphanumeric(40), qualityGate);
  }

  private void callWebHooks(@Nullable String analysisUUid, @Nullable QualityGate qualityGate) {
    Project project = newProjectBuilder()
      .setUuid(secure().nextAlphanumeric(3))
      .setKey(secure().nextAlphanumeric(4))
      .setName(secure().nextAlphanumeric(5))
      .build();
    CeTask ceTask = newCeTaskBuilder()
      .setStatus(CeTask.Status.values()[random.nextInt(CeTask.Status.values().length)])
      .setId(secure().nextAlphanumeric(6))
      .build();
    Date date = new Date();
    Map<String, String> properties = ImmutableMap.of(secure().nextAlphanumeric(17), secure().nextAlphanumeric(18));
    Branch branch = newBranchBuilder()
      .setIsMain(random.nextBoolean())
      .setType(Branch.Type.values()[random.nextInt(Branch.Type.values().length)])
      .setName(secure().nextAlphanumeric(29))
      .build();

    PostProjectAnalysisTaskTester.of(underTest)
      .at(date)
      .withCeTask(ceTask)
      .withProject(project)
      .withBranch(branch)
      .withQualityGate(qualityGate)
      .withScannerContext(newScannerContextBuilder()
        .addProperties(properties)
        .build())
      .withAnalysisUuid(analysisUUid)
      .withQualityGate(qualityGate)
      .execute();

    ArgumentCaptor<Supplier> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
    verify(webHooks)
      .sendProjectAnalysisUpdate(
        eq(new WebHooks.Analysis(project.getUuid(),
          analysisUUid,
          ceTask.getId())),
        supplierCaptor.capture(),
        any(LogStatistics.class));

    assertThat(supplierCaptor.getValue().get()).isSameAs(webhookPayload);

    EvaluatedQualityGate webQualityGate = null;
    if (qualityGate != null) {
      QualityGate.Condition condition = qualityGate.getConditions().iterator().next();
      Condition qgCondition = new Condition(
        condition.getMetricKey(),
        Condition.Operator.valueOf(condition.getOperator().name()),
        condition.getErrorThreshold());
      webQualityGate = EvaluatedQualityGate.newBuilder()
        .setQualityGate(new org.sonar.server.qualitygate.QualityGate(qualityGate.getId(), qualityGate.getName(), Collections.singleton(qgCondition)))
        .setStatus(Metric.Level.valueOf(qualityGate.getStatus().name()))
        .addEvaluatedCondition(qgCondition, EvaluatedCondition.EvaluationStatus.valueOf(condition.getStatus().name()), condition.getValue())
        .build();
    }

    verify(payloadFactory).create(new ProjectAnalysis(
      new org.sonar.server.webhook.Project(project.getUuid(), project.getKey(), project.getName()),
      new org.sonar.server.webhook.CeTask(ceTask.getId(), org.sonar.server.webhook.CeTask.Status.valueOf(ceTask.getStatus().name())),
      analysisUUid == null ? null : new Analysis(analysisUUid, date.getTime(), null),
      new org.sonar.server.webhook.Branch(branch.isMain(), branch.getName().get(), org.sonar.server.webhook.Branch.Type.valueOf(branch.getType().name())),
      webQualityGate,
      analysisUUid == null ? null : date.getTime(),
      properties));
  }
}
