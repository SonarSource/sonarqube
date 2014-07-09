/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.search;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.search.script.ListUpdate;

import java.io.File;

public class ElasticSearch extends org.sonar.process.Process {

  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearch.class);

  static final String MISSING_ARGUMENTS = "2 arguments are required for org.sonar.search.Application";
  static final String MISSING_NAME_ARGUMENT = "Missing Name argument";
  static final String MISSING_PORT_ARGUMENT = "Missing Port argument";
  static final String COULD_NOT_PARSE_ARGUMENT_INTO_A_NUMBER = "Could not parse argument into a number";

  private final Node node;
  private final Integer esPort;

  public ElasticSearch(Integer esPort, String name, int port) {
    super(name, port);
    this.esPort = esPort;

    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());

    ImmutableSettings.Builder esSettings = ImmutableSettings.settingsBuilder()
      .put("index.merge.policy.max_merge_at_once", "200")
      .put("index.merge.policy.segments_per_tier", "200")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("index.store.type", "mmapfs")
//
      .put("indices.store.throttle.type", "merge")
      .put("indices.store.throttle.max_bytes_per_sec", "200mb")
//
      .put("script.default_lang", "native")
      .put("script.native." + ListUpdate.NAME + ".type", ListUpdate.UpdateListScriptFactory.class.getName())
//
      .put("cluster.name", "sonarqube")
//
      .put("node.name", "sonarqube-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("node.local", false)
//
//      .put("network.bind_host", "127.0.0.1")
//      .put("http.enabled", true)
//      .put("http.host", "127.0.0.1")

      .put("transport.tcp.port", this.esPort);
//      .put("http.port", 9200);

    File esDir = FileUtils.getTempDirectory();
    try {
      FileUtils.forceMkdir(esDir);
      esSettings.put("path.home", esDir.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create directory " + esDir.getAbsolutePath(), e);
    }

    node = NodeBuilder.nodeBuilder()
      .settings(esSettings)
      .build().start();
  }

  public Integer getEsPort() {
    return esPort;
  }

  @Override
  public void execute() {
    while (node != null && !node.isClosed()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("-- ES is done.");
  }

  public void shutdown() {
    this.node.close();
  }

  private static final String getName(String... args) {
    if (args[0].isEmpty()) {
      throw new IllegalStateException(MISSING_NAME_ARGUMENT);
    }
    return args[0];
  }

  private static final Integer getPort(String... args) {
    if (args[1].isEmpty()) {
      throw new IllegalStateException(MISSING_PORT_ARGUMENT);
    }
    try {
      return Integer.valueOf(args[1]);
    } catch (Exception e) {
      throw new IllegalStateException(COULD_NOT_PARSE_ARGUMENT_INTO_A_NUMBER);
    }
  }

  private static final Integer getEsPort(String... args) {
    if (args[2].isEmpty()) {
      throw new IllegalStateException(MISSING_PORT_ARGUMENT);
    }
    try {
      return Integer.valueOf(args[2]);
    } catch (Exception e) {
      throw new IllegalStateException(COULD_NOT_PARSE_ARGUMENT_INTO_A_NUMBER);
    }
  }

  public static void main(String... args) {
    if (args.length != 3) {
      throw new IllegalStateException(MISSING_ARGUMENTS);
    }
    String name = ElasticSearch.getName(args);
    Integer port = ElasticSearch.getPort(args);
    Integer esPort = ElasticSearch.getEsPort(args);
    LOGGER.info("Launching '{}' with heartbeat on port '{}'", name, port);
    ElasticSearch elasticSearch = new ElasticSearch(esPort, name, port);
    elasticSearch.execute();
  }
}