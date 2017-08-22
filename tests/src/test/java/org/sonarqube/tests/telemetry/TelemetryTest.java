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
package org.sonarqube.tests.telemetry;

import com.sonar.orchestrator.Orchestrator;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.HttpHeaders;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.xooPlugin;

public class TelemetryTest {

  private static Orchestrator orchestrator;

  private MockWebServer server;
  private String url;

  @Before
  public void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    url = server.url("").url().toString();
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  public void send_telemetry_data_at_startup() throws Exception {
    String serverId = randomAlphanumeric(40);
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.telemetry.url", url)
      .setServerProperty("sonar.telemetry.frequencyInSeconds", "1")
      .setServerProperty("sonar.core.id", serverId)
      .build();
    orchestrator.start();

    RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getBody().readUtf8()).contains(serverId);
    assertThat(request.getHeader(HttpHeaders.USER_AGENT)).contains("SonarQube");

    orchestrator.stop();
  }

  @Test
  public void opt_out_of_telemetry() throws Exception {
    String serverId = randomAlphanumeric(40);
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(xooPlugin())
      .setServerProperty("sonar.telemetry.enable", "false")
      .setServerProperty("sonar.telemetry.url", url)
      .setServerProperty("sonar.telemetry.frequencyInSeconds", "1")
      .setServerProperty("sonar.core.id", serverId)
      .build();
    orchestrator.start();

    RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);

    assertThat(request.getMethod()).isEqualTo("DELETE");
    assertThat(request.getBody().readUtf8()).contains(serverId);
    assertThat(request.getHeader(HttpHeaders.USER_AGENT)).contains("SonarQube");

    orchestrator.stop();
  }
}
