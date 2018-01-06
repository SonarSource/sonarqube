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
package org.sonar.application.process;

import com.google.common.net.HostAndPort;
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.es.EsInstallation;
import org.sonar.process.ProcessId;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.sonar.application.process.EsProcessMonitor.Status.CONNECTION_REFUSED;
import static org.sonar.application.process.EsProcessMonitor.Status.GREEN;
import static org.sonar.application.process.EsProcessMonitor.Status.KO;
import static org.sonar.application.process.EsProcessMonitor.Status.RED;
import static org.sonar.application.process.EsProcessMonitor.Status.YELLOW;

public class EsProcessMonitor extends AbstractProcessMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(EsProcessMonitor.class);
  private static final int WAIT_FOR_UP_DELAY_IN_MILLIS = 100;
  private static final int WAIT_FOR_UP_TIMEOUT = 10 * 60; /* 1min */

  private final AtomicBoolean nodeUp = new AtomicBoolean(false);
  private final AtomicBoolean nodeOperational = new AtomicBoolean(false);
  private final AtomicBoolean firstMasterNotDiscoveredLog = new AtomicBoolean(true);
  private final EsInstallation esConfig;
  private final EsConnector esConnector;
  private AtomicReference<TransportClient> transportClient = new AtomicReference<>(null);

  public EsProcessMonitor(Process process, ProcessId processId, EsInstallation esConfig, EsConnector esConnector) {
    super(process, processId);
    this.esConfig = esConfig;
    this.esConnector = esConnector;
  }

  @Override
  public boolean isOperational() {
    if (nodeOperational.get()) {
      return true;
    }

    boolean flag = false;
    try {
      flag = checkOperational();
    } catch (InterruptedException e) {
      LOG.trace("Interrupted while checking ES node is operational", e);
      Thread.currentThread().interrupt();
    } finally {
      if (flag) {
        transportClient.set(null);
        nodeOperational.set(true);
      }
    }
    return nodeOperational.get();
  }

  private boolean checkOperational() throws InterruptedException {
    int i = 0;
    Status status = checkStatus();
    do {
      if (status != Status.CONNECTION_REFUSED) {
        nodeUp.set(true);
      } else {
        Thread.sleep(WAIT_FOR_UP_DELAY_IN_MILLIS);
        i++;
        status = checkStatus();
      }
    } while (!nodeUp.get() && i < WAIT_FOR_UP_TIMEOUT);
    return status == YELLOW || status == GREEN;
  }

  static class MinimalTransportClient extends TransportClient {

    MinimalTransportClient(Settings settings) {
      super(settings, unmodifiableList(singletonList(Netty4Plugin.class)));
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

  private Status checkStatus() {
    try {
      switch (esConnector.getClusterHealthStatus(getTransportClient())) {
        case GREEN:
          return GREEN;
        case YELLOW:
          return YELLOW;
        case RED:
          return RED;
        default:
          return KO;
      }
    } catch (NoNodeAvailableException e) {
      return CONNECTION_REFUSED;
    } catch (MasterNotDiscoveredException e) {
      if (firstMasterNotDiscoveredLog.getAndSet(false)) {
        LOG.info("Elasticsearch is waiting for a master to be elected. Did you start all the search nodes ?");
      }
      return KO;
    } catch (Exception e) {
      LOG.error("Failed to check status", e);
      return KO;
    }
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
    esSettings.put("cluster.name", esConfig.getClusterName());

    TransportClient nativeClient = new MinimalTransportClient(esSettings.build());
    HostAndPort host = HostAndPort.fromParts(esConfig.getHost(), esConfig.getPort());
    addHostToClient(host, nativeClient);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Connected to Elasticsearch node: [{}]", displayedAddresses(nativeClient));
    }
    return nativeClient;
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

  enum Status {
    CONNECTION_REFUSED, KO, RED, YELLOW, GREEN
  }

  @Override
  public void askForStop() {
    process.destroy();
  }

  @Override
  public boolean askedForRestart() {
    // ES does not support asking for restart
    return false;
  }

  @Override
  public void acknowledgeAskForRestart() {
    // nothing to do
  }
}
