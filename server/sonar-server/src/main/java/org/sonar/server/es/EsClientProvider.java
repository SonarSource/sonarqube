/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.index.reindex.ReindexPlugin;
import org.elasticsearch.join.ParentJoinPlugin;
import org.elasticsearch.percolator.PercolatorPlugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.cluster.NodeType;

import static java.util.Collections.unmodifiableList;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_TYPE;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;
import static org.sonar.process.cluster.NodeType.SEARCH;

@ComputeEngineSide
@ServerSide
public class EsClientProvider extends ProviderAdapter {

  private static final Logger LOGGER = Loggers.get(EsClientProvider.class);

  private EsClient cache;

  public EsClient provide(Configuration config) {
    if (cache == null) {
      Settings.Builder esSettings = Settings.builder();

      // mandatory property defined by bootstrap process
      esSettings.put("cluster.name", config.get(CLUSTER_NAME.getKey()).get());

      boolean clusterEnabled = config.getBoolean(CLUSTER_ENABLED.getKey()).orElse(false);
      boolean searchNode = !clusterEnabled || SEARCH.equals(NodeType.parse(config.get(CLUSTER_NODE_TYPE.getKey()).orElse(null)));
      final TransportClient nativeClient = new MinimalTransportClient(esSettings.build());
      if (clusterEnabled && !searchNode) {
        esSettings.put("client.transport.sniff", true);
        Arrays.stream(config.getStringArray(CLUSTER_SEARCH_HOSTS.getKey()))
          .map(HostAndPort::fromString)
          .forEach(h -> addHostToClient(h, nativeClient));
        LOGGER.info("Connected to remote Elasticsearch: [{}]", displayedAddresses(nativeClient));
      } else {
        HostAndPort host = HostAndPort.fromParts(config.get(SEARCH_HOST.getKey()).get(), config.getInt(SEARCH_PORT.getKey()).get());
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

  static class MinimalTransportClient extends TransportClient {

    MinimalTransportClient(Settings settings) {
      super(settings, unmodifiableList(Arrays.asList(Netty4Plugin.class, ReindexPlugin.class, PercolatorPlugin.class, ParentJoinPlugin.class)));
    }

    @Override
    public void close() {
      super.close();
      if (!NetworkModule.TRANSPORT_TYPE_SETTING.exists(settings)
        || NetworkModule.TRANSPORT_TYPE_SETTING.get(settings).equals(Netty4Plugin.NETTY_TRANSPORT_NAME)) {
        try {
          GlobalEventExecutor.INSTANCE.awaitInactivity(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        try {
          ThreadDeathWatcher.awaitInactivity(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
