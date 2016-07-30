/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.net.InetAddress;
import java.util.Properties;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
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
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchServerTest {

  private static final String A_CLUSTER_NAME = "a_cluster";
  private static final String A_NODE_NAME = "a_node";

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.seconds(60));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private int port = NetworkUtils.freePort();
  private Client client;
  private SearchServer underTest;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.stop();
      underTest.awaitStop();
    }
    if (client != null) {
      client.close();
    }
  }

  @Test
  public void start_stop_server() throws Exception {
    Props props = new Props(new Properties());
    // the following properties have always default values (see ProcessProperties)
    InetAddress host = InetAddress.getLocalHost();
    props.set(ProcessProperties.SEARCH_HOST, host.getHostAddress());
    props.set(ProcessProperties.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessProperties.SEARCH_CLUSTER_NAME, A_CLUSTER_NAME);
    props.set(EsSettings.CLUSTER_SEARCH_NODE_NAME, A_NODE_NAME);
    props.set(ProcessProperties.PATH_HOME, temp.newFolder().getAbsolutePath());
    props.set(ProcessEntryPoint.PROPERTY_SHARED_PATH, temp.newFolder().getAbsolutePath());

    underTest = new SearchServer(props);
    underTest.start();
    assertThat(underTest.isUp()).isTrue();

    Settings settings = Settings.builder().put("cluster.name", A_CLUSTER_NAME).build();
    client = TransportClient.builder().settings(settings).build()
      .addTransportAddress(new InetSocketTransportAddress(host, port));
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);

    underTest.stop();
    underTest.awaitStop();
    underTest = null;
    try {
      client.admin().cluster().prepareClusterStats().get();
      fail();
    } catch (NoNodeAvailableException exception) {
      // ok
    }
  }
}
