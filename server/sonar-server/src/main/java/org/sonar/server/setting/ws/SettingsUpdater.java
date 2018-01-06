/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.sonar.server.setting.ws.PropertySetExtractor.extractPropertySetKeys;

public class SettingsUpdater {

  private final DbClient dbClient;
  private final PropertyDefinitions definitions;

  public SettingsUpdater(DbClient dbClient, PropertyDefinitions definitions) {
    this.dbClient = dbClient;
    this.definitions = definitions;
  }

  public void deleteGlobalSettings(DbSession dbSession, String... settingKeys) {
    deleteGlobalSettings(dbSession, asList(settingKeys));
  }

  public void deleteGlobalSettings(DbSession dbSession, List<String> settingKeys) {
    checkArgument(!settingKeys.isEmpty(), "At least one setting key is required");
    settingKeys.forEach(key -> delete(dbSession, key, Optional.empty()));
  }

  public void deleteComponentSettings(DbSession dbSession, ComponentDto componentDto, String... settingKeys) {
    deleteComponentSettings(dbSession, componentDto, asList(settingKeys));
  }

  public void deleteComponentSettings(DbSession dbSession, ComponentDto componentDto, List<String> settingKeys) {
    checkArgument(!settingKeys.isEmpty(), "At least one setting key is required");
    for (String propertyKey : settingKeys) {
      delete(dbSession, propertyKey, Optional.of(componentDto));
    }
  }

  private void delete(DbSession dbSession, String settingKey, Optional<ComponentDto> componentDto) {
    PropertyDefinition definition = definitions.get(settingKey);
    if (definition == null || !definition.type().equals(PropertyType.PROPERTY_SET)) {
      deleteSetting(dbSession, settingKey, componentDto);
    } else {
      deletePropertySet(dbSession, settingKey, definition, componentDto);
    }
  }

  private void deleteSetting(DbSession dbSession, String settingKey, Optional<ComponentDto> componentDto) {
    if (componentDto.isPresent()) {
      dbClient.propertiesDao().deleteProjectProperty(settingKey, componentDto.get().getId(), dbSession);
    } else {
      dbClient.propertiesDao().deleteGlobalProperty(settingKey, dbSession);
    }
  }

  private void deletePropertySet(DbSession dbSession, String settingKey, PropertyDefinition definition, Optional<ComponentDto> componentDto) {
    Optional<PropertyDto> propertyDto = selectPropertyDto(dbSession, settingKey, componentDto);
    if (!propertyDto.isPresent()) {
      // Setting doesn't exist, nothing to do
      return;
    }
    Set<String> settingSetKeys = extractPropertySetKeys(propertyDto.get(), definition);
    for (String key : settingSetKeys) {
      deleteSetting(dbSession, key, componentDto);
    }
    deleteSetting(dbSession, settingKey, componentDto);
  }

  private Optional<PropertyDto> selectPropertyDto(DbSession dbSession, String settingKey, Optional<ComponentDto> componentDto) {
    if (componentDto.isPresent()) {
      return Optional.ofNullable(dbClient.propertiesDao().selectProjectProperty(dbSession, componentDto.get().getId(), settingKey));
    } else {
      return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, settingKey));
    }
  }

}
