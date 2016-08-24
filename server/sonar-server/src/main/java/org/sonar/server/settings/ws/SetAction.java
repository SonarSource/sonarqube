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

package org.sonar.server.settings.ws;

import java.util.Locale;
import java.util.Optional;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_SET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_VALUE;

import static org.sonar.server.ws.WsUtils.checkRequest;

public class SetAction implements SettingsWsAction {
  private final PropertyDefinitions propertyDefinitions;
  private final I18n i18n;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SetAction(PropertyDefinitions propertyDefinitions, I18n i18n, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.propertyDefinitions = propertyDefinitions;
    this.i18n = i18n;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SET)
      .setDescription("Update a setting value.<br>" +
        "Either '%s' or '%s' can be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY)
      .setSince("6.1")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setDescription("Setting key")
      .setExampleValue("sonar.links.scm")
      .setRequired(true);

    action.createParam(PARAM_VALUE)
      .setDescription("Setting value. To reset a value, please use the reset web service.")
      .setExampleValue("git@github.com:SonarSource/sonarqube.git")
      .setRequired(true);

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Component id")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT_KEY)
      .setDescription("Component key")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SetRequest setRequest = toWsRequest(request);
      Optional<ComponentDto> component = searchComponent(dbSession, setRequest);
      checkPermissions(component);

      validate(setRequest, component);
      dbClient.propertiesDao().insertProperty(dbSession, toProperty(setRequest, component));
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }

    response.noContent();
  }

  private void validate(SetRequest request, Optional<ComponentDto> component) {
    PropertyDefinition definition = propertyDefinitions.get(request.getKey());
    if (definition == null) {
      return;
    }

    PropertyDefinition.Result result = definition.validate(request.getValue());

    checkRequest(result.isValid(),
      i18n.message(Locale.ENGLISH, "property.error." + result.getErrorKey(), "Error when validating setting with key '%s' and value '%s'"),
      request.getKey(), request.getValue());

    checkRequest(component.isPresent() || definition.global(), "Setting '%s' cannot be global", request.getKey());
    String qualifier = component.isPresent() ? component.get().qualifier() : "";
    checkRequest(!component.isPresent()
      || definition.qualifiers().contains(component.get().qualifier()),
      "Setting '%s' cannot be set on a %s", request.getKey(), i18n.message(Locale.ENGLISH, "qualifier." + qualifier, null));
  }

  private void checkPermissions(Optional<ComponentDto> component) {
    if (component.isPresent()) {
      userSession.checkComponentUuidPermission(UserRole.ADMIN, component.get().uuid());
    } else {
      userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    }
  }

  private static SetRequest toWsRequest(Request request) {
    return SetRequest.builder()
      .setKey(request.mandatoryParam(PARAM_KEY))
      .setValue(request.mandatoryParam(PARAM_VALUE))
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setComponentKey(request.param(PARAM_COMPONENT_KEY))
      .build();
  }

  private Optional<ComponentDto> searchComponent(DbSession dbSession, SetRequest request) {
    if (request.getComponentId() == null && request.getComponentKey() == null) {
      return Optional.empty();
    }

    ComponentDto project = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponentKey(), ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY);

    return Optional.of(project);
  }

  private PropertyDto toProperty(SetRequest request, Optional<ComponentDto> component) {
    PropertyDefinition definition = propertyDefinitions.get(request.getKey());
    // handles deprecated key but persist the new key
    String key = definition == null ? request.getKey() : definition.key();
    PropertyDto property = new PropertyDto()
      .setKey(key)
      .setValue(request.getValue());

    if (component.isPresent()) {
      property.setResourceId(component.get().getId());
    }

    return property;
  }
}
