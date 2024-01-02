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
package org.sonar.application.process;

import java.net.ConnectException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.application.es.EsConnector;
import org.sonar.process.ProcessId;

import static org.sonar.application.process.EsManagedProcess.Status.CONNECTION_REFUSED;
import static org.sonar.application.process.EsManagedProcess.Status.GREEN;
import static org.sonar.application.process.EsManagedProcess.Status.KO;
import static org.sonar.application.process.EsManagedProcess.Status.RED;
import static org.sonar.application.process.EsManagedProcess.Status.YELLOW;

public class EsManagedProcess extends AbstractManagedProcess {
  private static final Logger LOG = LoggerFactory.getLogger(EsManagedProcess.class);
  private static final int WAIT_FOR_UP_DELAY_IN_MILLIS = 100;

  private volatile boolean nodeOperational = false;
  private final int waitForUpTimeout;
  private final EsConnector esConnector;

  public EsManagedProcess(Process process, ProcessId processId, EsConnector esConnector) {
    this(process, processId, esConnector, 10 * 60);
  }

  EsManagedProcess(Process process, ProcessId processId, EsConnector esConnector, int waitForUpTimeout) {
    super(process, processId);
    this.esConnector = esConnector;
    this.waitForUpTimeout = waitForUpTimeout;
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
        esConnector.stop();
        nodeOperational = true;
      }
    }
    return nodeOperational;
  }

  private boolean checkOperational() throws InterruptedException {
    int i = 0;
    Status status = checkStatus();
    do {
      if (status != Status.CONNECTION_REFUSED) {
        break;
      } else {
        Thread.sleep(WAIT_FOR_UP_DELAY_IN_MILLIS);
        i++;
        status = checkStatus();
      }
    } while (i < waitForUpTimeout);
    return status == YELLOW || status == GREEN;
  }

  private Status checkStatus() {
    try {
      return esConnector.getClusterHealthStatus()
        .map(EsManagedProcess::convert)
        .orElse(CONNECTION_REFUSED);
    } catch (ElasticsearchException e) {
      if (e.getRootCause() instanceof ConnectException) {
        return CONNECTION_REFUSED;
      }
      LOG.error("Failed to check status", e);
      return KO;
    } catch (Exception e) {
      LOG.error("Failed to check status", e);
      return KO;
    }
  }

  private static Status convert(ClusterHealthStatus clusterHealthStatus) {
    switch (clusterHealthStatus) {
      case GREEN:
        return GREEN;
      case YELLOW:
        return YELLOW;
      case RED:
        return RED;
      default:
        return KO;
    }
  }

  enum Status {
    CONNECTION_REFUSED, KO, RED, YELLOW, GREEN
  }

  @Override
  public void askForStop() {
    askForHardStop();
  }

  @Override
  public void askForHardStop() {
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
