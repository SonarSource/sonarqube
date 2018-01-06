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

import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;

import static org.sonar.api.PropertyType.PROPERTY_SET;
import static org.sonar.server.setting.ws.PropertySetExtractor.extractPropertySetKeys;

public class SettingsFinder {

  private static final Splitter DOT_SPLITTER = Splitter.on(".").omitEmptyStrings();

  private final DbClient dbClient;
  private final PropertyDefinitions definitions;

  public SettingsFinder(DbClient dbClient, PropertyDefinitions definitions) {
    this.dbClient = dbClient;
    this.definitions = definitions;
  }

  public List<Setting> loadGlobalSettings(DbSession dbSession, Set<String> keys) {
    List<PropertyDto> properties = dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, keys);
    List<PropertyDto> propertySets = dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, getPropertySetKeys(properties));
    return properties.stream()
      .map(property -> Setting.createFromDto(property, getPropertySets(property.getKey(), propertySets, null), definitions.get(property.getKey())))
      .collect(Collectors.toList());
  }

  /**
   * Return list of settings by component uuid, sorted from project to lowest module
   */
  public Multimap<String, Setting> loadComponentSettings(DbSession dbSession, Set<String> keys, ComponentDto component) {
    List<String> componentUuids = DOT_SPLITTER.splitToList(component.moduleUuidPath());
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
    Set<Long> componentIds = componentDtos.stream().map(ComponentDto::getId).collect(Collectors.toSet());
    Map<Long, String> uuidsById = componentDtos.stream().collect(Collectors.toMap(ComponentDto::getId, ComponentDto::uuid));
    List<PropertyDto> properties = dbClient.propertiesDao().selectPropertiesByKeysAndComponentIds(dbSession, keys, componentIds);
    List<PropertyDto> propertySets = dbClient.propertiesDao().selectPropertiesByKeysAndComponentIds(dbSession, getPropertySetKeys(properties), componentIds);

    Multimap<String, Setting> settingsByUuid = TreeMultimap.create(Ordering.explicit(componentUuids), Ordering.arbitrary());
    for (PropertyDto propertyDto : properties) {
      Long componentId = propertyDto.getResourceId();
      String componentUuid = uuidsById.get(componentId);
      String propertyKey = propertyDto.getKey();
      settingsByUuid.put(componentUuid,
        Setting.createFromDto(propertyDto, getPropertySets(propertyKey, propertySets, componentId), definitions.get(propertyKey)));
    }
    return settingsByUuid;
  }

  private Set<String> getPropertySetKeys(List<PropertyDto> properties) {
    return properties.stream()
      .filter(propertyDto -> definitions.get(propertyDto.getKey()) != null)
      .filter(propertyDto -> definitions.get(propertyDto.getKey()).type().equals(PROPERTY_SET))
      .flatMap(propertyDto -> extractPropertySetKeys(propertyDto, definitions.get(propertyDto.getKey())).stream())
      .collect(Collectors.toSet());
  }

  private static List<PropertyDto> getPropertySets(String propertyKey, List<PropertyDto> propertySets, @Nullable Long componentId) {
    return propertySets.stream()
      .filter(propertyDto -> Objects.equals(propertyDto.getResourceId(), componentId))
      .filter(propertyDto -> propertyDto.getKey().startsWith(propertyKey + "."))
      .collect(Collectors.toList());
  }

}
