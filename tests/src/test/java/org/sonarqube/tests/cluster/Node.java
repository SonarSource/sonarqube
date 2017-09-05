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
package org.sonarqube.tests.cluster;

import com.google.common.util.concurrent.Uninterruptibles;
import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.sonarqube.tests.LogsTailer;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

class Node {

  private final NodeConfig config;
  private final Orchestrator orchestrator;
  private LogsTailer logsTailer;
  private final LogsTailer.Content content = new LogsTailer.Content();

  Node(NodeConfig config, Orchestrator orchestrator) {
    this.config = config;
    this.orchestrator = orchestrator;
  }

  NodeConfig getConfig() {
    return config;
  }

  /**
   * Non-blocking startup of node. The method does not wait for
   * node to be started because Orchestrator uses a StartupLogWatcher
   * that returns as soon as a log is generated.
   */
  void start() {
    orchestrator.start();
    logsTailer = LogsTailer.builder()
      .addFile(orchestrator.getServer().getWebLogs())
      .addFile(orchestrator.getServer().getCeLogs())
      .addFile(orchestrator.getServer().getEsLogs())
      .addFile(orchestrator.getServer().getAppLogs())
      .addConsumer(content)
      .build();
  }

  void stop() {
    orchestrator.stop();
    if (logsTailer != null) {
      logsTailer.close();
    }
  }

  void cleanUpLogs() {
    if (orchestrator.getServer() != null) {
      FileUtils.deleteQuietly(orchestrator.getServer().getWebLogs());
      FileUtils.deleteQuietly(orchestrator.getServer().getCeLogs());
      FileUtils.deleteQuietly(orchestrator.getServer().getEsLogs());
      FileUtils.deleteQuietly(orchestrator.getServer().getAppLogs());
    }
  }

  boolean isStartupLeader() {
    return webLogsContain("Cluster enabled (startup leader)");
  }

  boolean isStartupFollower() {
    return webLogsContain("Cluster enabled (startup follower)");
  }

  void waitForStatusUp() {
    waitForStatus("UP");
  }

  void waitForStatus(String expectedStatus) {
    String status = null;
    try {
      while (!expectedStatus.equals(status)) {
        if (orchestrator.getServer() != null) {
          try {
            Map<String, Object> json = ItUtils.jsonToMap(orchestrator.getServer().newHttpCall("api/system/status").executeUnsafely().getBodyAsString());
            status = (String) json.get("status");
          } catch (Exception e) {
            // ignored
          }
        }

        Thread.sleep(500);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  void assertThatProcessesAreUp() {
    assertThat(arePortsBound()).as(getConfig().getType().toString()).isTrue();
    switch (config.getType()) {
      case SEARCH:
        assertThat(anyLogsContain("Process[es] is up")).isTrue();
        assertThat(anyLogsContain("Process[web] is up")).isFalse();
        assertThat(anyLogsContain("Elasticsearch cluster enabled")).isTrue();
        break;
      case APPLICATION:
        assertThat(anyLogsContain("Process[es] is up")).isFalse();
        assertThat(anyLogsContain("Process[web] is up")).isTrue();
        assertThat(anyLogsContain("Elasticsearch cluster enabled")).isFalse();
        break;
    }
  }

  void waitForCeLogsContain(String expectedMessage) {
    boolean found = false;
    while (!found) {
      found = orchestrator.getServer() != null && fileContains(orchestrator.getServer().getCeLogs(), expectedMessage);
      if (!found) {
        Uninterruptibles.sleepUninterruptibly(1_000, TimeUnit.MILLISECONDS);
      }
    }
  }

  boolean hasStartupLeaderOperations() throws IOException {
    if (orchestrator.getServer() == null) {
      return false;
    }
    String logs = FileUtils.readFileToString(orchestrator.getServer().getWebLogs());
    return logs.contains("Register metrics") &&
      logs.contains("Register rules");
  }

  boolean hasCreatedSearchIndices() throws IOException {
    if (orchestrator.getServer() == null) {
      return false;
    }
    String logs = FileUtils.readFileToString(orchestrator.getServer().getWebLogs());
    return logs.contains("[o.s.s.e.IndexCreator] Create index");
  }

  boolean anyLogsContain(String message) {
    return content.hasText(message);
  }

  private boolean webLogsContain(String message) {
    if (orchestrator.getServer() == null) {
      return false;
    }
    return fileContains(orchestrator.getServer().getWebLogs(), message);
  }

  private static boolean fileContains(@Nullable File logFile, String message) {
    try {
      return logFile != null && logFile.exists() && FileUtils.readFileToString(logFile).contains(message);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean arePortsBound() {
    return isPortBound(config.getHzPort()) &&
      config.getSearchPort().map(this::isPortBound).orElse(true) &&
      config.getWebPort().map(this::isPortBound).orElse(true);
  }

  private boolean isPortBound(int port) {
    try (ServerSocket socket = new ServerSocket(port, 50, config.getAddress())) {
      return false;
    } catch (IOException e) {
      return true;
    }
  }

}
