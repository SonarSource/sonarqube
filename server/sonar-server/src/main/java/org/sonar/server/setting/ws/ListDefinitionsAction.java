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

import java.util.List;
import java.util.Optional;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
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
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ListDefinitionsWsResponse;
import org.sonarqube.ws.client.setting.ListDefinitionsRequest;

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;
import static org.sonar.server.setting.ws.SettingsWsComponentParameters.addComponentParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_LIST_DEFINITIONS;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_KEY;

public class ListDefinitionsAction implements SettingsWsAction {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final PropertyDefinitions propertyDefinitions;

  public ListDefinitionsAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, PropertyDefinitions propertyDefinitions) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.propertyDefinitions = propertyDefinitions;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_LIST_DEFINITIONS)
      .setDescription(String.format("List settings definitions.<br>" +
        "Either '%s' or '%s' can be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY))
      .setResponseExample(getClass().getResource("list_definitions-example.json"))
      .setSince("6.1")
      .setInternal(true)
      .setHandler(this);
    addComponentParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(doHandle(request), request, response);
  }

  private ListDefinitionsWsResponse doHandle(Request request) {
    ListDefinitionsRequest wsRequest = toWsRequest(request);
    Optional<String> qualifier = getQualifier(wsRequest);
    ListDefinitionsWsResponse.Builder wsResponse = ListDefinitionsWsResponse.newBuilder();

    propertyDefinitions.getAll().stream()
      .filter(definition -> qualifier.isPresent() ? definition.qualifiers().contains(qualifier.get()) : definition.global())
      .filter(definition -> !definition.type().equals(PropertyType.LICENSE))
      .forEach(definition -> addDefinition(definition, wsResponse));
    return wsResponse.build();
  }

  private static ListDefinitionsRequest toWsRequest(Request request) {
    return ListDefinitionsRequest.builder()
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setComponentKey(request.param(PARAM_COMPONENT_KEY))
      .build();
  }

  private Optional<String> getQualifier(ListDefinitionsRequest wsRequest) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<ComponentDto> component = getComponent(dbSession, wsRequest);
      checkAdminPermission(component);
      return component.isPresent() ? Optional.of(component.get().qualifier()) : Optional.empty();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private Optional<ComponentDto> getComponent(DbSession dbSession, ListDefinitionsRequest wsRequest) {
    String componentId = wsRequest.getComponentId();
    String componentKey = wsRequest.getComponentKey();
    if (componentId != null || componentKey != null) {
      return Optional.of(componentFinder.getByUuidOrKey(dbSession, componentId, componentKey, ID_AND_KEY));
    }
    return Optional.empty();
  }

  private void checkAdminPermission(Optional<ComponentDto> component) {
    if (component.isPresent()) {
      userSession.checkComponentUuidPermission(UserRole.ADMIN, component.get().uuid());
    } else {
      userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    }
  }

  private void addDefinition(PropertyDefinition definition, ListDefinitionsWsResponse.Builder wsResponse) {
    String key = definition.key();
    Settings.Definition.Builder builder = wsResponse.addDefinitionsBuilder()
      .setKey(key)
      .setType(Settings.Type.valueOf(definition.type().name()))
      .setMultiValues(definition.multiValues());
    String deprecatedKey = definition.deprecatedKey();
    if (!isNullOrEmpty(deprecatedKey)) {
      builder.setDeprecatedKey(deprecatedKey);
    }
    String name = definition.name();
    if (!isNullOrEmpty(name)) {
      builder.setName(name);
    }
    String description = definition.description();
    if (!isNullOrEmpty(description)) {
      builder.setDescription(description);
    }
    String category = propertyDefinitions.getCategory(key);
    if (!isNullOrEmpty(category)) {
      builder.setCategory(category);
    }
    String subCategory = propertyDefinitions.getSubCategory(key);
    if (!isNullOrEmpty(subCategory)) {
      builder.setSubCategory(subCategory);
    }
    String defaultValue = definition.defaultValue();
    if (!isNullOrEmpty(defaultValue)) {
      builder.setDefaultValue(defaultValue);
    }
    List<String> options = definition.options();
    if (!options.isEmpty()) {
      builder.addAllOptions(options);
    }
    List<PropertyFieldDefinition> fields = definition.fields();
    if (!fields.isEmpty()) {
      fields.stream()
        .filter(fieldDefinition -> !fieldDefinition.type().equals(PropertyType.LICENSE))
        .forEach(fieldDefinition -> addField(fieldDefinition, builder));
    }
  }

  private static void addField(PropertyFieldDefinition fieldDefinition, Settings.Definition.Builder builder) {
    builder.addFieldsBuilder()
      .setKey(fieldDefinition.key())
      .setName(fieldDefinition.name())
      .setDescription(fieldDefinition.description())
      .setType(Settings.Type.valueOf(fieldDefinition.type().name()))
      .addAllOptions(fieldDefinition.options())
      .build();
  }

}
