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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.telemetry.StepsTelemetryHolder;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactory;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;
import org.sonar.telemetry.core.TelemetryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.step.SendAnalysisTelemetryStep.ANALYZED_LINES_OF_CODE_METRIC_KEY;
import static org.sonar.ce.task.projectanalysis.step.SendAnalysisTelemetryStep.ANALYZED_LINES_OF_CODE_NOT_SET_VALUE;

class SendAnalysisTelemetryStepTest {

  private static final int MAX_METRICS = 1000;

  private final TelemetryClient telemetryClient = mock();
  private final ScannerReportReader scannerReportReader = mock();
  private final UuidFactory uuidFactory = mock();
  private final Server server = mock();
  private final ComputationStep.Context context = mock();
  private final Configuration configuration = mock();
  private final AnalysisMetadataHolder analysisMetadataHolder = mock();
  private final MetricRepository metricRepository = mock();
  private final MeasureRepository measureRepository = mock();
  private final TreeRootHolder treeRootHolder = mock();
  private final StepsTelemetryHolder stepsTelemetryHolder = mock();
  private final Branch branch = mock();
  private final SendAnalysisTelemetryStep underTest = new SendAnalysisTelemetryStep(telemetryClient, scannerReportReader, uuidFactory,
    server, configuration, analysisMetadataHolder, stepsTelemetryHolder, metricRepository, measureRepository, treeRootHolder);

  {
    when(uuidFactory.create()).thenReturn("uuid");
    when(server.getId()).thenReturn("serverId");
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
    when(analysisMetadataHolder.getProject()).thenReturn(new Project("uuid", "key", "name", null, Collections.emptyList()));
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(branch.isMain()).thenReturn(true);
    when(stepsTelemetryHolder.getTelemetryMetrics()).thenReturn(Collections.emptyMap());
    when(scannerReportReader.readMetadata()).thenReturn(ScannerReport.Metadata.newBuilder().build());
  }

  @Test
  void execute_whenNoMetrics_sendPopulatedAnalyzedLinesOfCode() throws JsonProcessingException {
    // analyzed_ncloc has populated value
    when(measureRepository.getRawMeasure(any(), any())).thenReturn(Optional.of(newMeasureBuilder().create(50)));
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.emptyCloseableIterator());

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient).uploadMetricAsync(argumentCaptor.capture());
    assertAnalyzedLinesOfCodeMetric(argumentCaptor.getValue(), "50");
  }

  @Test
  void execute_whenNoMetrics_sendNotPopulatedAnalyzedLinesOfCode() throws JsonProcessingException {
    // analyzed_ncloc has empty value
    when(measureRepository.getRawMeasure(any(), any())).thenReturn(Optional.empty());
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.emptyCloseableIterator());

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient).uploadMetricAsync(argumentCaptor.capture());
    assertAnalyzedLinesOfCodeMetric(argumentCaptor.getValue(), ANALYZED_LINES_OF_CODE_NOT_SET_VALUE);
  }

  private void assertAnalyzedLinesOfCodeMetric(String jsonBaseMessage, String expectedValue) throws JsonProcessingException {
    var message = new ObjectMapper().readValue(jsonBaseMessage, JsonNode.class);
    assertThat(message.get("metric_values")).hasSize(1)
      .extracting(values -> List.of(values.get("key").asText(), values.get("value").asText()))
      .containsExactly(List.of(ANALYZED_LINES_OF_CODE_METRIC_KEY, expectedValue));
  }

  @Test
  void execute_whenTwoScannerReportMetrics_callTelemetryClientOnce() {
    Set<ScannerReport.TelemetryEntry> telemetryEntries = Set.of(
      ScannerReport.TelemetryEntry.newBuilder().setKey("key1").setValue("value1").build(),
      ScannerReport.TelemetryEntry.newBuilder().setKey("key2").setValue("value2").build());
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.from(telemetryEntries.iterator()));

    underTest.execute(context);

    verify(telemetryClient, times(1)).uploadMetricAsync(anyString());
  }

  @Test
  void execute_whenMetricsPresentAndTelemetryNotEnabled_dontCallTelemetryClient() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));
    Set<ScannerReport.TelemetryEntry> telemetryEntries = Set.of(
      ScannerReport.TelemetryEntry.newBuilder().setKey("key1").setValue("value1").build(),
      ScannerReport.TelemetryEntry.newBuilder().setKey("key2").setValue("value2").build());
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.from(telemetryEntries.iterator()));

    Map<String, Object> telemetryMetrics = new LinkedHashMap<>();
    telemetryMetrics.put("step.metric.1", "value1");
    when(stepsTelemetryHolder.getTelemetryMetrics()).thenReturn(telemetryMetrics);

    underTest.execute(context);

    verifyNoInteractions(telemetryClient);
  }

  @Test
  void execute_when2000entries_sendOnly1000entries() {
    Set<ScannerReport.TelemetryEntry> scannerReportTelemetryEntries = new HashSet<>();
    for (int i = 0; i < 700; i++) {
      scannerReportTelemetryEntries.add(ScannerReport.TelemetryEntry.newBuilder().setKey(String.valueOf(i)).setValue("value" + i).build());
    }
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.from(scannerReportTelemetryEntries.iterator()));

    Map<String, Object> telemetryMetrics = new LinkedHashMap<>();
    for (int i = 0; i < 700; i++) {
      telemetryMetrics.put(String.format("step.metric.%s", i), "value" + i);
    }
    when(stepsTelemetryHolder.getTelemetryMetrics()).thenReturn(telemetryMetrics);

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());

    String capturedArgument = argumentCaptor.getValue();
    // +1 because split on "key" will create an extra element before the first match
    assertThat(capturedArgument.split("key")).hasSize(MAX_METRICS + 1);
    assertThat(argumentCaptor.getValue()).contains(ANALYZED_LINES_OF_CODE_METRIC_KEY);
  }

  @Test
  void execute_whenIndexedFileCountMetrics_sendMetricsWithCorrectFormat() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .putAnalyzedIndexedFileCountPerType("java", 150)
      .putAnalyzedIndexedFileCountPerType("js", 43)
      .putNotAnalyzedIndexedFileCountPerType("ts", 27)
      .putNotAnalyzedIndexedFileCountPerType("py", 10)
      .build();
    when(scannerReportReader.readMetadata()).thenReturn(metadata);
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.emptyCloseableIterator());

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());

    String capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument).contains(
      "\"key\":\"indexed_files.java.analyzed.total\",\"value\":\"150\"",
      "\"key\":\"indexed_files.js.analyzed.total\",\"value\":\"43\"",
      "\"key\":\"indexed_files.ts.unanalyzed.total\",\"value\":\"27\"",
      "\"key\":\"indexed_files.py.unanalyzed.total\",\"value\":\"10\"");
  }

  @Test
  void execute_whenIndexedFileCountMetricsAndOtherMetrics_respectMaxLimit() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .putAnalyzedIndexedFileCountPerType("java", 150)
      .putNotAnalyzedIndexedFileCountPerType("js", 43)
      .build();
    when(scannerReportReader.readMetadata()).thenReturn(metadata);

    Set<ScannerReport.TelemetryEntry> scannerReportTelemetryEntries = new HashSet<>();
    for (int i = 0; i < 600; i++) {
      scannerReportTelemetryEntries.add(ScannerReport.TelemetryEntry.newBuilder().setKey(String.valueOf(i)).setValue("value" + i).build());
    }
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.from(scannerReportTelemetryEntries.iterator()));

    Map<String, Object> telemetryMetrics = new LinkedHashMap<>();
    for (int i = 0; i < 600; i++) {
      telemetryMetrics.put(String.format("step.metric.%s", i), "value" + i);
    }
    when(stepsTelemetryHolder.getTelemetryMetrics()).thenReturn(telemetryMetrics);

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());

    String capturedArgument = argumentCaptor.getValue();
    // Verify we still respect the max limit of 1000 metrics
    // +1 because split on "key" will create an extra element before the first match
    assertThat(capturedArgument.split("key")).hasSize(MAX_METRICS + 1);
    assertThat(capturedArgument).contains("indexed_files.java.analyzed.total", "indexed_files.js.unanalyzed.total");
  }

  @Test
  void execute_whenNotMainBranch_dontSendIndexedFileCountMetrics() {
    when(branch.isMain()).thenReturn(false);

    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .putAnalyzedIndexedFileCountPerType("java", 150)
      .putAnalyzedIndexedFileCountPerType("js", 43)
      .putNotAnalyzedIndexedFileCountPerType("ts", 27)
      .build();
    when(scannerReportReader.readMetadata()).thenReturn(metadata);

    Set<ScannerReport.TelemetryEntry> telemetryEntries = Set.of(
      ScannerReport.TelemetryEntry.newBuilder().setKey("key1").setValue("value1").build(),
      ScannerReport.TelemetryEntry.newBuilder().setKey("key2").setValue("value2").build());
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.from(telemetryEntries.iterator()));

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());

    String capturedArgument = argumentCaptor.getValue();

    assertThat(capturedArgument)
      .doesNotContain("indexed_files.java.analyzed.total", "indexed_files.js.analyzed.total", "indexed_files.ts.unanalyzed.total")
      .contains("key1", "key2");
  }

  @Test
  void execute_sanitizeExtensionsBeforeSendingToTelemetry() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .putAnalyzedIndexedFileCountPerType("ja!v#a", 150)
      .putAnalyzedIndexedFileCountPerType("pYth-On", 12)
      .putNotAnalyzedIndexedFileCountPerType("j$S", 43)
      .putNotAnalyzedIndexedFileCountPerType("Ruby@", 7)
      .build();
    when(scannerReportReader.readMetadata()).thenReturn(metadata);
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.emptyCloseableIterator());

    underTest.execute(context);

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient, times(1)).uploadMetricAsync(argumentCaptor.capture());
    String capturedArgument = argumentCaptor.getValue();
    assertThat(capturedArgument).contains(
      "\"key\":\"indexed_files.ja_v_a.analyzed.total\",\"value\":\"150\"",
      "\"key\":\"indexed_files.pyth_on.analyzed.total\",\"value\":\"12\"",
      "\"key\":\"indexed_files.j_s.unanalyzed.total\",\"value\":\"43\"",
      "\"key\":\"indexed_files.ruby_.unanalyzed.total\",\"value\":\"7\""
    );
  }
}
