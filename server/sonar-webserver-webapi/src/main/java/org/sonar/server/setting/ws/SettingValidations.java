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

import com.github.erosb.jsonsKema.JsonParseException;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.github.erosb.jsonsKema.ValidationFailure;
import com.github.erosb.jsonsKema.Validator;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.UrlValidatorUtil;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
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
  private static final Set<String> SUPPORTED_QUALIFIERS = Set.of(ComponentQualifiers.PROJECT, ComponentQualifiers.VIEW, ComponentQualifiers.APP, ComponentQualifiers.SUBVIEW);
  private static final Set<PropertyType> SSRF_CHECK_PROPERTIES = Set.of(
          PropertyType.JSON, PropertyType.STRING, PropertyType.TEXT, PropertyType.PASSWORD,
          PropertyType.REGULAR_EXPRESSION, PropertyType.PROPERTY_SET, PropertyType.USER_LOGIN,
          PropertyType.FORMATTED_TEXT, PropertyType.EMAIL, PropertyType.KEY_VALUE_MAP
  );

  private final PropertyDefinitions definitions;
  private final DbClient dbClient;
  private final I18n i18n;
  private final ValueTypeValidation valueTypeValidation;

  public SettingValidations(PropertyDefinitions definitions, DbClient dbClient, I18n i18n) {
    this.definitions = definitions;
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.valueTypeValidation = new ValueTypeValidation();
  }

  public void validateScope(SettingData data) {
    PropertyDefinition definition = definitions.get(data.key);
    checkRequest(data.entity != null || definition == null || definition.global() || isGlobal(definition),
      "Setting '%s' cannot be global", data.key);
  }

  public void validateQualifier(SettingData data) {
    String qualifier = data.entity == null ? "" : data.entity.getQualifier();
    PropertyDefinition definition = definitions.get(data.key);
    checkRequest(checkComponentQualifier(data, definition),
      "Setting '%s' cannot be set on a %s", data.key, i18n.message(Locale.ENGLISH, "qualifier." + qualifier, null));
  }

  public void validateValueType(SettingData data) {
    valueTypeValidation.validateValueType(data);
  }

  public void validateSSRF(SettingData data) {
    valueTypeValidation.validateSSRF(data);
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

  private static boolean isGlobal(PropertyDefinition definition) {
    return !definition.global() && definition.qualifiers().isEmpty();
  }

  public static class SettingData {
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

  private class ValueTypeValidation {

    private final Validator schemaValidator;

    public ValueTypeValidation() {
      this.schemaValidator = Optional.ofNullable(this.getClass().getClassLoader().getResourceAsStream("json-schemas/security.json"))
        .map(schemaStream -> {
          try {
            return IOUtils.toString(schemaStream, StandardCharsets.UTF_8);
          } catch (IOException e) {
            return null;
          }
        }).map(schemaString -> new JsonParser(schemaString).parse())
        .map(schemaJson -> new SchemaLoader(schemaJson).load())
        .map(Validator::forSchema)
        .orElseThrow(() -> new IllegalStateException("Unable to create security schema validator"));
    }

    public void validateValueType(SettingData data) {
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

    public void validateSSRF(SettingData data) {
      PropertyDefinition definition = definitions.get(data.key);
      if (definition == null) {
        return;
      }

      if (data.values != null && SSRF_CHECK_PROPERTIES.contains(definition.type())) {
        for (String value : data.values) {
          if (!UrlValidatorUtil.textContainsValidUrl(value)) {
            throw BadRequestException.create(format("Error when validating setting with key '%s' and value [%s]",
                    data.key, String.join(", ", data.values)));
          }
        }
      }
    }

    private void validateOtherTypes(SettingData data, PropertyDefinition definition) {
      data.values.stream()
        .map(definition::validate)
        .filter(result -> !result.isValid())
        .findAny()
        .ifPresent(result -> {
          throw BadRequestException.create(i18n.message(Locale.ENGLISH, "property.error." + result.getErrorKey(),
            format("Error when validating setting with key '%s' and value [%s]", data.key, String.join(", ", data.values))));
        });
    }

    private void validateLogin(SettingData data) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        List<UserDto> users = dbClient.userDao().selectByLogins(dbSession, data.values).stream().filter(UserDto::isActive).toList();
        checkRequest(data.values.size() == users.size(), "Error when validating login setting with key '%s' and values [%s]. A value is not a valid login.",
          data.key, String.join(", ", data.values));
      }
    }

    private void validateJson(SettingData data, PropertyDefinition definition) {
      Optional<String> jsonContent = data.values.stream().findFirst();
      if (jsonContent.isPresent()) {
        try {
          new Gson().getAdapter(JsonElement.class).fromJson(jsonContent.get());
          validateJsonSchema(jsonContent.get(), definition);
        } catch (JsonParseException | IOException e) {
          throw new IllegalArgumentException("Provided JSON is invalid");
        }
      }
    }

    private void validateJsonSchema(String json, PropertyDefinition definition) {
      if (SECURITY_JSON_PROPERTIES.contains(definition.key())) {
        JsonValue jsonToValidate = new JsonParser(json).parse();
        Optional.ofNullable(schemaValidator.validate(jsonToValidate))
          .ifPresent(validationFailure -> {
            ValidationFailure rootCause = getRootCause(validationFailure);
            throw new IllegalArgumentException(String.format("Provided JSON is invalid : [%s at %s]", rootCause.getMessage(), rootCause.getInstance().getLocation()));
          });
      }
    }

    private static ValidationFailure getRootCause(ValidationFailure base) {
      return base.getCauses().stream()
        .map(ValueTypeValidation::getRootCause)
        .findFirst()
        .orElse(base);
    }
  }
}
