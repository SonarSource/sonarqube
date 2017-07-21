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
package org.sonar.server.es;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.search.SearchServer;

public class EsServerHolder {

  private static EsServerHolder HOLDER = null;
  private final String clusterName;
  private final InetAddress address;
  private final int port;
  private final File homeDir;
  private final SearchServer server;

  private EsServerHolder(SearchServer server, String clusterName, InetAddress address, int port, File homeDir) {
    this.server = server;
    this.clusterName = clusterName;
    this.address = address;
    this.port = port;
    this.homeDir = homeDir;
  }

  public String getClusterName() {
    return clusterName;
  }

  public int getPort() {
    return port;
  }

  public InetAddress getAddress() {
    return address;
  }

  public SearchServer getServer() {
    return server;
  }

  public File getHomeDir() {
    return homeDir;
  }

  private void reset() {
    TransportClient client = new PreBuiltTransportClient(Settings.builder()
      .put("network.bind_host", "localhost")
      .put("cluster.name", clusterName)
      .build(), Collections.emptyList()) {};
    client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getLoopbackAddress(), port));

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

  public static synchronized EsServerHolder get() throws IOException {
    if (HOLDER == null) {
      File homeDir = Files.createTempDirectory("tmp-es-").toFile();
      homeDir.delete();
      homeDir.mkdir();

      String clusterName = "testCluster";
      InetAddress address = InetAddress.getLoopbackAddress();
      int port = NetworkUtils.getNextAvailablePort(address);

      Properties properties = new Properties();
      properties.setProperty(ProcessProperties.CLUSTER_NAME, clusterName);
      properties.setProperty(ProcessProperties.SEARCH_PORT, String.valueOf(port));
      properties.setProperty(ProcessProperties.SEARCH_HOST, address.getHostAddress());
      properties.setProperty(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
      properties.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, homeDir.getAbsolutePath());
      SearchServer server = new SearchServer(new Props(properties));
      server.start();
      HOLDER = new EsServerHolder(server, clusterName, address, port, homeDir);
    }
    HOLDER.reset();
    return HOLDER;
  }
}
