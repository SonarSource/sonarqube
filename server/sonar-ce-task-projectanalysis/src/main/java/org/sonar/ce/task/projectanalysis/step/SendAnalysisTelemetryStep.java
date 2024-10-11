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
package org.sonar.ce.task.projectanalysis.step;

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.ComputationStep;
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

  private final TelemetryClient telemetryClient;
  private final BatchReportReader batchReportReader;
  private final Server server;
  private final UuidFactory uuidFactory;
  private final Configuration config;

  public SendAnalysisTelemetryStep(TelemetryClient telemetryClient, BatchReportReader batchReportReader,
    UuidFactory uuidFactory, Server server, Configuration configuration) {
    this.telemetryClient = telemetryClient;
    this.batchReportReader = batchReportReader;
    this.server = server;
    this.uuidFactory = uuidFactory;
    this.config = configuration;
  }

  @Override
  public void execute(Context context) {
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }
    try (CloseableIterator<ScannerReport.TelemetryEntry> it = batchReportReader.readTelemetryEntries()) {
      Set<Metric> metrics = new HashSet<>();
      // it was agreed to limit the number of telemetry entries to 1000 per one analysis
      final int limit = 1000;
      int count = 0;
      while (it.hasNext() && count++ < limit) {
        ScannerReport.TelemetryEntry telemetryEntry = it.next();
        metrics.add(new AnalysisMetric(telemetryEntry.getKey(), telemetryEntry.getValue()));
      }

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

  }

  @Override
  public String getDescription() {
    return "This step pushes telemetry data from the Sonar analyzers to Telemetry V2 server in case telemetry is enabled.";
  }
}