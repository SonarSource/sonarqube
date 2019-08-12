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
package org.sonar.server.setting.ws;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class SettingValidations {
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
      checkRequest(data.component != null || definition == null || definition.global() || isGlobal(definition),
        "Setting '%s' cannot be global", data.key);
    };
  }

  private static final Set<String> SUPPORTED_QUALIFIERS = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP, Qualifiers.MODULE, Qualifiers.SUBVIEW);

  public Consumer<SettingData> qualifier() {
    return data -> {
      String qualifier = data.component == null ? "" : data.component.qualifier();
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(checkComponentScopeAndQualifier(data, definition),
        "Setting '%s' cannot be set on a %s", data.key, i18n.message(Locale.ENGLISH, "qualifier." + qualifier, null));
    };
  }

  private static boolean checkComponentScopeAndQualifier(SettingData data, @Nullable PropertyDefinition definition) {
    ComponentDto component = data.component;
    if (component == null) {
      return true;
    }
    if (!Scopes.PROJECT.equals(component.scope())) {
      return false;
    }
    if (definition == null) {
      return SUPPORTED_QUALIFIERS.contains(component.qualifier());
    }
    return definition.qualifiers().contains(component.qualifier());
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
    private final ComponentDto component;

    SettingData(String key, List<String> values, @Nullable ComponentDto component) {
      this.key = requireNonNull(key);
      this.values = requireNonNull(values);
      this.component = component;
    }
  }

  private class ValueTypeValidation implements Consumer<SettingData> {
    @Override
    public void accept(SettingData data) {
      PropertyDefinition definition = definitions.get(data.key);
      if (definition == null) {
        return;
      }

      if (definition.type() == PropertyType.METRIC) {
        validateMetric(data);
      } else if (definition.type() == PropertyType.USER_LOGIN) {
        validateLogin(data);
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
        List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, data.values).stream().filter(MetricDto::isEnabled).collect(Collectors.toList());
        checkRequest(data.values.size() == metrics.size(), "Error when validating metric setting with key '%s' and values [%s]. A value is not a valid metric key.",
          data.key, data.values.stream().collect(Collectors.joining(", ")));
      }
    }

    private void validateLogin(SettingData data) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        List<UserDto> users = dbClient.userDao().selectByLogins(dbSession, data.values).stream().filter(UserDto::isActive).collect(Collectors.toList());
        checkRequest(data.values.size() == users.size(), "Error when validating login setting with key '%s' and values [%s]. A value is not a valid login.",
          data.key, data.values.stream().collect(Collectors.joining(", ")));
      }
    }
  }
}
