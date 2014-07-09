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
import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import static org.fest.assertions.Assertions.assertThat;

public class ElasticSearchTest {

  DatagramSocket socket;
  ElasticSearch elasticSearch;

  @Before
  public void setup() throws SocketException {
    socket = new DatagramSocket(0);
    elasticSearch = new ElasticSearch("test", socket.getLocalPort());
  }

  @After
  public void tearDown() {
    socket.close();
    elasticSearch.shutdown();
  }

  @Test
  public void can_connect() throws IOException, InterruptedException {

    // 0 assert that application is running
    assertHeartBeat();

    Settings settings = ImmutableSettings.settingsBuilder()
      .put("client.transport.sniff", true)
      .put("cluster.name", "sonarqube")
      .build();
    TransportClient client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));


    // 0 assert that we have a OK cluster available
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);

    // 1 assert that we get a heartBeat from the application
    assertHeartBeat();
  }

  private void assertHeartBeat() throws IOException {
    DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
    socket.receive(packet);
    assertThat(packet.getData()).isNotNull();
  }
}