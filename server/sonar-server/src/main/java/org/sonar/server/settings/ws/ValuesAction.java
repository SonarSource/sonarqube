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

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ValuesWsResponse;

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.PARAM_COMPONENT_ID;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.PARAM_COMPONENT_KEY;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.addComponentParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ValuesAction implements SettingsWsAction {

  private static final Splitter MULTI_VALUE_SPLITTER = Splitter.on(",");

  static final String PARAM_KEYS = "keys";

  private final DbClient dbClient;
  private final SettingsWsComponentParameters settingsWsComponentParameters;
  private final PropertyDefinitions propertyDefinitions;

  public ValuesAction(DbClient dbClient, SettingsWsComponentParameters settingsWsComponentParameters, PropertyDefinitions propertyDefinitions) {
    this.dbClient = dbClient;
    this.settingsWsComponentParameters = settingsWsComponentParameters;
    this.propertyDefinitions = propertyDefinitions;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("values")
      .setDescription(String.format("Returns values of given properties.<br>" +
        "If no value have been set for a property, then the default value is returned.<br>" +
        "Either '%s' or '%s' could be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY))
      .setResponseExample(getClass().getResource("values-example.json"))
      .setSince("6.1")
      .setHandler(this);
    addComponentParameters(action);
    action.createParam(PARAM_KEYS)
      .setDescription("List of property keys")
      .setRequired(true)
      .setExampleValue("sonar.technicalDebt.hoursInDay,sonar.dbcleaner.cleanDirectory");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(doHandle(request), request, response);
  }

  private ValuesWsResponse doHandle(Request request) {
    DbSession dbSession = dbClient.openSession(true);
    try {
      ComponentDto component = settingsWsComponentParameters.getComponent(dbSession, request);
      settingsWsComponentParameters.checkAdminPermission(component);
      Set<String> keys = new HashSet<>(request.mandatoryParamAsStrings(PARAM_KEYS));

      List<PropertyDefinition> definitions = getDefinitions(keys);
      Map<String, PropertyDefinition> definitionsByKey = definitions.stream()
        .collect(Collectors.toMap(PropertyDefinition::key, Function.identity()));

      ValuesWsResponse.Builder valuesBuilder = ValuesWsResponse.newBuilder();
      new ValuesBuilder(dbSession, valuesBuilder, definitionsByKey, keys, component).build();
      return valuesBuilder.build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private class ValuesBuilder {
    private final DbSession dbSession;
    private final ValuesWsResponse.Builder valuesWsBuilder;
    private final Map<String, PropertyDefinition> definitionsByKey;
    private final Set<String> keys;
    private final ComponentDto component;

    private final Map<String, Settings.Value.Builder> valueBuilderByKey = new HashMap<>();

    ValuesBuilder(DbSession dbSession, ValuesWsResponse.Builder valuesWsBuilder, Map<String, PropertyDefinition> definitionsByKey,
      Set<String> keys, @Nullable ComponentDto component) {
      this.dbSession = dbSession;
      this.valuesWsBuilder = valuesWsBuilder;
      this.definitionsByKey = definitionsByKey;
      this.keys = keys;
      this.component = component;
    }

    void build() {
      processDefinitions();
      processPropertyDtos(dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, keys));
      if (component != null) {
        processPropertyDtos(dbClient.propertiesDao().selectComponentPropertiesByKeys(dbSession, keys, component.getId()));
      }
      valueBuilderByKey.values().forEach(Settings.Value.Builder::build);
    }

    private void processDefinitions() {
      definitionsByKey.values().stream()
        .filter(defaultProperty -> !isNullOrEmpty(defaultProperty.defaultValue()))
        .forEach(this::processDefaultValue);
    }

    private void processDefaultValue(PropertyDefinition definition) {
      Settings.Value.Builder valueBuilder = valuesWsBuilder.addValuesBuilder()
        .setKey(definition.key())
        .setIsDefault(true);
      setValue(valueBuilder, definition.defaultValue());
      valueBuilderByKey.put(definition.key(), valueBuilder);
    }

    private void processPropertyDtos(List<PropertyDto> properties) {
      properties.stream()
        .filter(propertyDto -> !isNullOrEmpty(propertyDto.getValue()))
        .forEach(this::processDtoValue);
    }

    private void processDtoValue(PropertyDto property) {
      Settings.Value.Builder valueBuilder = valueBuilderByKey.get(property.getKey());
      if (valueBuilder == null) {
        valueBuilder = valuesWsBuilder.addValuesBuilder().setKey(property.getKey());
        valueBuilderByKey.put(property.getKey(), valueBuilder);
      }
      valueBuilder.setIsInherited(component != null && property.getResourceId() == null);
      valueBuilder.setIsDefault(false);
      setValue(valueBuilder, property.getValue());
    }

    private void setValue(Settings.Value.Builder valueBuilder, String value) {
      PropertyDefinition definition = definitionsByKey.get(valueBuilder.getKey());
      if (definition != null && definition.multiValues()) {
        valueBuilder.addAllValues(MULTI_VALUE_SPLITTER.split(value));
      } else {
        valueBuilder.setValue(value);
      }
    }
  }

  private List<PropertyDefinition> getDefinitions(Set<String> keys) {
    return propertyDefinitions.getAll().stream()
      .filter(def -> keys.contains(def.key()))
      .collect(Collectors.toList());
  }

}
