/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.setting.ws;

import java.util.Optional;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.setting.ResetRequest;

import static org.sonar.server.setting.ws.SettingsWsComponentParameters.addComponentParameters;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_RESET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEY;

public class ResetAction implements SettingsWsAction {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final SettingsUpdater settingsUpdater;
  private final UserSession userSession;
  private final PropertyDefinitions definitions;

  public ResetAction(DbClient dbClient, ComponentFinder componentFinder, SettingsUpdater settingsUpdater, UserSession userSession, PropertyDefinitions definitions) {
    this.dbClient = dbClient;
    this.settingsUpdater = settingsUpdater;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.definitions = definitions;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_RESET)
      .setDescription("Remove a setting value.<br>" +
        "Either '%s' or '%s' can be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY)
      .setSince("6.1")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setDescription("Setting key")
      .setExampleValue("sonar.links.scm")
      .setRequired(true);
    addComponentParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ResetRequest resetRequest = toWsRequest(request);
      Optional<ComponentDto> component = getComponent(dbSession, resetRequest);
      checkPermissions(component);

      PropertyDefinition definition = definitions.get(resetRequest.getKey());
      String key = definition != null ? definition.key() : resetRequest.getKey();
      if (component.isPresent()) {
        settingsUpdater.deleteComponentSetting(dbSession, key, component.get());
      } else {
        settingsUpdater.deleteGlobalSetting(dbSession, key);
      }
      dbSession.commit();
      response.noContent();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static ResetRequest toWsRequest(Request request) {
    return ResetRequest.builder()
      .setKey(request.mandatoryParam(PARAM_KEY))
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setComponentKey(request.param(PARAM_COMPONENT_KEY))
      .build();
  }

  private Optional<ComponentDto> getComponent(DbSession dbSession, ResetRequest request) {
    if (request.getComponentId() == null && request.getComponentKey() == null) {
      return Optional.empty();
    }
    ComponentDto project = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponentKey(), ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY);
    return Optional.of(project);
  }

  private void checkPermissions(Optional<ComponentDto> component) {
    if (component.isPresent()) {
      userSession.checkComponentUuidPermission(UserRole.ADMIN, component.get().uuid());
    } else {
      userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    }
  }
}
