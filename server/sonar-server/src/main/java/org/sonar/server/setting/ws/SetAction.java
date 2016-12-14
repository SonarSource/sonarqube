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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
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
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.platform.SettingsChangeNotifier;
import org.sonar.server.setting.ws.SettingValidations.SettingData;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_SET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_FIELD_VALUES;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_VALUE;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_VALUES;

public class SetAction implements SettingsWsAction {
  private static final Collector<CharSequence, ?, String> COMMA_JOINER = Collectors.joining(",");
  private static final String MSG_NO_EMPTY_VALUE = "A non empty value must be provided";

  private final PropertyDefinitions propertyDefinitions;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;
  private final SettingsUpdater settingsUpdater;
  private final SettingsChangeNotifier settingsChangeNotifier;
  private final SettingValidations validations;

  public SetAction(PropertyDefinitions propertyDefinitions, DbClient dbClient, ComponentFinder componentFinder, UserSession userSession,
    SettingsUpdater settingsUpdater, SettingsChangeNotifier settingsChangeNotifier, SettingValidations validations) {
    this.propertyDefinitions = propertyDefinitions;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
    this.settingsUpdater = settingsUpdater;
    this.settingsChangeNotifier = settingsChangeNotifier;
    this.validations = validations;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SET)
      .setDescription("Update a setting value.<br>" +
        "Either '%s' or '%s' must be provided, not both.<br> " +
        "Either '%s' or '%s' can be provided, not both.<br> " +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>",
        PARAM_VALUE, PARAM_VALUES,
        PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY)
      .setSince("6.1")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setDescription("Setting key")
      .setExampleValue("sonar.links.scm")
      .setRequired(true);

    action.createParam(PARAM_VALUE)
      .setDescription("Setting value. To reset a value, please use the reset web service.")
      .setExampleValue("git@github.com:SonarSource/sonarqube.git");

    action.createParam(PARAM_VALUES)
      .setDescription("Setting multi value. To set several values, the parameter must be called once for each value.")
      .setExampleValue("values=firstValue&values=secondValue&values=thirdValue");

    action.createParam(PARAM_FIELD_VALUES)
      .setDescription("Setting field values. To set several values, the parameter must be called once for each value.")
      .setExampleValue(PARAM_FIELD_VALUES + "={\"firstField\":\"first value\", \"secondField\":\"second value\", \"thirdField\":\"third value\"}");

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
      doHandle(dbSession, toWsRequest(request));
    } finally {
      dbClient.closeSession(dbSession);
    }

    response.noContent();
  }

  private void doHandle(DbSession dbSession, SetRequest request) {
    Optional<ComponentDto> component = searchComponent(dbSession, request);
    checkPermissions(component);

    PropertyDefinition definition = propertyDefinitions.get(request.getKey());

    String value;

    commonChecks(request, component);

    if (!request.getFieldValues().isEmpty()) {
      value = doHandlePropertySet(dbSession, request, definition, component);
    } else {
      validate(request);
      PropertyDto property = toProperty(request, component);
      value = property.getValue();
      dbClient.propertiesDao().saveProperty(dbSession, property);
    }

    dbSession.commit();

    if (!component.isPresent()) {
      settingsChangeNotifier.onGlobalPropertyChange(persistedKey(request), value);
    }
  }

  private String doHandlePropertySet(DbSession dbSession, SetRequest request, @Nullable PropertyDefinition definition, Optional<ComponentDto> component) {
    validatePropertySet(request, definition);

    int[] fieldIds = IntStream.rangeClosed(1, request.getFieldValues().size()).toArray();
    String inlinedFieldKeys = IntStream.of(fieldIds).mapToObj(String::valueOf).collect(COMMA_JOINER);
    String key = persistedKey(request);
    Long componentId = component.isPresent() ? component.get().getId() : null;

    deleteSettings(dbSession, component, key);
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(key).setValue(inlinedFieldKeys).setResourceId(componentId));

    List<String> fieldValues = request.getFieldValues();
    IntStream.of(fieldIds).boxed()
      .flatMap(i -> readOneFieldValues(fieldValues.get(i - 1), request.getKey()).entrySet().stream()
        .map(entry -> new KeyValue(key + "." + i + "." + entry.getKey(), entry.getValue())))
      .forEach(keyValue -> dbClient.propertiesDao().saveProperty(dbSession, toFieldProperty(keyValue, componentId)));

    return inlinedFieldKeys;
  }

  private void deleteSettings(DbSession dbSession, Optional<ComponentDto> component, String key) {
    if (component.isPresent()) {
      settingsUpdater.deleteComponentSettings(dbSession, component.get(), key);
    } else {
      settingsUpdater.deleteGlobalSettings(dbSession, key);
    }
  }

  private void commonChecks(SetRequest request, Optional<ComponentDto> component) {
    checkValueIsSet(request);
    SettingData settingData = new SettingData(request.getKey(), valuesFromRequest(request), component.orElse(null));
    ImmutableList.of(validations.scope(), validations.qualifier(), validations.valueType())
      .forEach(validation -> validation.accept(settingData));
  }

  private static void validatePropertySet(SetRequest request, @Nullable PropertyDefinition definition) {
    checkRequest(definition != null, "Setting '%s' is undefined", request.getKey());
    checkRequest(PropertyType.PROPERTY_SET.equals(definition.type()), "Parameter '%s' is used for setting of property set type only", PARAM_FIELD_VALUES);

    Set<String> fieldKeys = definition.fields().stream().map(PropertyFieldDefinition::key).collect(Collectors.toSet());
    ListMultimap<String, String> valuesByFieldKeys = ArrayListMultimap.create(fieldKeys.size(), request.getFieldValues().size() * fieldKeys.size());

    request.getFieldValues().stream()
      .map(oneFieldValues -> readOneFieldValues(oneFieldValues, request.getKey()))
      .peek(map -> checkRequest(map.values().stream().anyMatch(StringUtils::isNotBlank), MSG_NO_EMPTY_VALUE))
      .flatMap(map -> map.entrySet().stream())
      .peek(entry -> valuesByFieldKeys.put(entry.getKey(), entry.getValue()))
      .forEach(entry -> checkRequest(fieldKeys.contains(entry.getKey()), "Unknown field key '%s' for setting '%s'", entry.getKey(), request.getKey()));

    checkFieldType(request, definition, valuesByFieldKeys);
  }

  private void validate(SetRequest request) {
    PropertyDefinition definition = propertyDefinitions.get(request.getKey());
    if (definition == null) {
      return;
    }

    checkSingleOrMultiValue(request, definition);
  }

  private static void checkFieldType(SetRequest request, PropertyDefinition definition, ListMultimap<String, String> valuesByFieldKeys) {
    for (PropertyFieldDefinition fieldDefinition : definition.fields()) {
      for (String value : valuesByFieldKeys.get(fieldDefinition.key())) {
        PropertyDefinition.Result result = fieldDefinition.validate(value);
        checkRequest(result.isValid(),
          "Error when validating setting with key '%s'. Field '%s' has incorrect value '%s'.",
          request.getKey(), fieldDefinition.key(), value);
      }
    }
  }

  private static void checkSingleOrMultiValue(SetRequest request, PropertyDefinition definition) {
    checkRequest(definition.multiValues() ^ request.getValue() != null,
      "Parameter '%s' must be used for single value setting. Parameter '%s' must be used for multi value setting.", PARAM_VALUE, PARAM_VALUES);
  }

  private static void checkValueIsSet(SetRequest request) {
    checkRequest(
      request.getValue() != null
        ^ !request.getValues().isEmpty()
        ^ !request.getFieldValues().isEmpty(),
      "One and only one of '%s', '%s', '%s' must be provided", PARAM_VALUE, PARAM_VALUES, PARAM_FIELD_VALUES);
    checkRequest(request.getValues().stream().allMatch(StringUtils::isNotBlank), MSG_NO_EMPTY_VALUE);
    checkRequest(request.getValue() == null || StringUtils.isNotBlank(request.getValue()), MSG_NO_EMPTY_VALUE);
  }

  private static List<String> valuesFromRequest(SetRequest request) {
    return request.getValue() == null ? request.getValues() : Collections.singletonList(request.getValue());
  }

  private String persistedKey(SetRequest request) {
    PropertyDefinition definition = propertyDefinitions.get(request.getKey());
    // handles deprecated key but persist the new key
    return definition == null ? request.getKey() : definition.key();
  }

  private static String persistedValue(SetRequest request) {
    return request.getValue() == null
      ? request.getValues().stream().map(value -> value.replace(",", "%2C")).collect(COMMA_JOINER)
      : request.getValue();
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
      .setValue(request.param(PARAM_VALUE))
      .setValues(request.multiParam(PARAM_VALUES))
      .setFieldValues(request.multiParam(PARAM_FIELD_VALUES))
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setComponentKey(request.param(PARAM_COMPONENT_KEY))
      .build();
  }

  private static Map<String, String> readOneFieldValues(String json, String key) {
    Type type = new TypeToken<Map<String, String>>() {
    }.getType();
    Gson gson = GsonHelper.create();
    try {
      return (Map<String, String>) gson.fromJson(json, type);
    } catch (JsonSyntaxException e) {
      throw new BadRequestException(String.format("JSON '%s' does not respect expected format for setting '%s'. Ex: {\"field1\":\"value1\", \"field2\":\"value2\"}", json, key));
    }
  }

  private Optional<ComponentDto> searchComponent(DbSession dbSession, SetRequest request) {
    if (request.getComponentId() == null && request.getComponentKey() == null) {
      return Optional.empty();
    }

    ComponentDto project = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponentKey(), ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY);

    return Optional.of(project);
  }

  private PropertyDto toProperty(SetRequest request, Optional<ComponentDto> component) {
    String key = persistedKey(request);
    String value = persistedValue(request);

    PropertyDto property = new PropertyDto()
      .setKey(key)
      .setValue(value);

    if (component.isPresent()) {
      property.setResourceId(component.get().getId());
    }

    return property;
  }

  private static PropertyDto toFieldProperty(KeyValue keyValue, @Nullable Long componentId) {
    return new PropertyDto().setKey(keyValue.key).setValue(keyValue.value).setResourceId(componentId);
  }

  private static class KeyValue {
    private final String key;
    private final String value;

    private KeyValue(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }
}
