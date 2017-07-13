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
package org.sonar.search;

import java.io.IOException;
import org.apache.lucene.util.StringHelper;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Jmx;
import org.sonar.process.MinimumViableSystem;
import org.sonar.process.Monitored;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.Props;

public class SearchServer implements Monitored {
  // VisibleForTesting
  protected static Logger LOGGER = LoggerFactory.getLogger(SearchServer.class);

  private static final String MIMINUM_MASTER_NODES = "discovery.zen.minimum_master_nodes";
  private static final String INITIAL_STATE_TIMEOUT = "discovery.initial_state_timeout";
  private final EsSettings settings;
  private Node node;

  public SearchServer(Props props) {
    this.settings = new EsSettings(props);
    new MinimumViableSystem()
      .checkWritableTempDir();
  }

  @Override
  public void start() {
    Jmx.register(EsSettingsMBean.OBJECT_NAME, settings);
    initBootstrap();
    Settings esSettings = settings.build();
    if (esSettings.getAsInt(MIMINUM_MASTER_NODES, 1) >= 2) {
      LOGGER.info("Elasticsearch is waiting {} for {} node(s) to be up to start.",
        esSettings.get(INITIAL_STATE_TIMEOUT),
        esSettings.get(MIMINUM_MASTER_NODES));
    }
    node = new Node(settings.build());
    try {
      node.start();
    } catch (NodeValidationException e) {
      throw new RuntimeException("Failed to start ES", e);
    }
  }

  // copied from https://github.com/elastic/elasticsearch/blob/v2.3.3/core/src/main/java/org/elasticsearch/bootstrap/Bootstrap.java
  private static void initBootstrap() {
    // init lucene random seed. it will use /dev/urandom where available:
    StringHelper.randomId();
  }

  @Override
  public Status getStatus() {
    boolean esStatus = node != null && node.client().admin().cluster().prepareHealth()
      .setWaitForYellowStatus()
      .setTimeout(TimeValue.timeValueSeconds(30L))
      .get()
      .getStatus() != ClusterHealthStatus.RED;
    if (esStatus) {
      return Status.OPERATIONAL;
    }
    return Status.DOWN;
  }

  @Override
  public void awaitStop() {
    try {
      while (node != null && !node.isClosed()) {
        Thread.sleep(200L);
      }
    } catch (InterruptedException e) {
      // Restore the interrupted status
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void stop() {
    if (node != null && !node.isClosed()) {
      try {
        node.close();
      } catch (IOException e) {
        LOGGER.error("Failed to stop ES cleanly", e);
      }
    }
    Jmx.unregister(EsSettingsMBean.OBJECT_NAME);
  }

  public static void main(String... args) {
    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(args);
    new SearchLogging().configure(entryPoint.getProps());
    SearchServer searchServer = new SearchServer(entryPoint.getProps());
    entryPoint.launch(searchServer);
  }
}
