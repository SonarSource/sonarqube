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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.ParamChange;
import org.sonar.core.util.RuleActivationListener;
import org.sonar.core.util.RuleChange;
import org.sonar.core.util.RuleSetChangedEvent;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.pushapi.qualityprofile.RuleActivatorEventsDistributor;

import static java.util.Arrays.asList;

@ServerSide
public class SonarLintClientsRegistry implements RuleActivationListener {

  private static final Logger LOG = Loggers.get(SonarLintClientsRegistry.class);

  private final SonarLintClientPermissionsValidator sonarLintClientPermissionsValidator;
  private final List<SonarLintClient> clients = new CopyOnWriteArrayList<>();
  private final RuleActivatorEventsDistributor eventsDistributor;

  private boolean registeredToEvents = false;

  public SonarLintClientsRegistry(RuleActivatorEventsDistributor ruleActivatorEventsDistributor, SonarLintClientPermissionsValidator permissionsValidator) {
    this.sonarLintClientPermissionsValidator = permissionsValidator;
    this.eventsDistributor = ruleActivatorEventsDistributor;
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
      eventsDistributor.subscribe(this);
      registeredToEvents = true;
    } catch (RuntimeException e) {
      LOG.warn("Can not listen to rule activation events for server push. Web Server might not have started fully yet.", e);
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
    broadcastMessage(ruleSetChangedEvent, getFilterForEvent(ruleSetChangedEvent));
  }

  private static Predicate<SonarLintClient> getFilterForEvent(RuleSetChangedEvent ruleSetChangedEvent) {
    List<String> affectedProjects = asList(ruleSetChangedEvent.getProjects());
    return client -> {
      Set<String> clientProjectKeys = client.getClientProjectKeys();
      Set<String> languages = client.getLanguages();
      return !Collections.disjoint(clientProjectKeys, affectedProjects) && languages.contains(ruleSetChangedEvent.getLanguage());
    };
  }

  public void broadcastMessage(RuleSetChangedEvent event, Predicate<SonarLintClient> filter) {
    clients.stream().filter(filter).forEach(c -> {
      Set<String> projectKeysInterestingForClient = new HashSet<>(c.getClientProjectKeys());
      projectKeysInterestingForClient.retainAll(Set.of(event.getProjects()));
      try {
        sonarLintClientPermissionsValidator.validateUserCanReceivePushEventForProjects(c.getUserUuid(), projectKeysInterestingForClient);
        RuleSetChangedEvent personalizedEvent = new RuleSetChangedEvent(projectKeysInterestingForClient.toArray(String[]::new), event.getActivatedRules(),
          event.getDeactivatedRules(), event.getLanguage());
        String message = getMessage(personalizedEvent);
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
  private static String getMessage(RuleSetChangedEvent ruleSetChangedEvent) {
    return "event: " + ruleSetChangedEvent.getEvent() + "\n"
      + "data: " + toJson(ruleSetChangedEvent);
  }

  private static String toJson(RuleSetChangedEvent ruleSetChangedEvent) {
    JSONObject data = new JSONObject();
    data.put("projects", ruleSetChangedEvent.getProjects());

    JSONArray activatedRulesJson = new JSONArray();
    for (RuleChange rule : ruleSetChangedEvent.getActivatedRules()) {
      activatedRulesJson.put(toJson(rule));
    }
    data.put("activatedRules", activatedRulesJson);

    JSONArray deactivatedRulesJson = new JSONArray();
    for (String ruleKey : ruleSetChangedEvent.getDeactivatedRules()) {
      deactivatedRulesJson.put(ruleKey);
    }
    data.put("deactivatedRules", deactivatedRulesJson);

    return data.toString();
  }

  private static JSONObject toJson(RuleChange rule) {
    JSONObject ruleJson = new JSONObject();
    ruleJson.put("key", rule.getKey());
    ruleJson.put("language", rule.getLanguage());
    ruleJson.put("severity", rule.getSeverity());
    ruleJson.put("templateKey", rule.getTemplateKey());

    JSONArray params = new JSONArray();
    for (ParamChange paramChange : rule.getParams()) {
      params.put(toJson(paramChange));
    }
    ruleJson.put("params", params);
    return ruleJson;
  }

  private static JSONObject toJson(ParamChange paramChange) {
    JSONObject param = new JSONObject();
    param.put("key", paramChange.getKey());
    param.put("value", paramChange.getValue());
    return param;
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
