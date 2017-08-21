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

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class TelemetryDaemonTest {

  private TelemetryClient client = mock(TelemetryClient.class);
  private FakeServer server = new FakeServer();
  private MapSettings settings;

  private TelemetryDaemon underTest;

  @Before
  public void setUp() throws Exception {
    settings = new MapSettings(new PropertyDefinitions(TelemetryProperties.class));

    underTest = new TelemetryDaemon(client, server, settings.asConfig());
  }

  @Test
  public void send_data_via_client_at_startup_after_initial_delay() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    underTest.start();

    verify(client, timeout(2_000).atLeastOnce()).send(anyString());
  }

  @Test
  public void send_data_periodically() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    underTest = new TelemetryDaemon(client, server, settings.asConfig());

    underTest.start();

    verify(client, timeout(3_000).atLeast(2)).send(anyString());
  }

  @Test
  public void send_server_id() {
    settings.setProperty("sonar.telemetry.frequency", "1");
    String id = randomAlphanumeric(40);
    server.setId(id);
    underTest.start();

    verify(client, timeout(2_000).atLeastOnce()).send(contains(id));
  }
}
