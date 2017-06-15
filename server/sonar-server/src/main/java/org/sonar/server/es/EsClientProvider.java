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

import com.google.common.net.HostAndPort;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;

@ComputeEngineSide
@ServerSide
public class EsClientProvider extends ProviderAdapter {

  private static final Logger LOGGER = Loggers.get(EsClientProvider.class);

  private EsClient cache;

  public EsClient provide(Settings settings) {
    if (cache == null) {
      TransportClient nativeClient;
      org.elasticsearch.common.settings.Settings.Builder esSettings = org.elasticsearch.common.settings.Settings.builder();

      // mandatory property defined by bootstrap process
      esSettings.put("cluster.name", settings.getString(ProcessProperties.CLUSTER_NAME));

      boolean clusterEnabled = settings.getBoolean(ProcessProperties.CLUSTER_ENABLED);
      if (clusterEnabled && settings.getBoolean(ProcessProperties.CLUSTER_SEARCH_DISABLED)) {
        esSettings.put("client.transport.sniff", true);
        nativeClient = TransportClient.builder().settings(esSettings).build();
        Arrays.stream(settings.getStringArray(ProcessProperties.CLUSTER_SEARCH_HOSTS))
          .map(HostAndPort::fromString)
          .forEach(h -> addHostToClient(h, nativeClient));
        LOGGER.info("Connected to remote Elasticsearch: [{}]", displayedAddresses(nativeClient));
      } else {
        nativeClient = TransportClient.builder().settings(esSettings).build();
        HostAndPort host = HostAndPort.fromParts(settings.getString(ProcessProperties.SEARCH_HOST), settings.getInt(ProcessProperties.SEARCH_PORT));
        addHostToClient(host, nativeClient);
        LOGGER.info("Connected to local Elasticsearch: [{}]", displayedAddresses(nativeClient));
      }

      cache = new EsClient(nativeClient);
    }
    return cache;
  }

  private static void addHostToClient(HostAndPort host, TransportClient client) {
    try {
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host.getHostText()), host.getPortOrDefault(9001)));
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Can not resolve host [" + host + "]", e);
    }
  }

  private static String displayedAddresses(TransportClient nativeClient) {
    return nativeClient.transportAddresses().stream().map(TransportAddress::toString).collect(Collectors.joining(", "));
  }
}
