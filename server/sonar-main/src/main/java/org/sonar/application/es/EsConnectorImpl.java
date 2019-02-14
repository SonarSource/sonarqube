/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.application.es;

import com.google.common.net.HostAndPort;
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.elasticsearch.common.unit.TimeValue.timeValueSeconds;

public class EsConnectorImpl implements EsConnector {

  private static final Logger LOG = LoggerFactory.getLogger(EsConnectorImpl.class);

  private final AtomicReference<TransportClient> transportClient = new AtomicReference<>(null);
  private final String clusterName;
  private final Set<HostAndPort> hostAndPorts;

  public EsConnectorImpl(String clusterName, Set<HostAndPort> hostAndPorts) {
    this.clusterName = clusterName;
    this.hostAndPorts = hostAndPorts;
  }

  @Override
  public ClusterHealthStatus getClusterHealthStatus() {
    return getTransportClient().admin().cluster()
      .health(new ClusterHealthRequest().waitForStatus(ClusterHealthStatus.YELLOW).timeout(timeValueSeconds(30)))
      .actionGet().getStatus();
  }

  @Override
  public void stop() {
    transportClient.set(null);
  }

  private TransportClient getTransportClient() {
    TransportClient res = this.transportClient.get();
    if (res == null) {
      res = buildTransportClient();
      if (this.transportClient.compareAndSet(null, res)) {
        return res;
      }
      return this.transportClient.get();
    }
    return res;
  }

  private TransportClient buildTransportClient() {
    Settings.Builder esSettings = Settings.builder();

    // mandatory property defined by bootstrap process
    esSettings.put("cluster.name", clusterName);

    TransportClient nativeClient = new MinimalTransportClient(esSettings.build(), hostAndPorts);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Connected to Elasticsearch node: [{}]", displayedAddresses(nativeClient));
    }
    return nativeClient;
  }

  private static String displayedAddresses(TransportClient nativeClient) {
    return nativeClient.transportAddresses().stream().map(TransportAddress::toString).collect(Collectors.joining(", "));
  }

  private static class MinimalTransportClient extends TransportClient {

    public MinimalTransportClient(Settings settings, Set<HostAndPort> hostAndPorts) {
      super(settings, unmodifiableList(singletonList(Netty4Plugin.class)));

      boolean connectedToOneHost = false;
      for (HostAndPort hostAndPort : hostAndPorts) {
        try {
          addTransportAddress(new TransportAddress(InetAddress.getByName(hostAndPort.getHostText()), hostAndPort.getPortOrDefault(9001)));
          connectedToOneHost = true;
        } catch (UnknownHostException e) {
          LOG.debug("Can not resolve host [" + hostAndPort.getHostText() + "]", e);
        }
      }
      if (!connectedToOneHost) {
        throw new IllegalStateException(format("Can not connect to one node from [%s]",
          hostAndPorts.stream()
            .map(h -> format("%s:%d", h.getHostText(), h.getPortOrDefault(9001)))
            .collect(Collectors.joining(","))));
      }
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
