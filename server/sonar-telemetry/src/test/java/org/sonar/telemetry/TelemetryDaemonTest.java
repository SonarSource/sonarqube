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
package org.sonar.telemetry;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbTester;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;
import org.sonar.server.util.GlobalLockManager;
import org.sonar.server.util.GlobalLockManagerImpl;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.legacy.TelemetryData;
import org.sonar.telemetry.legacy.TelemetryDataJsonWriter;
import org.sonar.telemetry.legacy.TelemetryDataLoader;
import org.sonar.telemetry.metrics.TelemetryMetricsLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_FREQUENCY_IN_SECONDS;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

class TelemetryDaemonTest {

  protected final TestSystem2 system2 = new TestSystem2().setNow(System.currentTimeMillis());

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);
  @RegisterExtension
  public DbTester db = DbTester.create(system2);

  private static final long ONE_HOUR = 60 * 60 * 1_000L;
  private static final long ONE_DAY = 24 * ONE_HOUR;
  private static final TelemetryData SOME_TELEMETRY_DATA = TelemetryData.builder()
    .setServerId("foo")
    .setVersion("bar")
    .setMessageSequenceNumber(1L)
    .setPlugins(Collections.emptyMap())
    .setDatabase(new TelemetryData.Database("H2", "11"))
    .build();

  private final TelemetryClient client = mock(TelemetryClient.class);
  private final InternalProperties internalProperties = spy(new MapInternalProperties());
  private final GlobalLockManager lockManager = mock(GlobalLockManagerImpl.class);
  private final MapSettings settings = new MapSettings();
  private final TelemetryDataLoader dataLoader = mock(TelemetryDataLoader.class);
  private final TelemetryDataJsonWriter dataJsonWriter = mock(TelemetryDataJsonWriter.class);
  private final TelemetryMetricsLoader metricsLoader = mock(TelemetryMetricsLoader.class);
  private final TelemetryDaemon underTest = new TelemetryDaemon(dataLoader, dataJsonWriter, client, settings.asConfig(), internalProperties, lockManager, system2, metricsLoader, db.getDbClient());

  @BeforeEach
  void setUp() {
    when(metricsLoader.loadData()).thenReturn(new TelemetryMetricsLoader.Context());
  }

  @AfterEach
  void tearDown() {
    underTest.stop();
  }

  @Test
  void start_sendsDataAtStartupAfterInitialDelay() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    when(dataLoader.load()).thenReturn(SOME_TELEMETRY_DATA);
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(client, timeout(4_000).atLeastOnce()).upload(anyString());
    verify(dataJsonWriter).writeTelemetryData(any(JsonWriter.class), same(SOME_TELEMETRY_DATA));
  }

  @Test
  void start_whenLastPingEarlierThanOneDayAgo_shouldNotSendData() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    long now = system2.now();
    long twentyHoursAgo = now - (ONE_HOUR * 20L);
    mockDataJsonWriterDoingSomething();

    internalProperties.write("telemetry.lastPing", String.valueOf(twentyHoursAgo));
    underTest.start();

    verify(client, after(2_000).never()).upload(anyString());
  }

  @Test
  void start_whenExceptionThrown_shouldNotRepeatedlySendDataAndLastPingPropIsStillSet() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    long today = parseDate("2017-08-01").getTime();
    system2.setNow(today);
    settings.removeProperty("telemetry.lastPing");
    mockDataJsonWriterDoingSomething();
    when(dataLoader.load()).thenThrow(new IllegalStateException("Some error was thrown."));

    underTest.start();

    verify(client, after(2_000).never()).upload(anyString());
    verify(internalProperties, timeout(4_000).atLeastOnce()).write("telemetry.lastPing", String.valueOf(today));
  }

  @Test
  void start_whenLastPingOverOneDayAgo_shouldSendData() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    long today = parseDate("2017-08-01").getTime();
    system2.setNow(today);
    long oneDayAgo = today - ONE_DAY - ONE_HOUR;
    internalProperties.write("telemetry.lastPing", String.valueOf(oneDayAgo));
    reset(internalProperties);
    when(dataLoader.load()).thenReturn(SOME_TELEMETRY_DATA);
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(internalProperties, timeout(4_000)).write("telemetry.lastPing", String.valueOf(today));
    verify(client).upload(anyString());
  }

  @Test
  void start_whenOptOut_shouldSendOnceAndLogsPresent() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    settings.setProperty("sonar.telemetry.enable", "false");
    mockDataJsonWriterDoingSomething();

    underTest.start();
    underTest.start();

    verify(client, after(2_000).never()).upload(anyString());
    verify(client, timeout(2_000).times(1)).optOut(anyString());
    assertThat(logTester.logs(Level.INFO)).contains("Sharing of SonarQube statistics is disabled.");
  }

  @Test
  void start_whenSequenceNotPresent_shouldBeSetToOne() {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(internalProperties, timeout(4_000)).write("telemetry.messageSeq", "1");
  }

  @Test
  void start_whenSequencePresent_shouldIncrementCorrectly() {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    internalProperties.write("telemetry.messageSeq", "10");
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(internalProperties, timeout(4_000)).write("telemetry.messageSeq", "10");

    // force another ping
    internalProperties.write("telemetry.lastPing", String.valueOf(system2.now() - ONE_DAY));

    verify(internalProperties, timeout(4_000)).write("telemetry.messageSeq", "11");
  }

  private void mockDataJsonWriterDoingSomething() {
    doAnswer(t -> {
      JsonWriter json = t.getArgument(0);
      json.beginObject().prop("foo", "bar").endObject();
      return null;
    })
      .when(dataJsonWriter)
      .writeTelemetryData(any(), any());
  }

  private void initTelemetrySettingsToDefaultValues() {
    settings.setProperty(SONAR_TELEMETRY_ENABLE.getKey(), SONAR_TELEMETRY_ENABLE.getDefaultValue());
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), SONAR_TELEMETRY_URL.getDefaultValue());
    settings.setProperty(SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getKey(), SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getDefaultValue());
  }

}
