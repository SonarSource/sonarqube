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

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Process;
import org.sonar.process.Props;
import org.sonar.search.script.ListUpdate;

public class ElasticSearch extends Process {

  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearch.class);

  public static final String ES_DEBUG_PROPERTY = "esDebug";
  public static final String ES_PORT_PROPERTY = "esPort";
  public static final String ES_CLUSTER_PROPERTY = "esCluster";
  public static final String ES_HOME_PROPERTY = "esHome";

  public static final String MISSING_ES_PORT = "Missing ES port Argument";
  public static final String MISSING_ES_HOME = "Missing ES home directory Argument";

  public static final String DEFAULT_CLUSTER_NAME = "sonarqube";

  private final Node node;

  public ElasticSearch(Props props) {
    super(props);


    String home = props.of(ES_HOME_PROPERTY);
    if (home == null) {
      throw new IllegalStateException(MISSING_ES_HOME);
    }

    Integer port = props.intOf(ES_PORT_PROPERTY);
    if (port == null) {
      throw new IllegalStateException(MISSING_ES_PORT);
    }

    String clusterName = props.of(ES_CLUSTER_PROPERTY, DEFAULT_CLUSTER_NAME);

    LOGGER.info("Starting ES[{}] on port: {}", clusterName, port);

    ImmutableSettings.Builder esSettings = ImmutableSettings.settingsBuilder()

      .put("es.foreground", "yes")

      .put("discovery.zen.ping.multicast.enabled", "false")

      .put("index.merge.policy.max_merge_at_once", "200")
      .put("index.merge.policy.segments_per_tier", "200")
      .put("index.number_of_shards", "1")
      .put("index.number_of_replicas", "0")
      .put("index.store.type", "mmapfs")
      .put("indices.store.throttle.type", "merge")
      .put("indices.store.throttle.max_bytes_per_sec", "200mb")

      .put("script.default_lang", "native")
      .put("script.native." + ListUpdate.NAME + ".type", ListUpdate.UpdateListScriptFactory.class.getName())

      .put("cluster.name", clusterName)
      .put("node.name", "sonarqube-" + System.currentTimeMillis())
      .put("node.data", true)
      .put("node.local", false)
      .put("transport.tcp.port", port)
      .put("path.home", home);

//    if (props.booleanOf(ES_DEBUG_PROPERTY, false)) {
      esSettings
        .put("http.enabled", true)
        .put("http.port", 9200);
//    } else {
//      esSettings.put("http.enabled", false);
//    }

    node = NodeBuilder.nodeBuilder()
      .settings(esSettings)
      .build();
  }


  @Override
  public boolean isReady() {
    ClusterHealthStatus status = node.client().admin().cluster().prepareClusterStats()
      .get().getStatus();
    return status != null && status == ClusterHealthStatus.GREEN;
  }

  @Override
  public void onStart() {
    node.start();
    while (!node.isClosed()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void onStop() {
    if (node != null) {
      node.close();
    }
  }

  public static void main(String... args) throws InterruptedException {
    Props props = Props.create(System.getProperties());
    final ElasticSearch elasticSearch = new ElasticSearch(props);
    elasticSearch.start();
  }
}
