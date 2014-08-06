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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.JmxUtils;
import org.sonar.process.MonitoredProcess;
import org.sonar.process.Props;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class SearchServerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  SearchServer searchServer;
  int freePort;
  int freeESPort;

  @Before
  public void setup() throws IOException {
    ServerSocket socket = new ServerSocket(0);
    freePort = socket.getLocalPort();
    socket.close();

    socket = new ServerSocket(0);
    freeESPort = socket.getLocalPort();
    socket.close();
  }

  @After
  public void tearDown() throws MBeanRegistrationException, InstanceNotFoundException {
    resetMBeanServer();
  }

  private void resetMBeanServer() throws MBeanRegistrationException, InstanceNotFoundException {
    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      mbeanServer.unregisterMBean(JmxUtils.objectName("ES"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void can_connect() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(MonitoredProcess.NAME_PROPERTY, "ES");
    properties.setProperty("sonar.path.data", temp.newFolder().getAbsolutePath());
    properties.setProperty("sonar.path.logs", temp.newFolder().getAbsolutePath());
    properties.setProperty("sonar.path.temp", temp.newFolder().getAbsolutePath());
    properties.setProperty(SearchServer.ES_PORT_PROPERTY, Integer.toString(freeESPort));
    properties.setProperty(SearchServer.ES_CLUSTER_PROPERTY, "sonarqube");

    searchServer = new SearchServer(new Props(properties));
    new Thread(new Runnable() {
      @Override
      public void run() {
        searchServer.start();
      }
    }).start();
    assertThat(searchServer.isReady()).isFalse();

    int count = 0;
    while (!searchServer.isReady() && count < 100) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      count++;
    }
    assertThat(count).isLessThan(100);

    Settings settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", "sonarqube")
      .build();
    TransportClient client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress("localhost", freeESPort));

    // 0 assert that we have a OK cluster available
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);

    // 2 assert that we can shut down ES
    searchServer.terminate();
    try {
      client.admin().cluster().prepareClusterStats().get().getStatus();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("No node available");
    }
  }
}
