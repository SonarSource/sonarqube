/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import org.sonar.api.server.ServerSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.exceptions.ForbiddenException;

@ServerSide
public class SonarLintClientsRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(SonarLintClientsRegistry.class);

  private final SonarLintClientPermissionsValidator sonarLintClientPermissionsValidator;
  private final List<SonarLintClient> clients = new CopyOnWriteArrayList<>();

  public SonarLintClientsRegistry(SonarLintClientPermissionsValidator permissionsValidator) {
    this.sonarLintClientPermissionsValidator = permissionsValidator;
  }

  public void registerClient(SonarLintClient sonarLintClient) {
    clients.add(sonarLintClient);
    sonarLintClient.scheduleHeartbeat();
    sonarLintClient.addListener(new SonarLintClientEventsListener(sonarLintClient));
    LOG.debug("Registering new SonarLint client");
  }

  public void unregisterClient(SonarLintClient client) {
    client.close();
    clients.remove(client);
    LOG.debug("Removing SonarLint client");
  }

  public List<SonarLintClient> getClients() {
    return clients;
  }

  public long countConnectedClients() {
    return clients.size();
  }

  public void broadcastMessage(SonarLintPushEvent event) {
    clients.stream().filter(client -> isRelevantEvent(event, client))
      .forEach(c -> {
        Set<String> clientProjectUuids = new HashSet<>(c.getClientProjectUuids());
        clientProjectUuids.retainAll(Set.of(event.getProjectUuid()));
        try {
          sonarLintClientPermissionsValidator.validateUserCanReceivePushEventForProjectUuids(c.getUserUuid(), clientProjectUuids);
          c.writeAndFlush(event.serialize());
        } catch (ForbiddenException forbiddenException) {
          logClientUnauthenticated(forbiddenException);
          unregisterClient(c);
        } catch (IllegalStateException | IOException e) {
          logUnexpectedError(e);
          unregisterClient(c);
        }
      });
  }

  private static boolean isRelevantEvent(SonarLintPushEvent event, SonarLintClient client) {
    return client.getClientProjectUuids().contains(event.getProjectUuid())
      && (!event.getName().equals("RuleSetChanged") || client.getLanguages().contains(event.getLanguage()));
  }

  private static void logUnexpectedError(Exception e) {
    LOG.error("Unable to send message to a client: " + e.getMessage());
  }

  private static void logClientUnauthenticated(ForbiddenException forbiddenException) {
    LOG.debug("Client is no longer authenticated: " + forbiddenException.getMessage());
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
      // nothing to do on start
    }

    @Override
    public void onTimeout(AsyncEvent event) {
      unregisterClient(client);
    }
  }

}
