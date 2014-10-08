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
import org.elasticsearch.common.hppc.cursors.ObjectCursor;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.internal.InternalNode;
import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessLogging;
import org.sonar.process.Props;

public class SearchServer implements Monitored {

  private final EsSettings settings;
  private InternalNode node;

  public SearchServer(Props props) {
    this.settings = new EsSettings(props);
    new MinimumViableSystem().check();
  }

  @Override
  public void start() {
    LoggerFactory.getLogger(SearchServer.class).info("Starting Elasticsearch[{}] on port {}", settings.clusterName(), settings.tcpPort());

    node = new InternalNode(settings.build(), true);
    node.start();

    // When joining a cluster, make sur the master(s) have a
    // replication factor on all indices > 0
    if (settings.inCluster()) {
      for (ObjectCursor<Settings> settingCursor : node.client().admin().indices()
        .prepareGetSettings().get().getIndexToSettings().values()) {
        Settings settings = settingCursor.value;
        String clusterReplicationFactor = settings.get("index.number_of_replicas", "-1");
        if (Integer.parseInt(clusterReplicationFactor) <= 0) {
          node.stop();
          throw new MessageException("Index configuration is not set to cluster. Please start the master node with 'sonar.cluster.activation=true'");
        }
      }
    }

    node.client().admin().indices()
      .preparePutTemplate("default")
      .setTemplate("*")
      .addMapping("_default_", "{\"dynamic\": \"strict\"}")
      .get();
  }

  @Override
  public boolean isReady() {
    return node != null && node.client().admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(TimeValue.timeValueSeconds(3L))
      .get()
      .getStatus() != ClusterHealthStatus.RED;
  }

  @Override
  public void awaitStop() {
    while (node != null && !node.isClosed()) {
      try {
        Thread.sleep(200L);
      } catch (InterruptedException e) {
        // Ignore
      }
    }
  }

  @Override
  public void stop() {
    if (node != null && !node.isClosed()) {
      node.close();
    }
  }

  public static void main(String... args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    new ProcessLogging().configure(entryPoint.getProps(), "/org/sonar/search/logback.xml");
    SearchServer searchServer = new SearchServer(entryPoint.getProps());
    entryPoint.launch(searchServer);
  }
}
