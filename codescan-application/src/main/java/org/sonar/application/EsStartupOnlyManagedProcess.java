/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static org.sonar.application.EsStartupOnlyManagedProcess.Status.CONNECTION_REFUSED;
import static org.sonar.application.EsStartupOnlyManagedProcess.Status.GREEN;
import static org.sonar.application.EsStartupOnlyManagedProcess.Status.KO;
import static org.sonar.application.EsStartupOnlyManagedProcess.Status.RED;
import static org.sonar.application.EsStartupOnlyManagedProcess.Status.YELLOW;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_SEARCH_HOSTS;

import com.google.common.net.HostAndPort;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.discovery.MasterNotDiscoveredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.es.EsConnector;
import org.sonar.application.es.EsConnectorImpl;
import org.sonar.application.process.ManagedProcess;
import org.sonar.process.Props;

public class EsStartupOnlyManagedProcess implements ManagedProcess {

    private static final Logger LOG = LoggerFactory.getLogger(EsStartupOnlyManagedProcess.class);
    private static final int WAIT_FOR_UP_DELAY_IN_MILLIS = 1000;
    private static final int WAIT_FOR_UP_TIMEOUT = 10 * 60; /* 1min */

    private volatile boolean nodeOperational = false;
    private final AtomicBoolean firstMasterNotDiscoveredLog = new AtomicBoolean(true);
    private final EsConnector esConnector;
    private boolean isActive = true;

    enum Status {
        CONNECTION_REFUSED, KO, RED, YELLOW, GREEN
    }

    public EsStartupOnlyManagedProcess(Props props) {
        String searchHosts = props.nonNullValue(CLUSTER_SEARCH_HOSTS.getKey());
        Set<HostAndPort> hostAndPorts = Arrays.stream(searchHosts.split(","))
                .map(HostAndPort::fromString)
                .collect(Collectors.toSet());
        this.esConnector = new EsConnectorImpl(hostAndPorts, null, null, null);
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
        if (nodeOperational) {
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
                nodeOperational = true;
            }
        }
        return nodeOperational;
    }

    private boolean checkOperational() throws InterruptedException {
        int i = 0;
        Status status = checkStatus();
        do {
            LOG.debug("Elasticsearch status: [{}]", status);
            if (status != Status.CONNECTION_REFUSED) {
                break;
            } else {
                Thread.sleep(WAIT_FOR_UP_DELAY_IN_MILLIS);
                i++;
                status = checkStatus();
            }
        } while (i < WAIT_FOR_UP_TIMEOUT);
        return status == YELLOW || status == GREEN;
    }

    private Status checkStatus() {
        try {
            switch (esConnector.getClusterHealthStatus().orElse(ClusterHealthStatus.RED)) {
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
