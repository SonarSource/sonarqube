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
package org.sonar.scanner.qualitygate;

import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import org.sonar.api.Startable;
import org.sonar.api.utils.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.report.CeTaskReportDataHolder;
import org.sonar.scanner.scan.ScanProperties;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.TaskStatus;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Qualitygates;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse.Status;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

public class QualityGateCheck implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(QualityGateCheck.class);
  private static final EnumSet<TaskStatus> TASK_TERMINAL_STATUSES = EnumSet.of(TaskStatus.SUCCESS, TaskStatus.FAILED, TaskStatus.CANCELED);
  private static final int POLLING_INTERVAL_IN_MS = 5000;

  private final DefaultScannerWsClient wsClient;
  private final GlobalAnalysisMode analysisMode;
  private final CeTaskReportDataHolder ceTaskReportDataHolder;
  private final ScanProperties properties;

  private long qualityGateTimeoutInMs;
  private boolean enabled;

  public QualityGateCheck(DefaultScannerWsClient wsClient, GlobalAnalysisMode analysisMode, CeTaskReportDataHolder ceTaskReportDataHolder,
    ScanProperties properties) {
    this.wsClient = wsClient;
    this.properties = properties;
    this.ceTaskReportDataHolder = ceTaskReportDataHolder;
    this.analysisMode = analysisMode;
  }

  @Override
  public void start() {
    this.enabled = properties.shouldWaitForQualityGate();
    this.qualityGateTimeoutInMs = Duration.of(properties.qualityGateWaitTimeout(), ChronoUnit.SECONDS).toMillis();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public void await() {
    if (!enabled) {
      LOG.debug("Quality Gate check disabled - skipping");
      return;
    }

    if (analysisMode.isMediumTest()) {
      throw new IllegalStateException("Quality Gate check not available in medium test mode");
    }

    LOG.info("Waiting for the analysis report to be processed (max {}s)", properties.qualityGateWaitTimeout());
    String taskId = ceTaskReportDataHolder.getCeTaskId();

    Ce.Task task = waitForCeTaskToFinish(taskId);

    if (!TaskStatus.SUCCESS.equals(task.getStatus())) {
      throw MessageException.of(String.format("CE Task finished abnormally with status: %s, you can check details here: %s",
        task.getStatus().name(), ceTaskReportDataHolder.getCeTaskUrl()));
    }

    Status qualityGateStatus = getQualityGateStatus(task.getAnalysisId());

    if (Status.OK.equals(qualityGateStatus)) {
      LOG.info("QUALITY GATE STATUS: PASSED - View details on " + ceTaskReportDataHolder.getDashboardUrl());
    } else {
      throw MessageException.of("QUALITY GATE STATUS: FAILED - View details on " + ceTaskReportDataHolder.getDashboardUrl());
    }
  }

  private Ce.Task waitForCeTaskToFinish(String taskId) {
    GetRequest getTaskResultReq = new GetRequest("api/ce/task")
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("id", taskId);

    long currentTime = 0;
    while (qualityGateTimeoutInMs > currentTime) {
      try {
        WsResponse getTaskResultResponse = wsClient.call(getTaskResultReq).failIfNotSuccessful();
        Ce.Task task = parseCeTaskResponse(getTaskResultResponse);
        if (TASK_TERMINAL_STATUSES.contains(task.getStatus())) {
          return task;
        }

        Thread.sleep(POLLING_INTERVAL_IN_MS);
        currentTime += POLLING_INTERVAL_IN_MS;
      } catch (HttpException e) {
        throw MessageException.of(String.format("Failed to get CE Task status - %s", DefaultScannerWsClient.createErrorMessage(e)));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Quality Gate check has been interrupted", e);
      }
    }
    throw MessageException.of("Quality Gate check timeout exceeded - View details on " + ceTaskReportDataHolder.getDashboardUrl());
  }

  private static Ce.Task parseCeTaskResponse(WsResponse response) {
    try (InputStream protobuf = response.contentStream()) {
      return Ce.TaskResponse.parser().parseFrom(protobuf).getTask();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse response from " + response.requestUrl(), e);
    }
  }

  private Status getQualityGateStatus(String analysisId) {
    GetRequest getQualityGateReq = new GetRequest("api/qualitygates/project_status")
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("analysisId", analysisId);
    try {
      WsResponse getTaskResultResponse = wsClient.call(getQualityGateReq).failIfNotSuccessful();
      Qualitygates.ProjectStatusResponse.ProjectStatus status = parseQualityGateResponse(getTaskResultResponse);
      return status.getStatus();
    } catch (HttpException e) {
      throw MessageException.of(String.format("Failed to get Quality Gate status - %s", DefaultScannerWsClient.createErrorMessage(e)));
    }
  }

  private static Qualitygates.ProjectStatusResponse.ProjectStatus parseQualityGateResponse(WsResponse response) {
    try (InputStream protobuf = response.contentStream()) {
      return Qualitygates.ProjectStatusResponse.parser().parseFrom(protobuf).getProjectStatus();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse response from " + response.requestUrl(), e);
    }
  }
}
