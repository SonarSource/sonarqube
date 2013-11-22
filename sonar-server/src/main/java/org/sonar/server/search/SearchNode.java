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

import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.TempFolder;

/**
 * Manages the ElasticSearch Node instance used to connect to the index.
 * @since 4.1
 */
public class SearchNode {

  private static final Logger LOG = LoggerFactory.getLogger(SearchIndex.class);

  private String nodeDir;
  private Settings settings;

  private ImmutableSettings.Builder nodeSettingsBuilder;
  private NodeBuilder nodeBuilder;

  private Node node;

  public SearchNode(TempFolder tempFolder, Settings settings) {
    this(tempFolder, settings, ImmutableSettings.builder(), NodeBuilder.nodeBuilder());
  }

  @VisibleForTesting
  SearchNode(TempFolder tempFolder, Settings settings, ImmutableSettings.Builder nodeSettingsBuilder, NodeBuilder nodeBuilder) {
    this.nodeDir = tempFolder.newDir("es").getAbsolutePath();
    this.settings = settings;
    this.nodeSettingsBuilder = nodeSettingsBuilder;
    this.nodeBuilder = nodeBuilder;
  }

  public void start() {
    LOG.info("Starting {} in {}", this.getClass().getSimpleName(), nodeDir);
    nodeSettingsBuilder
      .put("node.name", "sonarqube")
      .put("node.path.conf", nodeDir)
      .put("node.path.data", nodeDir)
      .put("node.path.work", nodeDir)
      .put("node.path.logs", nodeDir)
      .put("gateway.type", "none");

    String httpHost = settings.getString("sonar.es.http.host");
    String httpPort = settings.getString("sonar.es.http.port");
    if (httpPort == null) {
      LOG.info("HTTP access to search cache disabled");
      nodeSettingsBuilder.put("http.enabled", false);
    } else {
      if (httpHost == null) {
        httpHost = "127.0.0.1";
      }
      LOG.info("Enabling HTTP access to search cache on ports {}", httpPort);
      nodeSettingsBuilder.put("http.enabled", true);
      nodeSettingsBuilder.put("http.host", httpHost);
      nodeSettingsBuilder.put("http.port", httpPort);
    }

    node = nodeBuilder
      .local(true)
      .clusterName("sonarqube")
      .settings(nodeSettingsBuilder)
      .node();
  }

  public void stop() {
    if(node != null) {
      node.close();
      node = null;
    }
  }

  public Client client() {
    if (node == null) {
      throw new IllegalStateException(this.getClass().getSimpleName() + " not started");
    }
    return node.client();
  }
}
