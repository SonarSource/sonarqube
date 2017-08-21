/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.sonar.api.utils.DateUtils.parseDate;

public class TelemetryDaemonTest {

  private static final long ONE_HOUR = 60 * 60 * 1_000L;
  private static final long ONE_DAY = 24 * ONE_HOUR;

  private TelemetryClient client = mock(TelemetryClient.class);
  private InternalProperties internalProperties = new MapInternalProperties();
  private FakeServer server = new FakeServer();
  private TestSystem2 system2 = new TestSystem2();
  private MapSettings settings;

  private TelemetryDaemon underTest;

  @Before
  public void setUp() throws Exception {
    settings = new MapSettings(new PropertyDefinitions(TelemetryProperties.class));
    system2.setNow(System.currentTimeMillis());

    underTest = new TelemetryDaemon(client, internalProperties, server, system2, settings.asConfig());
  }

  @Test
  public void send_data_via_client_at_startup_after_initial_delay() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    underTest.start();

    verify(client, timeout(2_000).atLeastOnce()).send(anyString());
  }

  @Test
  public void check_if_should_send_data_periodically() {
    long now = system2.now();
    long sixDaysAgo = now - (ONE_DAY * 6L);
    long sevenDaysAgo = now - (ONE_DAY * 7L);
    internalProperties.write("sonar.telemetry.lastPing", String.valueOf(sixDaysAgo));
    settings.setProperty("sonar.telemetry.frequency", "1");
    underTest = new TelemetryDaemon(client, internalProperties, server, system2, settings.asConfig());
    underTest.start();
    verify(client, timeout(1_000).never()).send(anyString());
    internalProperties.write("sonar.telemetry.lastPing", String.valueOf(sevenDaysAgo));

    verify(client, timeout(1_000).atLeastOnce()).send(anyString());
  }

  @Test
  public void send_server_id() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    String id = randomAlphanumeric(40);
    server.setId(id);
    underTest.start();

    verify(client, timeout(2_000).atLeastOnce()).send(contains(id));
  }

  @Test
  public void do_not_send_data_if_last_ping_earlier_than_one_week_ago() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    long now = system2.now();
    long sixDaysAgo = now - (ONE_DAY * 6L);

    internalProperties.write("sonar.telemetry.lastPing", String.valueOf(sixDaysAgo));
    underTest.start();

    verify(client, timeout(2_000).never()).send(anyString());
  }

  @Test
  public void send_data_if_last_ping_is_one_week_ago() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    long today = parseDate("2017-08-01").getTime();
    system2.setNow(today + 15 * ONE_HOUR);
    long now = system2.now();
    long sevenDaysAgo = now - (ONE_DAY * 7L);
    internalProperties.write("sonar.telemetry.lastPing", String.valueOf(sevenDaysAgo));

    underTest.start();

    verify(client, timeout(2_000)).send(anyString());
    assertThat(internalProperties.read("sonar.telemetry.lastPing").get()).isEqualTo(String.valueOf(today));
  }
}
