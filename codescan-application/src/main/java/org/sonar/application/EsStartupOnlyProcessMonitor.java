/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

package org.sonar.application;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.sonar.application.EsStartupOnlyProcessMonitor.Status.CONNECTION_REFUSED;
import static org.sonar.application.EsStartupOnlyProcessMonitor.Status.GREEN;
import static org.sonar.application.EsStartupOnlyProcessMonitor.Status.KO;
import static org.sonar.application.EsStartupOnlyProcessMonitor.Status.RED;
import static org.sonar.application.EsStartupOnlyProcessMonitor.Status.YELLOW;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NAME;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;

import com.google.common.net.HostAndPort;
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.elasticsearch.transport.Netty4Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.es.EsConnector;
import org.sonar.application.es.EsConnectorImpl;
import org.sonar.application.process.ManagedProcess;
import org.sonar.process.Props;

public class EsStartupOnlyProcessMonitor implements ManagedProcess {

    private static final Logger LOG = LoggerFactory.getLogger(EsStartupOnlyProcessMonitor.class);
    private static final int WAIT_FOR_UP_DELAY_IN_MILLIS = 1000;
    private static final int WAIT_FOR_UP_TIMEOUT = 10 * 60; /* 1min */

    private final AtomicBoolean nodeUp = new AtomicBoolean(false);
    private final AtomicBoolean nodeOperational = new AtomicBoolean(false);
    private final AtomicBoolean firstMasterNotDiscoveredLog = new AtomicBoolean(true);
    private final EsConnector esConnector;
    private final Props props;
    private AtomicReference<TransportClient> transportClient = new AtomicReference<>(null);
    private boolean isActive = true;

    enum Status {
        CONNECTION_REFUSED, KO, RED, YELLOW, GREEN
    }

    public EsStartupOnlyProcessMonitor(Props props) {
        String searchHosts = props.nonNullValue(CLUSTER_SEARCH_HOSTS.getKey());
        Set<HostAndPort> hostAndPorts = Arrays.stream(searchHosts.split(","))
                .map(HostAndPort::fromString)
                .collect(Collectors.toSet());
        this.esConnector = new EsConnectorImpl(props.nonNullValue(CLUSTER_NAME.getKey()), hostAndPorts);
        this.props = props;
    }

    @Override
    public void askForStop() {
        isActive = false;
    }

    @Override
    public void askForHardStop() {
        isActive = false;
    }

    @Override
    public boolean askedForRestart() {
        return false;
    }

    public void destroyForcibly() {
        isActive = false;
    }

    public boolean isAlive() {
        return isActive;
    }

    public void waitFor() throws InterruptedException {
        Thread.currentThread().join();
    }

    public void waitFor(long timeout, TimeUnit unit) throws InterruptedException {
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
            LOG.debug("Elasticsearch status: [{}]", status);
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
            switch (esConnector.getClusterHealthStatus()) {
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

    private static String displayedAddresses(TransportClient nativeClient) {
        return nativeClient.transportAddresses().stream().map(TransportAddress::toString)
                .collect(Collectors.joining(", "));
    }

    private TransportClient buildTransportClient() {
        Settings.Builder esSettings = Settings.builder();

        // mandatory property defined by bootstrap process
        esSettings.put("cluster.name", props.value("sonar.cluster.name"));

        TransportClient nativeClient = new MinimalTransportClient(esSettings.build());
        esSettings.put("client.transport.sniff", true);
        LOG.info("Hosts: {}", props.value(CLUSTER_SEARCH_HOSTS.getKey()));
        Arrays.stream(props.value(CLUSTER_SEARCH_HOSTS.getKey()).split(","))
                .map(HostAndPort::fromString)
                .forEach(h -> {
                    LOG.info("Checking ES server: {}", h);
                    addHostToClient(h, nativeClient);
                });

        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected to Elasticsearch node: [{}] on cluster [{}]", displayedAddresses(nativeClient),
                    props.value("sonar.cluster.name"));
        }
        return nativeClient;
    }

    private static void addHostToClient(HostAndPort host, TransportClient client) {
        try {
            client.addTransportAddress(
                    new TransportAddress(InetAddress.getByName(host.getHost()), host.getPortOrDefault(9001)));
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Can not resolve host [" + host + "]", e);
        }
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public InputStream getErrorStream() {
        return null;
    }

    @Override
    public void closeStreams() {
    }

    @Override
    public void acknowledgeAskForRestart() {
    }
}
