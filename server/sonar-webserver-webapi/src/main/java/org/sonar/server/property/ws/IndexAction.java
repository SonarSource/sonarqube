/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.property.ws;

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsAction;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.server.setting.ws.SettingsWsSupport.DOT_SECURED;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class IndexAction implements WsAction {

  private static final Splitter DOT_SPLITTER = Splitter.on(".").omitEmptyStrings();
  private static final Splitter COMMA_SPLITTER = Splitter.on(",");
  private static final String COMMA_ENCODED_VALUE = "%2C";

  public static final String PARAM_ID = "id";
  public static final String PARAM_COMPONENT = "resource";
  public static final String PARAM_FORMAT = "format";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PropertyDefinitions propertyDefinitions;

  public IndexAction(DbClient dbClient, UserSession userSession, PropertyDefinitions propertyDefinitions) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.propertyDefinitions = propertyDefinitions;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("index")
      .setDescription("This web service is deprecated, please use api/settings/values instead.")
      .setDeprecatedSince("6.3")
      .setResponseExample(getClass().getResource("index-example.json"))
      .setSince("2.6")
      .setHandler(this);
    action.createParam(PARAM_ID)
      .setDescription("Setting key")
      .setExampleValue("sonar.test.inclusions");
    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key or database id")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_FORMAT)
      .setDescription("Only json response format is available")
      .setPossibleValues("json");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    request.param(PARAM_FORMAT);
    JsonWriter json = response.newJsonWriter();
    json.beginArray();
    doHandle(json, request);
    json.endArray();
    json.close();
  }

  private void doHandle(JsonWriter json, Request request) {
    try (DbSession dbSession = dbClient.openSession(true)) {
      Optional<ComponentDto> component = loadComponent(dbSession, request);
      String key = request.param(PARAM_ID);
      List<PropertyDto> propertyDtos = loadProperties(dbSession, component, Optional.ofNullable(key));
      new ResponseBuilder(propertyDtos).build(json);
    }
  }

  private Optional<ComponentDto> loadComponent(DbSession dbSession, Request request) {
    String component = request.param(PARAM_COMPONENT);
    if (component == null) {
      return Optional.empty();
    }
    return loadComponent(dbSession, component);
  }

  private Optional<ComponentDto> loadComponent(DbSession dbSession, String component) {
    try {
      long componentId = Long.parseLong(component);
      return Optional.ofNullable(dbClient.componentDao().selectById(dbSession, componentId).orElse(null));
    } catch (NumberFormatException e) {
      return Optional.ofNullable(dbClient.componentDao().selectByKey(dbSession, component).orElse(null));
    }
  }

  private List<PropertyDto> loadProperties(DbSession dbSession, Optional<ComponentDto> component, Optional<String> key) {
    // List of settings must be kept in the following orders : default -> global -> component
    List<PropertyDto> propertyDtos = new ArrayList<>();
    propertyDtos.addAll(loadDefaultSettings(key));
    propertyDtos.addAll(loadGlobalSettings(dbSession, key));
    component.ifPresent(componentDto -> propertyDtos.addAll(loadComponentSettings(dbSession, key, componentDto).values()));
    return propertyDtos.stream().filter(isVisible(component)).collect(Collectors.toList());
  }

  Predicate<PropertyDto> isVisible(Optional<ComponentDto> component) {
    return propertyDto -> !propertyDto.getKey().endsWith(DOT_SECURED)
      || hasAdminPermission(component);
  }

  private boolean hasAdminPermission(Optional<ComponentDto> component) {
    return component
      .map(c -> userSession.hasComponentPermission(ADMIN, c))
      .orElse(userSession.isSystemAdministrator());
  }

  private List<PropertyDto> loadGlobalSettings(DbSession dbSession, Optional<String> key) {
    if (key.isPresent()) {
      return dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, Collections.singleton(key.get()));
    }
    return dbClient.propertiesDao().selectGlobalProperties(dbSession);
  }

  /**
   * Return list of propertyDto by component uuid, sorted from project to lowest module
   */
  private Multimap<String, PropertyDto> loadComponentSettings(DbSession dbSession, Optional<String> key, ComponentDto component) {
    List<String> componentUuids = DOT_SPLITTER.splitToList(component.moduleUuidPath());
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
    Set<Long> componentIds = componentDtos.stream().map(ComponentDto::getId).collect(Collectors.toSet());
    Map<Long, String> uuidsById = componentDtos.stream().collect(Collectors.toMap(ComponentDto::getId, ComponentDto::uuid));
    List<PropertyDto> properties = key.isPresent() ? dbClient.propertiesDao().selectPropertiesByKeysAndComponentIds(dbSession, Collections.singleton(key.get()), componentIds)
      : dbClient.propertiesDao().selectPropertiesByComponentIds(dbSession, componentIds);

    Multimap<String, PropertyDto> propertyDtosByUuid = TreeMultimap.create(Ordering.explicit(componentUuids), Ordering.arbitrary());
    for (PropertyDto propertyDto : properties) {
      Long componentId = propertyDto.getResourceId();
      String componentUuid = uuidsById.get(componentId);
      propertyDtosByUuid.put(componentUuid, propertyDto);
    }
    return propertyDtosByUuid;
  }

  private List<PropertyDto> loadDefaultSettings(Optional<String> key) {
    return propertyDefinitions.getAll().stream()
      .filter(definition -> !key.isPresent() || key.get().equals(definition.key()))
      .filter(defaultProperty -> !isEmpty(defaultProperty.defaultValue()))
      .map(definition -> new PropertyDto().setKey(definition.key()).setValue(definition.defaultValue()))
      .collect(Collectors.toList());
  }

  private class ResponseBuilder {
    private final List<PropertyDto> propertyDtos;
    private final Map<String, Property> propertiesByKey = new HashMap<>();

    ResponseBuilder(List<PropertyDto> propertyDtos) {
      this.propertyDtos = propertyDtos;
    }

    void build(JsonWriter json) {
      processSettings();
      propertiesByKey.values().forEach(property -> {
        json.beginObject()
          .prop("key", property.key)
          .prop("value", property.value);
        if (!property.values.isEmpty()) {
          json.name("values").beginArray().values(property.values).endArray();
        }
        json.endObject();
      });
    }

    private void processSettings() {
      propertyDtos.forEach(setting -> {
        Property property = createProperty(setting.getKey());
        setValue(setting, property);
      });
    }

    private Property createProperty(String key) {
      return propertiesByKey.computeIfAbsent(key, k -> new Property(key));
    }

    private void setValue(PropertyDto propertyDto, Property property) {
      String value = propertyDto.getValue();
      property.setValue(value);
      PropertyDefinition definition = propertyDefinitions.get(propertyDto.getKey());
      if (definition != null && (definition.multiValues() || definition.type().equals(PROPERTY_SET))) {
        property.setValues(createValues(value));
      }
    }

    private List<String> createValues(String value) {
      return COMMA_SPLITTER.splitToList(value).stream().map(v -> v.replace(COMMA_ENCODED_VALUE, ",")).collect(Collectors.toList());
    }
  }

  private static class Property {
    private final String key;
    private String value;
    private List<String> values = new ArrayList<>();

    public Property(String key) {
      this.key = key;
    }

    public Property setValue(String value) {
      this.value = value;
      return this;
    }

    public Property setValues(List<String> values) {
      this.values = values;
      return this;
    }
  }

}
