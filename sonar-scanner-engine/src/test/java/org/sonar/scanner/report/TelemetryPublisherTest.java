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
package org.sonar.scanner.report;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.TelemetryCache;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TelemetryPublisherTest {
  private final ScannerReportWriter writer = mock(ScannerReportWriter.class);
  private final TelemetryCache telemetryCache = new TelemetryCache();
  private final TelemetryPublisher underTest = new TelemetryPublisher(telemetryCache);

  @Test
  void publish_writes_telemetry_to_report() {
    telemetryCache.put("key1", "value1");
    telemetryCache.put("key2", "value2");

    underTest.publish(writer);

    List<ScannerReport.TelemetryEntry> expected = Arrays.asList(
      newTelemetryEntry("key1", "value1"),
      newTelemetryEntry("key2", "value2"));
    expectWritten(expected);
  }

  private void expectWritten(List<ScannerReport.TelemetryEntry> expected) {
    verify(writer).writeTelemetry(argThat(entries -> {
      List<ScannerReport.TelemetryEntry> copy = Lists.newArrayList(entries);
      copy.removeAll(expected);
      return copy.isEmpty();
    }));
  }

  private static ScannerReport.TelemetryEntry newTelemetryEntry(String key, String value) {
    return ScannerReport.TelemetryEntry.newBuilder()
      .setKey(key)
      .setValue(value)
      .build();
  }
}
