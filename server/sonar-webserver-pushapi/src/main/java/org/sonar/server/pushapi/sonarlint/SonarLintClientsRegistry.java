/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.sonarlint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@ServerSide
public class SonarLintClientsRegistry {

  private static final Logger LOG = Loggers.get(SonarLintClientsRegistry.class);

  private final List<SonarLintClient> clients = new CopyOnWriteArrayList<>();

  public void registerClient(SonarLintClient sonarLintClient) {
    clients.add(sonarLintClient);
    sonarLintClient.scheduleHeartbeat();
    sonarLintClient.addListener(new SonarLintClientEventsListener(sonarLintClient));
    LOG.debug("Registering new SonarLint client");
  }

  public void unregisterClient(SonarLintClient client) {
    clients.remove(client);
    LOG.debug("Removing SonarLint client");
  }

  public long countConnectedClients() {
    return clients.size();
  }

  class SonarLintClientEventsListener implements AsyncListener {
    private final SonarLintClient client;

    public SonarLintClientEventsListener(SonarLintClient sonarLintClient) {
      this.client = sonarLintClient;
    }

    @Override
    public void onComplete(AsyncEvent event) {
      unregisterClient(client);
    }

    @Override
    public void onError(AsyncEvent event) {
      unregisterClient(client);
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
      //nothing to do on start
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      unregisterClient(client);
    }
  }

}
