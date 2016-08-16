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

import java.util.List;
import javax.annotation.CheckForNull;
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

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListDefinitionsAction implements SettingsWsAction {

  private static final String PARAM_COMPONENT_ID = "componentId";
  private static final String PARAM_COMPONENT_KEY = "componentKey";

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
    WebService.NewAction action = context.createAction("list_definitions")
      .setDescription(String.format("Returns definitions of properties.<br>" +
        "Either '%s' or '%s' could be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY))
      .setResponseExample(getClass().getResource("list_definitions-example.json"))
      .setSince("6.1")
      .setHandler(this);

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Component id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT_KEY)
      .setDescription("Component key")
      .setExampleValue("my_component_key");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(doHandle(request), request, response);
  }

  private ListDefinitionsWsResponse doHandle(Request request) {
    String qualifier = getQualifier(request);
    ListDefinitionsWsResponse.Builder wsResponse = ListDefinitionsWsResponse.newBuilder();

    propertyDefinitions.getAll().stream()
      .filter(definition -> qualifier == null ? definition.global() : definition.qualifiers().contains(qualifier))
      .filter(definition -> !definition.type().equals(PropertyType.LICENSE))
      .forEach(definition -> addDefinition(definition, wsResponse));
    return wsResponse.build();
  }

  private void addDefinition(PropertyDefinition definition, ListDefinitionsWsResponse.Builder wsResponse) {
    String key = definition.key();
    Settings.Definition.Builder builder = wsResponse.addDefinitionsBuilder()
      .setKey(key)
      .setType(Settings.Type.valueOf(definition.type().name()))
      .setMultiValues(definition.multiValues());
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
      .setType(Settings.Type.valueOf(fieldDefinition.type().name()))
      .setIndicativeSize(fieldDefinition.indicativeSize())
      .addAllOptions(fieldDefinition.options())
      .build();
  }

  @CheckForNull
  private String getQualifier(Request request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      if (request.hasParam(PARAM_COMPONENT_ID) || request.hasParam(PARAM_COMPONENT_KEY)) {
        ComponentDto component = componentFinder.getByUuidOrKey(dbSession, request.param(PARAM_COMPONENT_ID), request.param(PARAM_COMPONENT_KEY),
          ComponentFinder.ParamNames.ID_AND_KEY);
        userSession.checkComponentUuidPermission(UserRole.ADMIN, component.uuid());
        return component.qualifier();
      } else {
        userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
        return null;
      }
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

}
