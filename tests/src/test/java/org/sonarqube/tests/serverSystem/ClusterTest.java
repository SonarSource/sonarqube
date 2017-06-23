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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.server.StartupLogWatcher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.rule.SearchWsRequest;
import org.sonarqube.ws.client.setting.ValuesRequest;
import util.ItUtils;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newWsClient;

@Ignore("temporarily ignored")
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
      .setServerProperty("sonar.cluster.name", "secondary_nodes_do_not_write_to_datastores_at_startup")
      .setServerProperty("sonar.cluster.web.startupLeader", "true")
      .setServerProperty("sonar.log.level", "TRACE")
      .addPlugin(ItUtils.xooPlugin())
      .build();
    orchestrator.start();

    expectLog(orchestrator, "Cluster enabled (startup leader)");
    expectWriteOperations(orchestrator, true);
    // verify that datastores are populated by requesting rules
    assertThat(newWsClient(orchestrator).rules().search(new SearchWsRequest()).getTotal()).isGreaterThan(0);

    FileUtils.write(orchestrator.getServer().getWebLogs(), "", false);
    updateSonarPropertiesFile(orchestrator, ImmutableMap.of("sonar.cluster.web.startupLeader", "false"));
    orchestrator.restartServer();

    expectLog(orchestrator, "Cluster enabled (startup follower)");
    expectWriteOperations(orchestrator, false);

    orchestrator.stop();
  }

  @Test
  public void start_cluster_of_elasticsearch_and_web_nodes() throws IOException {
    Orchestrator elasticsearch = null;
    Orchestrator web = null;

    try {
      ElasticsearchStartupWatcher esWatcher = new ElasticsearchStartupWatcher();
      elasticsearch = Orchestrator.builderEnv()
        .setServerProperty("sonar.cluster.enabled", "true")
        .setServerProperty("sonar.cluster.name", "start_cluster_of_elasticsearch_and_web_nodes")
        .setServerProperty("sonar.cluster.web.disabled", "true")
        .setServerProperty("sonar.cluster.ce.disabled", "true")
        .setStartupLogWatcher(esWatcher)
        .build();
      elasticsearch.start();
      assertThat(esWatcher.port).isGreaterThan(0);
      assertThat(FileUtils.readFileToString(elasticsearch.getServer().getAppLogs())).doesNotContain("Process[web]");

      web = Orchestrator.builderEnv()
        .setServerProperty("sonar.cluster.enabled", "true")
        .setServerProperty("sonar.cluster.name", "start_cluster_of_elasticsearch_and_web_nodes")
        .setServerProperty("sonar.cluster.web.startupLeader", "true")
        .setServerProperty("sonar.cluster.search.disabled", "true")
        .setServerProperty("sonar.cluster.search.hosts", "localhost:" + esWatcher.port)
        // no need for compute engine in this test. Disable it for faster test.
        .setServerProperty("sonar.cluster.ce.disabled", "true")
        // override the default watcher provided by Orchestrator
        // which waits for Compute Engine to be up
        .setStartupLogWatcher(log -> log.contains("SonarQube is up"))
        .build();
      web.start();

      String coreId = getPropertyValue(web, "sonar.core.id");
      String startTime = getPropertyValue(web, "sonar.core.startTime");

      assertThat(FileUtils.readFileToString(web.getServer().getAppLogs())).doesNotContain("Process[es]");
      // call a web service that requires Elasticsearch
      Issues.SearchWsResponse wsResponse = newWsClient(web).issues().search(new org.sonarqube.ws.client.issue.SearchWsRequest());
      assertThat(wsResponse.getIssuesCount()).isEqualTo(0);

      web.restartServer();

      // sonar core id must not change after restart
      assertThat(getPropertyValue(web, "sonar.core.id")).isEqualTo(coreId);
      // startTime must change at each startup
      assertThat(getPropertyValue(web, "sonar.core.startTime")).isNotEqualTo(startTime);
    } finally {
      if (web != null) {
        web.stop();
      }
      if (elasticsearch != null) {
        elasticsearch.stop();
      }
    }
  }

  private static String getPropertyValue(Orchestrator web, String property) {
    Settings.ValuesWsResponse response = ItUtils.newAdminWsClient(web).settings().values(ValuesRequest.builder().setKeys(property).build());
    List<Settings.Setting> settingsList = response.getSettingsList();
    if (settingsList.isEmpty()) {
      return null;
    }
    assertThat(settingsList).hasSize(1);
    return settingsList.iterator().next().getValue();
  }

  private static class ElasticsearchStartupWatcher implements StartupLogWatcher {
    private final Pattern pattern = Pattern.compile("Elasticsearch listening on .*:(\\d+)");
    private int port = -1;

    @Override
    public boolean isStarted(String log) {
      Matcher matcher = pattern.matcher(log);
      if (matcher.find()) {
        port = Integer.parseInt(matcher.group(1));
      }
      return log.contains("Process[es] is up");
    }
  }

  private static void expectLog(Orchestrator orchestrator, String expectedLog) throws IOException {
    File logFile = orchestrator.getServer().getWebLogs();
    try (Stream<String> lines = Files.lines(logFile.toPath())) {
      assertThat(lines.anyMatch(s -> StringUtils.containsIgnoreCase(s, expectedLog))).isTrue();
    }
  }

  private static void expectWriteOperations(Orchestrator orchestrator, boolean expected) throws IOException {
    try (Stream<String> lines = Files.lines(orchestrator.getServer().getWebLogs().toPath())) {
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
    return log.contains("web[][sql]") && (containsIgnoreCase(log, "sql=insert") ||
      containsIgnoreCase(log, "sql=update") ||
      containsIgnoreCase(log, "sql=delete") ||
      containsIgnoreCase(log, "sql=create"));
  }

  private static boolean isEsWriteOperation(String log) {
    return log.contains("web[][es]") && (containsIgnoreCase(log, "Create index") ||
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
