/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.search;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.slf4j.Slf4jESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ServerFileSystem;

import java.io.File;

/**
 * Manages the ElasticSearch Node instance used to connect to the index.
 * @since 4.1
 */
public class SearchNode implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(SearchIndex.class);
  private static final String INSTANCE_NAME = "sonarqube";
  static final String DATA_DIR = "data/es";

  private final ServerFileSystem fileSystem;
  private final Settings settings;

  // available only after startup
  private Node node;

  public SearchNode(ServerFileSystem fileSystem, Settings settings) {
    this.fileSystem = fileSystem;
    this.settings = settings;
  }

  @Override
  public void start() {
    LOG.info("Starting Elasticsearch...");

    initLogging();
    ImmutableSettings.Builder esSettings = ImmutableSettings.builder().put("node.name", INSTANCE_NAME);
    initDirs(esSettings);
    initRestConsole(esSettings);

    node = NodeBuilder.nodeBuilder()
      .clusterName(INSTANCE_NAME)
      .local(true)
      .data(true)
      .settings(esSettings)
      .node();
    node.start();
    LOG.info("Elasticsearch started");
  }

  private void initLogging() {
    ESLoggerFactory.setDefaultFactory(new Slf4jESLoggerFactory());
  }

  private void initRestConsole(ImmutableSettings.Builder esSettings) {
    int httpPort = settings.getInt("sonar.es.http.port");
    if (httpPort > 0) {
      LOG.warn("Elasticsearch HTTP console enabled on port {}. Only for debugging purpose.", httpPort);
      esSettings.put("http.enabled", true);
      esSettings.put("http.host", "127.0.0.1");
      esSettings.put("http.port", httpPort);
    } else {
      esSettings.put("http.enabled", false);
    }
  }

  private void initDirs(ImmutableSettings.Builder esSettings) {
    File esDir = new File(fileSystem.getHomeDir(), DATA_DIR);
    try {
      FileUtils.forceMkdir(esDir);
      esSettings.put("path.home", esDir.getAbsolutePath());
      LOG.debug("Elasticsearch data stored in {}", esDir.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create directory " + esDir.getAbsolutePath(), e);
    }
  }

  @Override
  public void stop() {
    if (node != null) {
      node.close();
      node = null;
    }
  }

  public Client client() {
    if (node == null) {
      throw new IllegalStateException("Elasticsearch is not started");
    }
    return node.client();
  }
}
