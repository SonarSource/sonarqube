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

import java.util.Optional;
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

public class SetAction implements SettingsWsAction {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public SetAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set")
      .setDescription("Update a setting value.<br>" +
        "Either '%s' or '%s' can be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", "componentId", "componentKey")
      .setSince("6.1")
      .setPost(true)
      .setHandler(this);

    action.createParam("key")
      .setDescription("Setting key")
      .setExampleValue("sonar.links.scm")
      .setRequired(true);

    action.createParam("value")
      .setDescription("Setting value. To reset a value, please use the reset web service.")
      .setExampleValue("git@github.com:SonarSource/sonarqube.git")
      .setRequired(true);

    action.createParam("componentId")
      .setDescription("Component id")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam("componentKey")
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

      dbClient.propertiesDao().insertProperty(dbSession, toProperty(setRequest, component));
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }

    response.noContent();
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
      .setKey(request.mandatoryParam("key"))
      .setValue(request.mandatoryParam("value"))
      .setComponentId(request.param("componentId"))
      .setComponentKey(request.param("componentKey"))
      .build();
  }

  private Optional<ComponentDto> searchComponent(DbSession dbSession, SetRequest request) {
    if (request.getComponentId() == null && request.getComponentKey() == null) {
      return Optional.empty();
    }

    ComponentDto project = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponentKey(), ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY);

    return Optional.of(project);
  }

  private static PropertyDto toProperty(SetRequest request, Optional<ComponentDto> component) {
    PropertyDto property = new PropertyDto()
      .setKey(request.getKey())
      .setValue(request.getValue());

    if (component.isPresent()) {
      property.setResourceId(component.get().getId());
    }

    return property;
  }
}
