/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.webhook;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.server.computation.task.projectanalysis.api.posttask.BranchImpl;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newConditionBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newQualityGateBuilder;
import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newScannerContextBuilder;
import static org.sonar.test.JsonAssert.assertJson;

public class WebhookPayloadFactoryImplTest {

  private static final String PROJECT_KEY = "P1";

  private Server server = mock(Server.class);
  private System2 system2 = mock(System2.class);
  private WebhookPayloadFactory underTest = new WebhookPayloadFactoryImpl(server, system2);

  @Before
  public void setUp() throws Exception {
    when(server.getPublicRootUrl()).thenReturn("http://foo");
    when(system2.now()).thenReturn(1_500_999L);
  }

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
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, gate, null, 1_500_000_000_000L, emptyMap());

    WebhookPayload payload = underTest.create(analysis);
    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "  \"serverUrl\": \"http://foo\"," +
        "  \"taskId\": \"#1\"," +
        "  \"status\": \"SUCCESS\"," +
        "  \"analysedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"changedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"project\": {" +
        "    \"key\": \"P1\"," +
        "    \"name\": \"Project One\"," +
        "    \"url\": \"http://foo/project/dashboard?id=P1\"" +
        "  }," +
        "  \"qualityGate\": {" +
        "    \"name\": \"Gate One\"," +
        "    \"status\": \"WARN\"," +
        "    \"conditions\": [" +
        "      {" +
        "        \"metric\": \"coverage\"," +
        "        \"operator\": \"GREATER_THAN\"," +
        "        \"value\": \"74.0\"," +
        "        \"status\": \"WARN\"," +
        "        \"onLeakPeriod\": true," +
        "        \"errorThreshold\": \"70.0\"," +
        "        \"warningThreshold\": \"75.0\"" +
        "      }" +
        "    ]" +
        "  }," +
        "  \"properties\": {" +
        "  }" +
        "}");
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
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, gate, null, 1_500_000_000_000L, emptyMap());

    WebhookPayload payload = underTest.create(analysis);
    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "  \"serverUrl\": \"http://foo\"," +
        "  \"taskId\": \"#1\"," +
        "  \"status\": \"SUCCESS\"," +
        "  \"analysedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"changedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"project\": {" +
        "    \"key\": \"P1\"," +
        "    \"name\": \"Project One\"," +
        "    \"url\": \"http://foo/project/dashboard?id=P1\"" +
        "  }," +
        "  \"qualityGate\": {" +
        "    \"name\": \"Gate One\"," +
        "    \"status\": \"WARN\"," +
        "    \"conditions\": [" +
        "      {" +
        "        \"metric\": \"coverage\"," +
        "        \"operator\": \"GREATER_THAN\"," +
        "        \"status\": \"NO_VALUE\"," +
        "        \"onLeakPeriod\": false," +
        "        \"errorThreshold\": \"70.0\"," +
        "        \"warningThreshold\": \"75.0\"" +
        "      }" +
        "    ]" +
        "  }" +
        "}");
  }

  @Test
  public void create_payload_with_analysis_properties() {
    CeTask task = newCeTaskBuilder()
      .setStatus(CeTask.Status.SUCCESS)
      .setId("#1")
      .build();
    QualityGate gate = newQualityGateBuilder()
      .setId("G1")
      .setName("Gate One")
      .setStatus(QualityGate.Status.WARN)
      .build();
    Map<String, String> scannerProperties = ImmutableMap.of(
      "sonar.analysis.revision", "ab45d24",
      "sonar.analysis.buildNumber", "B123",
      "not.prefixed.with.sonar.analysis", "should be ignored",
      "ignored", "should be ignored too");
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, gate, null, 1_500_000_000_000L, scannerProperties);

    WebhookPayload payload = underTest.create(analysis);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "  \"serverUrl\": \"http://foo\"," +
        "  \"taskId\": \"#1\"," +
        "  \"status\": \"SUCCESS\"," +
        "  \"analysedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"changedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"project\": {" +
        "    \"key\": \"P1\"," +
        "    \"name\": \"Project One\"," +
        "    \"url\": \"http://foo/project/dashboard?id=P1\"" +
        "  }," +
        "  \"qualityGate\": {" +
        "    \"name\": \"Gate One\"," +
        "    \"status\": \"WARN\"," +
        "    \"conditions\": [" +
        "    ]" +
        "  }," +
        "  \"properties\": {" +
        "    \"sonar.analysis.revision\": \"ab45d24\"," +
        "    \"sonar.analysis.buildNumber\": \"B123\"" +
        "  }" +
        "}");
    assertThat(payload.getJson())
      .doesNotContain("not.prefixed.with.sonar.analysis")
      .doesNotContain("ignored");
  }

  @Test
  public void create_payload_for_failed_analysis() {
    CeTask ceTask = newCeTaskBuilder().setStatus(CeTask.Status.FAILED).setId("#1").build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(ceTask, null, null, 1_500_000_000_000L, emptyMap());

    WebhookPayload payload = underTest.create(analysis);

    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "  \"serverUrl\": \"http://foo\"," +
        "  \"taskId\": \"#1\"," +
        "  \"status\": \"FAILED\"," +
        "  \"changedAt\": \"2017-07-14T04:40:00+0200\"," +
        "  \"project\": {" +
        "    \"key\": \"P1\"," +
        "    \"name\": \"Project One\"," +
        "    \"url\": \"http://foo/project/dashboard?id=P1\"" +
        "  }," +
        "  \"properties\": {" +
        "  }" +
        "}");
  }

  @Test
  public void create_payload_for_no_analysis_date() {
    CeTask ceTask = newCeTaskBuilder().setStatus(CeTask.Status.FAILED).setId("#1").build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(ceTask, null, null, null, emptyMap());

    WebhookPayload payload = underTest.create(analysis);

    assertThat(payload.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "  \"serverUrl\": \"http://foo\"," +
        "  \"taskId\": \"#1\"," +
        "  \"status\": \"FAILED\"," +
        "  \"changedAt\": \"1970-01-01T01:25:00+0100\"," +
        "  \"project\": {" +
        "    \"key\": \"P1\"," +
        "    \"name\": \"Project One\"" +
        "  }," +
        "  \"properties\": {" +
        "  }" +
        "}");
  }

  @Test
  public void create_payload_on_short_branch() {
    CeTask task = newCeTaskBuilder()
      .setStatus(CeTask.Status.SUCCESS)
      .setId("#1")
      .build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, null, new BranchImpl(false, "feature/foo", Branch.Type.SHORT), 1_500_000_000_000L, emptyMap());

    WebhookPayload payload = underTest.create(analysis);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "\"branch\": {" +
        "  \"name\": \"feature/foo\"" +
        "  \"type\": \"SHORT\"" +
        "  \"isMain\": false," +
        "  \"url\": \"http://foo/project/issues?branch=feature%2Ffoo&id=P1&resolved=false\"" +
        "}" +
        "}");
  }

  @Test
  public void create_payload_on_long_branch() {
    CeTask task = newCeTaskBuilder()
      .setStatus(CeTask.Status.SUCCESS)
      .setId("#1")
      .build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, null, new BranchImpl(false, "feature/foo", Branch.Type.LONG), 1_500_000_000_000L, emptyMap());

    WebhookPayload payload = underTest.create(analysis);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "\"branch\": {" +
        "  \"name\": \"feature/foo\"" +
        "  \"type\": \"LONG\"" +
        "  \"isMain\": false," +
        "  \"url\": \"http://foo/project/dashboard?branch=feature%2Ffoo&id=P1\"" +
        "}" +
        "}");
  }

  @Test
  public void create_payload_on_main_branch_without_name() {
    CeTask task = newCeTaskBuilder()
      .setStatus(CeTask.Status.SUCCESS)
      .setId("#1")
      .build();
    PostProjectAnalysisTask.ProjectAnalysis analysis = newAnalysis(task, null, new BranchImpl(true, null, Branch.Type.LONG), 1_500_000_000_000L, emptyMap());

    WebhookPayload payload = underTest.create(analysis);
    assertJson(payload.getJson())
      .isSimilarTo("{" +
        "\"branch\": {" +
        "  \"type\": \"LONG\"" +
        "  \"isMain\": true," +
        "  \"url\": \"http://foo/project/dashboard?id=P1\"" +
        "}" +
        "}");
  }

  private static PostProjectAnalysisTask.ProjectAnalysis newAnalysis(CeTask task, @Nullable QualityGate gate,
    @Nullable Branch branch, @Nullable Long analysisDate, Map<String, String> scannerProperties) {
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
      public Optional<Branch> getBranch() {
        return Optional.ofNullable(branch);
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
        return Optional.ofNullable(analysisDate).map(Date::new);
      }

      @Override
      public ScannerContext getScannerContext() {
        return newScannerContextBuilder()
          .addProperties(scannerProperties)
          .build();
      }
    };
  }

}
