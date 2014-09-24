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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.NetworkUtils;
import org.sonar.process.Props;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class SearchServerTest {

  Integer port;
  String cluster;

  @Before
  public void setUp() throws Exception {
    port = NetworkUtils.freePort();
    cluster = "unitTest";
  }

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test(timeout = 10000L)
  public void start_stop_server() throws Exception {

    Properties props = new Properties();
    props.put(SearchServer.ES_PORT_PROPERTY, port.toString());
    props.put(SearchServer.ES_CLUSTER_PROPERTY, cluster);
    props.put(SearchServer.SONAR_PATH_HOME, temp.getRoot().getAbsolutePath());

    SearchServer searchServer = new SearchServer(new Props(props));
    assertThat(searchServer).isNotNull();

    searchServer.start();
    assertThat(searchServer.isReady()).isTrue();

    Settings settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", cluster).build();
    Client client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress("localhost", port));
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);

    searchServer.stop();
    searchServer.awaitStop();
    try {
      assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isNotEqualTo(ClusterHealthStatus.GREEN);
    } catch (NoNodeAvailableException exception) {
      assertThat(exception.getMessage()).isEqualTo("No node available");
    }
  }
}
