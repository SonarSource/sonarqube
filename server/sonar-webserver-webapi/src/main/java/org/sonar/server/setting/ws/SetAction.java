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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserTokenDto;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.setting.SettingsChangeNotifier;
import org.sonar.server.setting.ws.SettingValidations.SettingData;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.user.TokenUserSession;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.auth.github.GitHubSettings.GITHUB_API_URL;
import static org.sonar.auth.github.GitHubSettings.GITHUB_WEB_URL;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_FIELD_VALUES;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_KEY;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_VALUE;
import static org.sonar.server.setting.ws.SettingsWsParameters.PARAM_VALUES;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SetAction implements SettingsWsAction {
  private static final Collector<CharSequence, ?, String> COMMA_JOINER = Collectors.joining(",");
  private static final String MSG_NO_EMPTY_VALUE = "A non empty value must be provided";
  private static final int VALUE_MAXIMUM_LENGTH = 4000;
  private static final TypeToken<Map<String, String>> MAP_TYPE_TOKEN = new TypeToken<>() {};
  private static final Set<String> FORBIDDEN_KEYS = Set.of(GITLAB_AUTH_URL, GITHUB_API_URL, GITHUB_WEB_URL);


  private final PropertyDefinitions propertyDefinitions;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final SettingsUpdater settingsUpdater;
  private final SettingsChangeNotifier settingsChangeNotifier;
  private final SettingValidations validations;

  public SetAction(PropertyDefinitions propertyDefinitions, DbClient dbClient, UserSession userSession,
    SettingsUpdater settingsUpdater, SettingsChangeNotifier settingsChangeNotifier, SettingValidations validations) {
    this.propertyDefinitions = propertyDefinitions;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.settingsUpdater = settingsUpdater;
    this.settingsChangeNotifier = settingsChangeNotifier;
    this.validations = validations;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("set")
      .setDescription("Update a setting value.<br>" +
          "Either '%s' or '%s' must be provided.<br> " +
          "The settings defined in conf/sonar.properties are read-only and can't be changed.<br/>" +
          "Requires one of the following permissions: " +
          "<ul>" +
          "<li>'Administer System'</li>" +
          "<li>'Administer' rights on the specified component</li>" +
          "</ul>",
        PARAM_VALUE, PARAM_VALUES)
      .setSince("6.1")
      .setChangelog(
        new Change("10.1", "Param 'component' now only accept keys for projects, applications, portfolios or subportfolios"),
        new Change("10.1", format("The use of module keys in parameter '%s' is removed", PARAM_COMPONENT)),
        new Change("8.8", "Deprecated parameter 'componentKey' has been removed"),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)),
        new Change("7.1", "The settings defined in conf/sonar.properties are read-only and can't be changed"))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setDescription("Setting key")
      .setExampleValue("sonar.core.serverBaseURL")
      .setRequired(true);

    action.createParam(PARAM_VALUE)
      .setMaximumLength(VALUE_MAXIMUM_LENGTH)
      .setDescription("Setting value. To reset a value, please use the reset web service.")
      .setExampleValue("http://my-sonarqube-instance.com");

    action.createParam(PARAM_VALUES)
      .setDescription("Setting multi value. To set several values, the parameter must be called once for each value.")
      .setExampleValue("values=firstValue&values=secondValue&values=thirdValue");

    action.createParam(PARAM_FIELD_VALUES)
      .setDescription("Setting field values. To set several values, the parameter must be called once for each value.")
      .setExampleValue(PARAM_FIELD_VALUES + "={\"firstField\":\"first value\", \"secondField\":\"second value\", \"thirdField\":\"third value\"}");

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key. Only keys for projects, applications, portfolios or subportfolios are accepted.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SetRequest wsRequest = toWsRequest(request);
      throwIfForbiddenKey(wsRequest.getKey());
      SettingsWsSupport.validateKey(wsRequest.getKey());
      doHandle(dbSession, wsRequest);
    }
    response.noContent();
  }

  private static void throwIfForbiddenKey(String key) {
    if (FORBIDDEN_KEYS.contains(key)) {
      throw new IllegalArgumentException(format("For security reasons, the key '%s' cannot be updated using this webservice. Please use the API v2", key));
    }
  }

  private void doHandle(DbSession dbSession, SetRequest request) {
    Optional<EntityDto> component = searchEntity(dbSession, request);
    String projectKey = component.map(EntityDto::getKey).orElse(null);
    String projectName = component.map(EntityDto::getName).orElse(null);
    String qualifier = component.map(EntityDto::getQualifier).orElse(null);
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
      dbClient.propertiesDao().saveProperty(dbSession, property, null, projectKey, projectName, qualifier);
    }

    dbSession.commit();

    if (!component.isPresent()) {
      settingsChangeNotifier.onGlobalPropertyChange(persistedKey(request), value);
    }
  }

  private String doHandlePropertySet(DbSession dbSession, SetRequest request, @Nullable PropertyDefinition definition, Optional<EntityDto> component) {
    validatePropertySet(request, definition);

    int[] fieldIds = IntStream.rangeClosed(1, request.getFieldValues().size()).toArray();
    String inlinedFieldKeys = IntStream.of(fieldIds).mapToObj(String::valueOf).collect(COMMA_JOINER);
    String key = persistedKey(request);
    String componentUuid = component.isPresent() ? component.get().getUuid() : null;
    String componentKey = component.isPresent() ? component.get().getKey() : null;
    String componentName = component.isPresent() ? component.get().getName() : null;
    String qualifier = component.isPresent() ? component.get().getQualifier() : null;

    deleteSettings(dbSession, component, key);
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(key).setValue(inlinedFieldKeys)
      .setEntityUuid(componentUuid), null, componentKey, componentName, qualifier);

    List<String> fieldValues = request.getFieldValues();
    IntStream.of(fieldIds).boxed()
      .flatMap(i -> readOneFieldValues(fieldValues.get(i - 1), request.getKey()).entrySet().stream()
        .map(entry -> new KeyValue(key + "." + i + "." + entry.getKey(), entry.getValue())))
      .forEach(keyValue -> dbClient.propertiesDao().saveProperty(dbSession, toFieldProperty(keyValue, componentUuid),
        null, componentKey, componentName, qualifier));

    return inlinedFieldKeys;
  }

  private void deleteSettings(DbSession dbSession, Optional<EntityDto> component, String key) {
    if (component.isPresent()) {
      settingsUpdater.deleteComponentSettings(dbSession, component.get(), key);
    } else {
      settingsUpdater.deleteGlobalSettings(dbSession, key);
    }
  }

  private void commonChecks(SetRequest request, Optional<EntityDto> entity) {
    checkValueIsSet(request);
    String settingKey = request.getKey();
    SettingData settingData = new SettingData(settingKey, valuesFromRequest(request), entity.orElse(null));
    validations.validateScope(settingData);
    validations.validateQualifier(settingData);
    validations.validateValueType(settingData);
  }

  private static void validatePropertySet(SetRequest request, @Nullable PropertyDefinition definition) {
    checkRequest(definition != null, "Setting '%s' is undefined", request.getKey());
    checkRequest(PropertyType.PROPERTY_SET.equals(definition.type()), "Parameter '%s' is used for setting of property set type only", PARAM_FIELD_VALUES);

    Set<String> fieldKeys = definition.fields().stream().map(PropertyFieldDefinition::key).collect(Collectors.toSet());
    ListMultimap<String, String> valuesByFieldKeys = ArrayListMultimap.create(fieldKeys.size(), request.getFieldValues().size() * fieldKeys.size());

    List<Map<String, String>> maps = request.getFieldValues().stream()
      .map(oneFieldValues -> readOneFieldValues(oneFieldValues, request.getKey()))
      .toList();

    for (Map<String, String> map : maps) {
      checkRequest(map.values().stream().anyMatch(StringUtils::isNotBlank), MSG_NO_EMPTY_VALUE);
    }
    List<Map.Entry<String, String>> entries = maps.stream().flatMap(map -> map.entrySet().stream()).toList();
    entries.forEach(entry -> valuesByFieldKeys.put(entry.getKey(), entry.getValue()));
    entries.forEach(entry -> checkRequest(fieldKeys.contains(entry.getKey()), "Unknown field key '%s' for setting '%s'", entry.getKey(), request.getKey()));
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
      "Either '%s', '%s' or '%s' must be provided", PARAM_VALUE, PARAM_VALUES, PARAM_FIELD_VALUES);
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

  private void checkPermissions(Optional<EntityDto> entity) {
    if (entity.isPresent()) {
      if (userSession instanceof ThreadLocalUserSession) {
        UserSession tokenUserSession = ((ThreadLocalUserSession) userSession).get();
        if (tokenUserSession instanceof TokenUserSession) {
          UserTokenDto userToken = ((TokenUserSession) tokenUserSession).getUserToken();
          if (TokenType.PROJECT_ANALYSIS_TOKEN.name().equals(userToken.getType())) {
            if (userToken.getProjectKey().equals(entity.get().getKey())) {
              return;
            }
          }
        }
      }
      userSession.checkEntityPermission(UserRole.ADMIN, entity.get());
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private static SetRequest toWsRequest(Request request) {
    SetRequest set = new SetRequest()
      .setKey(request.mandatoryParam(PARAM_KEY))
      .setValue(request.param(PARAM_VALUE))
      .setValues(request.multiParam(PARAM_VALUES))
      .setFieldValues(request.multiParam(PARAM_FIELD_VALUES))
      .setEntity(request.param(PARAM_COMPONENT));
    checkArgument(set.getValues() != null, "Setting values must not be null");
    checkArgument(set.getFieldValues() != null, "Setting fields values must not be null");
    return set;
  }

  private static Map<String, String> readOneFieldValues(String json, String key) {
    Gson gson = GsonHelper.create();
    try {
      return gson.fromJson(json, MAP_TYPE_TOKEN);
    } catch (JsonSyntaxException e) {
      throw BadRequestException.create(format("JSON '%s' does not respect expected format for setting '%s'. Ex: {\"field1\":\"value1\", \"field2\":\"value2\"}", json, key));
    }
  }

  private Optional<EntityDto> searchEntity(DbSession dbSession, SetRequest request) {
    String entityKey = request.getEntity();
    if (entityKey == null) {
      return Optional.empty();
    }
    return Optional.of(dbClient.entityDao().selectByKey(dbSession, entityKey)
      .orElseThrow(() -> new NotFoundException(format("Component key '%s' not found", entityKey))));
  }

  private PropertyDto toProperty(SetRequest request, Optional<EntityDto> entity) {
    String key = persistedKey(request);
    String value = persistedValue(request);

    PropertyDto property = new PropertyDto()
      .setKey(key)
      .setValue(value);

    if (entity.isPresent()) {
      property.setEntityUuid(entity.get().getUuid());
    }

    return property;
  }

  private static PropertyDto toFieldProperty(KeyValue keyValue, @Nullable String componentUuid) {
    return new PropertyDto().setKey(keyValue.key).setValue(keyValue.value).setEntityUuid(componentUuid);
  }

  private static class KeyValue {
    private final String key;
    private final String value;

    private KeyValue(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  private static class SetRequest {

    private String entity;
    private List<String> fieldValues;
    private String key;
    private String value;
    private List<String> values;

    public SetRequest setEntity(@Nullable String entity) {
      this.entity = entity;
      return this;
    }

    @CheckForNull
    public String getEntity() {
      return entity;
    }

    public SetRequest setFieldValues(List<String> fieldValues) {
      this.fieldValues = fieldValues;
      return this;
    }

    public List<String> getFieldValues() {
      return fieldValues;
    }

    public SetRequest setKey(String key) {
      this.key = key;
      return this;
    }

    public String getKey() {
      return key;
    }

    public SetRequest setValue(@Nullable String value) {
      this.value = value;
      return this;
    }

    @CheckForNull
    public String getValue() {
      return value;
    }

    public SetRequest setValues(@Nullable List<String> values) {
      this.values = values;
      return this;
    }

    public List<String> getValues() {
      return values;
    }
  }
}
