/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.qualitygate;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.report.CeTaskReportDataHolder;
import org.sonar.scanner.scan.ScanProperties;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.TaskStatus;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse.Status;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.MockWsResponse;
import org.sonarqube.ws.client.WsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualityGateCheckTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DefaultScannerWsClient wsClient;
  @Mock
  private GlobalAnalysisMode analysisMode;
  @Mock
  private CeTaskReportDataHolder reportMetadataHolder;
  @Mock
  private ScanProperties properties;

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private QualityGateCheck underTest;

  @BeforeEach
  void before() {
    underTest = new QualityGateCheck(wsClient, analysisMode, reportMetadataHolder, properties);
    underTest.setPollingIntervalInSeconds(200);
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  void should_pass_if_quality_gate_ok() {
    when(reportMetadataHolder.getCeTaskId()).thenReturn("task-1234");
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("http://dashboard-url.com");
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(ceTaskWsResponse).when(wsClient).call(newGetCeTaskRequest());

    MockWsResponse qualityGateResponse = getQualityGateWsResponse(Status.OK);
    doReturn(qualityGateResponse).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    underTest.await();

    underTest.stop();

    assertThat(logTester.logs(Level.INFO))
      .contains(
        "Waiting for the analysis report to be processed (max 5s)",
        "QUALITY GATE STATUS: PASSED - View details on http://dashboard-url.com");
  }

  @Test
  void should_wait_and_then_pass_if_quality_gate_ok() {
    when(reportMetadataHolder.getCeTaskId()).thenReturn("task-1234");
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("http://dashboard-url.com");
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(10);

    MockWsResponse pendingTask = getCeTaskWsResponse(TaskStatus.PENDING);
    MockWsResponse successTask = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(pendingTask, successTask).when(wsClient).call(newGetCeTaskRequest());

    MockWsResponse qualityGateResponse = getQualityGateWsResponse(Status.OK);
    doReturn(qualityGateResponse).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    underTest.await();

    assertThat(logTester.logs())
      .contains("QUALITY GATE STATUS: PASSED - View details on http://dashboard-url.com");
  }

  @Test
  void should_fail_if_quality_gate_none() {
    when(reportMetadataHolder.getCeTaskId()).thenReturn("task-1234");
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("http://dashboard-url.com");
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(ceTaskWsResponse).when(wsClient).call(newGetCeTaskRequest());

    MockWsResponse qualityGateResponse = getQualityGateWsResponse(Status.ERROR);
    doReturn(qualityGateResponse).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessage("QUALITY GATE STATUS: FAILED - View details on http://dashboard-url.com");
  }

  @Test
  void should_fail_if_quality_gate_error() {
    when(reportMetadataHolder.getCeTaskId()).thenReturn("task-1234");
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("http://dashboard-url.com");
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(ceTaskWsResponse).when(wsClient).call(newGetCeTaskRequest());

    MockWsResponse qualityGateResponse = getQualityGateWsResponse(Status.ERROR);
    doReturn(qualityGateResponse).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessage("QUALITY GATE STATUS: FAILED - View details on http://dashboard-url.com");
  }

  @Test
  void should_wait_and_then_fail_if_quality_gate_error() {
    when(reportMetadataHolder.getCeTaskId()).thenReturn("task-1234");
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("http://dashboard-url.com");
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(10);

    MockWsResponse pendingTask = getCeTaskWsResponse(TaskStatus.PENDING);
    MockWsResponse successTask = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(pendingTask, successTask).when(wsClient).call(newGetCeTaskRequest());

    MockWsResponse qualityGateResponse = getQualityGateWsResponse(Status.ERROR);
    doReturn(qualityGateResponse).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessage("QUALITY GATE STATUS: FAILED - View details on http://dashboard-url.com");
  }

  @Test
  void should_fail_if_quality_gate_timeout_exceeded() {
    when(reportMetadataHolder.getCeTaskId()).thenReturn("task-1234");
    when(reportMetadataHolder.getDashboardUrl()).thenReturn("http://dashboard-url.com");
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(1);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(TaskStatus.PENDING);
    doReturn(ceTaskWsResponse).when(wsClient).call(newGetCeTaskRequest());

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessage("Quality Gate check timeout exceeded - View details on http://dashboard-url.com");
  }

  @Test
  void should_fail_if_cant_call_ws_for_quality_gate() {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(ceTaskWsResponse).when(wsClient).call(newGetCeTaskRequest());

    doThrow(new HttpException("quality-gate-url", 400, "content")).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessage("Failed to get Quality Gate status - HTTP code 400: content");
  }

  @Test
  void should_fail_if_invalid_response_from_quality_gate_ws() {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(TaskStatus.SUCCESS);
    doReturn(ceTaskWsResponse).when(wsClient).call(newGetCeTaskRequest());

    MockWsResponse qualityGateResponse = new MockWsResponse();
    qualityGateResponse.setRequestUrl("quality-gate-url");
    qualityGateResponse.setContent("blabla");
    doReturn(qualityGateResponse).when(wsClient).call(newGetQualityGateRequest());

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse response from quality-gate-url");
  }

  @Test
  void should_fail_if_cant_call_ws_for_task() {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    when(wsClient.call(newGetCeTaskRequest())).thenThrow(new HttpException("task-url", 400, "content"));

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessage("Failed to get CE Task status - HTTP code 400: content");
  }

  @Test
  void should_fail_if_invalid_response_from_ws_task() {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse getCeTaskRequest = new MockWsResponse();
    getCeTaskRequest.setRequestUrl("ce-task-url");
    getCeTaskRequest.setContent("blabla");

    when(wsClient.call(newGetCeTaskRequest())).thenReturn(getCeTaskRequest);

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse response from ce-task-url");
  }

  @ParameterizedTest
  @MethodSource("ceTaskNotSucceededStatuses")
  void should_fail_if_task_not_succeeded(TaskStatus taskStatus) {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(properties.qualityGateWaitTimeout()).thenReturn(5);

    MockWsResponse ceTaskWsResponse = getCeTaskWsResponse(taskStatus);
    when(wsClient.call(newGetCeTaskRequest())).thenReturn(ceTaskWsResponse);

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("CE Task finished abnormally with status: " + taskStatus.name());
  }

  private WsRequest newGetCeTaskRequest() {
    return argThat(new WsRequestPathMatcher("api/ce/task"));
  }

  private MockWsResponse getCeTaskWsResponse(TaskStatus status) {
    MockWsResponse submitMockResponse = new MockWsResponse();
    submitMockResponse.setContent(Ce.TaskResponse.newBuilder()
      .setTask(Ce.Task.newBuilder().setStatus(status))
      .build()
      .toByteArray());
    return submitMockResponse;
  }

  @Test
  void should_skip_wait_if_disabled() {
    when(properties.shouldWaitForQualityGate()).thenReturn(false);

    underTest.start();

    underTest.await();

    assertThat(logTester.logs())
      .contains("Quality Gate check disabled - skipping");
  }

  @Test
  void should_fail_if_enabled_with_medium_test() {
    when(properties.shouldWaitForQualityGate()).thenReturn(true);
    when(analysisMode.isMediumTest()).thenReturn(true);

    underTest.start();

    assertThatThrownBy(() -> underTest.await())
      .isInstanceOf(IllegalStateException.class);
  }

  private WsRequest newGetQualityGateRequest() {
    return argThat(new WsRequestPathMatcher("api/qualitygates/project_status"));
  }

  private MockWsResponse getQualityGateWsResponse(Status status) {
    MockWsResponse qualityGateWsResponse = new MockWsResponse();
    qualityGateWsResponse.setContent(Qualitygates.ProjectStatusResponse.newBuilder()
      .setProjectStatus(Qualitygates.ProjectStatusResponse.ProjectStatus.newBuilder()
        .setStatus(status)
        .build())
      .build()
      .toByteArray());
    return qualityGateWsResponse;
  }

  private static Stream<TaskStatus> ceTaskNotSucceededStatuses() {
    return Stream.of(
      TaskStatus.CANCELED,
      TaskStatus.FAILED
    );
  }

  private static class WsRequestPathMatcher implements ArgumentMatcher<WsRequest> {
    String path;

    WsRequestPathMatcher(String path) {
      this.path = path;
    }

    @Override
    public boolean matches(WsRequest right) {
      return path.equals(right.getPath());
    }
  }
}
