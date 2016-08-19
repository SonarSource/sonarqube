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
import com.google.common.collect.ImmutableTable;
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
import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.PARAM_COMPONENT_ID;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.PARAM_COMPONENT_KEY;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.addComponentParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ValuesAction implements SettingsWsAction {

  private static final Splitter COMMA_SPLITTER = Splitter.on(",");

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
      .setDescription("List settings values.<br>" +
        "If no value has been set for a setting, then the default value is returned.<br>" +
        "Either '%s' or '%s' can be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>", PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY)
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

      ValuesWsResponse.Builder valuesBuilder = ValuesWsResponse.newBuilder();
      new SettingsBuilder(dbSession, valuesBuilder, getDefinitions(keys), keys, component).build();
      ValuesWsResponse response = valuesBuilder.build();
      return response;
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private class SettingsBuilder {
    private final DbSession dbSession;
    private final ComponentDto component;

    private final ValuesWsResponse.Builder valuesWsBuilder;
    private final Map<String, PropertyDefinition> definitionsByKey;
    private final Set<String> keys;
    private final Set<String> propertySetKeys;

    private final Map<String, Settings.Setting.Builder> settingsBuilderByKey = new HashMap<>();

    SettingsBuilder(DbSession dbSession, ValuesWsResponse.Builder valuesWsBuilder, List<PropertyDefinition> definitions,
      Set<String> keys, @Nullable ComponentDto component) {
      this.dbSession = dbSession;
      this.valuesWsBuilder = valuesWsBuilder;
      this.definitionsByKey = definitions.stream()
        .collect(Collectors.toMap(PropertyDefinition::key, Function.identity()));
      this.keys = keys;
      this.component = component;

      this.propertySetKeys = definitions.stream()
        .filter(definition -> definition.type().equals(PROPERTY_SET))
        .map(PropertyDefinition::key)
        .collect(Collectors.toSet());
    }

    void build() {
      processDefinitions();
      processPropertyDtos(true);
      if (component != null) {
        processPropertyDtos(false);
      }
      settingsBuilderByKey.values().forEach(Settings.Setting.Builder::build);
    }

    private void processDefinitions() {
      definitionsByKey.values().stream()
        .filter(defaultProperty -> !isNullOrEmpty(defaultProperty.defaultValue()))
        .forEach(this::processDefaultValue);
    }

    private void processDefaultValue(PropertyDefinition definition) {
      Settings.Setting.Builder settingBuilder = valuesWsBuilder.addSettingsBuilder()
        .setKey(definition.key())
        .setInherited(false)
        .setDefault(true);
      setValue(settingBuilder, definition.defaultValue());
      settingsBuilderByKey.put(definition.key(), settingBuilder);
    }

    private void processPropertyDtos(boolean loadGlobal) {
      List<PropertyDto> properties = loadProperties(dbSession, loadGlobal, keys);
      PropertySetValues propertySetValues = new PropertySetValues(properties, loadGlobal);

      properties.forEach(property -> {
        String key = property.getKey();
        Settings.Setting.Builder valueBuilder = getOrCreateValueBuilder(key);
        valueBuilder.setInherited(component != null && property.getResourceId() == null);
        valueBuilder.setDefault(false);

        PropertyDefinition definition = definitionsByKey.get(key);
        if (definition != null && definition.type().equals(PROPERTY_SET)) {
          Settings.FieldsValues.Builder builder = Settings.FieldsValues.newBuilder();
          for (Map<String, String> propertySetMap : propertySetValues.get(key)) {
            builder.addFieldsValuesBuilder().putAllValue(propertySetMap);
          }
          valueBuilder.setFieldsValues(builder);
        } else {
          setValue(valueBuilder, property.getValue());
        }
      });
    }

    private Settings.Setting.Builder getOrCreateValueBuilder(String propertyKey) {
      Settings.Setting.Builder valueBuilder = settingsBuilderByKey.get(propertyKey);
      if (valueBuilder == null) {
        valueBuilder = valuesWsBuilder.addSettingsBuilder().setKey(propertyKey);
        settingsBuilderByKey.put(propertyKey, valueBuilder);
      }
      return valueBuilder;
    }

    private void setValue(Settings.Setting.Builder valueBuilder, String value) {
      PropertyDefinition definition = definitionsByKey.get(valueBuilder.getKey());
      if (definition != null && definition.multiValues()) {
        valueBuilder.setValues(Settings.Values.newBuilder().addAllValues(COMMA_SPLITTER.split(value)));
      } else {
        valueBuilder.setValue(value);
      }
    }

    private List<PropertyDto> loadProperties(DbSession dbSession, boolean loadGlobal, Set<String> keys) {
      if (loadGlobal) {
        return dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, keys);
      }
      return dbClient.propertiesDao().selectComponentPropertiesByKeys(dbSession, keys, component.getId());
    }

    private class PropertySetValues {
      private final Map<String, PropertyKeyWithFieldAndSetId> propertyKeyWithFieldAndSetIds = new HashMap<>();
      private final Map<String, PropertySetValue> propertySetValuesByPropertyKey = new HashMap<>();

      PropertySetValues(List<PropertyDto> properties, boolean loadGlobal) {
        properties.stream()
          .filter(propertyDto -> propertySetKeys.contains(propertyDto.getKey()))
          .forEach(propertyDto -> definitionsByKey.get(propertyDto.getKey()).fields()
            .forEach(field -> COMMA_SPLITTER.splitToList(propertyDto.getValue())
              .forEach(value -> add(propertyDto.getKey(), field.key(), value))));

        loadProperties(dbSession, loadGlobal, propertyKeyWithFieldAndSetIds.keySet())
          .forEach(propertySetDto -> {
            PropertyKeyWithFieldAndSetId propertyKeyWithFieldAndSetIdKey = propertyKeyWithFieldAndSetIds.get(propertySetDto.getKey());
            PropertySetValue propertySetValue = getOrCreatePropertySetValue(propertyKeyWithFieldAndSetIdKey.getPropertyKey());
            propertySetValue.add(propertyKeyWithFieldAndSetIdKey.getSetId(), propertyKeyWithFieldAndSetIdKey.getFieldKey(), propertySetDto.getValue());
          });
      }

      private void add(String propertyKey, String fieldKey, String value) {
        String propertySetKey = generatePropertySetKey(propertyKey, value, fieldKey);
        propertyKeyWithFieldAndSetIds.put(propertySetKey, new PropertyKeyWithFieldAndSetId(propertyKey, fieldKey, value));
      }

      private PropertySetValue getOrCreatePropertySetValue(String propertyKey) {
        PropertySetValue propertySetValue = propertySetValuesByPropertyKey.get(propertyKey);
        if (propertySetValue == null) {
          propertySetValue = new PropertySetValue();
          propertySetValuesByPropertyKey.put(propertyKey, propertySetValue);
        }
        return propertySetValue;
      }

      List<Map<String, String>> get(String propertyKey) {
        return propertySetValuesByPropertyKey.get(propertyKey).get();
      }

      private String generatePropertySetKey(String propertyKey, String id, String fieldKey) {
        return propertyKey + "." + id + "." + fieldKey;
      }
    }
  }

  private List<PropertyDefinition> getDefinitions(Set<String> keys) {
    return propertyDefinitions.getAll().stream()
      .filter(def -> keys.contains(def.key()))
      .collect(Collectors.toList());
  }

  private static class PropertyKeyWithFieldAndSetId {
    private final String propertyKey;
    private final String fieldKey;
    private final String setId;

    PropertyKeyWithFieldAndSetId(String propertyKey, String fieldKey, String setId) {
      this.propertyKey = propertyKey;
      this.fieldKey = fieldKey;
      this.setId = setId;
    }

    public String getPropertyKey() {
      return propertyKey;
    }

    public String getFieldKey() {
      return fieldKey;
    }

    public String getSetId() {
      return setId;
    }
  }

  private static class PropertySetValue {
    ImmutableTable.Builder<String, String, String> tableBuilder = new ImmutableTable.Builder<>();

    public void add(String setId, String fieldKey, String value) {
      tableBuilder.put(setId, fieldKey, value);
    }

    public List<Map<String, String>> get() {
      ImmutableTable<String, String, String> table = tableBuilder.build();
      return table.rowKeySet().stream()
        .map(table::row)
        .collect(Collectors.toList());
    }
  }

}
