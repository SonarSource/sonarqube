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
package org.sonarqube.tests.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
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

  @Test
  public void system_status_becomes_UP_when_web_server_is_started() throws Exception {
    try (Commander commander = new Commander()) {
      commander.startAsync();

      commander.waitFor(() -> commander.webLogsContain("ServerStartupLock - Waiting for file to be deleted"));
      assertThat(commander.status()).hasValue("STARTING");

      commander.unlock();
      commander.waitFor(() -> "UP".equals(commander.status().orElse(null)));
    }
  }

  private class Commander implements AutoCloseable {
    private final File lock = temp.newFile();
    private final Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("server-plugin"))
      .setServerProperty("sonar.test.serverStartupLock.path", lock.getCanonicalPath())
      .build();
    private Thread starter;

    Commander() throws Exception {

    }

    void startAsync() {
      checkState(starter == null);
      starter = new Thread(orchestrator::start);
      starter.start();
      while (orchestrator.getServer() == null) {
        sleep(100L);
      }
    }

    void unlock() throws IOException {
      FileUtils.forceDelete(lock);
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

    Optional<String> status() {
      if (orchestrator.getServer() != null) {
        WsClient wsClient = newWsClient(orchestrator);
        try {
          WsResponse statusResponse = wsClient.wsConnector().call(new GetRequest("api/system/status"));
          if (statusResponse.isSuccessful()) {
            Map<String, Object> json = ItUtils.jsonToMap(statusResponse.content());
            return Optional.ofNullable((String) json.get("status"));
          }
        } catch (Exception e) {
          // server does not accept connections
        }
      }
      return Optional.empty();
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
