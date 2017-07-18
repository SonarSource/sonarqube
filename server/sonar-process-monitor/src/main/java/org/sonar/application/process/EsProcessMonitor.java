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
package org.sonar.application.process;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsProcessMonitor extends AbstractProcessMonitor {
  private static final Logger LOG = LoggerFactory.getLogger(EsProcessMonitor.class);
  private static final int WAIT_FOR_UP_DELAY_IN_MILLIS = 100;
  private static final int WAIT_FOR_UP_TIMEOUT = 10 * 60; /* 1min */

  private final AtomicBoolean nodeUp = new AtomicBoolean(false);
  private final AtomicBoolean nodeOperational = new AtomicBoolean(false);
  private final URL healthCheckURL;

  public EsProcessMonitor(Process process, String url) throws MalformedURLException {
    super(process);
    this.healthCheckURL = new URL(url + "/_cluster/health?wait_for_status=yellow&timeout=30s");
  }

  @Override
  public boolean isOperational() {
    if (nodeOperational.get()) {
      return true;
    }

    try {
      boolean flag = checkOperational();
      if (flag) {
        nodeOperational.set(true);
      }
    } catch (InterruptedException e) {
      LOG.trace("Interrupted while checking ES node is operational", e);
      Thread.currentThread().interrupt();
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
    return status == Status.YELLOW || status == Status.GREEN;
  }

  private Status checkStatus() {
    try {
      URLConnection urlConnection = healthCheckURL.openConnection();
      urlConnection.connect();
      String response = IOUtils.toString(urlConnection.getInputStream());
      if (response.contains("\"status\":\"green\"")) {
        return Status.GREEN;
      } else if (response.contains("\"status\":\"yellow\"")) {
        return Status.YELLOW;
      } else if (response.contains("\"status\":\"red\"")) {
        return Status.RED;
      }
      return Status.KO;
    } catch (ConnectException e) {
      return Status.CONNECTION_REFUSED;
    } catch (IOException e) {
      LOG.error("Unexpected error occurred while checking ES node status using WebService API", e);
      return Status.KO;
    }
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
