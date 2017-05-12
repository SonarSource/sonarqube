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
import org.slf4j.Logger;
import org.sonar.process.Monitored;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SearchServerTest {

  private static final String A_CLUSTER_NAME = "a_cluster";
  private static final String A_NODE_NAME = "a_node";

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private int port = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
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
  public void log_information_on_startup() throws IOException {
    Props props = getClusterProperties();
    props.set(ProcessProperties.CLUSTER_ENABLED, "true");
    props.set(ProcessProperties.SEARCH_MINIMUM_MASTER_NODES, "2");
    // Set the timeout to 1sec
    props.set(ProcessProperties.SEARCH_INITIAL_STATE_TIMEOUT, "1s");
    underTest = new SearchServer(props);
    Logger logger = mock(Logger.class);
    underTest.LOGGER = logger;
    underTest.start();
    verify(logger).info(eq("Elasticsearch is waiting {} for {} node(s) to be up to start."), eq("1s"), eq("2"));
  }

  @Test
  public void no_log_information_on_startup() throws IOException {
    Props props = getClusterProperties();
    props.set(ProcessProperties.SEARCH_MINIMUM_MASTER_NODES, "1");
    // Set the timeout to 1sec
    props.set(ProcessProperties.SEARCH_INITIAL_STATE_TIMEOUT, "1s");
    underTest = new SearchServer(props);
    Logger logger = mock(Logger.class);
    underTest.LOGGER = logger;
    underTest.start();
    verify(logger, never()).info(eq("Elasticsearch is waiting {} for {} node(s) to be up to start."), eq("1s"), eq("2"));
  }

  @Test
  public void start_stop_server() throws Exception {
    underTest = new SearchServer(getClusterProperties());
    underTest.start();
    assertThat(underTest.getStatus()).isEqualTo(Monitored.Status.OPERATIONAL);

    Settings settings = Settings.builder().put("cluster.name", A_CLUSTER_NAME).build();
    client = TransportClient.builder().settings(settings).build()
      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), port));
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

  private Props getClusterProperties() throws IOException {
    Props props = new Props(new Properties());
    // the following properties have always default values (see ProcessProperties)
    InetAddress host = InetAddress.getLoopbackAddress();
    props.set(ProcessProperties.SEARCH_HOST, host.getHostAddress());
    props.set(ProcessProperties.SEARCH_PORT, String.valueOf(port));
    props.set(ProcessProperties.CLUSTER_NAME, A_CLUSTER_NAME);
    props.set(EsSettings.CLUSTER_SEARCH_NODE_NAME, A_NODE_NAME);
    props.set(ProcessProperties.PATH_HOME, temp.newFolder().getAbsolutePath());
    props.set(ProcessEntryPoint.PROPERTY_SHARED_PATH, temp.newFolder().getAbsolutePath());
    return props;
  }
}
