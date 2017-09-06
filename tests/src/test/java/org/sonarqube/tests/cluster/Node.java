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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.sonarqube.tests.LogsTailer;
import org.sonarqube.ws.WsSystem;
import org.sonarqube.ws.client.WsClient;
import util.ItUtils;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;

class Node {

  private final NodeConfig config;
  private final Orchestrator orchestrator;
  private final String systemPassCode;
  private LogsTailer logsTailer;
  private final LogsTailer.Content content = new LogsTailer.Content();

  Node(NodeConfig config, Orchestrator orchestrator, String systemPassCode) {
    this.config = config;
    this.orchestrator = orchestrator;
    this.systemPassCode = systemPassCode;
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
    waitFor(node -> WsSystem.Status.UP == node.getStatus().orElse(null));
  }

  /**
   * Waiting for health to be green... or yellow on the boxes that
   * have less than 15% of free disk space. In that case Elasticsearch
   * can't build shard replicas so it is yellow.
   */
  void waitForHealthGreen() {
    waitFor(node -> {
      Optional<WsSystem.HealthResponse> health = node.getHealth();
      if (!health.isPresent()) {
        return false;
      }
      if (health.get().getHealth() == WsSystem.Health.GREEN) {
        return true;
      }
      if (health.get().getHealth() == WsSystem.Health.YELLOW) {
        List<WsSystem.Cause> causes = health.get().getCausesList();
        return causes.size() == 1 && "Elasticsearch status is YELLOW".equals(causes.get(0).getMessage());
      }
      return false;
    });
  }

  void waitForHealth(WsSystem.Health expectedHealth) {
    waitFor(node -> expectedHealth.equals(node.getHealth().map(WsSystem.HealthResponse::getHealth).orElse(null)));
  }

  Optional<WsSystem.Status> getStatus() {
    checkState(config.getType() == NodeConfig.NodeType.APPLICATION);
    if (orchestrator.getServer() == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(ItUtils.newAdminWsClient(orchestrator).system().status().getStatus());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  Optional<WsSystem.HealthResponse> getHealth() {
    checkState(config.getType() == NodeConfig.NodeType.APPLICATION);
    if (orchestrator.getServer() == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(ItUtils.newSystemUserWsClient(orchestrator, systemPassCode).system().health());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  void waitFor(Predicate<Node> predicate) {
    try {
      while (!predicate.test(this)) {
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

  public WsClient wsClient() {
    checkState(config.getType() == NodeConfig.NodeType.APPLICATION);
    return ItUtils.newAdminWsClient(orchestrator);
  }
}
