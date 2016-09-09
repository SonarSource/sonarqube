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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.BadRequestException;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class SettingValidations {
  private final PropertyDefinitions definitions;
  private final DbClient dbClient;
  private final I18n i18n;

  public SettingValidations(PropertyDefinitions definitions, DbClient dbClient, I18n i18n) {
    this.definitions = definitions;
    this.dbClient = dbClient;
    this.i18n = i18n;
  }

  public SettingValidation scope() {
    return data -> {
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(data.component != null || definition == null || definition.global() || isGlobal(definition),
        "Setting '%s' cannot be global", data.key);
    };
  }

  public SettingValidation qualifier() {
    return data -> {
      String qualifier = data.component == null ? "" : data.component.qualifier();
      PropertyDefinition definition = definitions.get(data.key);
      checkRequest(data.component == null || definition == null || definition.qualifiers().contains(data.component.qualifier()),
        "Setting '%s' cannot be set on a %s", data.key, i18n.message(Locale.ENGLISH, "qualifier." + qualifier, null));
    };
  }

  public SettingValidation valueType() {
    return new ValueTypeValidation();
  }

  private static boolean isGlobal(PropertyDefinition definition) {
    return !definition.global() && definition.qualifiers().isEmpty();
  }

  @FunctionalInterface
  public interface SettingValidation {
    void validate(SettingData data);
  }

  public static class SettingData {
    private final String key;
    private final List<String> values;
    @CheckForNull
    private final ComponentDto component;

    public SettingData(String key, List<String> values, @Nullable ComponentDto component) {
      this.key = requireNonNull(key);
      this.values = requireNonNull(values);
      this.component = component;
    }
  }

  private class ValueTypeValidation implements SettingValidation {

    @Override
    public void validate(SettingData data) {
      PropertyDefinition definition = definitions.get(data.key);
      if (definition == null) {
        return;
      }

      if (definition.type() == PropertyType.METRIC) {
        metric(data);
      } else {
        otherTypes(data, definition);
      }
    }

    private void otherTypes(SettingData data, PropertyDefinition definition) {
      data.values.stream()
        .map(definition::validate)
        .filter(result -> !result.isValid())
        .findAny()
        .ifPresent(result -> {
          throw new BadRequestException(i18n.message(Locale.ENGLISH, "property.error." + result.getErrorKey(),
            format("Error when validating setting with key '%s' and value [%s]", data.key, data.values.stream().collect(Collectors.joining(", ")))));
        });
    }

    private void metric(SettingData data) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        List<MetricDto> metrics = dbClient.metricDao().selectByKeys(dbSession, data.values);
        checkRequest(data.values.size() == metrics.size(), "Error when validating metric setting with key '%s' and values [%s]. A value is not a valid metric key.",
          data.key, data.values.stream().collect(Collectors.joining(", ")));
      }
    }
  }
}
