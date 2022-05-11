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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.issue.IssueChangeListener;
import org.sonar.core.util.issue.IssueChangedEvent;
import org.sonar.core.util.rule.RuleActivationListener;
import org.sonar.core.util.rule.RuleSetChangedEvent;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.pushapi.issues.IssueChangeBroadcastUtils;
import org.sonar.server.pushapi.issues.IssueChangeEventsDistributor;
import org.sonar.server.pushapi.qualityprofile.RuleActivatorEventsDistributor;
import org.sonar.server.pushapi.qualityprofile.RuleSetChangeBroadcastUtils;

@ServerSide
public class SonarLintClientsRegistry implements RuleActivationListener, IssueChangeListener {

  private static final Logger LOG = Loggers.get(SonarLintClientsRegistry.class);

  private final SonarLintClientPermissionsValidator sonarLintClientPermissionsValidator;
  private final List<SonarLintClient> clients = new CopyOnWriteArrayList<>();
  private final RuleActivatorEventsDistributor ruleEventsDistributor;
  private final IssueChangeEventsDistributor issueChangeEventsDistributor;

  private boolean registeredToEvents = false;

  public SonarLintClientsRegistry(IssueChangeEventsDistributor issueChangeEventsDistributor,
    RuleActivatorEventsDistributor ruleActivatorEventsDistributor, SonarLintClientPermissionsValidator permissionsValidator) {
    this.issueChangeEventsDistributor = issueChangeEventsDistributor;
    this.sonarLintClientPermissionsValidator = permissionsValidator;
    this.ruleEventsDistributor = ruleActivatorEventsDistributor;
  }

  public void registerClient(SonarLintClient sonarLintClient) {
    ensureListeningToEvents();
    clients.add(sonarLintClient);
    sonarLintClient.scheduleHeartbeat();
    sonarLintClient.addListener(new SonarLintClientEventsListener(sonarLintClient));
    LOG.debug("Registering new SonarLint client");
  }

  private synchronized void ensureListeningToEvents() {
    if (registeredToEvents) {
      return;
    }
    try {
      ruleEventsDistributor.subscribe(this);
      issueChangeEventsDistributor.subscribe(this);
      registeredToEvents = true;
    } catch (RuntimeException e) {
      LOG.warn("Can not listen to rule activation or issue events for server push. Web Server might not have started fully yet.", e);
    }
  }

  public void unregisterClient(SonarLintClient client) {
    client.close();
    clients.remove(client);
    LOG.debug("Removing SonarLint client");
  }

  public long countConnectedClients() {
    return clients.size();
  }

  @Override
  public void listen(RuleSetChangedEvent ruleSetChangedEvent) {
    broadcastMessage(ruleSetChangedEvent, RuleSetChangeBroadcastUtils.getFilterForEvent(ruleSetChangedEvent));
  }

  @Override
  public void listen(IssueChangedEvent issueChangedEvent) {
    broadcastMessage(issueChangedEvent, IssueChangeBroadcastUtils.getFilterForEvent(issueChangedEvent));
  }

  public void broadcastMessage(RuleSetChangedEvent event, Predicate<SonarLintClient> filter) {
    clients.stream().filter(filter).forEach(c -> {
      Set<String> projectKeysInterestingForClient = new HashSet<>(c.getClientProjectKeys());
      projectKeysInterestingForClient.retainAll(Set.of(event.getProjects()));
      try {
        sonarLintClientPermissionsValidator.validateUserCanReceivePushEventForProjects(c.getUserUuid(), projectKeysInterestingForClient);
        RuleSetChangedEvent personalizedEvent = new RuleSetChangedEvent(projectKeysInterestingForClient.toArray(String[]::new), event.getActivatedRules(),
          event.getDeactivatedRules(), event.getLanguage());
        String message = RuleSetChangeBroadcastUtils.getMessage(personalizedEvent);
        c.writeAndFlush(message);
      } catch (ForbiddenException forbiddenException) {
        LOG.debug("Client is no longer authenticated: " + forbiddenException.getMessage());
        unregisterClient(c);
      } catch (IllegalStateException | IOException e) {
        LOG.error("Unable to send message to a client: " + e.getMessage());
        unregisterClient(c);
      }
    });
  }

  public void broadcastMessage(IssueChangedEvent event, Predicate<SonarLintClient> filter) {
    clients.stream().filter(filter).forEach(c -> {
      Set<String> projectKeysInterestingForClient = new HashSet<>(c.getClientProjectKeys());
      projectKeysInterestingForClient.retainAll(Set.of(event.getProjectKey()));
      try {
        sonarLintClientPermissionsValidator.validateUserCanReceivePushEventForProjects(c.getUserUuid(), projectKeysInterestingForClient);
        String message = IssueChangeBroadcastUtils.getMessage(event);
        c.writeAndFlush(message);
      } catch (ForbiddenException forbiddenException) {
        LOG.debug("Client is no longer authenticated: " + forbiddenException.getMessage());
        unregisterClient(c);
      } catch (IllegalStateException | IOException e) {
        LOG.error("Unable to send message to a client: " + e.getMessage());
        unregisterClient(c);
      }
    });
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
