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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ValuesWsResponse;
import org.sonarqube.ws.client.setting.ValuesRequest;

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.server.component.ComponentFinder.ParamNames.ID_AND_KEY;
import static org.sonar.server.settings.ws.SettingsWsComponentParameters.addComponentParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_VALUES;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEYS;

public class ValuesAction implements SettingsWsAction {

  private static final Splitter COMMA_SPLITTER = Splitter.on(",");

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final PropertyDefinitions propertyDefinitions;
  private final SettingsFinder settingsFinder;

  public ValuesAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, PropertyDefinitions propertyDefinitions, SettingsFinder settingsFinder) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.propertyDefinitions = propertyDefinitions;
    this.settingsFinder = settingsFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_VALUES)
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
      .setDescription("List of setting keys")
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
      ValuesRequest valuesRequest = toWsRequest(request);
      Optional<ComponentDto> component = getComponent(dbSession, valuesRequest);
      checkAdminPermission(component);
      Set<String> keys = new HashSet<>(valuesRequest.getKeys());
      Map<String, String> keysToDisplayMap = getKeysToDisplayMap(keys);
      return new ValuesResponseBuilder(loadSettings(dbSession, component, keysToDisplayMap.keySet()), component, keysToDisplayMap).build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static ValuesRequest toWsRequest(Request request) {
    return ValuesRequest.builder()
      .setKeys(request.mandatoryParamAsStrings(PARAM_KEYS))
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setComponentKey(request.param(PARAM_COMPONENT_KEY))
      .build();
  }

  private Optional<ComponentDto> getComponent(DbSession dbSession, ValuesRequest valuesRequest) {
    String componentId = valuesRequest.getComponentId();
    String componentKey = valuesRequest.getComponentKey();
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

  private List<Setting> loadSettings(DbSession dbSession, Optional<ComponentDto> component, Set<String> keys) {
    if (keys.isEmpty()) {
      return Collections.emptyList();
    }

    // List of settings must be kept in the following orders : default -> global -> component
    List<Setting> settings = new ArrayList<>();
    settings.addAll(loadDefaultSettings(keys));
    settings.addAll(settingsFinder.loadGlobalSettings(dbSession, keys));
    if (component.isPresent()) {
      settings.addAll(settingsFinder.loadComponentSettings(dbSession, keys, component.get()).values());
    }
    return settings;
  }

  private List<Setting> loadDefaultSettings(Set<String> keys) {
    return propertyDefinitions.getAll().stream()
      .filter(definition -> keys.contains(definition.key()))
      .filter(defaultProperty -> !isNullOrEmpty(defaultProperty.defaultValue()))
      .map(Setting::createForDefinition)
      .collect(Collectors.toList());
  }

  private Map<String, String> getKeysToDisplayMap(Set<String> keys) {
    return keys.stream()
      .collect(Collectors.toMap(propertyDefinitions::validKey, Function.identity()));
  }

  private class ValuesResponseBuilder {
    private final List<Setting> settings;
    private final Optional<ComponentDto> component;

    private final ValuesWsResponse.Builder valuesWsBuilder = ValuesWsResponse.newBuilder();
    private final Map<String, Settings.Setting.Builder> settingsBuilderByKey = new HashMap<>();
    private final Map<String, String> keysToDisplayMap;

    ValuesResponseBuilder(List<Setting> settings, Optional<ComponentDto> component, Map<String, String> keysToDisplayMap) {
      this.settings = settings;
      this.component = component;
      this.keysToDisplayMap = keysToDisplayMap;
    }

    ValuesWsResponse build() {
      processSettings();
      settingsBuilderByKey.values().forEach(Settings.Setting.Builder::build);
      return valuesWsBuilder.build();
    }

    private void processSettings() {
      settings.forEach(setting -> {
        Settings.Setting.Builder valueBuilder = getOrCreateValueBuilder(keysToDisplayMap.get(setting.getKey()));
        valueBuilder.setDefault(setting.isDefault());
        setInherited(setting, valueBuilder);
        setValue(setting, valueBuilder);
      });
    }

    private Settings.Setting.Builder getOrCreateValueBuilder(String key) {
      Settings.Setting.Builder valueBuilder = settingsBuilderByKey.get(key);
      if (valueBuilder == null) {
        valueBuilder = valuesWsBuilder.addSettingsBuilder().setKey(key);
        settingsBuilderByKey.put(key, valueBuilder);
      }
      return valueBuilder;
    }

    private void setValue(Setting setting, Settings.Setting.Builder valueBuilder) {
      PropertyDefinition definition = setting.getDefinition();
      String value = setting.getValue();
      if (definition == null) {
        valueBuilder.setValue(value);
        return;
      }
      if (definition.type().equals(PROPERTY_SET)) {
        Settings.FieldsValues.Builder builder = Settings.FieldsValues.newBuilder();
        for (Map<String, String> propertySetMap : setting.getPropertySets()) {
          builder.addFieldsValuesBuilder().putAllValue(propertySetMap);
        }
        valueBuilder.setFieldsValues(builder);
      } else if (definition.multiValues()) {
        valueBuilder.setValues(Settings.Values.newBuilder().addAllValues(COMMA_SPLITTER.split(value)));
      } else {
        valueBuilder.setValue(value);
      }
    }

    private void setInherited(Setting setting, Settings.Setting.Builder valueBuilder) {
      if (setting.isDefault()) {
        valueBuilder.setInherited(false);
      } else {
        valueBuilder.setInherited(component.isPresent() && !Objects.equals(setting.getComponentId(), component.get().getId()));
      }
    }
  }

}
