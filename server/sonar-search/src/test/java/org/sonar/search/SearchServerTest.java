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
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import java.net.InetAddress;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchServerTest {

  static final String CLUSTER_NAME = "unitTest";

  int port = NetworkUtils.freePort();
  SearchServer searchServer;
  Client client;

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.seconds(60));

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
    // the following properties have always default values (see ProcessProperties)
    String host = InetAddress.getLocalHost().getHostAddress();
    props.set(ProcessProperties.SEARCH_HOST, host);
    props.set(ProcessProperties.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessProperties.CLUSTER_NAME, CLUSTER_NAME);
    props.set(ProcessProperties.CLUSTER_NODE_NAME, "test");
    props.set(ProcessProperties.PATH_HOME, temp.newFolder().getAbsolutePath());

    searchServer = new SearchServer(props);
    searchServer.start();
    assertThat(searchServer.isReady()).isTrue();

    Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", CLUSTER_NAME).build();
    client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress(host, port));
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);

    searchServer.stop();
    searchServer.awaitStop();
    searchServer = null;
    try {
      client.admin().cluster().prepareClusterStats().get();
      fail();
    } catch (NoNodeAvailableException exception) {
      // ok
    }
  }
}
