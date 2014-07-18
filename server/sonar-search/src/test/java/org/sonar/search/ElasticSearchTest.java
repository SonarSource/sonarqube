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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ElasticSearchTest {

  File tempDirectory;
  ElasticSearch elasticSearch;
  int freePort;
  int freeESPort;

  @Before
  public void setup() throws IOException {
    tempDirectory = FileUtils.getTempDirectory();

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
      mbeanServer.unregisterMBean(Process.objectNameFor("ES"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Test
  public void missing_properties() throws IOException, MBeanRegistrationException, InstanceNotFoundException {

    Properties properties = new Properties();
    properties.setProperty(Process.SONAR_HOME, FileUtils.getTempDirectoryPath());
    properties.setProperty(Process.NAME_PROPERTY, "ES");
    properties.setProperty(Process.PORT_PROPERTY, Integer.toString(freePort));

    try {
      elasticSearch = new ElasticSearch(Props.create(properties));
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_ES_HOME);
    }

    properties.setProperty(ElasticSearch.ES_HOME_PROPERTY, tempDirectory.getAbsolutePath());
    try {
      resetMBeanServer();
      elasticSearch = new ElasticSearch(Props.create(properties));
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(ElasticSearch.MISSING_ES_PORT);
    }
    resetMBeanServer();

    properties.setProperty(ElasticSearch.ES_PORT_PROPERTY, Integer.toString(freeESPort));
    elasticSearch = new ElasticSearch(Props.create(properties));
    assertThat(elasticSearch).isNotNull();
  }

  @Test
  public void can_connect() throws SocketException {

    Properties properties = new Properties();
    properties.setProperty(Process.SONAR_HOME, FileUtils.getTempDirectoryPath());
    properties.setProperty(Process.NAME_PROPERTY, "ES");
    properties.setProperty(ElasticSearch.ES_HOME_PROPERTY, tempDirectory.getAbsolutePath());
    properties.setProperty(ElasticSearch.ES_PORT_PROPERTY, Integer.toString(freeESPort));

    elasticSearch = new ElasticSearch(Props.create(properties));
    new Thread(new Runnable() {
      @Override
      public void run() {
        elasticSearch.start();
      }
    }).start();
    assertThat(elasticSearch.isReady()).isFalse();

    int count = 0;
    while (!elasticSearch.isReady() && count < 100) {
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
    elasticSearch.terminate(true);
    try {
      client.admin().cluster().prepareClusterStats().get().getStatus();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("No node available");
    }
  }
}