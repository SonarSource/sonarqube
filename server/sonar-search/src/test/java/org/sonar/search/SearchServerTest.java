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
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessConstants;
import org.sonar.process.Props;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class SearchServerTest {

  static final String CLUSTER_NAME = "unitTest";

  int port = NetworkUtils.freePort();
  SearchServer searchServer;
  Client client;

  @Rule
  public Timeout timeout = new Timeout(60000);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @After
  public void tearDown() throws Exception {
    if (searchServer != null) {
      searchServer.stop();
      searchServer.awaitStop();
    }
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void start_stop_server() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessConstants.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessConstants.CLUSTER_NAME, CLUSTER_NAME);
    props.set(ProcessConstants.CLUSTER_NODE_NAME, "test");
    props.set(ProcessConstants.PATH_HOME, temp.newFolder().getAbsolutePath());

    searchServer = new SearchServer(props);
    searchServer.start();
    assertThat(searchServer.isReady()).isTrue();

    client = getSearchClient();

    searchServer.stop();
    searchServer.awaitStop();
    searchServer = null;
    try {
      assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isNotEqualTo(ClusterHealthStatus.GREEN);
    } catch (NoNodeAvailableException exception) {
      assertThat(exception.getMessage()).isEqualTo("No node available");
    }
  }

  @Test
  public void slave_success_replication() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessConstants.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessConstants.CLUSTER_ACTIVATE, "true");
    props.set(ProcessConstants.CLUSTER_MASTER, "true");
    props.set(ProcessConstants.CLUSTER_NODE_NAME, "MASTER");
    props.set(ProcessConstants.CLUSTER_NAME, CLUSTER_NAME);
    props.set(ProcessConstants.PATH_HOME, temp.newFolder().getAbsolutePath());
    searchServer = new SearchServer(props);
    assertThat(searchServer).isNotNull();

    searchServer.start();
    assertThat(searchServer.isReady()).isTrue();

    client = getSearchClient();
    client.admin().indices().prepareCreate("test").get();

    // start a slave
    props = new Props(new Properties());
    props.set(ProcessConstants.CLUSTER_ACTIVATE, "true");
    props.set(ProcessConstants.CLUSTER_MASTER_HOST, "localhost:" + port);
    props.set(ProcessConstants.CLUSTER_NAME, CLUSTER_NAME);
    props.set(ProcessConstants.CLUSTER_NODE_NAME, "SLAVE");
    props.set(ProcessConstants.SEARCH_PORT, String.valueOf(NetworkUtils.freePort()));
    props.set(ProcessConstants.PATH_HOME, temp.newFolder().getAbsolutePath());
    SearchServer slaveServer = new SearchServer(props);
    assertThat(slaveServer).isNotNull();

    slaveServer.start();
    assertThat(slaveServer.isReady()).isTrue();

    assertThat(client.admin().cluster().prepareClusterStats().get()
      .getNodesStats().getCounts().getTotal()).isEqualTo(2);

    searchServer.stop();
    slaveServer.stop();
    searchServer.awaitStop();
    slaveServer.awaitStop();
  }

  @Test
  public void slave_failed_replication() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessConstants.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessConstants.CLUSTER_ACTIVATE, "false");
    props.set(ProcessConstants.CLUSTER_NAME, CLUSTER_NAME);
    props.set(ProcessConstants.CLUSTER_NODE_NAME, "NOT_MASTER");
    props.set(ProcessConstants.PATH_HOME, temp.newFolder().getAbsolutePath());
    searchServer = new SearchServer(props);
    assertThat(searchServer).isNotNull();

    searchServer.start();
    assertThat(searchServer.isReady()).isTrue();

    client = getSearchClient();
    client.admin().indices().prepareCreate("test").get();

    // start a slave
    props = new Props(new Properties());
    props.set(ProcessConstants.CLUSTER_ACTIVATE, "true");
    props.set(ProcessConstants.CLUSTER_MASTER, "false");
    props.set(ProcessConstants.CLUSTER_MASTER_HOST, "localhost:" + port);
    props.set(ProcessConstants.CLUSTER_NAME, CLUSTER_NAME);
    props.set(ProcessConstants.CLUSTER_NODE_NAME, "SLAVE");
    props.set(ProcessConstants.SEARCH_PORT, String.valueOf(NetworkUtils.freePort()));
    props.set(ProcessConstants.PATH_HOME, temp.newFolder().getAbsolutePath());
    SearchServer slaveServer = new SearchServer(props);
    assertThat(slaveServer).isNotNull();

    try {
      slaveServer.start();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Index configuration is not set to cluster. Please start the master node with property " + ProcessConstants.CLUSTER_ACTIVATE + "=true");
    }

    assertThat(client.admin().cluster().prepareClusterStats().get()
      .getNodesStats().getCounts().getTotal()).isEqualTo(1);

    slaveServer.stop();
    slaveServer.awaitStop();
  }

  private Client getSearchClient() {
    Settings settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", CLUSTER_NAME).build();
    Client client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress("localhost", port));
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);
    return client;
  }
}
