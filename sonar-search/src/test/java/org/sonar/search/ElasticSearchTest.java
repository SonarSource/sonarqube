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

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.process.Process;
import org.sonar.process.Props;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ElasticSearchTest {

  DatagramSocket socket;
  File tempDirectory;
  ElasticSearch elasticSearch;

  @Before
  public void setup() throws IOException {
    socket = new DatagramSocket(0);
    tempDirectory = FileUtils.getTempDirectory();
    FileUtils.forceMkdir(tempDirectory);
  }

  @After
  public void tearDown() throws IOException {
    socket.close();
    FileUtils.deleteDirectory(tempDirectory);
  }

  @Test
  public void missing_properties() {

    Properties properties = new Properties();
    properties.setProperty(Process.NAME_PROPERTY, "ES");
    properties.setProperty(Process.HEARTBEAT_PROPERTY, Integer.toString(socket.getLocalPort()));

    try {
      new ElasticSearch(Props.create(properties));
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_ES_HOME);
    }

    properties.setProperty(ElasticSearch.ES_HOME_PROPERTY, tempDirectory.getAbsolutePath());
    try {
      new ElasticSearch(Props.create(properties));
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_ES_PORT);
    }
  }

  @Test
  public void can_connect() {

    int port = NetworkUtils.freePort();

    Properties properties = new Properties();
    properties.setProperty(Process.NAME_PROPERTY, "ES");
    properties.setProperty(Process.HEARTBEAT_PROPERTY, Integer.toString(socket.getLocalPort()));
    properties.setProperty(ElasticSearch.ES_HOME_PROPERTY, tempDirectory.getAbsolutePath());
    properties.setProperty(ElasticSearch.ES_PORT_PROPERTY, Integer.toString(port));

    elasticSearch = new ElasticSearch(Props.create(properties));


    // 0 assert that application is running
    assertHeartBeat();

    Settings settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", "sonarqube")
      .build();
    TransportClient client = new TransportClient(settings)
      .addTransportAddress(new InetSocketTransportAddress("localhost", port));

    // 0 assert that we have a OK cluster available
    assertThat(client.admin().cluster().prepareClusterStats().get().getStatus()).isEqualTo(ClusterHealthStatus.GREEN);

    // 1 assert that we get a heartBeat from the application
    assertHeartBeat();

    // 2 assert that we can shut down ES
    elasticSearch.shutdown();
    try {
      client.admin().cluster().prepareClusterStats().get().getStatus();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("No node available");
    }
  }

  private void assertHeartBeat() {
    DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
    try {
      socket.setSoTimeout(1200);
      socket.receive(packet);
    } catch (Exception e) {
      throw new IllegalStateException("Did not get a heartbeat");
    }
    assertThat(packet.getData()).isNotNull();
  }
}