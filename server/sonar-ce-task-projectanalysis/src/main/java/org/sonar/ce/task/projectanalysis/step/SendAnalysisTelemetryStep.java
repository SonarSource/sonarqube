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
package org.sonar.ce.task.projectanalysis.step;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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

import static java.util.Locale.ENGLISH;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

public class SendAnalysisTelemetryStep implements ComputationStep {

  private static final int MAX_METRICS = 1000;

  private static final Pattern EXTENSION_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9_.]");

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

    MetricsBuilder builder = new MetricsBuilder();

    if (analysisMetadataHolder.getBranch().isMain()) {
      builder.addMetrics(() -> getNotAnalyzedIndexedFileCountMetrics(projectUuid, analysisType, analysisUuid));
      builder.addMetrics(() -> getAnalyzedIndexedFileCountMetrics(projectUuid, analysisType, analysisUuid));
    }

    builder
      .addMetrics(() -> getScannerReportMetrics(projectUuid, analysisType, analysisUuid))
      .addMetrics(() -> getStepsTelemetryMetrics(projectUuid, analysisType, analysisUuid, stepsTelemetryHolder.getTelemetryMetrics()));

    sendMetrics(builder.build());
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

  private Set<Metric> getNotAnalyzedIndexedFileCountMetrics(String projectUuid, String analysisType, String analysisUuid) {
    Set<Metric> metrics = new HashSet<>();
    ScannerReport.Metadata metadata = scannerReportReader.readMetadata();

    metadata.getNotAnalyzedIndexedFileCountPerTypeMap().forEach((extension, count) -> {
      // The telemetry key does not support all characters
      String key = String.format("indexed_files.%s.unanalyzed.total", sanitizeExtension(extension));
      metrics.add(new AnalysisMetric(key, String.valueOf(count), projectUuid, analysisType, analysisUuid));
    });

    return metrics;
  }

  private Set<Metric> getAnalyzedIndexedFileCountMetrics(String projectUuid, String analysisType, String analysisUuid) {
    Set<Metric> metrics = new HashSet<>();
    ScannerReport.Metadata metadata = scannerReportReader.readMetadata();

    metadata.getAnalyzedIndexedFileCountPerTypeMap().forEach((extension, count) -> {
      // The telemetry key does not support all characters
      String key = String.format("indexed_files.%s.analyzed.total", sanitizeExtension(extension));
      metrics.add(new AnalysisMetric(key, String.valueOf(count), projectUuid, analysisType, analysisUuid));
    });

    return metrics;
  }

  private static String sanitizeExtension(String extension) {
    return EXTENSION_SANITIZE_PATTERN.matcher(extension)
      .replaceAll("_")
      .toLowerCase(ENGLISH);
  }

  private Set<Metric> getScannerReportMetrics(String projectUuid, String analysisType, String analysisUuid) {
    Set<Metric> metrics = new HashSet<>();
    try (CloseableIterator<ScannerReport.TelemetryEntry> it = scannerReportReader.readTelemetryEntries()) {
      while (it.hasNext()) {
        ScannerReport.TelemetryEntry telemetryEntry = it.next();
        metrics.add(new AnalysisMetric(telemetryEntry.getKey(), telemetryEntry.getValue(), projectUuid, analysisType, analysisUuid));
      }
    }
    return metrics;
  }

  private static Set<Metric> getStepsTelemetryMetrics(String projectUuid, String analysisType, String analysisUuid, Map<String, Object> telemetryMetrics) {
    return telemetryMetrics.entrySet().stream()
      .map(entry ->
        new AnalysisMetric(entry.getKey(), String.valueOf(entry.getValue()), projectUuid, analysisType, analysisUuid)
      )
      .collect(Collectors.toSet());
  }

  @Override
  public String getDescription() {
    return "This step pushes telemetry data from the Sonar analyzers to Telemetry V2 server in case telemetry is enabled.";
  }

  /**
   * <p>it was agreed to limit the number of telemetry entries to 1000 per one analysis.</p>
   * <p>The order of adding providers is important as earlier added providers have higher chance to have all their metrics included.</p>
   * <p>This builder is responsible for</p>
   * <ul>
   *   <li> ensuring that no more than {@link #MAX_METRICS} are collected
   *   <li> Collecting metrics from multiple providers </li>
   *   <li> Taking partial metrics from providers when capacity is exhausted </li>
   * </ul>
   */
  static class MetricsBuilder {
    private final Set<Metric> metrics = new HashSet<>();
    private int remainingCapacity = MAX_METRICS;

    public MetricsBuilder addMetrics(Supplier<Set<Metric>> provider) {
      if (remainingCapacity <= 0) {
        return this;
      }

      Set<Metric> providedMetrics = provider.get();
      int toAdd = Math.min(providedMetrics.size(), remainingCapacity);

      metrics.addAll(providedMetrics.stream()
        .limit(toAdd)
        .collect(Collectors.toSet()));

      remainingCapacity -= toAdd;
      return this;
    }

    public Set<Metric> build() {
      return metrics;
    }
  }
}
