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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class SettingValidations {
  private static final Set<String> SECURITY_JSON_PROPERTIES = Set.of(
    "sonar.security.config.javasecurity",
    "sonar.security.config.phpsecurity",
    "sonar.security.config.pythonsecurity",
    "sonar.security.config.roslyn.sonaranalyzer.security.cs"
  );
  private static final Set<String> SUPPORTED_QUALIFIERS = Set.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP, Qualifiers.SUBVIEW);

  private final PropertyDefinitions definitions;
  private final DbClient dbClient;
  private final I18n i18n;

  public SettingValidations(PropertyDefinitions definitions, DbClient dbClient, I18n i18n) {
    this.definitions = definitions;
    this.dbClient = dbClient;
    this.i18n = i18n;
  }

  public Consumer<SettingData> scope() {
    return data -> {
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(data.entity != null || definition == null || definition.global() || isGlobal(definition),
        "Setting '%s' cannot be global", data.key);
    };
  }

  public Consumer<SettingData> qualifier() {
    return data -> {
      String qualifier = data.entity == null ? "" : data.entity.getQualifier();
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(checkComponentQualifier(data, definition),
        "Setting '%s' cannot be set on a %s", data.key, i18n.message(Locale.ENGLISH, "qualifier." + qualifier, null));
    };
  }

  private static boolean checkComponentQualifier(SettingData data, @Nullable PropertyDefinition definition) {
    EntityDto entity = data.entity;
    if (entity == null) {
      return true;
    }
    if (definition == null) {
      return SUPPORTED_QUALIFIERS.contains(entity.getQualifier());
    }
    return definition.qualifiers().contains(entity.getQualifier());
  }

  public Consumer<SettingData> valueType() {
    return new ValueTypeValidation();
  }

  private static boolean isGlobal(PropertyDefinition definition) {
    return !definition.global() && definition.qualifiers().isEmpty();
  }

  static class SettingData {
    private final String key;
    private final List<String> values;
    @CheckForNull
    private final EntityDto entity;

    SettingData(String key, List<String> values, @Nullable EntityDto entity) {
      this.key = requireNonNull(key);
      this.values = requireNonNull(values);
      this.entity = entity;
    }
  }

  private class ValueTypeValidation implements Consumer<SettingData> {

    @Override
    public void accept(SettingData data) {
      PropertyDefinition definition = definitions.get(data.key);
      if (definition == null) {
        return;
      }

      if (definition.type() == PropertyType.USER_LOGIN) {
        validateLogin(data);
      } else if (definition.type() == PropertyType.JSON) {
        validateJson(data, definition);
      } else {
        validateOtherTypes(data, definition);
      }
    }

    private void validateOtherTypes(SettingData data, PropertyDefinition definition) {
      data.values.stream()
        .map(definition::validate)
        .filter(result -> !result.isValid())
        .findAny()
        .ifPresent(result -> {
          throw BadRequestException.create(i18n.message(Locale.ENGLISH, "property.error." + result.getErrorKey(),
            format("Error when validating setting with key '%s' and value [%s]", data.key, data.values.stream().collect(Collectors.joining(", ")))));
        });
    }

    private void validateMetric(SettingData data) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, data.values).stream().filter(MetricDto::isEnabled).toList();
        checkRequest(data.values.size() == metrics.size(), "Error when validating metric setting with key '%s' and values [%s]. A value is not a valid metric key.",
          data.key, data.values.stream().collect(Collectors.joining(", ")));
      }
    }

    private void validateLogin(SettingData data) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        List<UserDto> users = dbClient.userDao().selectByLogins(dbSession, data.values).stream().filter(UserDto::isActive).toList();
        checkRequest(data.values.size() == users.size(), "Error when validating login setting with key '%s' and values [%s]. A value is not a valid login.",
          data.key, data.values.stream().collect(Collectors.joining(", ")));
      }
    }

    private void validateJson(SettingData data, PropertyDefinition definition) {
      Optional<String> jsonContent = data.values.stream().findFirst();
      if (jsonContent.isPresent()) {
        try {
          new Gson().getAdapter(JsonElement.class).fromJson(jsonContent.get());
          validateJsonSchema(jsonContent.get(), definition);
        } catch (ValidationException e) {
          throw new IllegalArgumentException(String.format("Provided JSON is invalid [%s]", e.getMessage()));
        } catch (IOException e) {
          throw new IllegalArgumentException("Provided JSON is invalid");
        }
      }
    }

    private void validateJsonSchema(String json, PropertyDefinition definition) {
      if (SECURITY_JSON_PROPERTIES.contains(definition.key())) {
        InputStream jsonSchemaInputStream = this.getClass().getClassLoader().getResourceAsStream("json-schemas/security.json");
        if (jsonSchemaInputStream != null) {
          JSONObject jsonSchema = new JSONObject(new JSONTokener(jsonSchemaInputStream));
          JSONObject jsonSubject = new JSONObject(new JSONTokener(json));
          SchemaLoader.load(jsonSchema).validate(jsonSubject);
        }
      }
    }
  }
}
