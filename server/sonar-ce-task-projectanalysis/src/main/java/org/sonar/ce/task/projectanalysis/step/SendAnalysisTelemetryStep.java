/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.telemetry.StepsTelemetryHolder;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactory;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.MessageSerializer;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.core.schema.AnalysisMetric;
import org.sonar.telemetry.core.schema.BaseMessage;
import org.sonar.telemetry.core.schema.Metric;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

public class SendAnalysisTelemetryStep implements ComputationStep {

  private static final int MAX_METRICS = 1000;

  private final TelemetryClient telemetryClient;
  private final ScannerReportReader scannerReportReader;
  private final Server server;
  private final UuidFactory uuidFactory;
  private final Configuration config;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final StepsTelemetryHolder stepsTelemetryHolder;

  public SendAnalysisTelemetryStep(TelemetryClient telemetryClient, ScannerReportReader scannerReportReader,
    UuidFactory uuidFactory, Server server, Configuration configuration, AnalysisMetadataHolder analysisMetadataHolder,
    StepsTelemetryHolder stepsTelemetryHolder) {
    this.telemetryClient = telemetryClient;
    this.scannerReportReader = scannerReportReader;
    this.server = server;
    this.uuidFactory = uuidFactory;
    this.config = configuration;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.stepsTelemetryHolder = stepsTelemetryHolder;
  }

  @Override
  public void execute(Context context) {
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }

    String projectUuid = analysisMetadataHolder.getProject().getUuid();
    String analysisType = analysisMetadataHolder.isPullRequest() ? "pull_request" : "branch";
    String analysisUuid = uuidFactory.create();

    // it was agreed to limit the number of telemetry entries to 1000 per one analysis among scanner report metrics and step metrics
    Set<Metric> scannerReportMetrics = getScannerReportMetrics(projectUuid, analysisType, analysisUuid);
    Set<Metric> stepsStatisticsMetrics = getStepsTelemetryMetrics(projectUuid, analysisType, analysisUuid, MAX_METRICS - scannerReportMetrics.size(),
      stepsTelemetryHolder.getTelemetryMetrics());

    Set<Metric> metrics = new HashSet<>();
    metrics.addAll(scannerReportMetrics);
    metrics.addAll(stepsStatisticsMetrics);
    sendMetrics(metrics);
  }

  private void sendMetrics(Set<Metric> metrics) {
    if (metrics.isEmpty()) {
      return;
    }
    BaseMessage baseMessage = new BaseMessage.Builder()
      .setMessageUuid(uuidFactory.create())
      .setInstallationId(server.getId())
      .setDimension(Dimension.ANALYSIS)
      .setMetrics(metrics)
      .build();

    String jsonString = MessageSerializer.serialize(baseMessage);
    telemetryClient.uploadMetricAsync(jsonString);
  }

  private Set<Metric> getScannerReportMetrics(String projectUuid, String analysisType, String analysisUuid) {
    Set<Metric> metrics = new HashSet<>();
    try (CloseableIterator<ScannerReport.TelemetryEntry> it = scannerReportReader.readTelemetryEntries()) {
      int count = 0;
      while (it.hasNext() && count < MAX_METRICS) {
        ScannerReport.TelemetryEntry telemetryEntry = it.next();
        metrics.add(new AnalysisMetric(telemetryEntry.getKey(), telemetryEntry.getValue(), projectUuid, analysisType, analysisUuid));
        count++;
      }
    }
    return metrics;
  }

  private static Set<Metric> getStepsTelemetryMetrics(String projectUuid, String analysisType, String analysisUuid, int maxMetrics, Map<String, Object> telemetryMetrics) {
    return telemetryMetrics.entrySet().stream()
      .map(entry ->
        new AnalysisMetric(entry.getKey(), String.valueOf(entry.getValue()), projectUuid, analysisType, analysisUuid)
      )
      .limit(maxMetrics)
      .collect(Collectors.toSet());
  }

  @Override
  public String getDescription() {
    return "This step pushes telemetry data from the Sonar analyzers to Telemetry V2 server in case telemetry is enabled.";
  }
}
