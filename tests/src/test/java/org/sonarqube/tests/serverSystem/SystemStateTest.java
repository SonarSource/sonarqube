/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.tests.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Elasticsearch;
import org.sonarqube.ws.System;
import org.sonarqube.ws.client.WsClient;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newSystemUserWsClient;
import static util.ItUtils.newWsClient;
import static util.ItUtils.pluginArtifact;

/**
 * Test system status and health
 */
public class SystemStateTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(300));

  private final String systemPassCode = RandomStringUtils.randomAlphanumeric(15);

  @Test
  public void test_status_and_health_during_server_lifecycle() throws Exception {
    try (Commander commander = new Commander()) {
      Lock lock = new Lock();
      commander.start(lock);
      commander.waitFor(() -> commander.webLogsContain("ServerStartupLock - Waiting for file to be deleted"));

      commander.verifyStatus(System.Status.STARTING);
      commander.verifyHealth(System.Health.RED, "SonarQube webserver is not up");

      lock.unlockWeb();
      // status is UP as soon as web server is up, whatever the status of Compute Engine
      commander.waitFor(() -> System.Status.UP == commander.status().orElse(null));
      commander.verifyHealth(System.Health.RED, "Compute Engine is not operational");

      lock.unlockCe();
      commander.waitForHealth(System.Health.GREEN);
      commander.verifyStatus(System.Status.UP);
    }
  }

  @Test
  public void test_status_and_health_when_ES_becomes_yellow() throws Exception {
    try (Commander commander = new Commander()) {
      commander.start();
      commander.waitForHealth(System.Health.GREEN);

      commander.makeElasticsearchYellow();
      commander.waitForHealth(System.Health.YELLOW, "Elasticsearch status is YELLOW");
      commander.verifyStatus(System.Status.UP);

      commander.makeElasticsearchGreen();
      commander.waitForHealth(System.Health.GREEN);
      // status does not change after being UP
      commander.verifyStatus(System.Status.UP);
    }
  }

  private class Lock {
    private final File webFile;
    private final File ceFile;

    Lock() throws Exception {
      webFile = temp.newFile();
      ceFile = temp.newFile();
    }

    void unlockWeb() throws IOException {
      FileUtils.forceDelete(webFile);
    }

    void unlockCe() throws IOException {
      FileUtils.forceDelete(ceFile);
    }
  }

  private class Commander implements AutoCloseable {
    private final int esHttpPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
    private Orchestrator orchestrator;
    private Thread starter;
    private Elasticsearch elasticsearch;

    void start() throws Exception {
      Lock lock = new Lock();
      start(lock);
      lock.unlockWeb();
      lock.unlockCe();
    }

    void start(Lock lock) {
      checkState(orchestrator == null);
      orchestrator = Orchestrator.builderEnv()
        .addPlugin(pluginArtifact("server-plugin"))
        .setServerProperty("sonar.web.startupLock.path", lock.webFile.getAbsolutePath())
        .setServerProperty("sonar.ce.startupLock.path", lock.ceFile.getAbsolutePath())
        .setServerProperty("sonar.search.httpPort", "" + esHttpPort)
        .setServerProperty("sonar.web.systemPasscode", systemPassCode)
        .build();
      elasticsearch = new Elasticsearch(esHttpPort);

      starter = new Thread(orchestrator::start);
      starter.start();
      while (orchestrator.getServer() == null) {
        sleep(100L);
      }
    }

    boolean webLogsContain(String message) {
      try {
        return FileUtils.readFileToString(orchestrator.getServer().getWebLogs()).contains(message);
      } catch (IOException e) {
        return false;
      }
    }

    void sleep(long ms) {
      try {
        Thread.sleep(ms);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    void waitFor(Supplier<Boolean> predicate) {
      while (!predicate.get()) {
        sleep(100L);
      }
    }

    Optional<System.Status> status() {
      if (orchestrator.getServer() != null) {
        WsClient wsClient = newWsClient(orchestrator);
        try {
          return Optional.of(wsClient.system().status().getStatus());
        } catch (Exception e) {
          // server does not accept connections
        }
      }
      return Optional.empty();
    }

    void verifyStatus(System.Status expectedStatus) {
      assertThat(status()).hasValue(expectedStatus);
    }

    Optional<System.Health> health() {
      Optional<System.HealthResponse> response = healthResponse();
      return response.map(System.HealthResponse::getHealth);
    }

    Optional<System.HealthResponse> healthResponse() {
      if (orchestrator.getServer() != null) {
        WsClient wsClient = newSystemUserWsClient(orchestrator, systemPassCode);
        try {
          return Optional.of(wsClient.system().health());
        } catch (Exception e) {
          // server does not accept connections
        }
      }
      return Optional.empty();
    }

    void waitForHealth(System.Health expectedHealth, String... expectedMessages) {
      waitFor(() -> expectedHealth == health().orElse(null));
      verifyHealth(expectedHealth, expectedMessages);
    }

    void verifyHealth(System.Health expectedHealth, String... expectedMessages) {
      System.HealthResponse response = healthResponse().get();
      assertThat(response.getHealth())
        .describedAs("Expected status %s in response %s", expectedHealth, response)
        .isEqualTo(expectedHealth);
      assertThat(response.getCausesList())
        .extracting(System.Cause::getMessage)
        .describedAs("Expected causes %s in response %s", Arrays.asList(expectedMessages), response)
        .containsExactlyInAnyOrder(expectedMessages);
    }

    void makeElasticsearchYellow() throws Exception {
      elasticsearch.makeYellow();
    }

    void makeElasticsearchGreen() throws Exception {
      elasticsearch.makeGreen();
    }

    @Override
    public void close() {
      if (starter != null) {
        starter.interrupt();
        orchestrator.stop();
      }
    }
  }
}
