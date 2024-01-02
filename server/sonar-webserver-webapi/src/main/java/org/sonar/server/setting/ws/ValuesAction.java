/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.setting.ws;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.property.PropertyDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.Settings.ValuesWsResponse;

import static java.lang.String.format;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.api.CoreProperties.SERVER_ID;
import static org.sonar.api.CoreProperties.SERVER_STARTTIME;
import static org.sonar.api.PropertyType.FORMATTED_TEXT;
import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.setting.ws.PropertySetExtractor.extractPropertySetKeys;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_KEYS;
import static org.sonar.server.setting.ws.SettingsWsSupport.isSecured;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ValuesAction implements SettingsWsAction {

  private static final Splitter COMMA_SPLITTER = Splitter.on(",");
  private static final String COMMA_ENCODED_VALUE = "%2C";
  private static final Splitter DOT_SPLITTER = Splitter.on(".").omitEmptyStrings();
  private static final Set<String> SERVER_SETTING_KEYS = Set.of(SERVER_STARTTIME, SERVER_ID);

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final PropertyDefinitions propertyDefinitions;
  private final SettingsWsSupport settingsWsSupport;

  public ValuesAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession, PropertyDefinitions propertyDefinitions,
    SettingsWsSupport settingsWsSupport) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.propertyDefinitions = propertyDefinitions;
    this.settingsWsSupport = settingsWsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("values")
      .setDescription("List settings values.<br>" +
        "If no value has been set for a setting, then the default value is returned.<br>" +
        "The settings from conf/sonar.properties are excluded from results.<br>" +
        "Requires 'Browse' or 'Execute Analysis' permission when a component is specified.<br/>" +
        "Secured settings values are not returned by the endpoint.<br/>")
      .setResponseExample(getClass().getResource("values-example.json"))
      .setSince("6.3")
      .setChangelog(
        new Change("9.1", "The secured settings values are no longer returned. Secured settings keys that have a value " +
          "are now returned in setSecuredSettings array."),
        new Change("7.6", String.format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("7.1", "The settings from conf/sonar.properties are excluded from results."))
      .setHandler(this);
    action.createParam(PARAM_KEYS)
      .setDescription("List of setting keys")
      .setExampleValue("sonar.test.inclusions,sonar.exclusions");
    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    writeProtobuf(doHandle(request), request, response);
  }

  private ValuesWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      ValuesRequest valuesRequest = ValuesRequest.from(request);
      Optional<ComponentDto> component = loadComponent(dbSession, valuesRequest);
      BranchDto branchDto = component.map(c -> componentFinder.getBranchByUuid(dbSession, c.branchUuid())).orElse(null);

      Set<String> keys = loadKeys(valuesRequest);
      Map<String, String> keysToDisplayMap = getKeysToDisplayMap(keys);
      List<Setting> settings = loadSettings(dbSession, component, keysToDisplayMap.keySet(), branchDto);
      return new ValuesResponseBuilder(settings, component, keysToDisplayMap).build();
    }
  }

  private Set<String> loadKeys(ValuesRequest valuesRequest) {
    List<String> keys = valuesRequest.getKeys();
    Set<String> result;
    if (keys == null || keys.isEmpty()) {
      result = concat(propertyDefinitions.getAll().stream().map(PropertyDefinition::key), SERVER_SETTING_KEYS.stream()).collect(Collectors.toSet());
    } else {
      result = ImmutableSet.copyOf(keys);
    }
    result.forEach(SettingsWsSupport::validateKey);
    return result;
  }

  private Optional<ComponentDto> loadComponent(DbSession dbSession, ValuesRequest valuesRequest) {
    String componentKey = valuesRequest.getComponent();
    if (componentKey == null) {
      return Optional.empty();
    }
    ComponentDto component = componentFinder.getByKey(dbSession, componentKey);

    if (!userSession.hasComponentPermission(USER, component) &&
      !userSession.hasComponentPermission(UserRole.SCAN, component) &&
      !userSession.hasPermission(GlobalPermission.SCAN)) {
      throw insufficientPrivilegesException();
    }
    return Optional.of(component);
  }

  private List<Setting> loadSettings(DbSession dbSession, Optional<ComponentDto> component, Set<String> keys, @Nullable BranchDto branchDto) {
    // List of settings must be kept in the following orders : default -> global -> component -> branch
    List<Setting> settings = new ArrayList<>();
    settings.addAll(loadDefaultValues(keys));
    settings.addAll(loadGlobalSettings(dbSession, keys));
    String branch = getBranchKeySafely(branchDto);
    if (component.isPresent() && branch != null && component.get().getMainBranchProjectUuid() != null) {
      ComponentDto project = componentFinder.getByUuidFromMainBranch(dbSession, component.get().getMainBranchProjectUuid());
      settings.addAll(loadComponentSettings(dbSession, keys, project).values());
    }
    component.ifPresent(componentDto -> settings.addAll(loadComponentSettings(dbSession, keys, componentDto).values()));
    return settings.stream()
      .filter(s -> settingsWsSupport.isVisible(s.getKey(), component))
      .toList();
  }

  @CheckForNull
  private static String getBranchKeySafely(@Nullable BranchDto branchDto) {
    if (branchDto != null) {
      return branchDto.isMain() ? null : branchDto.getBranchKey();
    }
    return null;
  }

  private List<Setting> loadDefaultValues(Set<String> keys) {
    return propertyDefinitions.getAll().stream()
      .filter(definition -> keys.contains(definition.key()))
      .filter(defaultProperty -> !isEmpty(defaultProperty.defaultValue()))
      .map(Setting::createFromDefinition)
      .toList();
  }

  private Map<String, String> getKeysToDisplayMap(Set<String> keys) {
    return keys.stream()
      .collect(Collectors.toMap(propertyDefinitions::validKey, Function.identity(),
        (u, v) -> {
          throw new IllegalArgumentException(format("'%s' and '%s' cannot be used at the same time as they refer to the same setting", u, v));
        }));
  }

  private List<Setting> loadGlobalSettings(DbSession dbSession, Set<String> keys) {
    List<PropertyDto> properties = dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, keys);
    List<PropertyDto> propertySets = dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, getPropertySetKeys(properties));
    return properties.stream()
      .map(property -> Setting.createFromDto(property, getPropertySets(property.getKey(), propertySets, null), propertyDefinitions.get(property.getKey())))
      .toList();
  }

  /**
   * Return list of settings by component uuid, sorted from project to lowest module
   */
  private Multimap<String, Setting> loadComponentSettings(DbSession dbSession, Set<String> keys, ComponentDto component) {
    List<String> componentUuids = DOT_SPLITTER.splitToList(component.moduleUuidPath());
    List<PropertyDto> properties = dbClient.propertiesDao().selectPropertiesByKeysAndComponentUuids(dbSession, keys, componentUuids);
    List<PropertyDto> propertySets = dbClient.propertiesDao().selectPropertiesByKeysAndComponentUuids(dbSession, getPropertySetKeys(properties), componentUuids);

    Multimap<String, Setting> settingsByUuid = TreeMultimap.create(Ordering.explicit(componentUuids), Ordering.arbitrary());
    for (PropertyDto propertyDto : properties) {
      String componentUuid = propertyDto.getComponentUuid();
      String propertyKey = propertyDto.getKey();
      settingsByUuid.put(componentUuid,
        Setting.createFromDto(propertyDto, getPropertySets(propertyKey, propertySets, componentUuid), propertyDefinitions.get(propertyKey)));
    }
    return settingsByUuid;
  }

  private Set<String> getPropertySetKeys(List<PropertyDto> properties) {
    return properties.stream()
      .filter(propertyDto -> propertyDefinitions.get(propertyDto.getKey()) != null)
      .filter(propertyDto -> propertyDefinitions.get(propertyDto.getKey()).type().equals(PROPERTY_SET))
      .flatMap(propertyDto -> extractPropertySetKeys(propertyDto, propertyDefinitions.get(propertyDto.getKey())).stream())
      .collect(Collectors.toSet());
  }

  private static List<PropertyDto> getPropertySets(String propertyKey, List<PropertyDto> propertySets, @Nullable String componentUuid) {
    return propertySets.stream()
      .filter(propertyDto -> Objects.equals(propertyDto.getComponentUuid(), componentUuid))
      .filter(propertyDto -> propertyDto.getKey().startsWith(propertyKey + "."))
      .toList();
  }

  private class ValuesResponseBuilder {
    private final List<Setting> settings;
    private final Optional<ComponentDto> requestedComponent;

    private final ValuesWsResponse.Builder valuesWsBuilder = ValuesWsResponse.newBuilder();
    private final Map<String, Settings.Setting.Builder> settingsBuilderByKey = new HashMap<>();
    private final Map<String, Setting> settingsByParentKey = new HashMap<>();
    private final Map<String, String> keysToDisplayMap;

    ValuesResponseBuilder(List<Setting> settings, Optional<ComponentDto> requestedComponent, Map<String, String> keysToDisplayMap) {
      this.settings = settings;
      this.requestedComponent = requestedComponent;
      this.keysToDisplayMap = keysToDisplayMap;
    }

    ValuesWsResponse build() {
      processSettings();
      settingsBuilderByKey.values().forEach(Settings.Setting.Builder::build);
      return valuesWsBuilder.build();
    }

    private void processSettings() {
      settings.forEach(setting -> {
        if (isSecured(setting.getKey())) {
          if (!setting.isDefault()) {
            valuesWsBuilder.addSetSecuredSettings(setting.getKey());
          }
          return;
        }
        Settings.Setting.Builder valueBuilder = getOrCreateValueBuilder(keysToDisplayMap.get(setting.getKey()));
        setInherited(setting, valueBuilder);
        setValue(setting, valueBuilder);
        setParent(setting, valueBuilder);
      });
    }

    private Settings.Setting.Builder getOrCreateValueBuilder(String key) {
      return settingsBuilderByKey.computeIfAbsent(key, k -> valuesWsBuilder.addSettingsBuilder().setKey(key));
    }

    private void setInherited(Setting setting, Settings.Setting.Builder valueBuilder) {
      boolean isDefault = setting.isDefault();
      boolean isGlobal = !requestedComponent.isPresent();
      boolean isOnComponent = requestedComponent.isPresent() && Objects.equals(setting.getComponentUuid(), requestedComponent.get().uuid());
      boolean isSet = isGlobal || isOnComponent;
      valueBuilder.setInherited(isDefault || !isSet);
    }

    private void setValue(Setting setting, Settings.Setting.Builder valueBuilder) {
      PropertyDefinition definition = setting.getDefinition();
      String value = setting.getValue();
      if (definition == null) {
        valueBuilder.setValue(value);
        return;
      }
      if (definition.type().equals(PROPERTY_SET)) {
        valueBuilder.setFieldValues(createFieldValuesBuilder(filterVisiblePropertySets(setting.getPropertySets())));
      } else if (definition.type().equals(FORMATTED_TEXT)) {
        valueBuilder.setValues(createFormattedTextValuesBuilder(value));
      } else if (definition.multiValues()) {
        valueBuilder.setValues(createValuesBuilder(value));
      } else {
        valueBuilder.setValue(value);
      }
    }

    private void setParent(Setting setting, Settings.Setting.Builder valueBuilder) {
      Setting parent = settingsByParentKey.get(setting.getKey());
      if (parent != null) {
        String value = valueBuilder.getInherited() ? setting.getValue() : parent.getValue();
        PropertyDefinition definition = setting.getDefinition();
        if (definition == null) {
          valueBuilder.setParentValue(value);
          return;
        }

        if (definition.type().equals(PROPERTY_SET)) {
          valueBuilder.setParentFieldValues(
            createFieldValuesBuilder(valueBuilder.getInherited() ? filterVisiblePropertySets(setting.getPropertySets()) : filterVisiblePropertySets(parent.getPropertySets())));
        } else if (definition.multiValues()) {
          valueBuilder.setParentValues(createValuesBuilder(value));
        } else {
          valueBuilder.setParentValue(value);
        }
      }
      settingsByParentKey.put(setting.getKey(), setting);
    }

    private Settings.Values.Builder createValuesBuilder(String value) {
      List<String> values = COMMA_SPLITTER.splitToList(value).stream().map(v -> v.replace(COMMA_ENCODED_VALUE, ",")).toList();
      return Settings.Values.newBuilder().addAllValues(values);
    }

    private Settings.Values.Builder createFormattedTextValuesBuilder(String value) {
      List<String> values = List.of(value, Markdown.convertToHtml(value));
      return Settings.Values.newBuilder().addAllValues(values);
    }

    private Settings.FieldValues.Builder createFieldValuesBuilder(List<Map<String, String>> fieldValues) {
      Settings.FieldValues.Builder builder = Settings.FieldValues.newBuilder();
      for (Map<String, String> propertySetMap : fieldValues) {
        builder.addFieldValuesBuilder().putAllValue(propertySetMap);
      }
      return builder;
    }

    private List<Map<String, String>> filterVisiblePropertySets(List<Map<String, String>> propertySets) {
      List<Map<String, String>> filteredPropertySets = new ArrayList<>();
      propertySets.forEach(map -> {
        Map<String, String> set = new HashMap<>();
        map.entrySet().stream()
          .filter(entry -> settingsWsSupport.isVisible(entry.getKey(), requestedComponent))
          .forEach(entry -> set.put(entry.getKey(), entry.getValue()));
        filteredPropertySets.add(set);
      });
      return filteredPropertySets;
    }
  }

  private static class ValuesRequest {
    private String component;
    private List<String> keys;

    public ValuesRequest setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    @CheckForNull
    public String getComponent() {
      return component;
    }

    public ValuesRequest setKeys(@Nullable List<String> keys) {
      this.keys = keys;
      return this;
    }

    @CheckForNull
    public List<String> getKeys() {
      return keys;
    }

    private static ValuesRequest from(Request request) {
      ValuesRequest result = new ValuesRequest()
        .setComponent(request.param(PARAM_COMPONENT));
      if (request.hasParam(PARAM_KEYS)) {
        result.setKeys(request.paramAsStrings(PARAM_KEYS));
      }
      return result;
    }

  }
}
