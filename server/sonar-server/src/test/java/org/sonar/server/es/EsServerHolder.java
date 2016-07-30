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
package org.sonar.server.es;

import java.io.File;
import java.util.Properties;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.sonar.process.LoopbackAddress;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.search.SearchServer;
import org.sonar.test.TestUtils;

public class EsServerHolder {

  private static EsServerHolder HOLDER = null;
  private final String clusterName;
  private final int port;
  private final String hostName;
  private final File homeDir;
  private final SearchServer server;

  private EsServerHolder(SearchServer server, String clusterName, int port, String hostName, File homeDir) {
    this.server = server;
    this.clusterName = clusterName;
    this.port = port;
    this.hostName = hostName;
    this.homeDir = homeDir;
  }

  public String getClusterName() {
    return clusterName;
  }

  public int getPort() {
    return port;
  }

  public String getHostName() {
    return hostName;
  }

  public SearchServer getServer() {
    return server;
  }

  public File getHomeDir() {
    return homeDir;
  }

  private void reset() {
    TransportClient client = TransportClient.builder().settings(Settings.builder()
      .put("network.bind_host", "localhost")
      .put("cluster.name", clusterName)
      .build()).build();
    client.addTransportAddress(new InetSocketTransportAddress(LoopbackAddress.get(), port));

    // wait for node to be ready
    client.admin().cluster().prepareHealth()
      .setWaitForGreenStatus()
      .get();

    // delete the indices created by previous tests
    DeleteIndexResponse response = client.admin().indices().prepareDelete("_all").get();
    if (!response.isAcknowledged()) {
      throw new IllegalStateException("Fail to delete all indices");
    }
    client.close();
  }

  public static synchronized EsServerHolder get() {
    if (HOLDER == null) {
      File homeDir = TestUtils.newTempDir("tmp-es-");
      homeDir.delete();
      homeDir.mkdir();

      String clusterName = "testCluster";
      String hostName = "127.0.0.1";
      int port = NetworkUtils.freePort();

      Properties properties = new Properties();
      properties.setProperty(ProcessProperties.SEARCH_CLUSTER_NAME, clusterName);
      properties.setProperty(ProcessProperties.SEARCH_PORT, String.valueOf(port));
      properties.setProperty(ProcessProperties.SEARCH_HOST, hostName);
      properties.setProperty(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
      properties.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, homeDir.getAbsolutePath());
      SearchServer server = new SearchServer(new Props(properties));
      server.start();
      HOLDER = new EsServerHolder(server, clusterName, port, hostName, homeDir);
    }
    HOLDER.reset();
    return HOLDER;
  }
}
