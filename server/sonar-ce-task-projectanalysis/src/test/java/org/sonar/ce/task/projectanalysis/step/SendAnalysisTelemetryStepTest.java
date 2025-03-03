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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.telemetry.StepsTelemetryHolder;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactory;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;
import org.sonar.telemetry.core.TelemetryClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SendAnalysisTelemetryStepTest {

  private final TelemetryClient telemetryClient = mock();
  private final ScannerReportReader scannerReportReader = mock();
  private final UuidFactory uuidFactory = mock();
  private final Server server = mock();
  private final ComputationStep.Context context = mock();
  private final Configuration configuration = mock();
  private final AnalysisMetadataHolder analysisMetadataHolder = mock();
  private final StepsTelemetryHolder stepsTelemetryHolder = mock();
  private final SendAnalysisTelemetryStep underTest = new SendAnalysisTelemetryStep(telemetryClient, scannerReportReader, uuidFactory,
    server, configuration, analysisMetadataHolder, stepsTelemetryHolder);

  {
    when(uuidFactory.create()).thenReturn("uuid");
    when(server.getId()).thenReturn("serverId");
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
    when(analysisMetadataHolder.getProject()).thenReturn(new Project("uuid", "key", "name",null, Collections.emptyList()));
    when(stepsTelemetryHolder.getTelemetryMetrics()).thenReturn(Collections.emptyMap());
  }

  @Test
  void execute_whenNoMetrics_dontSendAnything() {
    when(scannerReportReader.readTelemetryEntries()).thenReturn(CloseableIterator.emptyCloseableIterator());

    underTest.execute(context);

    verifyNoInteractions(telemetryClient);
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
    assertEquals(1000 + 1, capturedArgument.split("key").length);
  }
}
