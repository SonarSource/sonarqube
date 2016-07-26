/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.serverSystem;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonarqube.ws.client.rule.SearchWsRequest;
import util.ItUtils;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newWsClient;

public class ClusterTest {

  private static final String CONF_FILE_PATH = "conf/sonar.properties";

  /**
   * SONAR-7899
   */
  @Test
  public void secondary_nodes_do_not_write_to_datastores_at_startup() throws Exception {
    // start "startup leader", which creates and populates datastores
    Orchestrator orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.cluster.enabled", "true")
      .setServerProperty("sonar.cluster.startupLeader", "true")
      .setServerProperty("sonar.log.level", "TRACE")
      .addPlugin(ItUtils.xooPlugin())
      .build();
    orchestrator.start();

    expectLog(orchestrator, "Cluster enabled (startup leader)");
    expectWriteOperations(orchestrator, true);
    // verify that datastores are populated by requesting rules
    assertThat(newWsClient(orchestrator).rules().search(new SearchWsRequest()).getTotal()).isGreaterThan(0);

    FileUtils.write(orchestrator.getServer().getLogs(), "", false);
    updateSonarPropertiesFile(orchestrator, ImmutableMap.of("sonar.cluster.startupLeader", "false"));
    orchestrator.restartServer();

    expectLog(orchestrator, "Cluster enabled (startup follower)");
    expectWriteOperations(orchestrator, false);

    orchestrator.stop();
  }

  private static void expectLog(Orchestrator orchestrator, String expectedLog) throws IOException {
    File logFile = orchestrator.getServer().getLogs();
    try (Stream<String> lines = Files.lines(logFile.toPath())) {
      assertThat(lines.anyMatch(s -> containsIgnoreCase(s, expectedLog))).isTrue();
    }
  }

  private static void expectWriteOperations(Orchestrator orchestrator, boolean expected) throws IOException {
    try (Stream<String> lines = Files.lines(orchestrator.getServer().getLogs().toPath())) {
      List<String> writeOperations = lines.filter(ClusterTest::isWriteOperation).collect(Collectors.toList());
      if (expected) {
        assertThat(writeOperations).isNotEmpty();
      } else {
        assertThat(writeOperations).as("Unexpected write operations: " + Joiner.on('\n').join(writeOperations)).isEmpty();

      }
    }
  }

  private static boolean isWriteOperation(String log) {
    return isDbWriteOperation(log) || isEsWriteOperation(log);
  }

  private static boolean isDbWriteOperation(String log) {
    return log.contains("web[sql]") && (containsIgnoreCase(log, "sql=insert") ||
      containsIgnoreCase(log, "sql=update") ||
      containsIgnoreCase(log, "sql=delete") ||
      containsIgnoreCase(log, "sql=create"));
  }

  private static boolean isEsWriteOperation(String log) {
    return log.contains("web[es]") && (containsIgnoreCase(log, "Create index") ||
      containsIgnoreCase(log, "Create type") ||
      containsIgnoreCase(log, "put mapping request") ||
      containsIgnoreCase(log, "refresh request") ||
      containsIgnoreCase(log, "index request"));
  }

  private static void updateSonarPropertiesFile(Orchestrator orchestrator, Map<String, String> props) throws IOException {
    Properties propsFile = new Properties();
    try (FileInputStream conf = FileUtils.openInputStream(new File(orchestrator.getServer().getHome(), CONF_FILE_PATH))) {
      propsFile.load(conf);
      propsFile.putAll(props);
    }
    try (FileOutputStream conf = FileUtils.openOutputStream(new File(orchestrator.getServer().getHome(), CONF_FILE_PATH))) {
      propsFile.store(conf, "");
    }
  }
}
