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

import java.util.Optional;
import java.util.Set;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;

import static org.sonar.server.setting.ws.PropertySetExtractor.extractPropertySetKeys;

public class SettingsUpdater {

  private final DbClient dbClient;
  private final PropertyDefinitions definitions;

  public SettingsUpdater(DbClient dbClient, PropertyDefinitions definitions) {
    this.dbClient = dbClient;
    this.definitions = definitions;
  }

  public void deleteGlobalSetting(DbSession dbSession, String propertyKey) {
    delete(dbSession, propertyKey, Optional.empty());
  }

  public void deleteComponentSetting(DbSession dbSession, String propertyKey, ComponentDto componentDto) {
    delete(dbSession, propertyKey, Optional.of(componentDto));
  }

  private void delete(DbSession dbSession, String propertyKey, Optional<ComponentDto> componentDto) {
    PropertyDefinition definition = definitions.get(propertyKey);
    if (definition == null || !definition.type().equals(PropertyType.PROPERTY_SET)) {
      deleteSetting(dbSession, propertyKey, componentDto);
    } else {
      deletePropertySet(dbSession, propertyKey, definition, componentDto);
    }
  }

  private void deleteSetting(DbSession dbSession, String propertyKey, Optional<ComponentDto> componentDto) {
    if (componentDto.isPresent()) {
      dbClient.propertiesDao().deleteProjectProperty(propertyKey, componentDto.get().getId(), dbSession);
    } else {
      dbClient.propertiesDao().deleteGlobalProperty(propertyKey, dbSession);
    }
  }

  private void deletePropertySet(DbSession dbSession, String propertyKey, PropertyDefinition definition, Optional<ComponentDto> componentDto) {
    Optional<PropertyDto> propertyDto = selectPropertyDto(dbSession, propertyKey, componentDto);
    if (!propertyDto.isPresent()) {
      // Setting doesn't exist, nothing to do
      return;
    }
    Set<String> propertySetKeys = extractPropertySetKeys(propertyDto.get(), definition);
    for (String key : propertySetKeys) {
      deleteSetting(dbSession, key, componentDto);
    }
    deleteSetting(dbSession, propertyKey, componentDto);
  }

  private Optional<PropertyDto> selectPropertyDto(DbSession dbSession, String propertyKey, Optional<ComponentDto> componentDto) {
    if (componentDto.isPresent()) {
      return Optional.ofNullable(dbClient.propertiesDao().selectProjectProperty(dbSession, componentDto.get().getId(), propertyKey));
    } else {
      return Optional.ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, propertyKey));
    }
  }

}
