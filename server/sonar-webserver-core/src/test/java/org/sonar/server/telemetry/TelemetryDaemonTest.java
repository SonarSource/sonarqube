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
package org.sonar.server.telemetry;

import java.io.IOException;
import java.util.Collections;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;
import org.sonar.server.util.GlobalLockManager;
import org.sonar.server.util.GlobalLockManagerImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_FREQUENCY_IN_SECONDS;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_URL;

public class TelemetryDaemonTest {
  @Rule
  public LogTester logger = new LogTester().setLevel(LoggerLevel.DEBUG);

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
  private final TestSystem2 system2 = new TestSystem2().setNow(System.currentTimeMillis());
  private final MapSettings settings = new MapSettings();

  private final TelemetryDataLoader dataLoader = mock(TelemetryDataLoader.class);
  private final TelemetryDataJsonWriter dataJsonWriter = mock(TelemetryDataJsonWriter.class);
  private final TelemetryDaemon underTest = new TelemetryDaemon(dataLoader, dataJsonWriter, client, settings.asConfig(), internalProperties, lockManager, system2);

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void send_data_via_client_at_startup_after_initial_delay() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    when(dataLoader.load()).thenReturn(SOME_TELEMETRY_DATA);
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(client, timeout(4_000).atLeastOnce()).upload(anyString());
    verify(dataJsonWriter).writeTelemetryData(any(JsonWriter.class), same(SOME_TELEMETRY_DATA));
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

  @Test
  public void check_if_should_send_data_periodically() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    long now = system2.now();
    long twentyHoursAgo = now - (ONE_HOUR * 20L);
    long oneDayAgo = now - ONE_DAY;
    internalProperties.write("telemetry.lastPing", String.valueOf(twentyHoursAgo));
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    when(dataLoader.load()).thenReturn(SOME_TELEMETRY_DATA);
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(dataJsonWriter, after(2_000).never()).writeTelemetryData(any(JsonWriter.class), same(SOME_TELEMETRY_DATA));
    verify(client, never()).upload(anyString());

    internalProperties.write("telemetry.lastPing", String.valueOf(oneDayAgo));

    verify(client, timeout(2_000)).upload(anyString());
    verify(dataJsonWriter).writeTelemetryData(any(JsonWriter.class), same(SOME_TELEMETRY_DATA));
  }

  @Test
  public void do_not_send_data_if_last_ping_earlier_than_one_day_ago() throws IOException {
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
  public void send_data_if_last_ping_is_over_one_day_ago() throws IOException {
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
  public void opt_out_sent_once() throws IOException {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    settings.setProperty("sonar.telemetry.enable", "false");
    mockDataJsonWriterDoingSomething();

    underTest.start();
    underTest.start();

    verify(client, after(2_000).never()).upload(anyString());
    verify(client, timeout(2_000).times(1)).optOut(anyString());
    assertThat(logger.logs(Level.INFO)).contains("Sharing of SonarQube statistics is disabled.");
  }

  @Test
  public void write_sequence_as_one_if_not_previously_present() {
    initTelemetrySettingsToDefaultValues();
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
    settings.setProperty("sonar.telemetry.frequencyInSeconds", "1");
    mockDataJsonWriterDoingSomething();

    underTest.start();

    verify(internalProperties, timeout(4_000)).write("telemetry.messageSeq", "1");
  }

  @Test
  public void write_sequence_correctly_incremented() {
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

  private void initTelemetrySettingsToDefaultValues() {
    settings.setProperty(SONAR_TELEMETRY_ENABLE.getKey(), SONAR_TELEMETRY_ENABLE.getDefaultValue());
    settings.setProperty(SONAR_TELEMETRY_URL.getKey(), SONAR_TELEMETRY_URL.getDefaultValue());
    settings.setProperty(SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getKey(), SONAR_TELEMETRY_FREQUENCY_IN_SECONDS.getDefaultValue());
  }

}
